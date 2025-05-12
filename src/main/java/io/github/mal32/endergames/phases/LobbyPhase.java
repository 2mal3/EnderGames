package io.github.mal32.endergames.phases;

import static org.apache.commons.lang3.StringUtils.capitalize;

import io.github.mal32.endergames.EnderGames;
import io.github.mal32.endergames.kits.AbstractKit;
import java.util.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.*;
import org.bukkit.block.structure.Mirror;
import org.bukkit.block.structure.StructureRotation;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.structure.Structure;
import org.bukkit.structure.StructureManager;
import org.bukkit.util.BlockVector;

public class LobbyPhase extends AbstractPhase {
  public Location playerSpawnLocation;
  KitSelector kitSel = new KitSelector(plugin, this.kits);

  public LobbyPhase(EnderGames plugin, Location spawn) {
    super(plugin, spawn);
    this.playerSpawnLocation =
        new Location(spawn.getWorld(), spawn.getX() + 0.5, spawn.getY() + 5, spawn.getZ() + 0.5);

    placeSpawnPlatform();

    World world = spawn.getWorld();

    world.setSpawnLocation(playerSpawnLocation);
    world.setGameRule(GameRule.SPAWN_RADIUS, 6);

    world.getWorldBorder().setSize(600);
    teleportToPlayerSpawns();

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

  private void teleportToPlayerSpawns() {
    List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
    int totalPlayers = players.size();
    totalPlayers = 24;
    if (totalPlayers == 0) return;

    List<BlockVector> offsets = makeSpawnOffsets();
    plugin
        .getLogger()
        .info(
            "SpawnLocation: "
                + spawnLocation.x()
                + " "
                + spawnLocation.y()
                + " "
                + spawnLocation.z());
    for (int i = 0; i < offsets.size(); i++) {
      plugin.getLogger().info("offset[" + i + "] = " + offsets.get(i));
    }

    int centerX = spawnLocation.getBlockX();
    int centerY = spawnLocation.getBlockY() + 1;
    int centerZ = spawnLocation.getBlockZ();

    for (int i = 0; i < offsets.size(); i++) {
      BlockVector off = offsets.get(i);
      int bx = centerX + off.getBlockX();
      int by = centerY + off.getBlockY(); // always 1
      int bz = centerZ + off.getBlockZ();
      spawnLocation.getWorld().getBlockAt(bx, by, bz).setType(Material.GREEN_CONCRETE);
    }
  }

  public List<BlockVector> makeSpawnOffsets() {
    // not correct yet
    return List.of(
        new BlockVector(0, 0, -9),
        new BlockVector(2, 0, -9),
        new BlockVector(4, 0, -8),
        new BlockVector(6, 0, -6),
        new BlockVector(8, 0, -4),
        new BlockVector(9, 0, -1),
        new BlockVector(9, 0, 0),
        new BlockVector(9, 0, 2),
        new BlockVector(8, 0, 4),
        new BlockVector(6, 0, 6),
        new BlockVector(4, 0, 8),
        new BlockVector(2, 0, 9),
        new BlockVector(0, 0, 9),
        new BlockVector(-2, 0, 9),
        new BlockVector(-4, 0, 8),
        new BlockVector(-6, 0, 6),
        new BlockVector(-8, 0, 4),
        new BlockVector(-9, 0, 2),
        new BlockVector(-9, 0, 0),
        new BlockVector(-9, 0, -1),
        new BlockVector(-8, 0, -3),
        new BlockVector(-6, 0, -5),
        new BlockVector(-4, 0, -7),
        new BlockVector(-2, 0, -8));
  }

  private void intiPlayer(Player player) {
    // player.teleport(playerSpawnLocation);

    player.getInventory().clear();
    player.setGameMode(GameMode.ADVENTURE);
    player.addPotionEffect(
        new PotionEffect(
            PotionEffectType.SATURATION, PotionEffect.INFINITE_DURATION, 1, true, false));
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

  @EventHandler
  public void onInventoryClick(InventoryClickEvent event) {
    Inventory inventory = event.getClickedInventory();
    if (inventory == null || !(inventory.getHolder(false) instanceof KitInventory kitInv)) {
      return;
    }
    event.setCancelled(true);

    ItemStack clicked = event.getCurrentItem();
    if (clicked == null || !clicked.hasItemMeta() || !clicked.getItemMeta().hasDisplayName()) {
      return;
    }

    Player player = (Player) event.getWhoClicked();
    Component displayNameComponent = clicked.getItemMeta().displayName();

    if (displayNameComponent == null) {
      return;
    }

    // Convert Component to plain string and strip formatting
    String displayName = LegacyComponentSerializer.legacySection().serialize(displayNameComponent);
    String kitName = displayName.length() > 2 ? displayName.substring(2).toLowerCase() : "";

    // Optional: check if the kit is valid (from your list)
    AbstractKit matchedKit =
        availablekits.stream()
            .filter(kit -> kit.getName().equalsIgnoreCase(kitName))
            .findFirst()
            .orElse(null);

    if (matchedKit == null) {
      player.sendMessage("Invalid kit: " + kitName);
      return;
    }
    // Feedback + sound
    player.sendMessage(
        Component.text("You selected the ")
            .append(Component.text(capitalize(kitName)).color(NamedTextColor.GOLD))
            .append(Component.text(" kit")));
    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1, 1);

    // Store kit in PersistentDataContainer
    NamespacedKey key = new NamespacedKey(plugin, "kit");
    player.getPersistentDataContainer().set(key, PersistentDataType.STRING, kitName);

    for (int i = 0; i < inventory.getSize(); i++) {
      ItemStack item = inventory.getItem(i);
      if (item == null || !item.hasItemMeta()) continue;

      ItemMeta meta = item.getItemMeta();

      // Remove enchantments and glowing effect
      meta.getEnchants().forEach((enchant, level) -> meta.removeEnchant(enchant));
      meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

      item.setItemMeta(meta);
      inventory.setItem(i, item);
    }

    // Apply enchantment to the selected item
    ItemStack clickedItem = event.getCurrentItem();
    if (clickedItem != null && clickedItem.getType() != Material.AIR) {
      ItemMeta clickedMeta = clickedItem.getItemMeta();
      clickedMeta.addEnchant(Enchantment.INFINITY, 1, true); // dummy enchantment for glow
      clickedMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
      clickedItem.setItemMeta(clickedMeta);
    }
  }

  public void openKitMenu(Player player) {
    player.playSound(player, Sound.BLOCK_CHEST_OPEN, 1, 1);
    KitInventory kitinv = new KitInventory(plugin, availablekits, player);
    player.openInventory(kitinv.getInventory());
  }
}

class KitInventory implements InventoryHolder {
  private final EnderGames plugin;
  private final List<AbstractKit> availablekits;
  private final Inventory inventory;
  private final Player player;

