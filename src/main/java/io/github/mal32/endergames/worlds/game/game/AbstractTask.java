package io.github.mal32.endergames.worlds.game.game;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;

/*
 * This class is an abstract representation of a task that runs at a specified interval.
 */
public abstract class AbstractTask extends AbstractModule {
  private BukkitTask task;

  public AbstractTask(JavaPlugin plugin) {
    super(plugin);
  }

  @Override
  public void enable() {
    super.enable();

    BukkitScheduler scheduler = plugin.getServer().getScheduler();
    task = scheduler.runTaskTimer(plugin, this::task, getDelay(), getDelay());
  }

  @Override
  public void disable() {
    super.disable();

    task.cancel();
  }

  public abstract int getDelay();

  public abstract void task();
}
