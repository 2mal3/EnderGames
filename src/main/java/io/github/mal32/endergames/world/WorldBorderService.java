package io.github.mal32.endergames.world;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldBorder;

public class WorldBorderService {
  public void configureBorder(World world) {
    WorldBorder border = world.getWorldBorder();
    border.setWarningDistance(32);
    border.setWarningTimeTicks(60 * 20);
    border.setDamageBuffer(1);
  }

  public void centerBorder(World world, Location center) {
    WorldBorder border = world.getWorldBorder();
    border.setCenter(center);
  }
}
