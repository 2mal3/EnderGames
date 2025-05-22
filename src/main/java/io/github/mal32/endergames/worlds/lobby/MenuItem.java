package io.github.mal32.endergames.worlds.lobby;

import io.github.mal32.endergames.EnderGames;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;

abstract class MenuItem {
  protected final EnderGames plugin;
  private final Material itemDisplay;
  private final String name;
  private final byte slot;

  protected MenuItem(EnderGames plugin, Material itemDisplay, String name, byte slot) {
    this.plugin = plugin;
    this.itemDisplay = itemDisplay;
    this.name = name;
    this.slot = slot;
  }

  public static HashMap<Material, MenuItem> getItems(EnderGames plugin) {
    HashMap<Material, MenuItem> menuItems = new HashMap<>();
    menuItems.put(Material.NETHER_STAR, new OperatorItem(plugin));
    return menuItems;
  }

  public void giveItem(Player player) {
    ItemStack startItem = new ItemStack(this.itemDisplay);
    ItemMeta meta = startItem.getItemMeta();
    if (meta == null) return;
    meta.displayName(Component.text(this.name));
    startItem.setItemMeta(meta);
    player.getInventory().setItem(this.slot, startItem);
  }

  public abstract void playerInteract(PlayerInteractEvent event);
}
