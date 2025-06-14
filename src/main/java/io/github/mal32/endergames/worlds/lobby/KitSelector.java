package io.github.mal32.endergames.worlds.lobby;

import static org.apache.commons.lang3.StringUtils.capitalize;

import io.github.mal32.endergames.EnderGames;
import io.github.mal32.endergames.kits.AbstractKit;
import io.github.mal32.endergames.kits.KitDescriptionItem;
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
import org.bukkit.advancement.Advancement;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

class KitSelector extends MenuItem {
  private final List<AbstractKit> availableKits;

  public KitSelector(EnderGames plugin) {
    super(plugin, Material.CHEST, "§6Select Kit", "kit_selector", (byte) 0);
    this.availableKits = AbstractKit.getKits(plugin);
  }

  @Override
  public void playerInteract(PlayerInteractEvent event) {
    Player player = event.getPlayer();
    openKitMenu(player);
  }

  public void openKitMenu(Player player) {
    player.playSound(player, Sound.BLOCK_CHEST_OPEN, 1, 1);
    KitInventory kiInv = new KitInventory(plugin, availableKits, player);
    player.openInventory(kiInv.getInventory());
  }
}

class KitInventory implements InventoryHolder, Listener {
  private final EnderGames plugin;
  private final List<AbstractKit> availableKits;
  private final Inventory inventory;
  private final Player player;
  private final NamespacedKey kitStorageKey;

  public KitInventory(EnderGames plugin, List<AbstractKit> availableKits, Player player) {
    this.plugin = plugin;
    this.availableKits = availableKits;
    this.player = player;
    this.inventory = plugin.getServer().createInventory(this, 27, Component.text("Select Kit"));
    this.kitStorageKey = new NamespacedKey(plugin, "kit");

    updateKitItems();

    Bukkit.getPluginManager().registerEvents(this, plugin);
  }

  @EventHandler
  public void onInventoryClick(InventoryClickEvent event) {
    Inventory inventory = event.getClickedInventory();
    if (inventory == null || !(inventory.getHolder() instanceof KitInventory kitInv)) return;
    event.setCancelled(true);

    ItemStack clickedItem = event.getCurrentItem();
    if (clickedItem == null
        || !clickedItem.hasItemMeta()
        || !clickedItem.getItemMeta().hasDisplayName()) return;

    Player player = (Player) event.getWhoClicked();
    Component displayNameComponent = clickedItem.getItemMeta().displayName();

    if (displayNameComponent == null) return;

    // Convert Component to plain string and strip formatting
    String displayName = LegacyComponentSerializer.legacySection().serialize(displayNameComponent);
    String kitName = displayName.length() > 2 ? displayName.substring(2).toLowerCase() : "";

    // Get the clicked kit
    AbstractKit matchedKit =
        availableKits.stream()
            .filter(kit -> kit.getName().equalsIgnoreCase(kitName))
            .findFirst()
            .orElse(null);
    if (matchedKit == null) {
      plugin.getComponentLogger().warn("Invalid kit selected: " + kitName);
      return;
    }

    // Check if the player has unlocked the kit
    Advancement kitAdvancement =
        plugin.getServer().getAdvancement(new NamespacedKey("enga", kitName));
    boolean kitUnlocked =
        kitAdvancement == null || player.getAdvancementProgress(kitAdvancement).isDone();
    if (!kitUnlocked) {
      player.sendMessage(
          Component.text()
              .append(Component.text("You haven't unlocked the ").color(NamedTextColor.RED))
              .append(Component.text(kitName).color(NamedTextColor.DARK_RED))
              .append(
                  Component.text(" kit yet. See the advancements tab.").color(NamedTextColor.RED)));
      player.playSound(player.getLocation(), Sound.ENTITY_GOAT_AMBIENT, 1, 1);
      return;
    }

    // Store kit in PersistentDataContainer
    player.getPersistentDataContainer().set(kitStorageKey, PersistentDataType.STRING, kitName);

    // Feedback + sound
    player.sendMessage(
        Component.text("You selected the ")
            .append(Component.text(capitalize(kitName)).color(NamedTextColor.GOLD))
            .append(Component.text(" kit")));
    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1, 1);

    // Update the item enchantements to show which item was selected
    updateKitItems();
  }

  @EventHandler
  public void onInventoryClose(InventoryCloseEvent event) {
    Inventory inventory = event.getInventory();
    if (!(inventory.getHolder() instanceof KitInventory)) return;

    HandlerList.unregisterAll(this);
  }

  @Override
  public @NotNull Inventory getInventory() {
    return this.inventory;
  }

  public void updateKitItems() {
    inventory.clear();

    String selectedKit =
        player.getPersistentDataContainer().get(kitStorageKey, PersistentDataType.STRING);

    for (AbstractKit kit : availableKits) {
      var kitDescription = kit.getDescriptionItem();

      var kitItem = new ItemStack(kitDescription.item, 1);
      var meta = kitItem.getItemMeta();

      meta.displayName(
          Component.text(kitDescription.name)
              .color(NamedTextColor.GOLD)
              .decoration(TextDecoration.ITALIC, false));
      meta.lore(getKitLore(kitDescription));

      kitItem.setItemMeta(meta);

      // Hightlight the selected kit with a glow effect
      if (kit.getName().equals(selectedKit)) {
        ItemMeta clickedMeta = kitItem.getItemMeta();
        clickedMeta.addEnchant(Enchantment.INFINITY, 1, true); // dummy enchantment for glow
        clickedMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        kitItem.setItemMeta(clickedMeta);
      }

      inventory.addItem(kitItem);
    }
  }

  private List<TextComponent> getKitLore(KitDescriptionItem kitDescription) {
    var lore = new ArrayList<TextComponent>();

    // Kit unlocked display
    Advancement kitAdvancement =
        plugin
            .getServer()
            .getAdvancement(new NamespacedKey("enga", kitDescription.name.toLowerCase()));
    boolean isKitUnlocked =
        kitAdvancement == null || player.getAdvancementProgress(kitAdvancement).isDone();
    if (!isKitUnlocked) {
      lore.add(
          Component.text("Not unlocked yet")
              .color(NamedTextColor.RED)
              .decoration(TextDecoration.ITALIC, false));
      lore.add(Component.text(""));
    }

    // Abilities
    var abilitiesHeaderComponent =
        Component.text("Abilities:")
            .color(NamedTextColor.GRAY)
            .decoration(TextDecoration.ITALIC, false);
    lore.add(abilitiesHeaderComponent);
    var abilitiesText = splitIntoLines(kitDescription.abilities);
    lore.addAll(convertTextListToComponents(abilitiesText));

    lore.add(Component.text(""));

    // Equipment
    if (kitDescription.equipment != null) {
      var equipmentHeaderComponent =
          Component.text("Equipment:")
              .color(NamedTextColor.GRAY)
              .decoration(TextDecoration.ITALIC, false);
      lore.add(equipmentHeaderComponent);
      var equipmentText = splitIntoLines(kitDescription.equipment);
      lore.addAll(convertTextListToComponents(equipmentText));

      lore.add(Component.text(""));
    }

    // Difficulty
    lore.add(
        Component.text("Difficulty:")
            .color(NamedTextColor.GRAY)
            .decoration(TextDecoration.ITALIC, false));

    switch (kitDescription.difficulty) {
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
