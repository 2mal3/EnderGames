package io.github.mal32.endergames.worlds.lobby;

import io.github.mal32.endergames.EnderGames;
import java.util.Random;
import org.bukkit.*;
import org.bukkit.block.structure.Mirror;
import org.bukkit.block.structure.StructureRotation;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.structure.Structure;
import org.bukkit.structure.StructureManager;

public class LobbyPhase implements Listener {
  private final KitSelector kitSelector;
  private final EnderGames plugin;
  private final World lobbyWorld = Bukkit.getWorlds().getFirst();
  private final Location spawnLocation = new Location(lobbyWorld, 0, 0, 0);

  public LobbyPhase(EnderGames plugin) {
    Bukkit.getPluginManager().registerEvents(this, plugin);
    this.plugin = plugin;

    this.kitSelector = new KitSelector(this.plugin);

    lobbyWorld.setSpawnLocation(spawnLocation);
    lobbyWorld.setGameRule(GameRule.SPAWN_RADIUS, 6);
    lobbyWorld.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
    lobbyWorld.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
    lobbyWorld.getWorldBorder().setSize(500);

    placeLobby();
  }

  private void placeLobby() {
    StructureManager manager = Bukkit.getServer().getStructureManager();
    Structure structure = manager.loadStructure(new NamespacedKey("enga", "lobby"));

    Location location = new Location(lobbyWorld, -5, 140, -5);
    structure.place(location, true, StructureRotation.NONE, Mirror.NONE, 0, 1.0f, new Random());
  }

  @EventHandler
  public void onPlayerJoin(PlayerJoinEvent event) {
    var player = event.getPlayer();

    player.getInventory().clear();
    kitSelector.giveKitSelector(player);

    player.setGameMode(GameMode.ADVENTURE);

    player.addPotionEffect(
        new PotionEffect(
            PotionEffectType.SATURATION, PotionEffect.INFINITE_DURATION, 1, true, false));

    player.teleport(spawnLocation.clone().add(0, 5, 0));
  }

  @EventHandler
  public void onPlayerDamage(EntityDamageEvent event) {
    if (!(event.getEntity() instanceof Player player)) return;
    if (!EnderGames.playerIsInLobbyWorld(player)) return;
    if (event.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK) return;

    event.setCancelled(true);
  }
}
