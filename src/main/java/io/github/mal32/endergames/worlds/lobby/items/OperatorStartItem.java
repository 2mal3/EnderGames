package io.github.mal32.endergames.worlds.lobby.items;

import io.github.mal32.endergames.EnderGames;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.scheduler.BukkitTask;

class OperatorStartItem extends MenuItem {
  private final MenuItem cancelItem;
  private BukkitTask startGameTask = null;

  public OperatorStartItem(EnderGames plugin) {
    super(
        plugin,
        Material.NETHER_STAR,
        Component.text("Start Game").color(NamedTextColor.GOLD),
        "start_game",
        (byte) 8);
    this.cancelItem = new CancelStartItem(this.plugin);
  }

  @Override
  public void initPlayer(Player player) {
    if (!player.isOp()) return;
    giveItem(player);
  }

  @Override
  public void playerInteract(PlayerInteractEvent event) {
    event.getPlayer().playSound(event.getPlayer().getLocation(), Sound.UI_BUTTON_CLICK, 1, 1);

    if (this.startGameTask == null) {
      scheduleGameStart();
    } else {
      stopGameStart();
    }
  }

  private void scheduleGameStart() {
    int startDelaySeconds = EnderGames.isInDebugMode() ? 1 : 5;

    this.startGameTask =
        Bukkit.getScheduler()
            .runTaskLater(
                this.plugin,
                () -> {
                  this.plugin.getGameWorld().startGame();
                  this.startGameTask = null;
                },
                startDelaySeconds * 20);

    for (Player player : Bukkit.getOnlinePlayers()) {
      if (player.isOp()) {
        player.sendActionBar(
            Component.text("The game will start in 5 seconds!").color(NamedTextColor.GREEN));
        cancelItem.giveItem(player);
      }
    }
  }

  private void stopGameStart() {
    this.startGameTask.cancel();
    this.startGameTask = null;

    for (Player player : Bukkit.getOnlinePlayers()) {
      if (player.isOp()) {
        player.sendActionBar(Component.text("Start was canceled!").color(NamedTextColor.RED));

        this.giveItem(player);
      }
    }
  }
}
