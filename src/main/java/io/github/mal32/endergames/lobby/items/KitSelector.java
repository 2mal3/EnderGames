package io.github.mal32.endergames.lobby.items;

import io.github.mal32.endergames.kitsystem.UnlockChecker;
import io.github.mal32.endergames.kitsystem.api.AbstractKit;
import io.github.mal32.endergames.kitsystem.api.KitDescription;
import io.github.mal32.endergames.kitsystem.api.KitSystem;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

class KitSelector extends AbstractMenuItem implements Listener {
  private final NamespacedKey kitName;
  private final KitSystem kitSystem;

  public KitSelector(JavaPlugin plugin, KitSystem kitSystem) {
    super(
        plugin,
        (byte) 0,
        "kit_selector",
        Material.ENDER_CHEST,
        Component.text("Select Kit").color(NamedTextColor.GOLD));
    this.kitName = new NamespacedKey(plugin, "kit_name");
    this.kitSystem = kitSystem;
    Bukkit.getPluginManager().registerEvents(this, plugin);
  }

  @Override
  public void initPlayer(Player player) {
    this.giveItem(player);
  }

  @Override
  public void playerInteract(PlayerInteractEvent event) {
    Player player = event.getPlayer();

    player.playSound(player, Sound.BLOCK_CHEST_OPEN, 1, 1);
    KitInventory kiInv = new KitInventory(plugin, kitSystem, kitName, player);
    player.openInventory(kiInv.getInventory());
  }

  @EventHandler
  public void onInventoryClick(InventoryClickEvent event) {
    Inventory clickedInv = event.getClickedInventory();
    if (clickedInv == null || !(clickedInv.getHolder() instanceof KitInventory kitInv)) return;
    event.setCancelled(true);

    ItemStack clickedItem = event.getCurrentItem();
    if (clickedItem == null
        || !clickedItem.hasItemMeta()
        || !clickedItem.getItemMeta().getPersistentDataContainer().has(kitName)) return;

    Player player = (Player) event.getWhoClicked();

    final String rawKit =
        clickedItem
            .getItemMeta()
            .getPersistentDataContainer()
            .get(kitName, PersistentDataType.STRING);
    final Optional<AbstractKit> optionalKit = kitSystem.manager().get(rawKit);
    if (optionalKit.isEmpty()) {
      plugin
          .getComponentLogger()
          .warn("Invalid kit selected: {}", clickedItem.getItemMeta().displayName());
      return;
    }
    final AbstractKit kit = optionalKit.get();

    if (!UnlockChecker.isUnlocked(player, kit)) {
      player.sendMessage(
          Component.text("Unlock the matching advancement to use that kit.")
              .color(NamedTextColor.RED));
      player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
      return;
    }

    player.sendMessage(
        Component.text("You selected the ")
            .append(Component.text(kit.description().displayName()).color(NamedTextColor.GOLD))
            .append(Component.text(" kit")));
    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1, 1);
    kitSystem.service().set(player, kit);
    kitInv.displayKitItems();
  }

  @EventHandler
  public void onInventoryDrag(InventoryDragEvent event) {
    if (!(event.getInventory().getHolder() instanceof KitInventory)) return;
    event.setCancelled(true);
  }
}

class KitInventory implements InventoryHolder {
  private final Inventory inventory;
  private final KitSystem kitSystem;
  private final NamespacedKey kitName;
  private final Player player;

  public KitInventory(
      JavaPlugin plugin, KitSystem kitSystem, NamespacedKey kitName, Player player) {
    this.inventory = plugin.getServer().createInventory(this, 27, Component.text("Select Kit"));
    this.player = player;
    this.kitSystem = Objects.requireNonNull(kitSystem);
    this.kitName = Objects.requireNonNull(kitName);

    displayKitItems();
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

  @Override
  public @NotNull Inventory getInventory() {
    return this.inventory;
  }

  public void displayKitItems() {
    inventory.clear();

    final AbstractKit selectedKit = kitSystem.service().get(player);

    for (AbstractKit kit : kitSystem.manager().all()) {
      final KitItem abstractKitItem = getKitItem(kit);
      final ItemStack item = ItemStack.of(abstractKitItem.item());
      item.editMeta(
          meta -> {
            meta.displayName(abstractKitItem.name());
            meta.lore(abstractKitItem.lore());
            meta.getPersistentDataContainer().set(kitName, PersistentDataType.STRING, kit.id());
          });

      // Highlight the selected kit with a glow effect
      if (kit.equals(selectedKit)) {
        item.editMeta(meta -> meta.setEnchantmentGlintOverride(true));
      }

      inventory.addItem(item);
    }
  }

  private KitItem getKitItem(AbstractKit kit) {
    boolean kitUnlocked = UnlockChecker.isUnlocked(player, kit);
    final KitDescription description = kit.description();

    var lore = new ArrayList<TextComponent>();

    // Abilities
    var abilitiesHeaderComponent =
        Component.text("Abilities:")
            .color(NamedTextColor.GRAY)
            .decoration(TextDecoration.ITALIC, false);
    lore.add(abilitiesHeaderComponent);
    var abilitiesText = splitIntoLines(description.abilities());
    lore.addAll(convertTextListToComponents(abilitiesText));

    lore.add(Component.text(""));

    // Equipment
    if (!description.equipment().isBlank()) {
      var equipmentHeaderComponent =
          Component.text("Equipment:")
              .color(NamedTextColor.GRAY)
              .decoration(TextDecoration.ITALIC, false);
      lore.add(equipmentHeaderComponent);
      var equipmentText = splitIntoLines(description.equipment());
      lore.addAll(convertTextListToComponents(equipmentText));

      lore.add(Component.text(""));
    }

    // Difficulty
    lore.add(
        Component.text("Difficulty:")
            .color(NamedTextColor.GRAY)
            .decoration(TextDecoration.ITALIC, false));

    switch (description.difficulty()) {
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

    // Unlocked state
    if (!kitUnlocked) {
      lore.add(Component.text(""));
      lore.add(
          Component.text("Locked")
              .color(NamedTextColor.RED)
              .decoration(TextDecoration.ITALIC, false));
      lore.add(
          Component.text("See Advancement Tab")
              .color(NamedTextColor.RED)
              .decoration(TextDecoration.ITALIC, false));
    }

    var name =
        Component.text(description.displayName())
            .color(kitUnlocked ? NamedTextColor.GOLD : NamedTextColor.RED)
            .decoration(TextDecoration.ITALIC, false);

    return new KitItem(name, lore, kitUnlocked ? description.icon() : Material.BARRIER);
  }
}

record KitItem(TextComponent name, List<TextComponent> lore, Material item) {}
