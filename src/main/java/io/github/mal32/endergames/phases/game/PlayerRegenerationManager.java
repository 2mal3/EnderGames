package io.github.mal32.endergames.phases.game;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class PlayerRegenerationManager extends AbstractTask {
  public PlayerRegenerationManager(JavaPlugin plugin) {
    super(plugin);
  }

  @Override
  public int getDelay() {
    return 20 * 8;
  }

  @Override
  public void task() {
    for (Player player : Bukkit.getOnlinePlayers()) {
      if (player.getGameMode() != GameMode.SURVIVAL) continue;
      if (player.getFoodLevel() < 6 || player.getFoodLevel() > 18) continue;

      player.setHealth(
          Math.min(player.getHealth() + 1, player.getAttribute(Attribute.MAX_HEALTH).getValue()));
    }
  }
}
