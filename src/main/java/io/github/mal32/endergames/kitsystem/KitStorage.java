package io.github.mal32.endergames.kitsystem;

import io.github.mal32.endergames.kitsystem.api.AbstractKit;
import java.util.Objects;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

public class KitStorage {
  public static final NamespacedKey KIT_KEY = new NamespacedKey("enga", "kit");

  public static void setKit(Player player, AbstractKit kit) {
    Objects.requireNonNull(player);
    Objects.requireNonNull(kit);
    player.getPersistentDataContainer().set(KIT_KEY, PersistentDataType.STRING, kit.id());
  }

  public static @Nullable AbstractKit getKit(JavaPlugin plugin, Player player) {
    Objects.requireNonNull(player);
    final String id = player.getPersistentDataContainer().get(KIT_KEY, PersistentDataType.STRING);
    if (id == null) return null;
    var kits = KitRegisty.getKits(plugin);
    if (!kits.containsKey(id)) return null;
    return kits.get(id);
  }

  public static boolean isUsing(JavaPlugin plugin, Player player, AbstractKit kit) {
    final AbstractKit current = getKit(plugin, player);
    if (current == null) return false;
    return current.id().equals(kit.id());
  }
}
