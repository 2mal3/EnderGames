package io.github.mal32.endergames.worlds.game.game;

import io.github.mal32.endergames.EnderGames;
import io.github.mal32.endergames.worlds.game.GameWorld;
import org.bukkit.Location;
import org.bukkit.Material;
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
  public int getBlockTeleportDelayTicks() {
    return 20 * 40;
  }

  @Override
  protected int blocksPerPlayer() {
    return 6;
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

    applySpeedEffect(player);

    speedObsidian.teleport(spawnLocation);
  }

  private void applySpeedEffect(Player player) {
    PotionEffect current = player.getPotionEffect(PotionEffectType.SPEED);
    if (current != null && current.getAmplifier() == 1) {
      // Already has Speed II: extend by 20 seconds
      int extendedDuration = current.getDuration() + 20 * 20;
      player.addPotionEffect(
          new PotionEffect(PotionEffectType.SPEED, extendedDuration, 1, true, true));
    } else {
      int oldAmp = current != null ? current.getAmplifier() : -1;
      int oldDuration = current != null ? current.getDuration() : 0;
      int newDuration = 20 * 10;
      // Give Speed II
      player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, newDuration, 1, true, true));
      if (current != null && oldAmp < 1) {
        // Reapply old effect after Speed II runs out
        plugin
            .getServer()
            .getScheduler()
            .runTaskLater(
                plugin,
                () ->
                    player.addPotionEffect(
                        new PotionEffect(PotionEffectType.SPEED, oldDuration, oldAmp, true, true)),
                newDuration);
      }
    }
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
