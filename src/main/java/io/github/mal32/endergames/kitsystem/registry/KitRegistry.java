package io.github.mal32.endergames.kitsystem.registry;

import io.github.mal32.endergames.EnderGames;
import io.github.mal32.endergames.kitsystem.api.KitManager;
import io.github.mal32.endergames.kitsystem.api.KitService;
import io.github.mal32.endergames.kitsystem.kits.*;

public final class KitRegistry {
  public static void registerAll(EnderGames plugin) {
    final KitManager kitManager = plugin.getKitSystem().manager();
    final KitService kitService = plugin.getKitSystem().service();
    kitManager.register(new Barbarian(kitService, plugin));
    kitManager.register(new Bird(kitService, plugin));
    kitManager.register(new Blaze(kitService, plugin));
    kitManager.register(new Bomber(kitService, plugin));
    kitManager.register(new Cactus(kitService, plugin));
    kitManager.register(new Cat(kitService, plugin));
    kitManager.register(new Dolphin(kitService, plugin));
    kitManager.register(new Enderman(kitService, plugin));
    kitManager.register(new ForestSpirit(kitService, plugin));
    kitManager.register(new Kangaroo(kitService, plugin));
    kitManager.register(new Knight(kitService, plugin));
    kitManager.register(new Lucker(kitService, plugin));
    kitManager.register(new Lumberjack(kitService, plugin));
    kitManager.register(new Mace(kitService, plugin));
    kitManager.register(new Rewind(kitService, plugin));
    kitManager.register(new Slime(kitService, plugin));
    kitManager.register(new Voodoo(kitService, plugin));
  }
}
