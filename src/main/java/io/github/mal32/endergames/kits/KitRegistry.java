package io.github.mal32.endergames.kits;

import io.github.mal32.endergames.EnderGames;
import io.github.mal32.endergames.services.KitType;
import java.util.EnumMap;
import java.util.Map;

public final class KitRegistry {
  private static final Map<KitType, AbstractKit> kits = new EnumMap<>(KitType.class);

  public static void register(KitType type, AbstractKit kit) {
    kits.put(type, kit);
  }

  public static void registerKits(EnderGames plugin) {
    register(KitType.LUMBERJACK, new Lumberjack(plugin));
    register(KitType.CAT, new Cat(plugin));
    register(KitType.CACTUS, new Cactus(plugin));
    register(KitType.BARBARIAN, new Barbarian(plugin));
    register(KitType.KNIGHT, new Knight(plugin));
    register(KitType.BLAZE, new Blaze(plugin));
    register(KitType.SLIME, new Slime(plugin));
    register(KitType.DOLPHIN, new Dolphin(plugin));
    register(KitType.MACE, new Mace(plugin));
    register(KitType.BIRD, new Bird(plugin));
    register(KitType.BOMBER, new Bomber(plugin));
    register(KitType.KANGAROO, new Kangaroo(plugin));
    register(KitType.ENDERMAN, new Enderman(plugin));
    register(KitType.LUCKER, new Lucker(plugin));
    register(KitType.REWIND, new Rewind(plugin));
    register(KitType.VOODOO, new Voodoo(plugin));
    register(KitType.FOREST_SPIRIT, new ForestSpirit(plugin));
  }

  public static AbstractKit[] getKits() {
    return kits.values().toArray(new AbstractKit[0]);
  }

  public static AbstractKit get(KitType type) {
    return kits.get(type);
  }
}
