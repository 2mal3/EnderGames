package io.github.mal32.endergames.lobby;

import io.github.mal32.endergames.BlockLocation;
import io.github.mal32.endergames.services.PlayerInWorld;
import java.time.Duration;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.Tag;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.attribute.AttributeModifier.Operation;
import org.bukkit.block.Block;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class PlayerDifficulty extends LobbyModule {
  private static final NamespacedKey ATTRIBUTE_NAME =
      new NamespacedKey("endergames", "difficulty_armor_modifier");
  private static final double STEP_SIZE = 0.10;
  private static final double MAX_FACTOR = 0.6;
  private static final double MIN_FACTOR = -0.6;
  private static final BlockLocation difficultySelectorPos =
      new BlockLocation(LobbyWorld.world, -23, 70, -6);

  public PlayerDifficulty(JavaPlugin plugin) {
    super(plugin);

    // highest possible refresh rate for fastest possible feedback to the player
    plugin.getServer().getScheduler().runTaskTimer(plugin, this::displayDifficultyTick, 1, 1);
  }

  private void displayDifficultyTick() {
    // all players near the difficulty selector and roughtly look in its direction
    List<Player> lookingPlayers =
        LobbyWorld.world.getNearbyPlayers(difficultySelectorPos.toLocation(), 3).stream()
            .filter((p) -> p.getLocation().getYaw() > 0 && p.getLocation().getYaw() < 180)
            .toList();

    for (Player player : lookingPlayers) {
      double factor = getArmorModifier(player);
      String percent = String.format("%.0f", (factor + 1) * 100) + "%";

      NamedTextColor factorColor;
      if (factor < 0) {
        factorColor = NamedTextColor.RED;
      } else if (factor > 0) {
        factorColor = NamedTextColor.GREEN;
      } else {
        factorColor = NamedTextColor.WHITE;
      }

      var times =
          Title.Times.times(Duration.ofMillis(0), Duration.ofMillis(50 * 5), Duration.ofMillis(0));
      var title =
          Title.title(
              Component.text(percent).color(factorColor),
              Component.text("Armor Multiplier"),
              times);
      player.showTitle(title);
    }
  }

  @EventHandler
  private void onButtonPress(PlayerInteractEvent event) {
    var player = event.getPlayer();
    if (!PlayerInWorld.LOBBY.is(player)) return;
    if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return; // prevent double trigger

    Block clickedBlock = event.getClickedBlock();
    if (clickedBlock == null) return;
    if (clickedBlock.getType() != Material.POLISHED_BLACKSTONE_BUTTON) return;

    var blockData = (Directional) clickedBlock.getBlockData();
    Block attachedBlock = clickedBlock.getRelative(blockData.getFacing().getOppositeFace());
    if (Tag.COPPER.isTagged(attachedBlock.getType())) {
      nerf(player);
    } else if (attachedBlock.getType() == Material.DIAMOND_BLOCK) {
      buff(player);
    }
  }

  private void nerf(Player player) {
    if (getArmorModifier(player) <= MIN_FACTOR) {
      return;
    }

    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, SoundCategory.UI, 1.0f, 1.0f);

    double factor = getArmorModifier(player);
    double newFactor = factor - STEP_SIZE;
    setArmorModifier(player, newFactor);
  }

  private void buff(Player player) {
    if (getArmorModifier(player) >= MAX_FACTOR) {
      return;
    }

    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_YES, SoundCategory.UI, 1.0f, 1.0f);

    double factor = getArmorModifier(player);
    double newFactor = factor + STEP_SIZE;
    setArmorModifier(player, newFactor);
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
