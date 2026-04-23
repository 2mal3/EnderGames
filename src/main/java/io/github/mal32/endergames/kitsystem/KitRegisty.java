package io.github.mal32.endergames.kitsystem;

import io.github.mal32.endergames.kitsystem.api.AbstractKit;
import io.github.mal32.endergames.kitsystem.api.KitUnlockAdvancement;
import io.github.mal32.endergames.kitsystem.kits.Barbarian;
import io.github.mal32.endergames.kitsystem.kits.Bird;
import io.github.mal32.endergames.kitsystem.kits.Blaze;
import io.github.mal32.endergames.kitsystem.kits.Bomber;
import io.github.mal32.endergames.kitsystem.kits.Cactus;
import io.github.mal32.endergames.kitsystem.kits.Cat;
import io.github.mal32.endergames.kitsystem.kits.Dolphin;
import io.github.mal32.endergames.kitsystem.kits.Enderman;
import io.github.mal32.endergames.kitsystem.kits.ForestSpirit;
import io.github.mal32.endergames.kitsystem.kits.Kangaroo;
import io.github.mal32.endergames.kitsystem.kits.Knight;
import io.github.mal32.endergames.kitsystem.kits.Lucker;
import io.github.mal32.endergames.kitsystem.kits.Lumberjack;
import io.github.mal32.endergames.kitsystem.kits.Mace;
import io.github.mal32.endergames.kitsystem.kits.Rewind;
import io.github.mal32.endergames.kitsystem.kits.Slime;
import io.github.mal32.endergames.kitsystem.kits.Spectator;
import io.github.mal32.endergames.kitsystem.kits.Spy;
import io.github.mal32.endergames.kitsystem.kits.Voodoo;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

public class KitRegisty {
  public static final List<AbstractKit> getKitsList(JavaPlugin plugin) {
    return List.of(
        new Barbarian(plugin),
        new Bird(plugin),
        new Blaze(plugin),
        new Bomber(plugin),
        new Cactus(plugin),
        new Cat(plugin),
        new Dolphin(plugin),
        new Enderman(plugin),
        new ForestSpirit(plugin),
        new Kangaroo(plugin),
        new Knight(plugin),
        new Lucker(plugin),
        new Lumberjack(plugin),
        new Mace(plugin),
        new Rewind(plugin),
        new Slime(plugin),
        new Voodoo(plugin),
        new Spy(plugin),
        new Spectator(plugin));
  }

  public static final Map<String, AbstractKit> getKits(JavaPlugin plugin) {
    HashMap<String, AbstractKit> kitMap = new HashMap<>();
    for (AbstractKit kit : getKitsList(plugin)) {
      validate(kit);
      kitMap.put(kit.id(), kit);
    }
    return kitMap;
  }

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
