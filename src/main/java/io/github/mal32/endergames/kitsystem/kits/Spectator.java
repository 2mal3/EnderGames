package io.github.mal32.endergames.kitsystem.kits;

import io.github.mal32.endergames.kitsystem.api.AbstractKit;
import io.github.mal32.endergames.kitsystem.api.Difficulty;
import io.github.mal32.endergames.kitsystem.api.KitDescription;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class Spectator extends AbstractKit {
  public Spectator(JavaPlugin plugin) {
    super(
        new KitDescription(
            "Spectator",
            Material.ENDER_EYE,
            "You are a spectator and cannot interact with the game.",
            "",
            Difficulty.EASY),
        plugin);
  }

  @Override
  public void initPlayer(Player player) {
    player.getInventory().clear();
    player.setGameMode(GameMode.SPECTATOR);
  }
}
