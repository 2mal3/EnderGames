package io.github.mal32.endergames.kitsystem.api;

import java.util.*;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.Plugin;

public class KitManager {
  private final Map<String, AbstractKit> kits = new LinkedHashMap<>(32);
  private final Plugin plugin;
  private final Set<AbstractKit> active = new HashSet<>();

  public KitManager(Plugin plugin) {
    this.plugin = plugin;
  }

  public void register(AbstractKit kit) {
    kits.put(kit.id(), kit);
  }

  public Collection<AbstractKit> all() {
    return Collections.unmodifiableCollection(kits.values());
  }

  public AbstractKit get(String id) { // TODO: handle null
    return kits.get(id);
  }

  public void enableKit(AbstractKit kit) {
    Objects.requireNonNull(kit);
    Bukkit.getPluginManager().registerEvents(kit, plugin);
    kit.onEnable();
    active.add(kit);
  }

  public void disableKit(AbstractKit kit) {
    Objects.requireNonNull(kit);
    HandlerList.unregisterAll(kit);
    kit.onDisable();
    active.remove(kit);
  }

  public void disableAll() {
    active.forEach(this::disableKit);
  }
}
