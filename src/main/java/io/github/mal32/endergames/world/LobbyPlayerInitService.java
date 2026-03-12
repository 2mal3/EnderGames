package io.github.mal32.endergames.world;

import io.github.mal32.endergames.services.PlayerInWorld;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class LobbyPlayerInitService extends PlayerInitService {
  @Override
  public void init(Player player, Location spawn) {
    resetBase(player);

    player.setGameMode(GameMode.ADVENTURE);
    player.teleportAsync(spawn.clone().add(0, 10, 0)); // TODO: when ready

    PlayerInWorld.LOBBY.set(player);

    player.addPotionEffect(
        new PotionEffect(
            PotionEffectType.SATURATION, PotionEffect.INFINITE_DURATION, 1, true, false, false));
    player.addPotionEffect(
        new PotionEffect(
            PotionEffectType.RESISTANCE, PotionEffect.INFINITE_DURATION, 0, true, false, false));
  }
}
