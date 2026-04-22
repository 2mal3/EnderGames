package io.github.mal32.endergames.lobby.items;

import java.util.Map;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

abstract class AbstractMenuItem {
  protected final JavaPlugin plugin;
  protected final byte slot;
  private final Map<String, ItemDisplay> display;
  private final String key;

  protected AbstractMenuItem(
      JavaPlugin plugin, byte slot, String key, Map<String, ItemDisplay> display) {
    this.plugin = plugin;
    this.display = display;
    this.key = key;
    this.slot = slot;
  }

  protected AbstractMenuItem(
      JavaPlugin plugin, byte slot, String key, Material display, Component name) {
    this.plugin = plugin;
    this.display = Map.of("", new ItemDisplay(display, name));
    this.key = key;
    this.slot = slot;
  }

  public void giveItem(Player player) {
    final String key = getState(player);
    assert this.display.containsKey(key);
    ItemStack item = ItemStack.of(this.display.get(key).display);
    ItemMeta meta = item.getItemMeta();
    if (meta == null) return;
    meta.itemName(this.display.get(key).name);
    if (this.display.get(key).itemModelKey != null) {
      meta.setItemModel(this.display.get(key).itemModelKey);
    }

    meta.getPersistentDataContainer()
        .set(new NamespacedKey(plugin, "menu"), PersistentDataType.STRING, this.key);

    item.setItemMeta(meta);
    player.getInventory().setItem(this.slot, item);
  }

  public String getKey() {
    return key;
  }

  protected @NotNull String getState(Player player) {
    return "";
  }

  public abstract void playerInteract(PlayerInteractEvent event);

  public void initPlayer(Player player) {}

  public void onGameStart(Player player) {}

  public void onGameEnd(Player player) {}

  public void onGameStartAbort() {}

  public void onGameStartAbort(Player player) {}

  record ItemDisplay(Material display, Component name, NamespacedKey itemModelKey) {
    ItemDisplay(Material display, Component name) {
      this(display, name, null);
    }
  }
}
