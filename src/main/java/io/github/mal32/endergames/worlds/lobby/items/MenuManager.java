package io.github.mal32.endergames.worlds.lobby.items;

import io.github.mal32.endergames.AbstractModule;
import io.github.mal32.endergames.EnderGames;
import java.util.HashMap;
import java.util.List;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

public class MenuManager extends AbstractModule {
  private final HashMap<String, MenuItem> items = new HashMap<>();
  private final NamespacedKey menuKey;

  public MenuManager(EnderGames plugin) {
    super(plugin);
    enable();

    var rawItems = List.of(new KitSelector(plugin), new OperatorStartItem(plugin));
    for (MenuItem item : rawItems) {
      items.put(item.getKey(), item);
    }

    this.menuKey = new NamespacedKey(plugin, "menu");
  }

  public void initPlayer(Player player) {
    for (MenuItem item : items.values()) {
      item.initPlayer(player);
    }
  }

  private boolean isMenuItem(ItemStack item) {
    if (item == null) return false;
    return item.getPersistentDataContainer().has(this.menuKey, PersistentDataType.STRING);
  }

  @EventHandler
  private void onPlayerInteract(PlayerInteractEvent event) {
    var player = event.getPlayer();
    if (!EnderGames.playerIsInLobbyWorld(player)) return;

    ItemStack item = event.getItem();
    if (!isMenuItem(item)) return;
    String itemKey = item.getPersistentDataContainer().get(this.menuKey, PersistentDataType.STRING);
    items.get(itemKey).playerInteract(event);

    event.setCancelled(true);
  }

  @EventHandler
  private void onInventoryClick(InventoryClickEvent event) {
    if (!(event.getWhoClicked() instanceof Player player)) return;
    if (!EnderGames.playerIsInLobbyWorld(player)) return;

    ItemStack item = event.getCurrentItem();
    if (!isMenuItem(item)) return;

    event.setCancelled(true);
  }

  @EventHandler
  private void onPlayerDropItem(PlayerDropItemEvent event) {
    if (!EnderGames.playerIsInLobbyWorld(event.getPlayer())) return;

    ItemStack item = event.getItemDrop().getItemStack();
    if (!isMenuItem(item)) return;

    event.setCancelled(true);
  }

  @EventHandler
  public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent event) {
    if (!EnderGames.playerIsInLobbyWorld(event.getPlayer())) return;

    ItemStack item = event.getOffHandItem();
    if (!isMenuItem(item)) return;

    event.setCancelled(true);
  }
}
