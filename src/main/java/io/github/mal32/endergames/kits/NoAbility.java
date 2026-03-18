package io.github.mal32.endergames.kits;

import io.github.mal32.endergames.EnderGames;
import io.github.mal32.endergames.services.KitType;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class NoAbility extends AbstractKit {
  public NoAbility(EnderGames plugin) {
    super(plugin, KitType.NO_ABILITY);
  }

  @Override
  public void initPlayer(Player player) {}

  @Override
  public KitDescription getDescription() {
    return new KitDescription(
        Material.ITEM_FRAME, "No Ability", "A kit that does nothing", "None", Difficulty.HARD);
  }
}
