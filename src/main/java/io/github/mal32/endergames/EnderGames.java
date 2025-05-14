package io.github.mal32.endergames;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.github.mal32.endergames.phases.*;
import io.github.mal32.endergames.phases.game.GamePhase;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.block.Biome;
import org.bukkit.event.Listener;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public class EnderGames extends JavaPlugin implements Listener {
  private Location spawnLocation;
  private AbstractPhase phase;
  private final NamespacedKey spawnKey = new NamespacedKey(this, "spawn");

  @Override
  public void onEnable() {
    final int PLUGIN_ID = 25844;
    Metrics metrics = new Metrics(this, PLUGIN_ID);

    this.getLifecycleManager()
        .registerEventHandler(
            LifecycleEvents.COMMANDS,
            commands -> commands.registrar().register(endergamesCommand()));

    World world = Bukkit.getWorld("world_enga_world");

    if (!world.getPersistentDataContainer().has(spawnKey)) {
      Bukkit.getServer().sendMessage(Component.text("First EnderGames server start"));
      spawnLocation = new Location(world, 0, 150, 0);
      updateSpawn();
    }

    List<Integer> rawSpawn =
        world
            .getPersistentDataContainer()
            .get(spawnKey, PersistentDataType.LIST.listTypeFrom(PersistentDataType.INTEGER));
    spawnLocation = new Location(world, rawSpawn.get(0), 150, rawSpawn.get(1));

    phase = new LobbyPhase(this, spawnLocation);
  }

  public void nextPhase() {
    phase.stop();
    if (phase instanceof LobbyPhase) {
      phase = new StartPhase(this, spawnLocation);
    } else if (phase instanceof StartPhase) {
      phase = new GamePhase(this, spawnLocation);
    } else if (phase instanceof GamePhase) {
      phase = new EndPhase(this, spawnLocation);

      findNewSpawnLocation();
      updateSpawn();
    } else if (phase instanceof EndPhase) {
      phase = new LobbyPhase(this, spawnLocation);
    }
  }

  private void findNewSpawnLocation() {
    Location spawnLocationCandidate = spawnLocation.clone();

    do {
      spawnLocationCandidate.add(1000, 0, 0);
      spawnLocationCandidate.getChunk().load(true);
    } while (isOcean(spawnLocationCandidate.getBlock().getBiome()));

    spawnLocation = spawnLocationCandidate;
  }

  // Why doesnt BiomeTagKeys.IS_OCEAN work?
  // using
  // https://github.com/misode/mcmeta/blob/data/data/minecraft/tags/worldgen/biome/is_ocean.json
  // directly
  private boolean isOcean(Biome biome) {
    return biome.equals(Biome.DEEP_FROZEN_OCEAN)
        || biome.equals(Biome.DEEP_COLD_OCEAN)
        || biome.equals(Biome.DEEP_OCEAN)
        || biome.equals(Biome.DEEP_LUKEWARM_OCEAN)
        || biome.equals(Biome.FROZEN_OCEAN)
        || biome.equals(Biome.OCEAN)
        || biome.equals(Biome.COLD_OCEAN)
        || biome.equals(Biome.LUKEWARM_OCEAN)
        || biome.equals(Biome.WARM_OCEAN);
  }

  private void updateSpawn() {
    World world = spawnLocation.getWorld();

    world
        .getPersistentDataContainer()
        .set(
            spawnKey,
            PersistentDataType.LIST.listTypeFrom(PersistentDataType.INTEGER),
            List.of((int) spawnLocation.getX(), (int) spawnLocation.getZ()));

    world.setSpawnLocation(spawnLocation);
    world.getWorldBorder().setCenter(spawnLocation);
  }

  private LiteralCommandNode<CommandSourceStack> endergamesCommand() {

    return Commands.literal("endergames")
        .then(
            Commands.literal("start")
                .requires(sender -> sender.getSender().isOp())
                .executes(
                    ctx -> {
                      nextPhase();
                      return Command.SINGLE_SUCCESS;
                    }))
        .build();
  }
}
