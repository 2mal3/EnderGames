package io.github.mal32.endergames.worlds.lobby.items;

import io.github.mal32.endergames.EnderGames;
import java.util.Map;
import java.util.Objects;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

public class PlayItem extends MenuItem {
  public static final NamespacedKey playingStorageKey = new NamespacedKey("endergames", "playing");

  public PlayItem(EnderGames plugin) {
    super(
        plugin,
        (byte) 4,
        "play_game",
        Map.of(
            State.PLAYING.toString(),
            new ItemDisplay(
                Material.ENDER_EYE, Component.text("Play").color(NamedTextColor.DARK_AQUA)),
            State.OBSERVING.toString(),
            new ItemDisplay(
                Material.SPYGLASS, Component.text("Spectate").color(NamedTextColor.DARK_AQUA)),
            State.SKIP.toString(),
            new ItemDisplay(
                Material.OAK_DOOR, Component.text("Skip").color(NamedTextColor.DARK_AQUA))));
  }

  public static boolean playerIsPlaying(Player player) {
    String playing =
        player.getPersistentDataContainer().get(playingStorageKey, PersistentDataType.STRING);
    return Objects.equals(playing, State.PLAYING.toString());
  }

  public static boolean playerIsObserving(Player player) {
    String playing =
        player.getPersistentDataContainer().get(playingStorageKey, PersistentDataType.STRING);
    return Objects.equals(playing, State.OBSERVING.toString());
  }

  public static Player[] getPlayingPlayers() {
    return Bukkit.getOnlinePlayers().stream()
        .filter(PlayItem::playerIsPlaying)
        .toArray(Player[]::new);
  }

  public static Player[] getObservingPlayers() {
    return Bukkit.getOnlinePlayers().stream()
        .filter(PlayItem::playerIsObserving)
        .toArray(Player[]::new);
  }

  public static Player[] getJoiningPlayers() {
    return Bukkit.getOnlinePlayers().stream()
        .filter(player -> PlayItem.playerIsObserving(player) || PlayItem.playerIsPlaying(player))
        .toArray(Player[]::new);
  }

  @Override
  public void initPlayer(Player player) {
    if (!player.getPersistentDataContainer().has(playingStorageKey)) {
      player
          .getPersistentDataContainer()
          .set(playingStorageKey, PersistentDataType.STRING, State.PLAYING.toString());
    }
    this.giveItem(player);
  }

  @Override
  protected @NotNull String getState(Player player) {
    String playing =
        player.getPersistentDataContainer().get(playingStorageKey, PersistentDataType.STRING);
    if (playing == null) return State.PLAYING.toString();
    return playing;
  }

  @Override
  public void playerInteract(PlayerInteractEvent event) {
    Player player = event.getPlayer();

    player.playSound(player, Sound.ITEM_GOAT_HORN_SOUND_0, 1, 1);

    String playing =
        player.getPersistentDataContainer().get(playingStorageKey, PersistentDataType.STRING);
    String newState = State.valueOf(playing).next;
    player.getPersistentDataContainer().set(playingStorageKey, PersistentDataType.STRING, newState);
    this.giveItem(player);
  }

  enum State {
    PLAYING("OBSERVING"),
    OBSERVING("SKIP"),
    SKIP("PLAYING");

    private final String next;

    State(String next) {
      this.next = next;
    }
  }
}
