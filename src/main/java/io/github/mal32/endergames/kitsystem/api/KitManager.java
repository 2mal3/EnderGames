package io.github.mal32.endergames.kitsystem.api;

import io.github.mal32.endergames.kitsystem.registry.KitValidator;
import java.util.*;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.Plugin;

public class KitManager {
  private final Map<String, AbstractKit> kits = new LinkedHashMap<>(32);
  private final Plugin plugin;

  public KitManager(Plugin plugin) {
    this.plugin = plugin;
  }

  public void register(AbstractKit kit) {
    KitValidator.validate(kit);
    kits.put(kit.id(), kit);
  }

  public Optional<AbstractKit> get(String id) {
    return Optional.ofNullable(kits.get(id));
  }

  public Collection<AbstractKit> all() {
    return Collections.unmodifiableCollection(kits.values());
  }

  public void enableKit(AbstractKit kit) {
    Objects.requireNonNull(kit);
    Bukkit.getPluginManager().registerEvents(kit, plugin);
    kit.onEnable();
  }

  public void disableKit(AbstractKit kit) {
    Objects.requireNonNull(kit);
    HandlerList.unregisterAll(kit);
    kit.onDisable();
  }

  public void disableAll() {
    new ArrayList<>(kits.values()).forEach(this::disableKit);
  }
}
