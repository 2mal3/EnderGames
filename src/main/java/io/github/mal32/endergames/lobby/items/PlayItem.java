package io.github.mal32.endergames.lobby.items;

import io.github.mal32.endergames.EnderGames;
import io.github.mal32.endergames.services.PlayerState;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.jetbrains.annotations.NotNull;

public class PlayItem extends MenuItem {

  public PlayItem(EnderGames plugin) {
    super(
        plugin,
        (byte) 4,
        "play_game",
        Map.of(
            PlayerState.PLAYING.name(),
            new ItemDisplay(
                Material.END_CRYSTAL, Component.text("Mode: Play").color(NamedTextColor.DARK_AQUA)),
            PlayerState.SPECTATING.name(),
            new ItemDisplay(
                Material.ENDER_EYE,
                Component.text("Mode: Spectate").color(NamedTextColor.DARK_AQUA),
                NamespacedKey.minecraft("spyglass")),
            PlayerState.IN_LOBBY.name(),
            new ItemDisplay(
                Material.DARK_OAK_DOOR,
                Component.text("Mode: Stay in Lobby").color(NamedTextColor.DARK_AQUA))));
  }

  @Override
  public void initPlayer(Player player) {
    this.giveItem(player);
  }

  @Override
  protected @NotNull String getState(Player player) {
    return PlayerState.get(player).name();
  }

  @Override
  public void playerInteract(PlayerInteractEvent event) {
    Player player = event.getPlayer();

    player.playSound(player, Sound.UI_BUTTON_CLICK, 1, 1);

    final PlayerState state = PlayerState.get(player);
    final PlayerState newState;
    switch (state) {
      case PLAYING -> newState = PlayerState.SPECTATING;
      case SPECTATING -> newState = PlayerState.IN_LOBBY;
      default -> newState = PlayerState.PLAYING;
    }
    newState.set(player);
    this.giveItem(player);
  }
}
