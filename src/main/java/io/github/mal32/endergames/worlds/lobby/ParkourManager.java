package io.github.mal32.endergames; // adjust package

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public class ParkourManager {
    private final JavaPlugin plugin;
    private final NamespacedKey resetKey;
    private final Map<UUID, ParkourSession> sessions = new ConcurrentHashMap<>();
    private final Map<UUID, ItemStack> savedHotbar = new ConcurrentHashMap<>();
    private final File recordsFile;
    private final YamlConfiguration recordsConfig = new YamlConfiguration();
    private final Map<UUID, Long> bestTimes = new ConcurrentHashMap<>();

    // change these to your desired coordinates/world if needed
    private final Location START_PLATE = new Location(Bukkit.getWorld("world_enga_lobby"), -6, 70, -1);
    private final Location RESET_LOCATION = new Location(Bukkit.getWorld("world_enga_lobby"), -5.5, 70, 1, 180f, 14f); // two blocks before

    private final Location FINISH_PLATE = new Location(Bukkit.getWorld("world_enga_lobby"), 15, 81, -23);

    // checkpoint locations (optional). add as many as you want.
    private final List<Location> CHECKPOINTS = List.of(
            // example: new Location(world, x,y,z)
            // new Location(Bukkit.getWorlds().get(0), 0, 70, 0)
            new Location(Bukkit.getWorld("world_enga_lobby"),-15,81,27)
    );

    // hotbar slot index (0-based). slot 1 = second hotbar slot (user requested "slot 2")
    private static final int RESET_HOTBAR_SLOT = 1;

    public ParkourManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.resetKey = new NamespacedKey(plugin, "parkour_reset");
        this.recordsFile = new File(plugin.getDataFolder(), "parkour-records.yml");
        loadRecords();
    }

    private void loadRecords() {
        try {
            if (!recordsFile.exists()) {
                plugin.getDataFolder().mkdirs();
                recordsFile.createNewFile();
            }
            YamlConfiguration cfg = YamlConfiguration.loadConfiguration(recordsFile);
            ConfigurationSection section = cfg.getConfigurationSection("bestTimes");
            Set<String> keys = section != null ? section.getKeys(false) : Collections.emptySet();

            for (String k : keys) {
                try {
                    UUID uuid = UUID.fromString(k);
                    long t = cfg.getLong("bestTimes." + k, -1);
                    if (t >= 0) bestTimes.put(uuid, t);
                } catch (IllegalArgumentException ex) {
                    plugin.getLogger().warning("Invalid UUID in parkour-records.yml: " + k);
                }
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Could not load parkour-records.yml: " + e.getMessage());
        }
    }

    private void saveRecords() {
        try {
            YamlConfiguration cfg = new YamlConfiguration();
            for (Map.Entry<UUID, Long> ent : bestTimes.entrySet()) {
                cfg.set("bestTimes." + ent.getKey().toString(), ent.getValue());
            }
            cfg.save(recordsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save parkour-records.yml: " + e.getMessage());
        }
    }

    public boolean isInParkour(Player p) {
        return sessions.containsKey(p.getUniqueId());
    }

    public void startParkour(Player p) {
        UUID id = p.getUniqueId();
        if (sessions.containsKey(id)) return; // already started
        long now = System.currentTimeMillis();
        ParkourSession s = new ParkourSession(now);
        sessions.put(id, s);

        // save current item in that hotbar slot
        ItemStack prev = p.getInventory().getItem(RESET_HOTBAR_SLOT);
        if (prev != null) savedHotbar.put(id, prev);

        // give reset item
        p.getInventory().setItem(RESET_HOTBAR_SLOT, createResetItem());

        // title + chat
        p.showTitle(Title.title(
                Component.text("Parkour challenge started", NamedTextColor.GOLD),
                Component.text("Good luck!", NamedTextColor.GRAY)
        ));

        p.sendMessage(Component.text("Parkour started! Use the reset item to go back.").color(NamedTextColor.GREEN));
        plugin.getLogger().info(p.getName() + " started parkour.");
    }

    public void resetPlayer(Player p) {
        // teleport to configured reset location (keeps them in parkour mode)
        p.teleport(RESET_LOCATION);
        p.sendMessage(Component.text("Reset to start.").color(NamedTextColor.RED));
    }

    public void checkpointReached(Player p, Location chkLocation) {
        UUID id = p.getUniqueId();
        ParkourSession s = sessions.get(id);
        if (s == null) return;
        long now = System.currentTimeMillis();

        // determine index of checkpoint
        int index = indexOfCheckpoint(chkLocation);
        if (index <= s.lastCheckpointIndex) {
            // ignore repeated triggers for same or earlier checkpoint
            return;
        }

        long delta = now - s.lastCheckpointTime;
        s.lastCheckpointTime = now;
        s.lastCheckpointIndex = index;

        String formatted = formatTime(delta);
        p.sendMessage(Component.text("Finished part " + index + " in " + formatted).color(NamedTextColor.AQUA));
    }

    private int indexOfCheckpoint(Location l) {
        for (int i = 0; i < CHECKPOINTS.size(); i++) {
            if (isSameBlock(CHECKPOINTS.get(i), l)) return i + 1;
        }
        return -1;
    }

    public void finishParkour(Player p) {
        UUID id = p.getUniqueId();
        ParkourSession s = sessions.remove(id);
        if (s == null) return;
        long now = System.currentTimeMillis();
        long elapsed = now - s.startTime;
        String formatted = formatTime(elapsed);
        p.showTitle(Title.title(
                Component.text("Parkour finished", NamedTextColor.GOLD),
                Component.text("", NamedTextColor.GRAY)
        ));
        p.sendMessage(Component.text( "You finished in " + formatted).color(NamedTextColor.GOLD));

        // save best time if better (or absent)
        Long best = bestTimes.get(id);
        if (best == null || elapsed < best) {
            bestTimes.put(id, elapsed);
            saveRecords();
            p.sendMessage(Component.text("New best time!").color(NamedTextColor.GREEN));
        } else {
            p.sendMessage(Component.text( "Best: " + formatTime(best)).color(NamedTextColor.GRAY));
        }

        // restore saved hotbar item
        restoreHotbar(id, p);
        plugin.getLogger().info(p.getName() + " finished parkour in " + elapsed + "ms");
    }

    /** abort (e.g. player quit) — restore items & remove session */
    public void abortParkour(Player p) {
        UUID id = p.getUniqueId();
        if (sessions.remove(id) != null) {
            restoreHotbar(id, p);
            p.sendMessage(Component.text("Parkour aborted.").color(NamedTextColor.RED));
        }
    }

    private void restoreHotbar(UUID id, Player p) {
        ItemStack prev = savedHotbar.remove(id);
        try {
            p.getInventory().setItem(RESET_HOTBAR_SLOT, prev);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to restore hotbar for " + id);
        }
    }

    public ItemStack createResetItem() {
        ItemStack item = new ItemStack(Material.RED_DYE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Reset", NamedTextColor.RED));
        meta.getPersistentDataContainer().set(resetKey, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    public boolean isResetItem(ItemStack stack) {
        if (stack == null) return false;
        if (!stack.hasItemMeta()) return false;
        Byte has = stack.getItemMeta().getPersistentDataContainer().get(resetKey, PersistentDataType.BYTE);
        return has != null && has == (byte) 1;
    }

    /** Called when an entity stepped on (or activated) a weighted plate block */
    public void handlePlateStepped(Player p, Location plateBlockLocation, Material plateType) {
        // Ensure block matches which plate we care about and same world
        if (isSameBlock(plateBlockLocation, START_PLATE) && plateType == Material.HEAVY_WEIGHTED_PRESSURE_PLATE) {
            // start
            startParkour(p);
            return;
        }

        if (isSameBlock(plateBlockLocation, FINISH_PLATE) && plateType == Material.LIGHT_WEIGHTED_PRESSURE_PLATE) {
            // finish
            if (isInParkour(p)) finishParkour(p);
            return;
        }

        // checkpoint (only if in parkour)
        if (isInParkour(p) && plateType == Material.HEAVY_WEIGHTED_PRESSURE_PLATE) {
            int idx = indexOfCheckpoint(plateBlockLocation);
            if (idx > 0) checkpointReached(p, plateBlockLocation);
        }
    }

    private boolean isSameBlock(Location a, Location b) {
        if (a == null || b == null) return false;
        if (a.getWorld() == null || b.getWorld() == null) return false;
        if (!a.getWorld().getName().equals(b.getWorld().getName())) return false;
        return a.getBlockX() == b.getBlockX()
                && a.getBlockY() == b.getBlockY()
                && a.getBlockZ() == b.getBlockZ();
    }

    private static String formatTime(long ms) {
        long minutes = (ms / 1000) / 60;
        long seconds = (ms / 1000) % 60;
        long millis = ms % 1000;
        return String.format("%d:%02d.%03d", minutes, seconds, millis);
    }

    public void shutdown() {
        // restore items for all active players and save records
        for (UUID id : new ArrayList<>(sessions.keySet())) {
            Player p = Bukkit.getPlayer(id);
            if (p != null && p.isOnline()) {
                restoreHotbar(id, p);
            }
        }
        sessions.clear();
        saveRecords();
    }

    private static class ParkourSession {
        final long startTime;
        long lastCheckpointTime;
        int lastCheckpointIndex = 0;

        ParkourSession(long now) {
            this.startTime = now;
            this.lastCheckpointTime = now;
        }
    }
}
