package io.github.mal32.endergames.phases.game.tasks;

import io.github.mal32.endergames.EnderGames;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;

public abstract class AbstractTask {
  protected final EnderGames plugin;
  protected final BukkitTask task;

  public AbstractTask(EnderGames plugin) {
    this.plugin = plugin;

    BukkitScheduler scheduler = plugin.getServer().getScheduler();
    task = scheduler.runTaskTimer(plugin, this::run, this.getDelay(), this.getDelay());
  }

  public abstract void run();

  public void stop() {
    if (task == null) return;
    task.cancel();
  }

  protected abstract int getDelay();
}
