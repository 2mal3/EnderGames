package io.github.mal32.endergames.worlds.game.game;

import io.github.mal32.endergames.EnderGames;
import io.github.mal32.endergames.phases.PhaseController;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class SpeedObsidianManager extends AbstractTeleportingBlockManager<SpeedObsidian> {
  public SpeedObsidianManager(EnderGames plugin, Location spawnLocation) {
    super(plugin, spawnLocation);
  }

  @Override
  public double getAvgBocksPerChunk() {
    return 0.05;
  }

  @Override
  protected int getBlockSecondsToLive() {
    return 60 * 3;
  }

  @Override
  protected SpeedObsidian getNewBlock(Location location) {
    return new SpeedObsidian(plugin, location, getBlockSecondsToLive());
  }

  @EventHandler
  public void onPlayerObsidianInteract(PlayerInteractEvent event) {
    if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
    if (event.getClickedBlock() == null || event.getClickedBlock().getType() != Material.OBSIDIAN)
      return;
    var player = event.getPlayer();
    if (!PhaseController.playerIsInGame(player)) return;

    var block = event.getClickedBlock();
    SpeedObsidian speedObsidian = getBlockAtLocation(block.getLocation());
    if (speedObsidian == null) return;
    removeBlock(speedObsidian);

    giveSpeed(player);
  }

  private void giveSpeed(Player player) {
    LivingEntity target = player;
    Entity vehicle = player.getVehicle();
    if (vehicle != null && vehicle instanceof LivingEntity) {
      target = (LivingEntity) vehicle;
    }

    if (target.isInWater()) {
      var dolphinGraceEffect =
          new PotionEffect(PotionEffectType.DOLPHINS_GRACE, 20 * 10, 0, false, false, true);
      PotionEffectsStacking.addPotionEffect(target, dolphinGraceEffect);
    } else {
      var speedEffect = new PotionEffect(PotionEffectType.SPEED, 20 * 20, 1, false, false, true);
      PotionEffectsStacking.addPotionEffect(target, speedEffect);
    }
  }
}

class SpeedObsidian extends AbstractTeleportingBlock {
  public SpeedObsidian(EnderGames plugin, Location location, int secondsToLive) {
    super(plugin, location, secondsToLive);
  }

  @Override
  public Material getBlockMaterial() {
    return Material.OBSIDIAN;
  }

  @Override
  public Material getFallingBlockMaterial() {
    return Material.OBSIDIAN;
  }
}
