package io.github.mal32.endergames.services;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;

public interface PlayerAttribute<T extends Enum<T> & PlayerAttribute<T>> {
  NamespacedKey getKey();

  Class<T> getType();

  default T getValue(Player player) {
    String raw = player.getPersistentDataContainer().get(getKey(), PersistentDataType.STRING);
    if (raw == null) return null;
    return Enum.valueOf(getType(), raw.toUpperCase());
  }

  default void set(Player player, T value) {
    player.getPersistentDataContainer().set(getKey(), PersistentDataType.STRING, value.name());
  }

  @SuppressWarnings("unchecked")
  default void set(Player player) {
    set(player, (T) this);
  }

  default boolean is(Player player, T value) {
    return getValue(player) == value;
  }

  default boolean is(Player player) {
    return getValue(player) == this;
  }

  default Player[] allWith(T value) {
    return Bukkit.getOnlinePlayers().stream().filter(p -> is(p, value)).toArray(Player[]::new);
  }

  default Player[] all() {
    return Bukkit.getOnlinePlayers().stream().filter(this::is).toArray(Player[]::new);
  }
}
