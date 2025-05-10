package io.github.mal32.endergames.phases;

import io.github.mal32.endergames.EnderGames;
import io.github.mal32.endergames.kits.AbstractKit;
import java.util.List;
import java.util.Random;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.*;
import org.bukkit.block.structure.Mirror;
import org.bukkit.block.structure.StructureRotation;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.structure.Structure;
import org.bukkit.structure.StructureManager;
import org.bukkit.util.BlockVector;

public class LobbyPhase extends AbstractPhase {
  public Location playerSpawnLocation;

  public LobbyPhase(EnderGames plugin, Location spawn) {
    super(plugin, spawn);
    this.playerSpawnLocation =
        new Location(spawn.getWorld(), spawn.getX() + 0.5, spawn.getY() + 5, spawn.getZ() + 0.5);

    placeSpawnPlatform();

    World world = spawn.getWorld();

    world.setSpawnLocation(playerSpawnLocation);
    world.setGameRule(GameRule.SPAWN_RADIUS, 6);

    world.getWorldBorder().setSize(600);

    for (Player player : Bukkit.getServer().getOnlinePlayers()) {
      intiPlayer(player);
    }
  }

  private void placeSpawnPlatform() {
    StructureManager manager = Bukkit.getServer().getStructureManager();
    Structure structure = manager.loadStructure(new NamespacedKey("enga", "spawn_platform"));

    BlockVector structureSize = structure.getSize();
    int posX = (int) (spawnLocation.x() - (structureSize.getBlockX() / 2.0));
    int posZ = (int) (spawnLocation.z() - (structureSize.getBlockZ() / 2.0));
    Location location =
        new Location(spawnLocation.getWorld(), (int) posX, spawnLocation.getY(), posZ);
    structure.place(location, true, StructureRotation.NONE, Mirror.NONE, 0, 1.0f, new Random());
  }

  private void intiPlayer(Player player) {
    player.teleport(playerSpawnLocation);

    player.getInventory().clear();
    player.setGameMode(GameMode.ADVENTURE);
    player.addPotionEffect(
        new PotionEffect(
            PotionEffectType.SATURATION, PotionEffect.INFINITE_DURATION, 1, true, false));
    KitSelector kitSel = new KitSelector(plugin, this.kits);
    kitSel.giveKitSelector(player);
  }

  @Override
  public void stop() {
    super.stop();

    for (Player player : plugin.getServer().getOnlinePlayers()) {
      player.clearActivePotionEffects();
    }
  }

  @EventHandler
  public void onPlayerJoin(PlayerJoinEvent event) {
    intiPlayer(event.getPlayer());
  }

  @EventHandler
  public void onPlayerMove(PlayerMoveEvent event) {
    if (!event.hasChangedBlock()) return;

    Player player = event.getPlayer();

    if (player.getGameMode() != GameMode.ADVENTURE) return;

    if (player.getLocation().distance(spawnLocation) > 20) {
      player.teleport(playerSpawnLocation);
    }
  }

  @EventHandler
  public void onPlayerDamage(EntityDamageEvent event) {
    if (!(event.getEntity() instanceof Player)) {
      return;
    }
    if (event.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK) {
      return;
    }

    event.setCancelled(true);
  }
}

class KitSelector implements Listener {
  private final EnderGames plugin;
  private final List<AbstractKit> availablekits;

  public KitSelector(EnderGames plugin, List<AbstractKit> kits) {
    this.plugin = plugin;
    this.availablekits = kits;
    Bukkit.getPluginManager().registerEvents(this, plugin);
  }

  public void giveKitSelector(Player player) {
    ItemStack chestItem = new ItemStack(Material.CHEST); // Chest item
    ItemMeta meta = chestItem.getItemMeta();
    if (meta != null) {
      meta.displayName(Component.text("§6Select Kit"));
      chestItem.setItemMeta(meta);
    }
    player.getInventory().addItem(chestItem);
  }

  @EventHandler
  public void onPlayerInteract(PlayerInteractEvent event) {
    if (event.getItem() != null && event.getItem().getType() == Material.CHEST) {
      ItemMeta meta = event.getItem().getItemMeta();
      if (meta == null || meta.displayName() == null) {
        return;
      }
      String displayName = PlainTextComponentSerializer.plainText().serialize(meta.displayName());
      if (displayName.equals("§6Select Kit")) {
        Player player = event.getPlayer();
        openKitMenu(player);
      }
    }
  }

  public void openKitMenu(Player player) {
    player.playSound(player, Sound.BLOCK_CHEST_OPEN, 1, 1);
    KitInventory kitinv = new KitInventory(plugin, availablekits);
    player.openInventory(kitinv.getInventory());
  }
}

class KitInventory implements InventoryHolder {
  private final EnderGames plugin;
  private final List<AbstractKit> availablekits;
  private final Inventory inventory;

  public KitInventory(EnderGames plugin, List<AbstractKit> availablekits) {
    this.plugin = plugin;
    this.availablekits = availablekits;
    this.inventory = plugin.getServer().createInventory(this, 27);
    fill_chest_with_kits();
  }

  @Override
  public Inventory getInventory() {
    return this.inventory;
  }

  public void fill_chest_with_kits() {
    for (int i = 0; i < this.availablekits.size(); i++) {
      //      plugin
      //          .getLogger()
      //          .info(
      //              "Kit at index "
      //                  + i
      //                  + ": "
      //                  + this.availablekits
      //                      .get(i)
      //                      .getDescriptionItem()
      //                      .getItemMeta()
      //                      .displayName().toString();
      inventory.setItem(i, availablekits.get(i).getDescriptionItem());
    }
  }
}
