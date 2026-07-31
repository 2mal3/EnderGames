package io.github.mal32.endergames.kitsystem.kits;

import io.github.mal32.endergames.kitsystem.api.AbstractKit;
import io.github.mal32.endergames.kitsystem.api.Difficulty;
import io.github.mal32.endergames.kitsystem.api.KitDescription;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class NoAbility extends AbstractKit {
  public NoAbility(JavaPlugin plugin) {
    super(
        new KitDescription(
            "No Ability", Material.ITEM_FRAME, "A kit that does nothing", "None", Difficulty.HARD),
        plugin);
  }

  @Override
  public void initPlayer(Player player) {}
}
