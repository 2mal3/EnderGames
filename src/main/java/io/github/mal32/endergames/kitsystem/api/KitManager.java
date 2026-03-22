package io.github.mal32.endergames.kitsystem.api;

import io.github.mal32.endergames.kitsystem.registry.KitValidator;
import java.util.*;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.Plugin;

public class KitManager {
  private final Map<String, AbstractKit> kits = new LinkedHashMap<>(32);
  private final Plugin plugin;
  private final Set<AbstractKit> activeKits = new HashSet<>();

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
    if (activeKits.contains(kit)) return;
    Bukkit.getPluginManager().registerEvents(kit, plugin);
    kit.onEnable();
    activeKits.add(kit);
  }

  public void disableKit(AbstractKit kit) {
    Objects.requireNonNull(kit);
    if (!activeKits.contains(kit)) return;
    HandlerList.unregisterAll(kit);
    kit.onDisable();
    activeKits.remove(kit);
  }

  public void disableAll() {
    new ArrayList<>(activeKits).forEach(this::disableKit);
  }
}
