package io.github.mal32.endergames.worlds.lobby.items;

import io.github.mal32.endergames.EnderGames;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

abstract class MenuItem {
  protected final EnderGames plugin;
  private final Material itemDisplay;
  private final Component name;
  private final String key;
  private final byte slot;

  protected MenuItem(
      EnderGames plugin, Material itemDisplay, Component name, String key, byte slot) {
    this.plugin = plugin;
    this.itemDisplay = itemDisplay;
    this.name = name;
    this.key = key;
    this.slot = slot;
  }

  public String getKey() {
    return key;
  }

  public abstract void initPlayer(Player player);

  public void giveItem(Player player) {
    ItemStack startItem = new ItemStack(this.itemDisplay);
    ItemMeta meta = startItem.getItemMeta();
    if (meta == null) return;
    meta.itemName(this.name);

    meta.getPersistentDataContainer()
        .set(new NamespacedKey(plugin, "menu"), PersistentDataType.STRING, this.key);

    startItem.setItemMeta(meta);
    player.getInventory().setItem(this.slot, startItem);
  }

  public abstract void playerInteract(PlayerInteractEvent event);
}
