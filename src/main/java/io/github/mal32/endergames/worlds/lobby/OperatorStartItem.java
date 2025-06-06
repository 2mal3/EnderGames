package io.github.mal32.endergames.worlds.lobby;

import io.github.mal32.endergames.EnderGames;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.scheduler.BukkitTask;

class OperatorStartItem extends MenuItem {
  private final MenuItem cancleItem;
  private BukkitTask startGameTask = null;

  public OperatorStartItem(EnderGames plugin) {
    super(plugin, Material.NETHER_STAR, "§6Start Game", "start_game", (byte) 8);
    this.cancleItem = new CancelStartItem(this.plugin);
  }

  @Override
  public void giveItem(Player player) {
    if (this.startGameTask == null) super.giveItem(player);
    else this.cancleItem.giveItem(player);
  }

  @Override
  public void playerInteract(PlayerInteractEvent event) {
    if (this.startGameTask == null) {
      this.startGameTask =
          Bukkit.getScheduler()
              .runTaskLater(
                  this.plugin,
                  () -> {
                    this.plugin.getGameWorld().startGame();
                    this.startGameTask = null;
                  },
                  5 * 20);

      for (Player player : Bukkit.getOnlinePlayers()) {
        if (player.isOp()) {
          player.sendMessage("§aThe game will start in 5 seconds!");
          this.giveItem(player);
        }
      }
    } else {
      this.startGameTask.cancel();
      this.startGameTask = null;

      for (Player player : Bukkit.getOnlinePlayers()) {
        if (player.isOp()) {
          player.sendMessage("§cStart was canceled!");
          this.giveItem(player);
        }
      }
    }
  }
}
