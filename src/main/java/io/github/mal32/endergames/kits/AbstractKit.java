package io.github.mal32.endergames.kits;

import java.util.Objects;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public abstract class AbstractKit implements Listener {
  JavaPlugin plugin;

  public AbstractKit(JavaPlugin plugin) {
    this.plugin = plugin;

    Bukkit.getPluginManager().registerEvents(this, plugin);
  }

  protected boolean playerHasKit(Player player) {
    return Objects.equals(
        player
            .getPersistentDataContainer()
            .get(new NamespacedKey(plugin, "kit"), PersistentDataType.STRING),
        getName());
  }

  public void stop() {
    HandlerList.unregisterAll(this);
  }

  public void start(Player player) {
    Bukkit.dispatchCommand(
        Bukkit.getConsoleSender(),
        "loot give " + player.getName() + " loot enga:kits/" + getName());
  }

  public String getName() {
    return this.getClass().getSimpleName().toLowerCase();
  }

  public abstract ItemStack getDescriptionItem();
}
