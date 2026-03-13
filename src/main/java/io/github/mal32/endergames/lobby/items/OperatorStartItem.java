package io.github.mal32.endergames.lobby.items;

import io.github.mal32.endergames.EnderGames;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.jspecify.annotations.NonNull;

class OperatorStartItem extends MenuItem {
  String state;
  private BukkitTask startGameTask = null;

  public OperatorStartItem(JavaPlugin plugin) {
    super(
        plugin,
        (byte) 8,
        "start_game",
        Map.of(
            "start",
            new ItemDisplay(
                Material.NETHER_STAR, Component.text("Start Game").color(NamedTextColor.GOLD)),
            "cancel",
            new ItemDisplay(
                Material.BARRIER, Component.text("Cancel Start").color(NamedTextColor.GOLD))));
    this.state = "start";
  }

  @Override
  protected @NonNull String getState(Player player) {
    return this.state;
  }

  @Override
  public void onGameEnd(Player player) {
    if (!player.isOp()) return;
    giveItem(player);
  }

  @Override
  public void onGameStartAbort() {
    this.state = "start";
  }

  @Override
  public void onGameStartAbort(Player player) {
    if (!player.isOp()) return;
    player.sendActionBar(Component.text("Start was canceled!").color(NamedTextColor.RED));

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
                  ((EnderGames) this.plugin)
                      .getPhaseController()
                      .start(); // TODO: call GameStartEvent?
                  this.startGameTask = null;
                  this.state = "start";
                },
                startDelaySeconds * 20);

    this.state = "cancel";

    for (Player player : Bukkit.getOnlinePlayers()) {
      if (player.isOp()) {
        player.sendActionBar(
            Component.text("The game will start in 5 seconds!").color(NamedTextColor.GREEN));
        giveItem(player);
      }
    }
  }

  private void stopGameStart() {
    this.startGameTask.cancel();
    this.startGameTask = null;
    this.state = "start";

    for (Player player : Bukkit.getOnlinePlayers()) {
      this.onGameStartAbort(player);
    }
  }
}
