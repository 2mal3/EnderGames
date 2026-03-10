package io.github.mal32.endergames.services;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;

public enum PlayerState implements PlayerAttribute<PlayerState> {
  PLAYING,
  SPECTATING,
  IN_LOBBY;

  public static final NamespacedKey KEY = new NamespacedKey("endergames", "playing");

  public static PlayerState get(Player player) {
    String raw = player.getPersistentDataContainer().get(KEY, PersistentDataType.STRING);
    if (raw == null) return PLAYING;
    return PlayerState.valueOf(raw.toUpperCase());
  }

  public static void init(Player player) {
    if (!player.getPersistentDataContainer().has(KEY)) {
      PlayerState.PLAYING.set(player);
    }
  }

  @Override
  public NamespacedKey getKey() {
    return KEY;
  }

  @Override
  public Class<PlayerState> getType() {
    return PlayerState.class;
  }
}
