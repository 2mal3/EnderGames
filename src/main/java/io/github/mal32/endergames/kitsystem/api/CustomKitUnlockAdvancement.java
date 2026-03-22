package io.github.mal32.endergames.kitsystem.api;

import io.github.lambdaphoenix.advancementLib.AdvancementAPI;

public interface CustomKitUnlockAdvancement extends KitUnlockAdvancement {
  /**
   * Registers the advancement trigger for this kit.
   *
   * @param api the advancement API used to build and register triggers
   */
  void registerAdvancement(AdvancementAPI api);
}
