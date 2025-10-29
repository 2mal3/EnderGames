package io.github.mal32.endergames.kits;

import io.github.lambdaphoenix.advancementLib.AdvancementAPI;
import io.github.mal32.endergames.EnderGames;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class Spectator extends AbstractKit {
  public Spectator(EnderGames plugin) {
    super(plugin);
  }

  public void start(Player player) {
    player.getInventory().clear();
    player.setGameMode(org.bukkit.GameMode.SPECTATOR);
  }

  public KitDescription getDescription() {
    return new KitDescription(
        Material.ENDER_EYE,
        "Spectator",
        "You are a spectator and cannot interact with the game.",
        null,
        Difficulty.EASY);
  }

  @Override
  public void registerAdvancement(AdvancementAPI api) {}
}
