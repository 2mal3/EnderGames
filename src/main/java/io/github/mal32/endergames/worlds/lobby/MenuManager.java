package io.github.mal32.endergames.worlds.lobby;

import io.github.mal32.endergames.EnderGames;
import java.util.HashMap;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.persistence.PersistentDataType;

class MenuManager implements Listener {
  protected final NamespacedKey namespaceKey;
  private final HashMap<String, MenuItem> items;

  public MenuManager(EnderGames plugin) {
    this.items = MenuItem.getItems(plugin);
    this.namespaceKey = new NamespacedKey(plugin, "menu_key");

    Bukkit.getPluginManager().registerEvents(this, plugin);
  }

  public MenuItem getItem(String item) {
    return this.items.get(item);
  }

  @EventHandler
  public void onPlayerInteract(PlayerInteractEvent event) {
    if (!EnderGames.playerIsInLobbyWorld(event.getPlayer())) return;
    if (event.getItem() == null) return;
    this.items
        .get(
            event
                .getItem()
                .getPersistentDataContainer()
                .get(this.namespaceKey, PersistentDataType.STRING))
        .playerInteract(event);

    event.setCancelled(true);
  }

  @EventHandler
  public void onInventoryClick(InventoryClickEvent event) {
    if (!(event.getWhoClicked() instanceof Player player)) return;
    if (!EnderGames.playerIsInLobbyWorld(player)) return;

    event.setCancelled(true);
  }

  @EventHandler
  public void onPlayerDropItem(PlayerDropItemEvent event) {
    if (!EnderGames.playerIsInLobbyWorld(event.getPlayer())) return;

    event.setCancelled(true);
  }

  @EventHandler
  public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent event) {
    if (!EnderGames.playerIsInLobbyWorld(event.getPlayer())) return;

    event.setCancelled(true);
  }
}
