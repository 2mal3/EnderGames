package io.github.mal32.endergames.kitsystem.api;

import io.github.lambdaphoenix.advancementLib.AdvancementAPI;

/**
 * Optional interface for kits that define custom advancement unlock conditions.
 *
 * <p>If a kit is annotated with {@link UnlockRequirement}, it may implement this interface to
 * register an advancement trigger using the external {@link AdvancementAPI}.
 *
 * <p>This is typically used for kits that require the player to perform a specific action (e.g. use
 * an item, kill mobs, break blocks) before the kit becomes available.
 */
public interface CustomAdvancementTrigger {
  /**
   * Registers the advancement trigger for this kit.
   *
   * @param api the advancement API used to build and register triggers
   */
  void registerAdvancement(AdvancementAPI api);
}
