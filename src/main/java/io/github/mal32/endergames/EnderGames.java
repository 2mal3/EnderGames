package io.github.mal32.endergames;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.github.mal32.endergames.phases.*;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public class EnderGames extends JavaPlugin implements Listener {
  private Location spawn;
  private AbstractPhase phase;
  private final NamespacedKey spawnKey = new NamespacedKey(this, "spawn");

  @Override
  public void onEnable() {
    this.getLifecycleManager()
        .registerEventHandler(
            LifecycleEvents.COMMANDS,
            commands -> commands.registrar().register(endergamesCommand()));

    World world = Bukkit.getWorlds().getFirst();

    if (!world.getPersistentDataContainer().has(spawnKey)) {
      Bukkit.getServer().sendMessage(Component.text("First EnderGames server start"));
      spawn = new Location(world, 0, 150, 0);
      updateSpawn();
    }

    List<Integer> rawSpawn =
        world
            .getPersistentDataContainer()
            .get(spawnKey, PersistentDataType.LIST.listTypeFrom(PersistentDataType.INTEGER));
    spawn = new Location(world, rawSpawn.get(0), 150, rawSpawn.get(1));

    phase = new LobbyPhase(this, spawn);
  }

  public void nextPhase() {
    phase.stop();
    if (phase instanceof LobbyPhase) {
      phase = new StartPhase(this, spawn);
    } else if (phase instanceof StartPhase) {
      phase = new GamePhase(this, spawn);
    } else if (phase instanceof GamePhase) {
      phase = new EndPhase(this, spawn);
    } else if (phase instanceof EndPhase) {
      spawn.add(1000, 0, 0);
      updateSpawn();
      phase = new LobbyPhase(this, spawn);
    }
  }

  private void updateSpawn() {
    World world = spawn.getWorld();

    world
        .getPersistentDataContainer()
        .set(
            spawnKey,
            PersistentDataType.LIST.listTypeFrom(PersistentDataType.INTEGER),
            List.of((int) spawn.getX(), (int) spawn.getZ()));

    world.setSpawnLocation(spawn);
    world.getWorldBorder().setCenter(spawn);
  }

  private LiteralCommandNode<CommandSourceStack> endergamesCommand() {
    final List<String> kits = List.of("lumberjack", "cat", "cactus", "barbarian", "blaze");

    return Commands.literal("endergames")
        .then(
            Commands.literal("start")
                .requires(sender -> sender.getSender().isOp())
                .executes(
                    ctx -> {
                      nextPhase();
                      return Command.SINGLE_SUCCESS;
                    }))
        .then(
            Commands.literal("kit")
                .then(
                    Commands.argument("kit", StringArgumentType.word())
                        .suggests(
                            (ctx, builder) -> {
                              kits.stream()
                                  .filter(
                                      entry ->
                                          entry
                                              .toLowerCase()
                                              .startsWith(builder.getRemainingLowerCase()))
                                  .forEach(builder::suggest);
                              return builder.buildFuture();
                            })
                        .executes(
                            ctx -> {
                              Player sender = (Player) ctx.getSource().getSender();

                              String selectedKit = StringArgumentType.getString(ctx, "kit");
                              sender.sendPlainMessage("You selected the " + selectedKit + " kit");
                              sender.playSound(
                                  sender.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1, 1);

                              NamespacedKey key = new NamespacedKey(this, "kit");
                              sender
                                  .getPersistentDataContainer()
                                  .set(key, PersistentDataType.STRING, selectedKit);

                              return Command.SINGLE_SUCCESS;
                            })))
        .build();
  }
}
