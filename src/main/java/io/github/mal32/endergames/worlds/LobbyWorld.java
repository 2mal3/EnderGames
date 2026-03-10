package io.github.mal32.endergames.worlds;

import io.github.mal32.endergames.EnderGames;
import io.github.mal32.endergames.services.KitType;
import io.github.mal32.endergames.services.PlayerInWorld;
import io.github.mal32.endergames.worlds.lobby.items.MenuManager;
import java.util.Random;
import org.bukkit.*;
import org.bukkit.block.structure.Mirror;
import org.bukkit.block.structure.StructureRotation;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.structure.Structure;
import org.bukkit.structure.StructureManager;

public class LobbyWorld extends AbstractWorld{
  private final World world;
  private final Location spawnLocation;

  public LobbyWorld(EnderGames plugin) {
    super(plugin);
    this.world = Bukkit.getWorld("world_enga_lobby");
    this.spawnLocation = new Location(world, 0, 64, 0);

    assert world != null;
    world.setSpawnLocation(spawnLocation);
    world.getChunkAt(spawnLocation).setForceLoaded(true);

    tryUpdatingLobby();
  }

  @Override
  protected void setupWorld() {
    world.setGameRule(GameRules.RESPAWN_RADIUS, 6);
    world.setGameRule(GameRules.ADVANCE_TIME, false);
    world.setGameRule(GameRules.ADVANCE_WEATHER, false);
    world.setGameRule(GameRules.LOCATOR_BAR, false);
    world.getWorldBorder().setSize(500);
  }

  @Override
  public void resetWorld() {}

  @Override
  public World getWorld() {
    return world;
  }

  @Override
  public Location getSpawnLocation() {
    return spawnLocation.clone();
  }

  @Override
  public void initPlayer(Player player) {
    resetPlayer(player);

    PlayerInWorld.LOBBY.set(player);

    player.setGameMode(GameMode.ADVENTURE);

    player.addPotionEffect(
        new PotionEffect(
            PotionEffectType.SATURATION, PotionEffect.INFINITE_DURATION, 1, true, false, false));
    player.addPotionEffect(
        new PotionEffect(
            PotionEffectType.RESISTANCE, PotionEffect.INFINITE_DURATION, 0, true, false, false));

    final MenuManager menuManager = plugin.getMenuManager();
    menuManager.initPlayer(player);
    
    if (plugin.getPhaseController().isLoading()) {
      menuManager.onGameEnd(player);
    } else {
      menuManager.onGameStart(player);
    }

    // TODO generic player init?
    KitType.init(player);

    teleport(player, spawnLocation.clone().add(0, 10, 0));
  }

  @Override
  protected boolean isInThisWorld(Player player) {
    return PlayerInWorld.LOBBY.is(player);
  }

  @Override
  protected void shutdown() {
    disable();
  }

  @EventHandler
  public void onPlayerJoin(PlayerJoinEvent event) {
    Player player = event.getPlayer();
    Bukkit.getScheduler().runTaskLater(plugin, () -> teleportPlayerWhenReady(player), 1);
  }

  private void tryUpdatingLobby() {
    final NamespacedKey placedLobbyVersionKey = new NamespacedKey(plugin, "placed_lobby_version");
    final int currentLobbyVersion = 1;

    var placedLobbyVersion =
        world.getPersistentDataContainer()
            .get(placedLobbyVersionKey, PersistentDataType.INTEGER);
    if (placedLobbyVersion == null || placedLobbyVersion != currentLobbyVersion) {
      plugin.getComponentLogger().info("Loading lobby, this could take a few seconds ...");
      placeLobby();
      world.getPersistentDataContainer()
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

  private void teleportPlayerWhenReady(Player player) {
    boolean ready = !(player.isDead() || player.getLocation().getWorld() == null);
    if (ready) {
      initPlayer(player);
    } else {
      Bukkit.getScheduler().runTaskLater(plugin, () -> teleportPlayerWhenReady(player), 1);
    }
  }

  @EventHandler
  public void onPlayerDamage(EntityDamageEvent event) {
    if (!(event.getEntity() instanceof Player player)) return;
    if (!isInThisWorld(player)) return;
    if (event.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK) return;

    event.setCancelled(true);
  }

  @EventHandler
  public void onFieldTrample(EntityChangeBlockEvent event) {
    if (!(event.getEntity() instanceof Player player)) return;
    if (!isInThisWorld(player)) return;
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
  }
}
