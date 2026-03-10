package io.github.mal32.endergames.services;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;

public enum KitType implements PlayerAttribute<KitType> {
  LUMBERJACK,
  CAT,
  CACTUS,
  BARBARIAN,
  KNIGHT,
  BLAZE,
  SLIME,
  DOLPHIN,
  MACE,
  BIRD,
  BOMBER,
  KANGAROO,
  ENDERMAN,
  LUCKER,
  REWIND,
  VOODOO,
  FOREST_SPIRIT;

  public static final NamespacedKey KEY = new NamespacedKey("endergames", "kit");

  public static KitType get(Player player) {
    String raw = player.getPersistentDataContainer().get(KEY, PersistentDataType.STRING);
    if (raw == null) return LUMBERJACK; // TODO: also if invalid?
    return KitType.valueOf(raw.toUpperCase());
  }

  public static void init(Player player) {
    if (!player.getPersistentDataContainer().has(KEY)) {
      KitType.LUMBERJACK.set(player);
    }
  }

  @Override
  public NamespacedKey getKey() {
    return KEY;
  }

  @Override
  public Class<KitType> getType() {
    return KitType.class;
  }
}
