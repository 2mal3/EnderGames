package io.github.mal32.endergames.worlds.lobby;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public class ParkourManager implements Listener {
  private final JavaPlugin plugin;
  private final NamespacedKey resetKey;
  private final NamespacedKey cancelKey;
  private final Map<UUID, ParkourSession> sessions = new ConcurrentHashMap<>();
  private final Map<UUID, ItemStack> savedHotbar = new ConcurrentHashMap<>();
  private final File recordsFile;
  private final YamlConfiguration recordsConfig = new YamlConfiguration();
  private final Map<UUID, Long> bestTimes = new ConcurrentHashMap<>();
  private final World world = Bukkit.getWorld("world_enga_lobby");

  // change these to your desired coordinates/world if needed
  private final Location START_PLATE = new Location(world, -3, 70, -0);
  private final Location RESET_LOCATION =
      new Location(world, -3.5, 70, 1.5, 180f, 14f); // two blocks before

  private final Location FINISH_PLATE = new Location(world, 17.5, 81, -22.5);

  // checkpoint locations (optional). add as many as you want.
  private final List<Checkpoint> CHECKPOINTS =
      List.of(
          // example: new Location(world, x,y,z)
          // new Location(Bukkit.getWorlds().get(0), 0, 70, 0)
          new Checkpoint(new Location(world, -12.5, 81, 27.5, -90f, 0f)),
          new Checkpoint(new Location(world, 21.5, 80, 0.5, 180f, 0f)));

  private static final int RESET_HOTBAR_SLOT = 1;
  private static final int CANCEL_HOTBAR_SLOT = 2;

  public ParkourManager(JavaPlugin plugin) {
    Bukkit.getPluginManager().registerEvents(this, plugin);
    this.plugin = plugin;
    this.resetKey = new NamespacedKey(plugin, "parkour_reset");
    this.cancelKey = new NamespacedKey(plugin, "parkour_cancel");
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

  @EventHandler
  private void onPressurePlateRedstone(BlockRedstoneEvent event) {
    Material type = event.getBlock().getType();
    if (type != Material.HEAVY_WEIGHTED_PRESSURE_PLATE
        && type != Material.LIGHT_WEIGHTED_PRESSURE_PLATE) {
      return;
    }

    int oldPower = event.getOldCurrent();
    int newPower = event.getNewCurrent();

    // Trigger only when plate is activated (rising edge)
    if (!(oldPower == 0 && newPower > 0)) return;

    // The plate just got stepped on or activated
    // Find players standing on this plate (there can be multiple)
    for (Player p : event.getBlock().getWorld().getPlayers()) {
      Location playerBlockLoc = p.getLocation().getBlock().getLocation();
      Location plateBlock = event.getBlock().getLocation();
      if (playerBlockLoc.distanceSquared(plateBlock) <= 1) {
        this.handlePlateStepped(p, plateBlock, type);
      }
    }
  }

  public boolean isInParkour(Player p) {
    return sessions.containsKey(p.getUniqueId());
  }

  public void startParkour(Player p) {
    UUID id = p.getUniqueId();
    long now = System.currentTimeMillis();
    if (sessions.containsKey(id)) {
      p.sendMessage(Component.text("Time reset to 0:00").color(NamedTextColor.GRAY));
      ParkourSession s = sessions.get(id);
      s.reset(now, RESET_LOCATION);
      // already started
      return;
    }
    ParkourSession s = new ParkourSession(now, RESET_LOCATION);
    sessions.put(id, s);

    ItemStack prev = p.getInventory().getItem(RESET_HOTBAR_SLOT);
    if (prev != null) savedHotbar.put(id, prev);

    // give reset and cancel item
    p.getInventory().setItem(RESET_HOTBAR_SLOT, createResetItem());
    p.getInventory().setItem(CANCEL_HOTBAR_SLOT, createCancelItem());

    // title + chat
    p.sendActionBar(
        Component.text("Parkour challenge started!     Good luck!", NamedTextColor.GOLD)
            .decoration(TextDecoration.ITALIC, false));

    p.sendMessage(
        Component.text("Parkour started! Use the reset item to go back.")
            .color(NamedTextColor.GREEN));
    plugin.getLogger().info(p.getName() + " started parkour.");
  }

  public void resetPlayer(Player p) {
    ParkourSession s = sessions.get(p.getUniqueId());
    Location respawn =
        (s != null && s.lastCheckpointLocation != null)
            ? s.lastCheckpointLocation.clone()
            : RESET_LOCATION.clone();
    // ensure safe teleport & preserve yaw/pitch if present
    p.teleport(respawn);
    p.sendMessage(Component.text("Reset to last checkpoint.").color(NamedTextColor.RED));
  }

  public void checkpointReached(Player p, Location chkLocation) {
    UUID id = p.getUniqueId();
    ParkourSession s = sessions.get(id);
    if (s == null) return; // not in a run

    int index = indexOfCheckpoint(chkLocation); // 1-based index or -1
    if (index <= 0) return;

    // strict ordering: only accept exactly the next checkpoint
    if (index != s.lastCheckpointIndex + 1) {
      if (index < s.lastCheckpointIndex) {
        p.sendMessage(
            Component.text("You already have a newer checkpoint. Use the reset item to get there.")
                .color(NamedTextColor.GRAY));
      } else if (index > s.lastCheckpointIndex) { // jumped ahead
        p.sendMessage(Component.text("You skipped a checkpoint").color(NamedTextColor.RED));
        abortParkour(p);
      }
      return;
    }

    long now = System.currentTimeMillis();
    long delta = now - s.lastCheckpointTime;
    s.lastCheckpointTime = now;
    s.lastCheckpointIndex = index;

    // set respawn to the checkpoint's configured respawn position
    Checkpoint cp = CHECKPOINTS.get(index - 1);
    s.lastCheckpointLocation = cp.getRespawn().clone();

    p.sendMessage(
        Component.text("Finished part " + index + " in " + formatTime(delta))
            .color(NamedTextColor.AQUA));
  }

  private int indexOfCheckpoint(Location l) {
    for (int i = 0; i < CHECKPOINTS.size(); i++) {
      if (isSameBlock(CHECKPOINTS.get(i).getPlate(), l)) return i + 1;
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
    p.showTitle(
        Title.title(
            Component.text("Parkour finished", NamedTextColor.GOLD),
            Component.text("", NamedTextColor.GRAY)));
    p.sendMessage(Component.text("You finished in " + formatted).color(NamedTextColor.GOLD));

    p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);

    // save best time if better (or absent)
    Long best = bestTimes.get(id);
    if (best == null || elapsed < best) {
      bestTimes.put(id, elapsed);
      saveRecords();
      p.sendMessage(Component.text("New best time!").color(NamedTextColor.GREEN));
    } else {
      p.sendMessage(Component.text("Best: " + formatTime(best)).color(NamedTextColor.GRAY));
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
      p.getInventory().setItem(CANCEL_HOTBAR_SLOT, null);
    } catch (Exception e) {
      plugin.getLogger().warning("Failed to restore hotbar for " + id);
    }
  }

  public ItemStack createResetItem() {
    ItemStack item = new ItemStack(Material.RED_DYE);
    ItemMeta meta = item.getItemMeta();
    meta.displayName(
        Component.text("Reset", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
    meta.getPersistentDataContainer().set(resetKey, PersistentDataType.BYTE, (byte) 1);
    item.setItemMeta(meta);
    return item;
  }

  public ItemStack createCancelItem() {
    ItemStack item = new ItemStack(Material.BARRIER);
    ItemMeta meta = item.getItemMeta();
    meta.displayName(
        Component.text("Cancel Parkour", NamedTextColor.RED)
            .decoration(TextDecoration.ITALIC, false));
    meta.getPersistentDataContainer().set(cancelKey, PersistentDataType.BYTE, (byte) 1);
    item.setItemMeta(meta);
    return item;
  }

  public boolean isResetItem(ItemStack stack) {
    if (stack == null) return false;
    if (!stack.hasItemMeta()) return false;
    Byte has =
        stack.getItemMeta().getPersistentDataContainer().get(resetKey, PersistentDataType.BYTE);
    return has != null && has == (byte) 1;
  }

  public boolean isCancelItem(ItemStack stack) {
    if (stack == null) return false;
    if (!stack.hasItemMeta()) return false;
    Byte has =
        stack.getItemMeta().getPersistentDataContainer().get(cancelKey, PersistentDataType.BYTE);
    return has != null && has == (byte) 1;
  }

  /** Called when an entity stepped on (or activated) a weighted plate block */
  public void handlePlateStepped(Player p, Location plateBlockLocation, Material plateType) {
    // Ensure block matches which plate we care about and same world
    if (isSameBlock(plateBlockLocation, START_PLATE)
        && plateType == Material.HEAVY_WEIGHTED_PRESSURE_PLATE) {
      // start
      startParkour(p);
      return;
    }

    if (isSameBlock(plateBlockLocation, FINISH_PLATE)
        && plateType == Material.LIGHT_WEIGHTED_PRESSURE_PLATE) {
      // finish
      ParkourSession s = sessions.get(p.getUniqueId());
      if (s == null) {
        // not in run -> ignore or tell player
        p.sendMessage(
            Component.text("You are not currently running the parkour.")
                .color(NamedTextColor.GRAY));
        return;
      }
      if (!CHECKPOINTS.isEmpty() && s.lastCheckpointIndex != CHECKPOINTS.size()) {
        p.sendMessage(Component.text("You didn't hit all checkpoints").color(NamedTextColor.RED));
        p.sendMessage(
            Component.text(
                    "Last checkpoint reached: "
                        + s.lastCheckpointIndex
                        + " / "
                        + CHECKPOINTS.size())
                .color(NamedTextColor.GRAY));
        abortParkour(p);
        return;
      }
      finishParkour(p);
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
    // compare by distance under one block
    return a.distanceSquared(b) < 3.0;
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
}

class Checkpoint {
  private final Location plate;
  private final Location respawn;

  /** default respawn = center of plate block (plate.x + .5, plate.y, plate.z + .5) */
  Checkpoint(Location plate) {
    this.plate = plate.clone();
    this.respawn = plate.clone();
  }

  Checkpoint(Location plate, Location respawn) {
    this.plate = plate.clone();
    this.respawn = respawn.clone();
  }

  Location getPlate() {
    return plate;
  }

  Location getRespawn() {
    return respawn;
  }
}

class ParkourSession {
  long startTime;
  long lastCheckpointTime;
  int lastCheckpointIndex = 0;
  Location lastCheckpointLocation;

  ParkourSession(long now, Location initialRespawn) {
    this.startTime = now;
    this.lastCheckpointTime = now;
    this.lastCheckpointLocation = (initialRespawn == null) ? null : initialRespawn.clone();
  }

  void reset(long now, Location initialRespawn) {
    this.startTime = now;
    this.lastCheckpointTime = now;
    this.lastCheckpointIndex = 0;
    this.lastCheckpointLocation = (initialRespawn == null) ? null : initialRespawn.clone();
  }
}
