package io.github.mal32.endergames.worlds.lobby;

import io.github.mal32.endergames.EnderGames;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;

abstract class MenuItem {
  protected final NamespacedKey namespaceKey;
  protected final EnderGames plugin;
  private final Material itemDisplay;
  private final String name;
  private final String key;
  private final byte slot;

  protected MenuItem(EnderGames plugin, Material itemDisplay, String name, String key, byte slot) {
    this.namespaceKey = new NamespacedKey(plugin, "menu_key");
    this.plugin = plugin;
    this.itemDisplay = itemDisplay;
    this.name = name;
    this.key = key;
    this.slot = slot;
  }

  public static HashMap<String, MenuItem> getItems(EnderGames plugin) {
    HashMap<String, MenuItem> menuItems = new HashMap<>();
    menuItems.put("start_game", new OperatorItem(plugin));
    menuItems.put("kit_selector", new KitSelector(plugin));
    return menuItems;
  }

  public void giveItem(Player player) {
    ItemStack startItem = new ItemStack(this.itemDisplay);
    ItemMeta meta = startItem.getItemMeta();
    if (meta == null) return;
    meta.displayName(Component.text(this.name));

    meta.getPersistentDataContainer().set(this.namespaceKey, PersistentDataType.STRING, this.key);

    startItem.setItemMeta(meta);
    player.getInventory().setItem(this.slot, startItem);
  }

  public abstract void playerInteract(PlayerInteractEvent event);
}
