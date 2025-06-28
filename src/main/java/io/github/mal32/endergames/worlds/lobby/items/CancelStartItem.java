package io.github.mal32.endergames.worlds.lobby.items;

import io.github.mal32.endergames.EnderGames;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;

class CancelStartItem extends MenuItem {

  public CancelStartItem(EnderGames plugin) {
    super(
        plugin,
        Material.BARRIER,
        Component.text("Cancel Start").color(NamedTextColor.GOLD),
        "start_game",
        (byte) 8);
  }

  @Override
  public void initPlayer(Player player) {}

  @Override
  public void playerInteract(PlayerInteractEvent event) {}
}
