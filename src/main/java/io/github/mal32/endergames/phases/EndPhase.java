package io.github.mal32.endergames.phases;

import io.github.mal32.endergames.GameManager;
import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;

public class EndPhase extends AbstractPhase {
    public EndPhase(JavaPlugin plugin, GameManager manager, Location spawn) {
        super(plugin, manager, spawn);
    }
}
