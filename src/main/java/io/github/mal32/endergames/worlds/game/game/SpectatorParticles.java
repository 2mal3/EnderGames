package io.github.mal32.endergames.worlds.game.game;

import io.github.mal32.endergames.EnderGames;
import io.github.mal32.endergames.worlds.game.GameWorld;
import org.bukkit.*;
import org.bukkit.entity.Player;

public class SpectatorParticles extends AbstractTask {
  public SpectatorParticles(EnderGames plugin) {
    super(plugin);
  }

  @Override
  public int getDelayTicks() {
    return 10;
  }

  @Override
  public void task() {
    for (Player player : GameWorld.getPlayersInGameWorld()) {
      if (player.getGameMode() != GameMode.SPECTATOR) continue;

      Location location = player.getLocation().clone().add(0, 0.5, 0);
      World world = player.getWorld();
      world.spawnParticle(Particle.PORTAL, location, 1, 0.1, 0.2, 0.1, 0);
    }
  }
}
