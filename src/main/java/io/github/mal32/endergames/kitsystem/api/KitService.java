package io.github.mal32.endergames.kitsystem.api;

import java.util.Objects;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

public class KitService {
  private final NamespacedKey kitKey;
  private final KitManager kitManager;

  public KitService(Plugin plugin, KitManager kitManager) {
    this.kitKey = new NamespacedKey(Objects.requireNonNull(plugin), "kit");
    this.kitManager = Objects.requireNonNull(kitManager);
  }

  public void set(Player player, AbstractKit kit) {
    Objects.requireNonNull(player);
    Objects.requireNonNull(kit);
    player.getPersistentDataContainer().set(kitKey, PersistentDataType.STRING, kit.id());
  }

  public AbstractKit get(Player player) {
    Objects.requireNonNull(player);
    final String id = player.getPersistentDataContainer().get(kitKey, PersistentDataType.STRING);
    return id == null ? null : kitManager.get(id);
  }

  public boolean playerHasKit(Player player) {
    return player.getPersistentDataContainer().has(kitKey);
  }
}
