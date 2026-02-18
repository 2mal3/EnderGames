package io.github.mal32.endergames.worlds.lobby.items;

import static org.apache.commons.lang3.StringUtils.capitalize;

import io.github.mal32.endergames.EnderGames;
import io.github.mal32.endergames.kits.AbstractKit;
import io.github.mal32.endergames.kits.KitDescription;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

class KitSelector extends MenuItem implements Listener {
  private final NamespacedKey kitStorageKey;
  private final List<AbstractKit> availableKits;

  public KitSelector(EnderGames plugin) {
    super(
        plugin,
        Material.CHEST,
        Component.text("Select Kit").color(NamedTextColor.GOLD),
        "kit_selector",
        (byte) 0);
    this.kitStorageKey = new NamespacedKey(plugin, "kit");
    this.availableKits = AbstractKit.getKits(plugin);
    Bukkit.getPluginManager().registerEvents(this, plugin);
  }

  @Override
  public void initPlayer(Player player) {
    giveItem(player);
  }

  @Override
  public void playerInteract(PlayerInteractEvent event) {
    Player player = event.getPlayer();

    player.playSound(player, Sound.BLOCK_CHEST_OPEN, 1, 1);
    String selectedKit =
        player.getPersistentDataContainer().get(kitStorageKey, PersistentDataType.STRING);
    KitInventory kiInv = new KitInventory(plugin, availableKits, selectedKit);
    player.openInventory(kiInv.getInventory());
  }

  @EventHandler
  public void onInventoryClick(InventoryClickEvent event) {
    Inventory clickedInv = event.getClickedInventory();
    if (clickedInv == null || !(clickedInv.getHolder() instanceof KitInventory)) return;
    event.setCancelled(true);

    ItemStack clickedItem = event.getCurrentItem();
    if (clickedItem == null
        || !clickedItem.hasItemMeta()
        || !clickedItem.getItemMeta().hasDisplayName()) return;

    Player player = (Player) event.getWhoClicked();
    Component displayName = clickedItem.getItemMeta().displayName();
    if (displayName == null) return;

    String nameText = LegacyComponentSerializer.legacySection().serialize(displayName);
    String kitName = nameText.length() > 2 ? nameText.substring(2).toLowerCase() : "";

    AbstractKit kit =
        availableKits.stream()
            .filter(k -> k.getNameLowercase().equalsIgnoreCase(kitName))
            .findFirst()
            .orElse(null);
    if (kit == null) {
      plugin.getComponentLogger().warn("Invalid kit selected: {}", kitName);
      return;
    }

    player.getPersistentDataContainer().set(kitStorageKey, PersistentDataType.STRING, kitName);
    player.sendMessage(
        Component.text("You selected the ")
            .append(Component.text(capitalize(kitName)).color(NamedTextColor.GOLD))
            .append(Component.text(" kit")));
    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1, 1);

    KitInventory kitInv = (KitInventory) event.getInventory().getHolder();
    kitInv.selectedKitName = kitName;
    kitInv.updateKitItems();
  }

  @EventHandler
  public void onInventoryDrag(InventoryDragEvent event) {
    if (!(event.getInventory().getHolder() instanceof KitInventory)) return;
    event.setCancelled(true);
  }
}

class KitInventory implements InventoryHolder {
  private final List<AbstractKit> availableKits;
  private final Inventory inventory;
  public String selectedKitName;

  public KitInventory(EnderGames plugin, List<AbstractKit> availableKits, String selectedKitName) {
    this.availableKits = availableKits;
    this.selectedKitName = selectedKitName;
    this.inventory = plugin.getServer().createInventory(this, 27, Component.text("Select Kit"));

    updateKitItems();
  }

  @Override
  public @NotNull Inventory getInventory() {
    return this.inventory;
  }

  public void updateKitItems() {
    inventory.clear();

    for (AbstractKit kit : availableKits) {
      var kitDescription = kit.getDescription();

      var kitItem = new ItemStack(kitDescription.item(), 1);
      var meta = kitItem.getItemMeta();

      meta.displayName(
          Component.text(kitDescription.name())
              .color(NamedTextColor.GOLD)
              .decoration(TextDecoration.ITALIC, false));
      meta.lore(getKitLore(kitDescription));

      kitItem.setItemMeta(meta);

      // Hightlight the selected kit with a glow effect
      if (kit.getNameLowercase().equals(selectedKitName)) {
        ItemMeta clickedMeta = kitItem.getItemMeta();
        clickedMeta.addEnchant(Enchantment.INFINITY, 1, true); // dummy enchantment for glow
        clickedMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        kitItem.setItemMeta(clickedMeta);
      }

      inventory.addItem(kitItem);
    }
  }

  private List<TextComponent> getKitLore(KitDescription kitDescription) {
    var lore = new ArrayList<TextComponent>();

    // Abilities
    var abilitiesHeaderComponent =
        Component.text("Abilities:")
            .color(NamedTextColor.GRAY)
            .decoration(TextDecoration.ITALIC, false);
    lore.add(abilitiesHeaderComponent);
    var abilitiesText = splitIntoLines(kitDescription.abilities());
    lore.addAll(convertTextListToComponents(abilitiesText));

    lore.add(Component.text(""));

    // Equipment
    if (kitDescription.equipment() != null) {
      var equipmentHeaderComponent =
          Component.text("Equipment:")
              .color(NamedTextColor.GRAY)
              .decoration(TextDecoration.ITALIC, false);
      lore.add(equipmentHeaderComponent);
      var equipmentText = splitIntoLines(kitDescription.equipment());
      lore.addAll(convertTextListToComponents(equipmentText));

      lore.add(Component.text(""));
    }

    // Difficulty
    lore.add(
        Component.text("Difficulty:")
            .color(NamedTextColor.GRAY)
            .decoration(TextDecoration.ITALIC, false));

    switch (kitDescription.difficulty()) {
      case EASY ->
          lore.add(
              Component.text("█▒▒ Easy")
                  .color(NamedTextColor.GREEN)
                  .decoration(TextDecoration.ITALIC, false));
      case MEDIUM ->
          lore.add(
              Component.text("██▒ Medium")
                  .color(NamedTextColor.YELLOW)
                  .decoration(TextDecoration.ITALIC, false));
      case HARD ->
          lore.add(
              Component.text("███ Hard")
                  .color(NamedTextColor.RED)
                  .decoration(TextDecoration.ITALIC, false));
    }

    return lore;
  }

  private static ArrayList<String> splitIntoLines(String text) {
    var lines = new ArrayList<String>();

    final int maxLineLength = 20;
    int charactersInLine = 0;
    int lineStartIndex = 0;
    for (int i = 0; i < text.length(); i++) {
      charactersInLine++;
      if (charactersInLine > maxLineLength && text.charAt(i) == ' ') {
        var line = text.substring(lineStartIndex, i);
        lines.add(line);
        charactersInLine = 0;
        lineStartIndex = i + 1;
      }
    }
    lines.add(text.substring(lineStartIndex));

    return lines;
  }

  private static List<TextComponent> convertTextListToComponents(ArrayList<String> lines) {
    return lines.stream()
        .map(
            line ->
                Component.text(line)
                    .color(NamedTextColor.WHITE)
                    .decoration(TextDecoration.ITALIC, false))
        .toList();
  }
}
