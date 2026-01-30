package io.github.mal32.endergames.worlds.game.game;

import io.github.mal32.endergames.EnderGames;
import java.util.ArrayList;
import java.util.Random;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.loot.LootContext;
import org.bukkit.loot.LootTable;
import org.jetbrains.annotations.NotNull;

public class EnderChestManager extends AbstractTeleportingBlockManager<EnderChest> {
  public EnderChestManager(EnderGames plugin, Location spawnLocation) {
    super(plugin, spawnLocation);
  }

  @Override
  public int getBlockTeleportDelayTicks() {
    return 20 * 45;
  }

  @Override
  protected int blockCount() {
    return 36;
  }

  @Override
  protected EnderChest getNewBlock(Location location) {
    return new EnderChest(plugin, location);
  }

  @EventHandler
  private void onEnderChestInteract(PlayerInteractEvent event) {
    if (event.getAction() != Action.RIGHT_CLICK_BLOCK
        || event.getClickedBlock() == null
        || event.getClickedBlock().getType() != Material.ENDER_CHEST) {
      return;
    }

    Location blockLocation = event.getClickedBlock().getLocation().clone();
    EnderChest enderChest = getBlockAtLocation(blockLocation);
    if (enderChest == null) {
      enderChest = new EnderChest(plugin, blockLocation);
      blocks.add(enderChest);
    }

    enderChest.prepareInventoryForOpen(event.getPlayer());

    EnderChest finalEnderChest = enderChest;
    Bukkit.getScheduler()
        .runTask(plugin, () -> event.getPlayer().openInventory(finalEnderChest.getInventory()));
  }
}

class EnderChest extends AbstractTeleportingBlock implements InventoryHolder {
  private final Inventory inventory;

  public EnderChest(EnderGames plugin, Location location) {
    super(plugin, location);

    this.inventory = plugin.getServer().createInventory(this, 27, Component.text("Ender Chest"));
  }

  @Override
  public void teleport(Location location) {
    super.teleport(location);

    new ArrayList<>(inventory.getViewers()).forEach(HumanEntity::closeInventory);
  }

  public void prepareInventoryForOpen(Player player) {
    if (hasBeenOpened) return;

    inventory.clear();

    float luck = (float) player.getAttribute(Attribute.LUCK).getValue();
    LootContext.Builder lootContextBuilder = new LootContext.Builder(location).luck(luck);
    LootContext lootContext = lootContextBuilder.build();
    LootTable lootTable = Bukkit.getLootTable(new NamespacedKey("enga", "ender_chest"));
    lootTable.fillInventory(this.inventory, new Random(), lootContext);

    open();
  }

  @Override
  @NotNull
  public Inventory getInventory() {
    return inventory;
  }

  @Override
  public Material getBlockMaterial() {
    return Material.ENDER_CHEST;
  }

  @Override
  public Material getFallingBlockMaterial() {
    return Material.OBSIDIAN;
  }
}
