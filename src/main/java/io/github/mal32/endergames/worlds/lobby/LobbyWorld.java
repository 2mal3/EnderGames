package io.github.mal32.endergames.worlds.lobby;

import io.github.mal32.endergames.EnderGames;
import io.github.mal32.endergames.worlds.AbstractWorld;
import io.github.mal32.endergames.worlds.lobby.items.MenuManager;
import java.util.Objects;
import java.util.Random;
import org.bukkit.*;
import org.bukkit.block.structure.Mirror;
import org.bukkit.block.structure.StructureRotation;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.structure.Structure;
import org.bukkit.structure.StructureManager;

public class LobbyWorld extends AbstractWorld {
  private final MenuManager menuManager;
  private final World lobbyWorld = Objects.requireNonNull(Bukkit.getWorld("world_enga_lobby"));
  private final Location spawnLocation = new Location(lobbyWorld, 0, 64, 0);

  public LobbyWorld(EnderGames plugin) {
    super(plugin);

    this.menuManager = new MenuManager(this.plugin);

    lobbyWorld.setSpawnLocation(spawnLocation);
    lobbyWorld.setGameRule(GameRule.SPAWN_RADIUS, 6);
    lobbyWorld.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
    lobbyWorld.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
    lobbyWorld.getWorldBorder().setSize(500);

    tryUpdatingLobby();
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

    Location location =
        spawnLocation
            .clone()
            .add(-structure.getSize().getX() / 2, 0, -structure.getSize().getZ() / 2);
    structure.place(location, true, StructureRotation.NONE, Mirror.NONE, 0, 1.0f, new Random());
  }

  @Override
  public void initPlayer(Player player) {
    player.getInventory().clear();

    menuManager.initPlayer(player);

    player.setGameMode(GameMode.ADVENTURE);

    player.addPotionEffect(
        new PotionEffect(
            PotionEffectType.SATURATION, PotionEffect.INFINITE_DURATION, 1, false, false, false));

    player.teleport(spawnLocation.clone().add(0, 10, 0));

    var kitKey = new NamespacedKey(plugin, "kit");
    String currentKit = player.getPersistentDataContainer().get(kitKey, PersistentDataType.STRING);
    if (currentKit == null || currentKit.isEmpty()) {
      player.getPersistentDataContainer().set(kitKey, PersistentDataType.STRING, "lumberjack");
    }
  }

  @EventHandler
  public void onPlayerDamage(EntityDamageEvent event) {
    if (!(event.getEntity() instanceof Player player)) return;
    if (!EnderGames.playerIsInLobbyWorld(player)) return;
    if (event.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK) return;

    event.setCancelled(true);
  }

  @EventHandler
  public void onFieldTrample(EntityChangeBlockEvent event) {
    if (!(event.getEntity() instanceof Player player)) return;
    if (!EnderGames.playerIsInLobbyWorld(player)) return;

    if (event.getBlock().getType() != Material.FARMLAND) return;

    event.setCancelled(true);
  }
}
