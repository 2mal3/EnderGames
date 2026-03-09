package io.github.mal32.endergames.worlds.lobby.items;

import io.github.mal32.endergames.AbstractModule;
import io.github.mal32.endergames.EnderGames;
import io.github.mal32.endergames.services.PlayerInWorld;
import io.github.mal32.endergames.services.PlayerState;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;
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
    this.enable();

    var rawItems =
        List.of(
            new KitSelector(plugin),
            new OperatorStartItem(plugin),
            new SpectatorItem(plugin),
            new PlayItem(plugin));
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

  private void forEachLobbyPlayer(Consumer<Player> action) {
    for (Player player : PlayerInWorld.LOBBY.all()) {
      action.accept(player);
    }
  }

  public void onGameStart(Player player) {
    for (MenuItem item : items.values()) {
      item.onGameStart(player);
    }
  }

  public void onGameStart() {
    for (Player player : PlayerState.SKIP.all()) {
      onGameStart(player);
    }
  }

  public void onGameEnd(Player player) {
    for (MenuItem item : items.values()) {
      item.onGameEnd(player);
    }
  }

  public void onGameEnd() {
    forEachLobbyPlayer(this::onGameEnd);
  }

  public void onGameStartAbort() {
    for (MenuItem item : items.values()) {
      item.onGameStartAbort();
    }
    forEachLobbyPlayer(
        player -> {
          for (MenuItem item : items.values()) {
            item.onGameStartAbort(player);
          }
        });
  }

  private boolean isMenuItem(ItemStack item) {
    if (item == null) return false;
    return item.getPersistentDataContainer().has(this.menuKey, PersistentDataType.STRING);
  }

  @EventHandler
  private void onPlayerInteract(PlayerInteractEvent event) {
    var player = event.getPlayer();
    if (!PlayerInWorld.LOBBY.is(player)) return;

    ItemStack item = event.getItem();
    if (!isMenuItem(item)) return;
    String itemKey = item.getPersistentDataContainer().get(this.menuKey, PersistentDataType.STRING);
    items.get(itemKey).playerInteract(event);

    event.setCancelled(true);
  }

  @EventHandler
  private void onInventoryClick(InventoryClickEvent event) {
    if (!(event.getWhoClicked() instanceof Player player)) return;
    if (!PlayerInWorld.LOBBY.is(player)) return;

    ItemStack item = event.getCurrentItem();
    if (!isMenuItem(item)) return;

    event.setCancelled(true);
  }

  @EventHandler
  private void onPlayerDropItem(PlayerDropItemEvent event) {
    if (!PlayerInWorld.LOBBY.is(event.getPlayer())) return;

    ItemStack item = event.getItemDrop().getItemStack();
    if (!isMenuItem(item)) return;

    event.setCancelled(true);
  }

  @EventHandler
  public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent event) {
    if (!PlayerInWorld.LOBBY.is(event.getPlayer())) return;

    ItemStack item = event.getOffHandItem();
    if (!isMenuItem(item)) return;

    event.setCancelled(true);
  }
}
