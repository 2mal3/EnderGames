package io.github.mal32.endergames.worlds.lobby.items;

import static org.apache.commons.lang3.StringUtils.capitalize;

import io.github.mal32.endergames.EnderGames;
import io.github.mal32.endergames.kits.AbstractKit;
import io.github.mal32.endergames.kits.KitDescription;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
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
  private static final Component LOCKED_KIT_MESSAGE =
      Component.text("Unlock the corresponding advancement to use this kit")
          .color(NamedTextColor.RED)
          .decoration(TextDecoration.ITALIC, false);
  private static final float LOCKED_KIT_SOUND_VOLUME = 1f;
  private static final float LOCKED_KIT_SOUND_PITCH = 1f;
  private static Sound lockedKitSound;

  private final NamespacedKey kitStorageKey;
  private final List<AbstractKit> availableKits;
  private final Set<String> warnedMissingAdvancements;

  public KitSelector(EnderGames plugin) {
    super(
        plugin,
        Material.CHEST,
        Component.text("Select Kit").color(NamedTextColor.GOLD),
        "kit_selector",
        (byte) 0);
    this.kitStorageKey = new NamespacedKey(plugin, "kit");
    this.availableKits = AbstractKit.getKits(plugin);
    this.warnedMissingAdvancements = new HashSet<>();
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
    KitInventory kiInv = new KitInventory(plugin, this, availableKits, selectedKit, player);
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

    if (!hasUnlockedKit(player, kit)) {
      sendLockedKitFeedback(player);
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

  private static NamespacedKey resolveKitAdvancement(AbstractKit kit) {
    String kitName = kit.getClass().getSimpleName().toLowerCase(Locale.ROOT);
    return new NamespacedKey("enga", kitName);
  }

  private Advancement getAdvancement(AbstractKit kit) {
    NamespacedKey key = resolveKitAdvancement(kit);
    Advancement advancement = Bukkit.getAdvancement(key);
    if (advancement == null) warnMissingAdvancement(kit, key);
    return advancement;
  }

  private boolean hasUnlockedKit(Player player, AbstractKit kit) {
    Advancement advancement = getAdvancement(kit);
    if (advancement == null) return false;
    return player.getAdvancementProgress(advancement).isDone();
  }

  private void warnMissingAdvancement(AbstractKit kit, NamespacedKey key) {
    String kitName = kit.getClass().getSimpleName();
    if (warnedMissingAdvancements.add(kitName)) {
      plugin
          .getComponentLogger()
          .warn("Missing advancement {} for kit {}", key.asString(), kitName);
    }
  }

  private void sendLockedKitFeedback(Player player) {
    player.playSound(
        player.getLocation(), getLockedKitSound(), LOCKED_KIT_SOUND_VOLUME, LOCKED_KIT_SOUND_PITCH);
    player.sendMessage(LOCKED_KIT_MESSAGE);
  }

  private static Sound getLockedKitSound() {
    if (lockedKitSound == null) {
      lockedKitSound = Sound.ENTITY_VILLAGER_NO;
    }
    return lockedKitSound;
  }

  private static class KitInventory implements InventoryHolder {
    private final EnderGames plugin;
    private final KitSelector selector;
    private final List<AbstractKit> availableKits;
    private final Inventory inventory;
    private final Player player;
    String selectedKitName;

    KitInventory(
        EnderGames plugin,
        KitSelector selector,
        List<AbstractKit> availableKits,
        String selectedKitName,
        Player player) {
      this.plugin = plugin;
      this.selector = selector;
      this.availableKits = availableKits;
      this.selectedKitName = selectedKitName;
      this.player = player;
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
        boolean unlocked = selector.hasUnlockedKit(player, kit);

        var kitItem = new ItemStack(kitDescription.item(), 1);
        var meta = kitItem.getItemMeta();

        var lore = new ArrayList<Component>(getKitLore(kitDescription));
        if (!unlocked) lore.add(LOCKED_KIT_MESSAGE);

        meta.displayName(
            Component.text(kitDescription.name())
                .color(NamedTextColor.GOLD)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);

        kitItem.setItemMeta(meta);

        if (kit.getNameLowercase().equals(selectedKitName)) {
          ItemMeta clickedMeta = kitItem.getItemMeta();
          clickedMeta.addEnchant(Enchantment.INFINITY, 1, true);
          clickedMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
          kitItem.setItemMeta(clickedMeta);
        }

        inventory.addItem(kitItem);
      }
    }

    private List<TextComponent> getKitLore(KitDescription kitDescription) {
      var lore = new ArrayList<TextComponent>();

      var abilitiesHeaderComponent =
          Component.text("Abilities:")
              .color(NamedTextColor.GRAY)
              .decoration(TextDecoration.ITALIC, false);
      lore.add(abilitiesHeaderComponent);
      var abilitiesText = splitIntoLines(kitDescription.abilities());
      lore.addAll(convertTextListToComponents(abilitiesText));

      lore.add(Component.text(""));

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
}
