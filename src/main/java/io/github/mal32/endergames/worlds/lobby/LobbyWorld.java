package io.github.mal32.endergames.worlds.lobby;

import io.github.mal32.endergames.EnderGames;
import io.github.mal32.endergames.worlds.AbstractWorld;
import org.bukkit.*;
import org.bukkit.block.structure.Mirror;
import org.bukkit.block.structure.StructureRotation;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.structure.Structure;
import org.bukkit.structure.StructureManager;

import java.util.Objects;
import java.util.Random;

public class LobbyWorld extends AbstractWorld implements Listener {
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

    this.menuManager.getItem(Material.CHEST).giveItem(player);
    if (player.isOp()) this.menuManager.getItem(Material.NETHER_STAR).giveItem(player);

    player.setGameMode(GameMode.ADVENTURE);

    player.addPotionEffect(
        new PotionEffect(
            PotionEffectType.SATURATION, PotionEffect.INFINITE_DURATION, 1, true, false));

    player.teleport(spawnLocation.clone().add(0, 10, 0));
  }

  @EventHandler
  public void onPlayerDamage(EntityDamageEvent event) {
    if (!(event.getEntity() instanceof Player player)) return;
    if (!EnderGames.playerIsInLobbyWorld(player)) return;
    if (event.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK) return;

    event.setCancelled(true);
  }
}