  public KitInventory(EnderGames plugin, List<AbstractKit> availablekits, Player player) {
    this.plugin = plugin;
    this.availablekits = availablekits;
    this.player = player;
    this.inventory = plugin.getServer().createInventory(this, 27, Component.text("§0Select Kit"));
    fill_chest_with_kits();
  }

  @Override
  public Inventory getInventory() {
    return this.inventory;
  }

  public void fill_chest_with_kits() {
    for (int i = 0; i < this.availablekits.size(); i++) {
      ItemStack descItem = availablekits.get(i).getDescriptionItem();
      Component displayNameComponent = descItem.getItemMeta().displayName();
      // Convert Component to plain string and strip formatting
      String displayName =
          LegacyComponentSerializer.legacySection().serialize(displayNameComponent);
      String kitName = displayName.length() > 2 ? displayName.substring(2).toLowerCase() : "";
      NamespacedKey key = new NamespacedKey(plugin, "kit");
      String selected = player.getPersistentDataContainer().get(key, PersistentDataType.STRING);
      if (kitName.equals(selected)) {
        ItemMeta clickedMeta = descItem.getItemMeta();
        clickedMeta.addEnchant(Enchantment.INFINITY, 1, true); // dummy enchantment for glow
        clickedMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        descItem.setItemMeta(clickedMeta);
      }
      inventory.setItem(i, descItem);
    }
  }
}
