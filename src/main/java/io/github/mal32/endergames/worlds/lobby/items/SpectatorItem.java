package io.github.mal32.endergames.worlds.lobby.items;

import io.github.mal32.endergames.EnderGames;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;

class SpectatorItem extends MenuItem {

  public SpectatorItem(EnderGames plugin) {
    super(
        plugin,
        (byte) 8,
        "spectate_game",
        Map.of(
            "",
            new ItemDisplay(
                Material.ENDER_EYE,
                Component.text("Spectate Game").color(NamedTextColor.GOLD),
                NamespacedKey.minecraft("spyglass"))));
  }

  @Override
  public void onGameStart(Player player) {
    this.giveItem(player);
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
