package io.github.mal32.endergames.worlds.lobby;

import io.github.mal32.endergames.EnderGames;
import org.bukkit.Material;
import org.bukkit.event.player.PlayerInteractEvent;

class CancelStartItem extends MenuItem {

    public CancelStartItem(EnderGames plugin) {
        super(plugin, Material.BARRIER, "§6Cancel Start", "start_game", (byte) 8);
    }

    @Override
    public void playerInteract(PlayerInteractEvent event) {
    }
}
