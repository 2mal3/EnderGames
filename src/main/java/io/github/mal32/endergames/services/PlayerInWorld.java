package io.github.mal32.endergames.services;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;

public enum PlayerInWorld implements PlayerAttribute<PlayerInWorld> {
  GAME,
  LOBBY;

  public static final NamespacedKey KEY = new NamespacedKey("endergames", "world");

  public static PlayerInWorld get(Player player) {
    String raw = player.getPersistentDataContainer().get(KEY, PersistentDataType.STRING);
    if (raw == null) return GAME;
    return PlayerInWorld.valueOf(raw.toUpperCase());
  }

  @Override
  public NamespacedKey getKey() {
    return KEY;
  }

  @Override
  public Class<PlayerInWorld> getType() {
    return PlayerInWorld.class;
  }
}
