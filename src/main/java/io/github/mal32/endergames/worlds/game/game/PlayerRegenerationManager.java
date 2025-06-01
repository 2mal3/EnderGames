package io.github.mal32.endergames.worlds.game.game;

import io.github.mal32.endergames.EnderGames;
import io.github.mal32.endergames.worlds.game.GameWorld;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;

public class PlayerRegenerationManager extends AbstractTask {
  public PlayerRegenerationManager(EnderGames plugin) {
    super(plugin);
  }

  @Override
  public int getDelayTicks() {
    return 20 * 8;
  }

  @Override
  public void task() {
    for (Player player : GameWorld.getPlayersInGame()) {
      if (player.getFoodLevel() < 6 || player.getFoodLevel() > 18) continue;

      player.setHealth(
          Math.min(player.getHealth() + 1, player.getAttribute(Attribute.MAX_HEALTH).getValue()));
    }
  }
}
