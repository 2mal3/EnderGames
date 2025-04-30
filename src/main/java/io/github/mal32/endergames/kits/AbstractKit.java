package io.github.mal32.endergames.kits;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public abstract class AbstractKit implements Listener {
    JavaPlugin plugin;

    public AbstractKit(JavaPlugin plugin) {
        this.plugin = plugin;

        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    protected boolean playerHasKit(Player player) {
        return Objects.equals(player.getPersistentDataContainer().get(new NamespacedKey(plugin, "kit"), PersistentDataType.STRING), getName());
    }

    public void stop() {
        HandlerList.unregisterAll(this);
    }

    public abstract void start(Player player);

    public abstract String getName();
}
