package io.github.mal32.endergames.worlds.lobby.items;

import io.github.mal32.endergames.EnderGames;
import java.util.Map;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

abstract class MenuItem {
  private final Map<String, ItemDisplay> display;

  protected final EnderGames plugin;
  protected final byte slot;

  protected MenuItem(EnderGames plugin, byte slot, String key, Map<String, ItemDisplay> display) {
    this.plugin = plugin;
    this.display = display;
    this.key = key;
    this.slot = slot;
  }

  private final String key;

  protected MenuItem(EnderGames plugin, byte slot, String key, Material display, Component name) {
    this.plugin = plugin;
    this.display = Map.of("", new ItemDisplay(display, name));
    this.key = key;
    this.slot = slot;
  }

  public void giveItem(Player player) {
    final String key = getState(player);
    assert this.display.containsKey(key);
    ItemStack item = new ItemStack(this.display.get(key).display);
    ItemMeta meta = item.getItemMeta();
    if (meta == null) return;
    meta.itemName(this.display.get(key).name);

    meta.getPersistentDataContainer()
        .set(new NamespacedKey(plugin, "menu"), PersistentDataType.STRING, this.key);

    item.setItemMeta(meta);
    player.getInventory().setItem(this.slot, item);
  }

  public String getKey() {
    return key;
  }

  public abstract void initPlayer(Player player);

  protected @NotNull String getState(Player player) {
    return "";
  }

  record ItemDisplay(Material display, Component name) {}

  public abstract void playerInteract(PlayerInteractEvent event);

  public void onGameEnd(Player player) {}
}
