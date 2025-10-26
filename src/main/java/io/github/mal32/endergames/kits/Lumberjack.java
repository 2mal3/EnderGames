package io.github.mal32.endergames.kits;

import io.github.mal32.endergames.EnderGames;
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

public class Lumberjack extends AbstractKit {
  public Lumberjack(EnderGames plugin) {
    super(plugin);
  }

  @Override
  public void start(Player player) {
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
    breakTree(location, event.getPlayer());
  }

  private void breakTree(Location location, Player player) {
    Block block = location.getBlock();
    Material material = block.getType();
    if (!Tag.LOGS.isTagged(material) && !Tag.LEAVES.isTagged(material)) return;

    if (Tag.LOGS.isTagged(material)) {
      var item = new ItemStack(material);
      player.getInventory().addItem(item);
    }

    block.setType(Material.AIR);

    breakTree(location.clone().add(1, 0, 0), player);
    breakTree(location.clone().add(-1, 0, 0), player);
    breakTree(location.clone().add(0, 1, 0), player);
    breakTree(location.clone().add(0, 0, 1), player);
    breakTree(location.clone().add(0, 0, -1), player);
  }

  @EventHandler
  private void onCraftItem(CraftItemEvent event) {
    if (!playerCanUseThisKit((Player) event.getWhoClicked())) return;

    ItemStack result = event.getRecipe().getResult();
    if (!Tag.ITEMS_AXES.isTagged(result.getType())) return;

    result.addEnchantment(Enchantment.SHARPNESS, 1);
    event.getInventory().setResult(result);
  }

  @Override
  public KitDescription getDescription() {
    return new KitDescription(
        Material.WOODEN_AXE,
        "Lumberjack",
        "The lumberjack can fell entire trees with a single axe swing. Every axe he crafts"
            + " automatically gains Sharpness I.",
        "Wooden axe, red leather chestplate, blue leather pants, black boots.",
        Difficulty.EASY);
  }
}
