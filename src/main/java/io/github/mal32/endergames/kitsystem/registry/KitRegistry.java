package io.github.mal32.endergames.kitsystem.registry;

import io.github.mal32.endergames.EnderGames;
import io.github.mal32.endergames.kitsystem.api.KitManager;
import io.github.mal32.endergames.kitsystem.api.KitService;
import io.github.mal32.endergames.kitsystem.kits.*;

/**
 * Central registry for all available kits in the EnderGames plugin.
 *
 * <p>This class is responsible for instantiating every kit and registering it with the global
 * {@link KitManager}. It is invoked once during plugin initialization to make all kits available to
 * the kit system.
 *
 * <h2>Responsibilities</h2>
 *
 * <ul>
 *   <li>Create instances of all kit classes
 *   <li>Register each kit with the {@link KitManager}
 *   <li>Provide the {@link KitService} instance to each kit
 * </ul>
 *
 * <p>All kits should be added here to become selectable and usable in-game.
 */
public final class KitRegistry {
  /**
   * Registers all available kits with the plugin's {@link KitManager}.
   *
   * @param plugin the main plugin instance providing access to the kit system
   */
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
    kitManager.register(new Spy(kitService, plugin));
    kitManager.register(new Spectator(kitService, plugin));
  }
}
