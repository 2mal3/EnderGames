package io.github.mal32.endergames.kitsystem.registry;

import io.github.mal32.endergames.kitsystem.api.AbstractKit;
import io.github.mal32.endergames.kitsystem.api.UnlockRequirement;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;

public final class KitValidator {
  public static void validate(AbstractKit kit) {
    if (kit.id() == null || kit.id().isBlank()) {
      throw new IllegalStateException("Kit has no valid id: " + kit.getClass().getName());
    }

    final UnlockRequirement req = kit.getClass().getAnnotation(UnlockRequirement.class);
    if (req != null) {
      final NamespacedKey key = NamespacedKey.fromString(req.advancement());
      if (key == null || Bukkit.getAdvancement(key) == null)
        throw new IllegalArgumentException(
            "Invalid advancement in Kit " + kit.id() + ": " + req.advancement());
    }
  }
}
