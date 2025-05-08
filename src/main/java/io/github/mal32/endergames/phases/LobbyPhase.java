package io.github.mal32.endergames.phases;

import io.github.mal32.endergames.EnderGames;
import java.util.Random;
import org.bukkit.*;
import org.bukkit.block.structure.Mirror;
import org.bukkit.block.structure.StructureRotation;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
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
