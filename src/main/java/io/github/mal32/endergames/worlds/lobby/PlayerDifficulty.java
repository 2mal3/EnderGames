package io.github.mal32.endergames.worlds.lobby;

import io.github.mal32.endergames.AbstractModule;
import io.github.mal32.endergames.EnderGames;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.attribute.AttributeModifier.Operation;
import org.bukkit.block.Block;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.Display.Billboard;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.jetbrains.annotations.Nullable;

public class PlayerDifficulty extends AbstractModule {
  private static final NamespacedKey ATTRIBUTE_NAME =
      new NamespacedKey("endergames", "difficulty_armor_modifier");
  private final TextDisplay display;

  public PlayerDifficulty(EnderGames plugin) {
    super(plugin);

    // /summon minecraft:text_display ~ ~ ~ {UUID:[I;1883270697,1892829000,-2013577149,-443118066]}
    String uuidString = "70406e29-70d2-4748-87fb-4043e5968e0e";
    UUID displayUuid = UUID.fromString(uuidString);
    display = (TextDisplay) LobbyWorld.lobbyWorld.getEntity(displayUuid);
  }

  @EventHandler
  private void onButtonPress(PlayerInteractEvent event) {
    var player = event.getPlayer();
    if (!LobbyWorld.playerIsInLobbyWorld(player)) return;
    if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return; // prevent double trigger

    Block clickedBlock = event.getClickedBlock();
    if (clickedBlock == null) return;
    if (clickedBlock.getType() != Material.POLISHED_BLACKSTONE_BUTTON) return;

    var blockData = (Directional) clickedBlock.getBlockData();
    Block attachedBlock = clickedBlock.getRelative(blockData.getFacing().getOppositeFace());
    switch (attachedBlock.getType()) {
      case COPPER_BLOCK -> nerf(player);
      case DIAMOND_BLOCK -> buff(player);
      default -> {}
    }

    updateDisplay(player);
  }

  @EventHandler
  private void onPlayerMove(PlayerMoveEvent event) {
    if (!event.hasChangedBlock()) return;
    Player player = event.getPlayer();
    if (!LobbyWorld.playerIsInLobbyWorld(player)) return;

    final double MAX_DISTANCE_SQUARED = Math.pow(5, 2);
    Location toBlockPos = event.getTo().getBlock().getLocation();
    Location fromBlockPos = event.getFrom().getBlock().getLocation();
    Location displayBlockPos = display.getLocation().getBlock().getLocation();
    double distanceTo = toBlockPos.distanceSquared(displayBlockPos);
    double distanceFrom = fromBlockPos.distanceSquared(displayBlockPos);
    if (distanceTo > MAX_DISTANCE_SQUARED && distanceFrom <= MAX_DISTANCE_SQUARED) {
      // player moved out of range, reset modifier
      updateDisplay(null);
    } else if (distanceTo <= MAX_DISTANCE_SQUARED && distanceFrom > MAX_DISTANCE_SQUARED) {
      // player moved into range, update display
      updateDisplay(player);
    }
  }

  private void nerf(Player player) {
    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, SoundCategory.UI, 1.0f, 1.0f);

    double factor = getArmorModifier(player);
    double newFactor = factor - 0.25;
    setArmorModifier(player, newFactor);
  }

  private void buff(Player player) {
    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_YES, SoundCategory.UI, 1.0f, 1.0f);

    double factor = getArmorModifier(player);
    double newFactor = factor + 0.25;
    setArmorModifier(player, newFactor);
  }

  private void updateDisplay(@Nullable Player player) {
    display.setBillboard(Billboard.CENTER);
    display.setGlowing(true);

    if (player == null) {
      display.text(
          Component.text("")
              .append(Component.text("Armor Multiplier\n").decorate(TextDecoration.BOLD))
              .color(NamedTextColor.WHITE));
      return;
    }

    double factor = getArmorModifier(player);

    NamedTextColor factorColor;
    if (factor < 0) {
      factorColor = NamedTextColor.GREEN;
    } else if (factor > 0) {
      factorColor = NamedTextColor.RED;
    } else {
      factorColor = NamedTextColor.WHITE;
    }

    display.text(
        Component.text("")
            .append(Component.text("Armor Multiplier\n").decorate(TextDecoration.BOLD))
            .append(Component.text(String.format("%.0f", factor * 100) + "%").color(factorColor))
            .color(NamedTextColor.WHITE));
  }

  private double getArmorModifier(Player player) {
    var armorModifier = player.getAttribute(Attribute.ARMOR).getModifier(ATTRIBUTE_NAME);
    if (armorModifier == null) {
      return 0.0;
    }
    return armorModifier.getAmount();
  }

  private void setArmorModifier(Player player, double factor) {
    player.getAttribute(Attribute.ARMOR).removeModifier(ATTRIBUTE_NAME);
    player
        .getAttribute(Attribute.ARMOR)
        .addModifier(new AttributeModifier(ATTRIBUTE_NAME, factor, Operation.ADD_SCALAR));
  }
}
