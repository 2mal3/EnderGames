package io.github.mal32.endergames.world;

import io.github.mal32.endergames.services.PlayerInWorld;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class GamePlayerInitService extends PlayerInitService {
  @Override
  public void init(Player player, Location spawn) {
    resetBase(player);

    player.teleportAsync(spawn.clone().add(0, 5, 0));

    PlayerInWorld.GAME.set(player);

    Bukkit.getPluginManager().callEvent(new PlayerEnteredGameEvent(player));
  }
}
