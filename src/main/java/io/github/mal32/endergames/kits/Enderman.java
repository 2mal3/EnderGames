package io.github.mal32.endergames.kits;

import io.github.mal32.endergames.EnderGames;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.BlockFace;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class Enderman extends AbstractKit {
  public Enderman(EnderGames plugin) {
    super(plugin);
  }

  @Override
  public void start(Player player) {
    var enderPerls = new ItemStack(Material.ENDER_PEARL, 5);
    player.getInventory().addItem(enderPerls);
  }

  @EventHandler
  private void onPlayerKill(PlayerDeathEvent event) {
    Player killer = event.getPlayer().getKiller();
    if (killer == null) return;
    if (!playerCanUseThisKit(killer)) return;

    killer.getInventory().addItem(new ItemStack(Material.ENDER_PEARL, 3));
  }

  @EventHandler
  private void onEnderPearlDamage(EntityDamageEvent event) {
    if (!(event.getEntity() instanceof Player player)) return;
    if (!playerCanUseThisKit(player)) return;
    if (event.getDamageSource().getDamageType() != DamageType.ENDER_PEARL) return;

    event.setCancelled(true);
  }

  @EventHandler
  private void onInteract(PlayerInteractEvent event) {
    if (event.getAction() != Action.RIGHT_CLICK_AIR) return;
    var player = event.getPlayer();
    if (!playerCanUseThisKit(player)) return;

    var passableBlocks =
        Set.of(
            Material.AIR,
            Material.WATER,
            Material.SNOW,
            Material.SHORT_GRASS,
            Material.TALL_GRASS,
            Material.SEAGRASS,
            Material.TALL_SEAGRASS);
    var targetBlock = player.getTargetBlock(passableBlocks, 32);
    if (targetBlock.getType() != Material.ENDER_CHEST) return;

    player.playSound(player, Sound.BLOCK_ENDER_CHEST_OPEN, 1, 1);
    player.playSound(player, Sound.BLOCK_ENDER_CHEST_CLOSE, 1, 1);

    PlayerInteractEvent chestOpenEvent =
        new PlayerInteractEvent(
            player, Action.RIGHT_CLICK_BLOCK, null, targetBlock, BlockFace.SELF);
    Bukkit.getServer().getPluginManager().callEvent(chestOpenEvent);
  }

  @Override
  public KitDescription getDescription() {
    return new KitDescription(
        Material.ENDER_PEARL,
        "Enderman",
        "Starts with 5 Ender Pearls. Gain 2 Ender Pearls per player kill. Doesn't take damage from"
            + " Ender Perls. Can open Ender Chests from further away.",
        "5 Ender Pearls",
        Difficulty.EASY);
  }
}
