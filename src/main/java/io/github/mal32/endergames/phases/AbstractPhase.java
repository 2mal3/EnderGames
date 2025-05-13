package io.github.mal32.endergames.phases;

import io.github.mal32.endergames.EnderGames;
import io.github.mal32.endergames.kits.*;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

public class AbstractPhase implements Listener {
  protected final EnderGames plugin;
  public Location spawnLocation;
  public List<AbstractKit> kits;

  public AbstractPhase(EnderGames plugin, Location spawn) {
    this.plugin = plugin;
    this.spawnLocation = spawn;
    this.kits =
        List.of(
            new Lumberjack(plugin),
            new Cat(plugin),
            new Cactus(plugin),
            new Barbarian(plugin),
            new Blaze(plugin),
            new Slime(plugin),
            new Dolphin(plugin));

    Bukkit.getPluginManager().registerEvents(this, plugin);
  }

  public void stop() {
    HandlerList.unregisterAll(this);
  }
}
