package io.github.mal32.endergames.kitsystem.util;

import io.github.mal32.endergames.kitsystem.api.AbstractKit;
import io.github.mal32.endergames.kitsystem.api.UnlockRequirement;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.advancement.Advancement;
import org.bukkit.entity.Player;

/**
 * Utility class for checking whether a player has unlocked a kit.
 *
 * <p>Kits may be annotated with {@link UnlockRequirement}, specifying an advancement that must be
 * completed before the kit becomes available. This class resolves the advancement and checks the
 * player's progress.
 */
public final class UnlockChecker {

  /**
   * Determines whether the given player has unlocked the specified kit.
   *
   * <p>The unlock logic works as follows:
   *
   * <ol>
   *   <li>If the kit has no {@link UnlockRequirement}, it is always unlocked.
   *   <li>If the advancement key is invalid, the kit is considered locked.
   *   <li>If the advancement does not exist on the server, the kit is considered locked.
   *   <li>If the player has not completed the advancement, the kit is locked.
   *   <li>Otherwise, the kit is unlocked.
   * </ol>
   *
   * @param player the player to check
   * @param kit the kit whose unlock status should be evaluated
   * @return true if the kit is unlocked for the player
   */
  public static boolean isUnlocked(Player player, AbstractKit kit) {
    UnlockRequirement req = kit.getClass().getAnnotation(UnlockRequirement.class);
    if (req == null) return true;

    NamespacedKey key = NamespacedKey.fromString(req.advancement());
    if (key == null) return false;

    Advancement adv = Bukkit.getAdvancement(key);
    if (adv == null) return false;

    return player.getAdvancementProgress(adv).isDone();
  }
}
