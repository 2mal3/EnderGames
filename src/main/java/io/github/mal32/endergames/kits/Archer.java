package io.github.mal32.endergames.kits;

import io.github.mal32.endergames.EnderGames;
import io.github.mal32.endergames.worlds.game.GameWorld;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;

public class Archer extends AbstractKit {
  private BukkitTask task;

  public Archer(EnderGames plugin) {
    super(plugin);
  }

  @Override
  public void enable() {
    super.enable();

    BukkitScheduler scheduler = plugin.getServer().getScheduler();
    task = scheduler.runTaskTimer(plugin, this::giveArrow, 12 * 20, 12 * 20);
  }

  @Override
  public void disable() {
    super.disable();

    task.cancel();
  }

  private void giveArrow() {
    for (Player player : GameWorld.getPlayersInGame()) {
      if (!playerCanUseThisKit(player)) continue;

      player.getInventory().addItem(new ItemStack(Material.ARROW));
    }
  }

  @Override
  public void start(Player player) {
    player.getInventory().addItem(new ItemStack(Material.BOW));
    player.getInventory().addItem(new ItemStack(Material.ARROW, 5));

    player.getInventory().setHelmet(new ItemStack(Material.GOLDEN_HELMET));
  }

  @Override
  public KitDescriptionItem getDescriptionItem() {
    return new KitDescriptionItem(
        Material.BOW,
        "Archer",
        "Gets a new Arrow every 12 seconds. Can overcharge the Bow to make more damage.",
        "Bow, 5 Arrows",
        Difficulty.MEDIUM);
  }
}
