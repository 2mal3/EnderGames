package io.github.mal32.endergames;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.datapack.Datapack;
import io.papermc.paper.datapack.DatapackRegistrar;
import io.papermc.paper.datapack.DiscoveredDatapack;
import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class EnderGames extends JavaPlugin implements Listener {
    private Manager manager;

    @Override
    public void onEnable() {
        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands -> commands.registrar().register(endergamesCommand()));

        World world = Bukkit.getWorlds().getFirst();
        Location location = new Location(world, 0, 100, 0);
        manager = new Manager(this, location);
    }

    private LiteralCommandNode<CommandSourceStack> endergamesCommand() {
        final List<String> kits = List.of("lumberjack");

        return Commands.literal("endergames")
                .then(Commands.literal("start")
                        .requires(sender -> sender.getSender().isOp())
                        .executes(ctx -> {
                            manager.startStartPhase();
                            return Command.SINGLE_SUCCESS;
                        })
                )
                .then(Commands.literal("kit")
                        .then(Commands.argument("kit", StringArgumentType.word())
                                .suggests((ctx, builder) -> {
                                    kits.stream()
                                            .filter(entry -> entry.toLowerCase().startsWith(builder.getRemainingLowerCase()))
                                            .forEach(builder::suggest);
                                    return builder.buildFuture();
                                })
                                .executes(ctx -> {
                                    Player sender = (Player) ctx.getSource().getSender();

                                    String selectedKit = StringArgumentType.getString(ctx, "kit");
                                    sender.sendPlainMessage("You selected the " + selectedKit + " kit");
                                    sender.playSound(sender.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1, 1);

                                    NamespacedKey key = new NamespacedKey(this, "kit");
                                    sender.getPersistentDataContainer().set(key, PersistentDataType.STRING, selectedKit);

                                    return Command.SINGLE_SUCCESS;
                                })
                ))
                .build();
    }
}
