package io.github.mal32.endergames;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.RenderType;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;

public class KDScoreboard implements Listener {
  private final NamespacedKey DEATH_COUNT;
  private final NamespacedKey KILL_COUNT;
  private final Scoreboard board;
  private final Objective objective;
  private final EnderGames plugin;

  public KDScoreboard(EnderGames plugin) {
    this.plugin = plugin;

    DEATH_COUNT = new NamespacedKey(plugin, "deathCount");
    KILL_COUNT = new NamespacedKey(plugin, "killCount");

    ScoreboardManager manager = plugin.getServer().getScoreboardManager();
    board = manager.getMainScoreboard();
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

    var deathCount =
        player
            .getPersistentDataContainer()
            .getOrDefault(DEATH_COUNT, PersistentDataType.INTEGER, 0);
    player
        .getPersistentDataContainer()
        .set(DEATH_COUNT, PersistentDataType.INTEGER, deathCount + 1);
    updateScoreboard(player);

    if (killer != null) {
      var killCount =
          killer
              .getPersistentDataContainer()
              .getOrDefault(KILL_COUNT, PersistentDataType.INTEGER, 0);
      killer
          .getPersistentDataContainer()
          .set(KILL_COUNT, PersistentDataType.INTEGER, killCount + 1);
      updateScoreboard(killer);
    }
  }

  @EventHandler
  private void onPlayerJoin(PlayerJoinEvent event) {
    var player = event.getPlayer();
    updateScoreboard(player);
  }

  private void updateScoreboard(Player player) {
    var deathCount =
        player
            .getPersistentDataContainer()
            .getOrDefault(DEATH_COUNT, PersistentDataType.INTEGER, 0);
    var killCount =
        player.getPersistentDataContainer().getOrDefault(KILL_COUNT, PersistentDataType.INTEGER, 0);

    double score;
    if (deathCount == 0) {
      score = 0;
    } else {
      score = ((double) killCount) / ((double) deathCount);
    }

    objective.getScore(player.getName()).setScore((int) (score * 100));
  }
}
