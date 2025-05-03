package io.github.mal32.endergames.phases;

import io.github.mal32.endergames.GameManager;
import org.bukkit.*;
import org.bukkit.block.structure.Mirror;
import org.bukkit.block.structure.StructureRotation;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.structure.Structure;
import org.bukkit.structure.StructureManager;
import org.bukkit.util.BlockVector;

import java.util.Random;

public class LobbyPhase extends AbstractPhase {
    public Location playerSpawnLocation;

    public LobbyPhase(JavaPlugin plugin, GameManager manager, Location spawn) {
        super(plugin, manager, spawn);
        this.playerSpawnLocation = new Location(spawn.getWorld(), spawn.getX() + 0.5, spawn.getY() + 5, spawn.getZ() + 0.5);

        placeSpawnPlatform();

        World world = spawn.getWorld();

        world.setSpawnLocation(playerSpawnLocation);
        world.setGameRule(GameRule.SPAWN_RADIUS, 6);

        WorldBorder border = world.getWorldBorder();
        border.setCenter(spawnLocation);
        border.setSize(600);
    }

    private void placeSpawnPlatform() {
        StructureManager manager = Bukkit.getServer().getStructureManager();
        Structure structure = manager.loadStructure(new NamespacedKey("enga", "spawn_platform"));

        BlockVector structureSize = structure.getSize();
        int posX = (int) (spawnLocation.x() - (structureSize.getBlockX() / 2.0));
        int posZ = (int) (spawnLocation.z() - (structureSize.getBlockZ() / 2.0));
        Location location = new Location(spawnLocation.getWorld(), (int) posX, 100, posZ);
        structure.place(location, true, StructureRotation.NONE, Mirror.NONE, 0, 1.0f, new Random());
    }

    @Override
    public void stop() {
        super.stop();

        for (Player player : plugin.getServer().getOnlinePlayers()) {
            player.clearActivePotionEffects();
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        player.setGameMode(GameMode.ADVENTURE);
        player.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, Integer.MAX_VALUE, 1, true, false));
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        if (player.getGameMode() != GameMode.ADVENTURE) {
            return;
        }
        if (!event.hasChangedBlock()) {
            return;
        }

        if (player.getLocation().distance(spawnLocation) > 20) {
            player.teleport(playerSpawnLocation);
            player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 20, 256, true, false));
        }
    }
}
