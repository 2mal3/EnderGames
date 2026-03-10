package io.github.mal32.endergames.minigames;

import io.github.mal32.endergames.EnderGames;
import io.github.mal32.endergames.worlds.lobby.ParkourManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class PakourGame implements MiniGame, Listener {
  private final EnderGames plugin;
  private final ParkourManager manager;

  public PakourGame(EnderGames plugin) {
    this.plugin = plugin;
    this.manager = new ParkourManager(plugin);
  }

  @Override
  public void enable() {
    Bukkit.getPluginManager().registerEvents(this, plugin);
  }

  @Override
  public void disable() {
    HandlerList.unregisterAll(this);
  }

  @Override
  public boolean isActive(Player player) {
    return manager.isInParkour(player);
  }

  @EventHandler
  public void onPlayerInteract(PlayerInteractEvent event) {
    if (event.getHand() != EquipmentSlot.HAND) return;

    ItemStack item = event.getItem();
    if (manager.isResetItem(item)) {
      event.setCancelled(true);
      Player p = event.getPlayer();
      manager.resetPlayer(p);
    } else if (manager.isCancelItem(item)) {
      event.setCancelled(true);
      Player p = event.getPlayer();
      manager.abortParkour(p);
    }
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent e) {
    manager.abortParkour(e.getPlayer());
  }

  @EventHandler
  public void onInventoryClick(InventoryClickEvent event) {
    ItemStack item = event.getCurrentItem();
    if (item != null && (manager.isResetItem(item) || manager.isCancelItem(item))) {
      event.setCancelled(true);
    }
  }

  @EventHandler
  public void onInventoryDrag(InventoryDragEvent event) {
    ItemStack item = event.getOldCursor();
    if (manager.isResetItem(item) || manager.isCancelItem(item)) {
      event.setCancelled(true);
    }
  }

  @EventHandler
  public void onPlayerDrop(PlayerDropItemEvent event) {
    ItemStack item = event.getItemDrop().getItemStack();
    if (manager.isResetItem(item) || manager.isCancelItem(item)) {
      event.setCancelled(true);
    }
  }
}
