package io.github.mal32.endergames.kitsystem.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation marking a kit as requiring an advancement to unlock.
 *
 * <p>The value must be a valid {@link org.bukkit.NamespacedKey} string. The advancement must exist
 * in the server's advancement registry.
 *
 * <p>Unlocking is checked via {@link io.github.mal32.endergames.kitsystem.util.UnlockChecker}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface UnlockRequirement {
  String advancement();
}
