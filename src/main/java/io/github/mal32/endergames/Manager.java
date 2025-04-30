package io.github.mal32.endergames;

import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;

public class Manager {
    private final JavaPlugin plugin;
    private final Location spawn;
    private AbstractPhase phase;

    public Manager(JavaPlugin plugin, Location spawn) {
        this.plugin = plugin;
        this.spawn = spawn;

//        phase = new LobbyPhase(plugin, this, spawn);
        phase = new GamePhase(plugin, this, spawn);
    }

    public void startStartPhase() {
        if (!(phase instanceof LobbyPhase)) {
            plugin.getComponentLogger().debug("Game is already started.");
            return;
        }

        phase.stop();
        phase = new StartPhase(plugin, this, spawn);
    }

    protected void startGamePhase() {
        phase.stop();
        phase = new GamePhase(plugin, this, spawn);
    }
}
