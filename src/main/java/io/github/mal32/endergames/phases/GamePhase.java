package io.github.mal32.endergames.phases;

import io.github.mal32.endergames.GameManager;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.LodestoneTracker;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.loot.LootContext;
import org.bukkit.loot.LootTable;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import net.kyori.adventure.text.Component;
import org.bukkit.scheduler.BukkitScheduler;
import io.github.mal32.endergames.kits.*;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.Random;

public class GamePhase extends AbstractPhase implements Listener {
    private List<AbstractKit> kits = List.of(new Lumberjack(plugin));

    public GamePhase(JavaPlugin plugin, GameManager manager, Location spawn) {
        super(plugin, manager, spawn);

        for (Player player : plugin.getServer().getOnlinePlayers()) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 20*60*3, 4, true));
            player.setGameMode(GameMode.SURVIVAL);

            player.give(new ItemStack(Material.COMPASS));

            String playerKit = player.getPersistentDataContainer().get(new NamespacedKey(plugin, "kit"), PersistentDataType.STRING);
            for (AbstractKit kit : kits) {
                if (Objects.equals(kit.getName(), playerKit)) {
                    kit.start(player);
                }
            }
        }

        World world = spawnLocation.getWorld();

        WorldBorder worldBorder = world.getWorldBorder();
        worldBorder.setCenter(spawnLocation);
        worldBorder.setSize(600);
        worldBorder.setSize(50, 20*60);
        worldBorder.setWarningDistance(10);
        worldBorder.setWarningTime(30);
        worldBorder.setDamageBuffer(1);

        BukkitScheduler scheduler = plugin.getServer().getScheduler();
        scheduler.runTaskLater(plugin, () -> {
            for (int x = spawn.blockX()-20; x <= spawn.blockX()+20; x++) {
                for (int z = spawn.blockZ()-20; z <= spawn.blockZ()+20; z++) {
                    for (int y = spawn.blockY()-20; y <= spawn.blockY()+20; y++) {
                        world.getBlockAt(x, y, z).setType(Material.AIR);
                    }
                }
            }
        }, 30*20);
    }

    @Override
    public void stop() {
        HandlerList.unregisterAll(this);
        for (AbstractKit kit : kits) {
            kit.stop();
        }
    }

    @EventHandler
    private void onTrackerClick(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.COMPASS) {
            return;
        }

        Location targetLocation = spawnLocation;
        Location currentLocation = player.getLocation();
        double distance = (int) currentLocation.distance(targetLocation);
        player.sendActionBar(Component.text(distance + " Blocks").style(Style.style(NamedTextColor.YELLOW)));
        item.setData(DataComponentTypes.LODESTONE_TRACKER, LodestoneTracker.lodestoneTracker().tracked(false).location(targetLocation).build());
    }

    @EventHandler
    private void onPlayerDeath(PlayerDeathEvent event) {
        event.setCancelled(true);

        Player player = event.getEntity();
        player.setGameMode(GameMode.SPECTATOR);
        player.setHealth(20);

        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null) {
                continue;
            }
            player.getWorld().dropItem(player.getLocation(), item);
        }
        player.getInventory().clear();

        for (Player p : Bukkit.getOnlinePlayers()) {
            player.playSound(p.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1, 1);
        }
    }

    @EventHandler
    private void onEnderChestInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null || event.getClickedBlock().getType() != Material.ENDER_CHEST) {
            return;
        }

        EnderChest inv = new EnderChest(plugin, event.getPlayer().getLocation());

        Bukkit.getScheduler().runTask(plugin, () -> {
            event.getPlayer().openInventory(inv.getInventory());
        });
    }
}

class EnderChest implements InventoryHolder {
    private final Inventory inventory;

    public EnderChest(JavaPlugin plugin, Location location) {
        this.inventory = plugin.getServer().createInventory(this, 27, "Ender Chest");

        LootTable lootTable = Bukkit.getLootTable(new NamespacedKey("enga", "ender_chest"));
        LootContext.Builder lootContextBuilder = new LootContext.Builder(location);
        LootContext lootContext = lootContextBuilder.build();
        lootTable.fillInventory(this.inventory, new Random(), lootContext);
    }

    @Override
    @NotNull
    public Inventory getInventory() {
        return inventory;
    }
}
