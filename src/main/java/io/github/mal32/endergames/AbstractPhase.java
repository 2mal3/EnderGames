package io.github.mal32.endergames;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public class AbstractPhase implements Listener {
    public JavaPlugin plugin;
    public Location spawnLocation;
    public Manager manager;

    public AbstractPhase(JavaPlugin plugin, Manager manager, Location spawn) {
        this.plugin = plugin;
        this.spawnLocation = spawn;
        this.manager = manager;

        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void stop() {
        HandlerList.unregisterAll(this);
    }
}
