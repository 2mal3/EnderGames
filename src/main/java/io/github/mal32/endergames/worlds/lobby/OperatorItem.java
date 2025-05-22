package io.github.mal32.endergames.worlds.lobby;

import io.github.mal32.endergames.EnderGames;
import org.bukkit.Material;
import org.bukkit.event.player.PlayerInteractEvent;

class OperatorItem extends MenuItem {

  public OperatorItem(EnderGames plugin) {
    super(plugin, Material.NETHER_STAR, "§6Start Game", (byte) 8);
  }

  @Override
  public void playerInteract(PlayerInteractEvent event) {
    this.plugin.getGameWorld().startGame();
  }
}
