package io.github.mal32.endergames.phases.lobby;

import io.github.mal32.endergames.EnderGames;
import io.github.mal32.endergames.phases.AbstractPhase;
import io.github.mal32.endergames.phases.StartPhase;
import java.util.Random;
import org.bukkit.*;
import org.bukkit.block.structure.Mirror;
import org.bukkit.block.structure.StructureRotation;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.structure.Structure;
import org.bukkit.structure.StructureManager;

public class LobbyPhase extends AbstractPhase {
  private final KitSelector kitSelector;

  public LobbyPhase(EnderGames plugin) {
    super(plugin);

    Bukkit.getPluginManager().registerEvents(this, plugin);

    NamespacedKey lobbyKey = new NamespacedKey(this.plugin, "lobby");
    this.kitSelector = new KitSelector(this.plugin);

    World lobby = Bukkit.getWorlds().getFirst();

    if (!lobby.getPersistentDataContainer().has(lobbyKey, PersistentDataType.STRING)) {
      lobby.getPersistentDataContainer().set(lobbyKey, PersistentDataType.BOOLEAN, true);
      lobby.setSpawnLocation(new Location(lobby, 0, 151, 0));
      lobby.setGameRule(GameRule.SPAWN_RADIUS, 6);
      lobby.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
      lobby.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
      lobby.getWorldBorder().setSize(500);
      this.placeLobby(lobby);
    }
  }

  private void placeLobby(World lobby) {
    StructureManager manager = Bukkit.getServer().getStructureManager();
    Structure structure = manager.loadStructure(new NamespacedKey("enga", "lobby"));

    Location location = new Location(lobby, -5, 140, -5);
    structure.place(location, true, StructureRotation.NONE, Mirror.NONE, 0, 1.0f, new Random());
  }

  public void initPlayer(Player player) {
    player.getInventory().clear();
    player.setGameMode(GameMode.ADVENTURE);
    player.addPotionEffect(
        new PotionEffect(
            PotionEffectType.SATURATION, PotionEffect.INFINITE_DURATION, 1, true, false));

    if (this.plugin.getCurrentPhaseName() == EnderGames.Phase.IDLE)
      kitSelector.giveKitSelector(player);
    else {
      // TODO: jump in as spectator
    }
  }

  @Override
  public void start() {
    this.kitSelector.enable();
    for (Player player : Bukkit.getOnlinePlayers()) {
      if (EnderGames.playerIsIdling(player)) continue;
      player.teleport(Bukkit.getWorlds().getFirst().getSpawnLocation());
      initPlayer(player);
    }

    ((StartPhase) this.plugin.getPhase(EnderGames.Phase.STARTING)).replaceSpawn();
  }

  @Override
  public void stop() {
    for (Player player : Bukkit.getOnlinePlayers()) {
      if (!EnderGames.playerIsPlaying(player)) continue;
      player.clearActivePotionEffects();
    }
    kitSelector.disable();
  }

  @EventHandler
  public void onPlayerDamage(EntityDamageEvent event) {
    if (!(event.getEntity() instanceof Player player)) return;
    if (!EnderGames.playerIsIdling(player)) return;
    if (event.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK) return;

    event.setCancelled(true);
  }
}
