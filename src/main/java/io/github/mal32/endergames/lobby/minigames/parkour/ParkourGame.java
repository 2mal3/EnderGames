package io.github.mal32.endergames.lobby.minigames.parkour;

import io.github.mal32.endergames.lobby.LobbyModule;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public class ParkourGame extends LobbyModule {
  private final ParkourManager manager;

  public ParkourGame(JavaPlugin plugin) {
    super(plugin);

    this.manager = new ParkourManager(plugin);
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

  @Override
  public void onDisable() {
    manager.shutdown();
    super.onDisable();
  }
}
