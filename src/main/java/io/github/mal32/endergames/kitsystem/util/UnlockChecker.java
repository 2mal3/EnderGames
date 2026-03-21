package io.github.mal32.endergames.kitsystem.util;

import io.github.mal32.endergames.kitsystem.api.AbstractKit;
import io.github.mal32.endergames.kitsystem.api.UnlockRequirement;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.advancement.Advancement;
import org.bukkit.entity.Player;

public final class UnlockChecker {
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
