package io.github.mal32.endergames.worlds.lobby;

import io.github.mal32.endergames.EnderGames;
import java.util.HashMap;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;

class MenuManager implements Listener {
  private final HashMap<Material, MenuItem> items;

  public MenuManager(EnderGames plugin) {
    this.items = MenuItem.getItems(plugin);

    Bukkit.getPluginManager().registerEvents(this, plugin);
  }

  public MenuItem getItem(Material item) {
    return this.items.get(item);
  }

  @EventHandler
  public void onPlayerInteract(PlayerInteractEvent event) {
    if (!EnderGames.playerIsInLobbyWorld(event.getPlayer())) return;
    if (event.getItem() == null) return;
    this.items.get(event.getItem().getType()).playerInteract(event);

    event.setCancelled(true);
  }

  // TODO: cancel drop item, swap item, etc.

}
