package io.github.mal32.endergames.worlds.lobby;

import static org.apache.commons.lang3.StringUtils.capitalize;

import io.github.mal32.endergames.EnderGames;
import io.github.mal32.endergames.kits.AbstractKit;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

class KitSelector implements Listener {
  private final EnderGames plugin;
  private final List<AbstractKit> availablekits;

  public KitSelector(EnderGames plugin) {
    this.plugin = plugin;
    this.availablekits = AbstractKit.getKits(plugin);
    Bukkit.getPluginManager().registerEvents(this, plugin);
  }

  public void enable() {
    Bukkit.getPluginManager().registerEvents(this, this.plugin);
  }

  public void disable() {
    HandlerList.unregisterAll(this);
  }

  public void giveKitSelector(Player player) {
    ItemStack chestItem = new ItemStack(Material.CHEST); // Chest item
    ItemMeta meta = chestItem.getItemMeta();
    if (meta != null) {
      meta.displayName(Component.text("§6Select Kit"));
      chestItem.setItemMeta(meta);
    }
    player.getInventory().addItem(chestItem);
  }

  @EventHandler
  public void onPlayerInteract(PlayerInteractEvent event) {
    if (event.getItem() != null && event.getItem().getType() == Material.CHEST) {
      ItemMeta meta = event.getItem().getItemMeta();
      if (meta == null || meta.displayName() == null) {
        return;
      }
      String displayName = PlainTextComponentSerializer.plainText().serialize(meta.displayName());
      if (displayName.equals("§6Select Kit")) {
        Player player = event.getPlayer();
        openKitMenu(player);
      }
    }
  }

  @EventHandler
  public void onInventoryClick(InventoryClickEvent event) {
    Inventory inventory = event.getClickedInventory();
    if (inventory == null || !(inventory.getHolder(false) instanceof KitInventory kitInv)) {
      return;
    }
    event.setCancelled(true);

    ItemStack clicked = event.getCurrentItem();
    if (clicked == null || !clicked.hasItemMeta() || !clicked.getItemMeta().hasDisplayName()) {
      return;
    }

    Player player = (Player) event.getWhoClicked();
    Component displayNameComponent = clicked.getItemMeta().displayName();

    if (displayNameComponent == null) {
      return;
    }

    // Convert Component to plain string and strip formatting
    String displayName = LegacyComponentSerializer.legacySection().serialize(displayNameComponent);
    String kitName = displayName.length() > 2 ? displayName.substring(2).toLowerCase() : "";

    // Optional: check if the kit is valid (from your list)
    AbstractKit matchedKit =
        availablekits.stream()
            .filter(kit -> kit.getName().equalsIgnoreCase(kitName))
            .findFirst()
            .orElse(null);

    if (matchedKit == null) {
      player.sendMessage("Invalid kit: " + kitName);
      return;
    }
    // Feedback + sound
    player.sendMessage(
        Component.text("You selected the ")
            .append(Component.text(capitalize(kitName)).color(NamedTextColor.GOLD))
            .append(Component.text(" kit")));
    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1, 1);

    // Store kit in PersistentDataContainer
    NamespacedKey key = new NamespacedKey(plugin, "kit");
    player.getPersistentDataContainer().set(key, PersistentDataType.STRING, kitName);

    for (int i = 0; i < inventory.getSize(); i++) {
      ItemStack item = inventory.getItem(i);
      if (item == null || !item.hasItemMeta()) continue;

      ItemMeta meta = item.getItemMeta();

      // Remove enchantments and glowing effect
      meta.getEnchants().forEach((enchant, level) -> meta.removeEnchant(enchant));
      meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

      item.setItemMeta(meta);
      inventory.setItem(i, item);
    }

    // Apply enchantment to the selected item
    ItemStack clickedItem = event.getCurrentItem();
    if (clickedItem != null && clickedItem.getType() != Material.AIR) {
      ItemMeta clickedMeta = clickedItem.getItemMeta();
      clickedMeta.addEnchant(Enchantment.INFINITY, 1, true); // dummy enchantment for glow
      clickedMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
      clickedItem.setItemMeta(clickedMeta);
    }
  }

  public void openKitMenu(Player player) {
    player.playSound(player, Sound.BLOCK_CHEST_OPEN, 1, 1);
    KitInventory kitinv = new KitInventory(plugin, availablekits, player);
    player.openInventory(kitinv.getInventory());
  }

  static class KitInventory implements InventoryHolder {
    private final EnderGames plugin;
    private final List<AbstractKit> availablekits;
    private final Inventory inventory;
    private final Player player;

    public KitInventory(EnderGames plugin, List<AbstractKit> availablekits, Player player) {
      this.plugin = plugin;
      this.availablekits = availablekits;
      this.player = player;
      this.inventory = plugin.getServer().createInventory(this, 27, Component.text("§0Select Kit"));
      fill_chest_with_kits();
    }

    @Override
    public @NotNull Inventory getInventory() {
      return this.inventory;
    }

    public void fill_chest_with_kits() {
      for (int i = 0; i < this.availablekits.size(); i++) {
        ItemStack descItem = availablekits.get(i).getDescriptionItem();
        Component displayNameComponent = descItem.getItemMeta().displayName();
        // Convert Component to plain string and strip formatting
        String displayName =
            LegacyComponentSerializer.legacySection().serialize(displayNameComponent);
        String kitName = displayName.length() > 2 ? displayName.substring(2).toLowerCase() : "";
        NamespacedKey key = new NamespacedKey(plugin, "kit");
        String selected = player.getPersistentDataContainer().get(key, PersistentDataType.STRING);
        if (kitName.equals(selected)) {
          ItemMeta clickedMeta = descItem.getItemMeta();
          clickedMeta.addEnchant(Enchantment.INFINITY, 1, true); // dummy enchantment for glow
          clickedMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
          descItem.setItemMeta(clickedMeta);
        }
        inventory.setItem(i, descItem);
      }
    }
  }
}
