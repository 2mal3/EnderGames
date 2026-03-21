package io.github.mal32.endergames.kitsystem.kits;

import static io.github.mal32.endergames.kitsystem.util.KitUtils.colorLeatherArmor;
import static io.github.mal32.endergames.kitsystem.util.KitUtils.enchantItem;

import io.github.mal32.endergames.kitsystem.api.AbstractKit;
import io.github.mal32.endergames.kitsystem.api.Difficulty;
import io.github.mal32.endergames.kitsystem.api.KitDescription;
import io.github.mal32.endergames.kitsystem.api.KitService;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public class Lumberjack extends AbstractKit {
  public static final String id = "Lumberjack";

  public Lumberjack(KitService kitService, JavaPlugin plugin) {
    super(
        new KitDescription(
            Lumberjack.id,
            Material.WOODEN_AXE,
            "The lumberjack can fell entire trees with a single axe swing. Every axe he crafts automatically gains Sharpness I.",
            "Wooden axe, red leather chestplate, blue leather pants, black boots.",
            Difficulty.EASY),
        kitService,
        plugin);
  }

  @Override
  public void initPlayer(Player player) {
    player
        .getInventory()
        .setChestplate(
            colorLeatherArmor(new ItemStack(Material.LEATHER_CHESTPLATE), Color.fromRGB(10820909)));
    player
        .getInventory()
        .setLeggings(
            colorLeatherArmor(new ItemStack(Material.LEATHER_LEGGINGS), Color.fromRGB(1728436)));
    player
        .getInventory()
        .addItem(enchantItem(new ItemStack(Material.WOODEN_AXE), Enchantment.SHARPNESS, 1));
  }

  @EventHandler
  private void onBlockBreak(BlockBreakEvent event) {
    if (!Tag.LOGS.isTagged(event.getBlock().getType())) return;

    if (!playerCanUseThisKit(event.getPlayer())) return;

    Location location = event.getBlock().getLocation().add(0, 1, 0);
    breakTree(location, event.getPlayer(), 5, false);
  }

  private void breakTree(Location location, Player player, double leaveBudget, boolean leaveMode) {
    if (leaveBudget <= 0) return;
    Block block = location.getBlock();
    Material material = block.getType();
    if (!Tag.LOGS.isTagged(material) && !Tag.LEAVES.isTagged(material)) return;

    if (Tag.LOGS.isTagged(material)) {
      if (leaveMode) return;
      var item = new ItemStack(material);
      player.getInventory().addItem(item);
    }

    if (Tag.LEAVES.isTagged(material)) {
      leaveMode = true;
      leaveBudget--;
    }

    block.setType(Material.AIR);

    breakTree(location.clone().add(0, 1, 0), player, leaveBudget, leaveMode);
    breakTree(location.clone().add(1, 0, 0), player, leaveBudget, leaveMode);
    breakTree(location.clone().add(-1, 0, 0), player, leaveBudget, leaveMode);
    breakTree(location.clone().add(0, 0, 1), player, leaveBudget, leaveMode);
    breakTree(location.clone().add(0, 0, -1), player, leaveBudget, leaveMode);
  }

  @EventHandler
  private void onCraftItem(CraftItemEvent event) {
    if (!playerCanUseThisKit((Player) event.getWhoClicked())) return;

    ItemStack result = event.getRecipe().getResult();
    if (!Tag.ITEMS_AXES.isTagged(result.getType())) return;

    result.addEnchantment(Enchantment.SHARPNESS, 1);
    event.getInventory().setResult(result);
  }
}
