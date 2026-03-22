package io.github.mal32.endergames.kitsystem.registry;

import io.github.mal32.endergames.kitsystem.api.AbstractKit;
import io.github.mal32.endergames.kitsystem.api.KitUnlockAdvancement;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;

/**
 * Validates kits before they are registered in the {@link
 * io.github.mal32.endergames.kitsystem.api.KitManager}.
 *
 * <p>The validator ensures:
 *
 * <ul>
 *   <li>The kit has a non-null, non-empty ID
 *   <li>If the kit has an {@link UnlockRequirement}, the advancement key is valid
 *   <li>The referenced advancement exists on the server
 * </ul>
 *
 * <p>Invalid kits cause registration to fail with an exception.
 */
public final class KitValidator {
  /**
   * Validates the given kit.
   *
   * <p>This method checks:
   *
   * <ul>
   *   <li>that {@link AbstractKit#id()} is not null or blank
   *   <li>that any {@link UnlockRequirement} annotation contains a valid {@link NamespacedKey}
   *   <li>that the referenced advancement exists in the server registry
   * </ul>
   *
   * @param kit the kit to validate
   * @throws IllegalStateException if the kit ID is null or blank
   * @throws IllegalArgumentException if the advancement key is invalid or the advancement does not
   *     exist
   */
  public static void validate(AbstractKit kit) {
    if (kit.id() == null || kit.id().isBlank()) {
      throw new IllegalStateException("Kit has no valid id: " + kit.getClass().getName());
    }

    if (kit instanceof KitUnlockAdvancement advancementKit) {
      final NamespacedKey key = NamespacedKey.fromString(advancementKit.getKitAdvancementKey());
      if (key == null || Bukkit.getAdvancement(key) == null)
        throw new IllegalArgumentException(
            "Invalid advancement in Kit "
                + kit.id()
                + ": "
                + advancementKit.getKitAdvancementKey());
    }
  }
}
