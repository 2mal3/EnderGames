package io.github.mal32.endergames.worlds.game.game;

import io.github.mal32.endergames.AbstractModule;
import io.github.mal32.endergames.EnderGames;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;

/*
 * This class is an abstract representation of a task that runs at a specified interval.
 */
public abstract class AbstractTask extends AbstractModule {
  private BukkitTask task;

  public AbstractTask(EnderGames plugin) {
    super(plugin);
  }

  @Override
  public void enable() {
    super.enable();

    BukkitScheduler scheduler = plugin.getServer().getScheduler();
    task = scheduler.runTaskTimer(plugin, this::task, getDelayTicks(), getDelayTicks());
  }

  @Override
  public void disable() {
    super.disable();

    task.cancel();
  }

  public abstract int getDelayTicks();

  public abstract void task();
}
