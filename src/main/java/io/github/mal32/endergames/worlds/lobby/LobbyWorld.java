package io.github.mal32.endergames.worlds.lobby;

import io.github.mal32.endergames.AbstractModule;
import io.github.mal32.endergames.EnderGames;
import io.github.mal32.endergames.kits.AbstractKit;
import io.github.mal32.endergames.services.PlayerInWorld;
import io.github.mal32.endergames.services.PlayerState;
import io.github.mal32.endergames.worlds.AbstractWorld;
import io.github.mal32.endergames.worlds.lobby.items.MenuManager;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import org.bukkit.*;
import org.bukkit.block.structure.Mirror;
import org.bukkit.block.structure.StructureRotation;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.structure.Structure;
import org.bukkit.structure.StructureManager;

public class LobbyWorld extends AbstractWorld {
  public static final World lobbyWorld =
      Objects.requireNonNull(Bukkit.getWorld("world_enga_lobby"));
  private final MenuManager menuManager;
  private final Location spawnLocation = new Location(lobbyWorld, 0, 64, 0);
  private final ParkourManager pmanager;

  public LobbyWorld(EnderGames plugin) {
    super(plugin);

    this.pmanager = new ParkourManager(plugin);
    this.menuManager = new MenuManager(this.plugin);

    assert lobbyWorld != null;
    lobbyWorld.setSpawnLocation(spawnLocation);
    lobbyWorld.setGameRule(GameRules.RESPAWN_RADIUS, 6);
    lobbyWorld.setGameRule(GameRules.ADVANCE_TIME, false);
    lobbyWorld.setGameRule(GameRules.ADVANCE_WEATHER, false);
    lobbyWorld.getWorldBorder().setSize(500);

    lobbyWorld.setGameRule(GameRules.LOCATOR_BAR, false);

    tryUpdatingLobby();

    var modules = List.of(new PlayerDifficulty(plugin));
    for (AbstractModule module : modules) {
      module.enable();
    }

    lobbyWorld.getChunkAt(0, 0).setForceLoaded(true); // ensure item frames for map wall are loaded
  }

  public MenuManager getMenuManager() {
    return this.menuManager;
  }

  private void tryUpdatingLobby() {
    final NamespacedKey placedLobbyVersionKey = new NamespacedKey(plugin, "placed_lobby_version");
    final int currentLobbyVersion = 1;

    var placedLobbyVersion =
        lobbyWorld
            .getPersistentDataContainer()
            .get(placedLobbyVersionKey, PersistentDataType.INTEGER);
    if (placedLobbyVersion == null || placedLobbyVersion != currentLobbyVersion) {
      plugin.getComponentLogger().info("Loading lobby, this could take a few seconds ...");
      placeLobby();
      lobbyWorld
          .getPersistentDataContainer()
          .set(placedLobbyVersionKey, PersistentDataType.INTEGER, currentLobbyVersion);
    }
  }

  private void placeLobby() {
    StructureManager manager = Bukkit.getServer().getStructureManager();
    Structure structure = manager.loadStructure(new NamespacedKey("enga", "lobby"));

    assert structure != null;
    Location location =
        spawnLocation
            .clone()
            .add(-structure.getSize().getX() / 2, 0, -structure.getSize().getZ() / 2);
    structure.place(location, true, StructureRotation.NONE, Mirror.NONE, 0, 1.0f, new Random());
  }

  @EventHandler
  public void onPlayerJoin(PlayerJoinEvent event) {
    Player player = event.getPlayer();
    Bukkit.getScheduler().runTaskLater(plugin, () -> teleportPlayerWhenReady(player), 1);
  }

  private void teleportPlayerWhenReady(Player player) {
    boolean ready = !(player.isDead() || player.getLocation().getWorld() == null);
    if (ready) {
      teleportPlayerToLobby(player);
    } else {
      Bukkit.getScheduler().runTaskLater(plugin, () -> teleportPlayerWhenReady(player), 1);
    }
  }

  public void teleportPlayerToLobby(Player player) {
    PlayerInWorld.LOBBY.set(player);
    initPlayer(player);
  }

  @Override
  public void initPlayer(Player player) {
    player.getInventory().clear();

    PlayerState.init(player);

    menuManager.initPlayer(player);

    if (this.plugin.getGameWorld().isGameRunning()) {
      menuManager.onGameStart(player);
    } else {
      menuManager.onGameEnd(player);
    }

    player.setGameMode(GameMode.ADVENTURE);

    for (PotionEffect effect : player.getActivePotionEffects()) {
      player.removePotionEffect(effect.getType());
    }
    // Together prevent killing of player by other players
    player.addPotionEffect(
        new PotionEffect(
            PotionEffectType.SATURATION, PotionEffect.INFINITE_DURATION, 1, true, false, false));
    player.addPotionEffect(
        new PotionEffect(
            PotionEffectType.RESISTANCE, PotionEffect.INFINITE_DURATION, 0, true, false, false));

    player.teleport(spawnLocation.clone().add(0, 10, 0));

    String currentKit =
        player
            .getPersistentDataContainer()
            .get(AbstractKit.kitStorageKey, PersistentDataType.STRING);
    if (currentKit == null || currentKit.isEmpty()) {
      player
          .getPersistentDataContainer()
          .set(AbstractKit.kitStorageKey, PersistentDataType.STRING, "lumberjack");
    }
  }

  @EventHandler
  public void onPlayerDamage(EntityDamageEvent event) {
    if (!(event.getEntity() instanceof Player player)) return;
    if (!PlayerInWorld.LOBBY.is(player)) return;
    if (event.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK) return;

    event.setCancelled(true);
  }

  @EventHandler
  public void onFieldTrample(EntityChangeBlockEvent event) {
    if (!(event.getEntity() instanceof Player player)) return;
    if (!PlayerInWorld.LOBBY.is(player)) return;

    if (event.getBlock().getType() != Material.FARMLAND) return;

    event.setCancelled(true);
  }

  @EventHandler
  public void onPlayerInteract(PlayerInteractEvent event) {
    if (event.getHand() != EquipmentSlot.HAND) return;
    if (event.getClickedBlock() != null) {
      Material type = event.getClickedBlock().getType();

      if (type.name().contains("TRAPDOOR")) {
        event.setCancelled(true);
      }
    }

    // Parkour reset item
    ItemStack item = event.getItem();
    if (pmanager.isResetItem(item)) {
      event.setCancelled(true);
      Player p = event.getPlayer();
      pmanager.resetPlayer(p);
    } else if (pmanager.isCancelItem(item)) {
      event.setCancelled(true);
      Player p = event.getPlayer();
      pmanager.abortParkour(p);
    }
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent e) {
    pmanager.abortParkour(e.getPlayer());
  }

  // Prevent moving the parkour items
  @EventHandler
  public void onInventoryClick(InventoryClickEvent event) {
    ItemStack item = event.getCurrentItem();
    if (item != null && (pmanager.isResetItem(item) || pmanager.isCancelItem(item))) {
      event.setCancelled(true);
    }
  }

  @EventHandler
  public void onInventoryDrag(InventoryDragEvent event) {
    ItemStack item = event.getOldCursor();
    if (pmanager.isResetItem(item) || pmanager.isCancelItem(item)) {
      event.setCancelled(true);
    }
  }

  @EventHandler
  public void onPlayerDrop(PlayerDropItemEvent event) {
    ItemStack item = event.getItemDrop().getItemStack();
    if (pmanager.isResetItem(item) || pmanager.isCancelItem(item)) {
      event.setCancelled(true);
    }
  }
}
