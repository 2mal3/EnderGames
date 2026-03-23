package io.github.mal32.endergames;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.RenderType;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;

public class KDScoreboard implements Listener {
  private final Objective objective;

  public KDScoreboard(EnderGames plugin) {
    ScoreboardManager manager = plugin.getServer().getScoreboardManager();
    Scoreboard board = manager.getMainScoreboard();
    if (board.getObjective("kd") != null) {
      board.getObjective("kd").unregister();
    }
    objective =
        board.registerNewObjective("kd", Criteria.DUMMY, Component.text("KD"), RenderType.INTEGER);
    objective.setDisplaySlot(DisplaySlot.PLAYER_LIST);

    Bukkit.getPluginManager().registerEvents(this, plugin);
  }

  @EventHandler
  private void onPlayerDeath(PlayerDeathEvent event) {
    var player = event.getEntity();
    var killer = player.getKiller();

    updateScoreboard(player);

    if (killer != null) {
      updateScoreboard(killer);
    }
  }

  @EventHandler
  private void onPlayerJoin(PlayerJoinEvent event) {
    var player = event.getPlayer();
    updateScoreboard(player);
  }

  private void updateScoreboard(Player player) {
    int deathCount = player.getStatistic(Statistic.DEATHS);
    int killCount = player.getStatistic(Statistic.PLAYER_KILLS);

    double score;
    if (deathCount == 0) {
      score = 0;
    } else {
      score = ((double) killCount) / ((double) deathCount);
    }

    objective.getScore(player.getName()).setScore((int) (MoreMath.roundN(score * 100, 0)));
  }
}
