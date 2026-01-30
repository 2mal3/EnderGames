package io.github.mal32.endergames.worlds.game.game;

import io.github.mal32.endergames.EnderGames;
import io.github.mal32.endergames.worlds.game.GameWorld;
import org.bukkit.Location;
import org.bukkit.Material;
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
  public int getBlockTeleportDelayTicks() {
    return 20 * 40;
  }

  @Override
  protected int blockCount() {
    return 36;
  }

  @Override
  protected SpeedObsidian getNewBlock(Location location) {
    return new SpeedObsidian(plugin, location);
  }

  @EventHandler
  public void onPlayerObsidianInteract(PlayerInteractEvent event) {
    if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
    if (event.getClickedBlock() == null || event.getClickedBlock().getType() != Material.OBSIDIAN)
      return;
    var player = event.getPlayer();
    if (!GameWorld.playerIsInGame(player)) return;

    var block = event.getClickedBlock();
    SpeedObsidian speedObsidian = getBlockAtLocation(block.getLocation());
    if (speedObsidian == null) return;

    if (player.isInWater()) {
      var dolphinGraceEffect =
          new PotionEffect(PotionEffectType.DOLPHINS_GRACE, 20 * 10, 0, false, false, true);
      PotionEffectsStacking.addPotionEffect(player, dolphinGraceEffect);
    } else {
      var speedEffect = new PotionEffect(PotionEffectType.SPEED, 20 * 20, 1, false, false, true);
      PotionEffectsStacking.addPotionEffect(player, speedEffect);
    }

    speedObsidian.teleport(spawnLocation);
  }
}

class SpeedObsidian extends AbstractTeleportingBlock {
  public SpeedObsidian(EnderGames plugin, Location location) {
    super(plugin, location);
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
