package io.github.mal32.endergames.phases;

import io.github.mal32.endergames.GameManager;
import com.destroystokyo.paper.Title;
import io.github.mal32.endergames.GameManager;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;

public class StartPhase extends AbstractPhase {
    public StartPhase(JavaPlugin plugin, GameManager manager, Location spawn) {
        super(plugin, manager, spawn);

        BukkitScheduler scheduler = plugin.getServer().getScheduler();

        for (Player player : plugin.getServer().getOnlinePlayers()) {
            player.getInventory().clear();

            for (int i = 0; i <= 4; i++) {
                int finalI = 5-i;
                scheduler.runTaskLater(plugin, () -> {
                    player.sendTitle(new Title(Integer.toString(finalI), "", 5, 10, 5));
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HARP, 1, 1);
                }, i*20);
            }
            scheduler.runTaskLater(plugin, () -> {
                player.sendTitle(new Title("Start!", "", 5, 10, 5));
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_FLUTE, 1, 1);
            }, 5*20);
        }

        scheduler.runTaskLater(plugin, manager::nextPhase, 5*20);
    }

    @Override
    public void stop() {
        super.stop();

        for (Player player : plugin.getServer().getOnlinePlayers()) {
            player.clearActivePotionEffects();
        }
    }

    @EventHandler
    private void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        if (!event.hasChangedBlock()) {
            return;
        }

        event.setCancelled(true);
    }
}

