package io.github.mal32.endergames.worlds.lobby.items;

import io.github.mal32.endergames.EnderGames;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;

class SpectatorItem extends MenuItem {

  public SpectatorItem(EnderGames plugin) {
    super(
        plugin,
        Material.ENDER_EYE,
        Component.text("Spectate Game").color(NamedTextColor.GOLD),
        "spectate_game",
        (byte) 4);
  }

  @Override
  public void initPlayer(Player player) {
    if (plugin.getGameWorld().isGameRunning()) {
      giveItem(player);
    }
  }

  @Override
  public void onGameEnd(Player player) {
    player.getInventory().setItem(slot, null);
  }

  @Override
  public void playerInteract(PlayerInteractEvent event) {
    Player player = event.getPlayer();
    player.playSound(player, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
    plugin.getGameWorld().teleportPlayerToGame(player);
  }
}
