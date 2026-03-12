package io.github.mal32.endergames.world;

import java.util.Objects;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public abstract class PlayerInitService {
  protected void resetBase(Player player) {
    player.getInventory().clear();
    player.setFireTicks(0);
    player.setFoodLevel(20);
    player.setHealth(Objects.requireNonNull(player.getAttribute(Attribute.MAX_HEALTH)).getValue());
    player.setFallDistance(0);
    player.setVelocity(new Vector(0, 0, 0));
    player.getActivePotionEffects().forEach(e -> player.removePotionEffect(e.getType()));
  }

  public abstract void init(Player player, Location spawn);
}
