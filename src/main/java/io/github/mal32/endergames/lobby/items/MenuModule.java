package io.github.mal32.endergames.lobby.items;

import io.github.mal32.endergames.game.phases.GameEndEvent;
import io.github.mal32.endergames.game.phases.GameStartAbortEvent;
import io.github.mal32.endergames.game.phases.GameStartingEvent;
import io.github.mal32.endergames.lobby.LobbyModule;
import io.github.mal32.endergames.services.PlayerInWorld;
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
import org.bukkit.plugin.java.JavaPlugin;

public class MenuModule extends LobbyModule {
  private final HashMap<String, AbstractMenuItem> items = new HashMap<>();
  private final NamespacedKey menuKey;

  public MenuModule(JavaPlugin plugin) {
    super(plugin);

    this.menuKey = new NamespacedKey(plugin, "menu");
  }

  @Override
  public void onRegister() {
    super.onRegister();

    registerDefaultItems();
  }

  void registerDefaultItems() {
    List<AbstractMenuItem> rawItems =
        List.of(new KitSelector(plugin), new OperatorStartItem(plugin), new SpectatorItem(plugin));

    for (AbstractMenuItem item : rawItems) {
      registerItem(item);
    }
  }

  void registerItem(AbstractMenuItem item) {
    items.put(item.getKey(), item);
  }

  @Override
  public void onPlayerJoinLobby(Player player) {
    for (AbstractMenuItem item : items.values()) {
      item.initPlayer(player);
    }
  }

  private void forEachLobbyPlayer(Consumer<Player> action) {
    for (Player player : PlayerInWorld.LOBBY.all()) {
      action.accept(player);
    }
  }

  public void onGameStart(Player player) {
    for (AbstractMenuItem item : items.values()) {
      item.onGameStart(player);
    }
  }

  @EventHandler
  public void onGameStarting(GameStartingEvent ignoredE) {
    for (Player player : PlayerInWorld.LOBBY.all()) {
      onGameStart(player);
    }
  }

  public void onGameEnd(Player player) {
    for (AbstractMenuItem item : items.values()) {
      item.onGameEnd(player);
    }
  }

  @EventHandler
  public void onGameEnd(GameEndEvent ignoredE) {
    forEachLobbyPlayer(this::onGameEnd);
  }

  @EventHandler
  public void onGameStartAbort(GameStartAbortEvent ignoredE) {
    for (AbstractMenuItem item : items.values()) {
      item.onGameStartAbort();
    }
    forEachLobbyPlayer(
        player -> {
          for (AbstractMenuItem item : items.values()) {
            item.onGameStartAbort(player);
          }
        });
  }

  private boolean isMenuItem(ItemStack item) {
    if (item == null) return false;
    return item.getPersistentDataContainer().has(menuKey, PersistentDataType.STRING);
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
