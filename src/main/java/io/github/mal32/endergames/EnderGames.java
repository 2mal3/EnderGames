package io.github.mal32.endergames;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.github.mal32.endergames.kits.AbstractKit;
import io.github.mal32.endergames.phases.AbstractPhase;
import io.github.mal32.endergames.phases.EndPhase;
import io.github.mal32.endergames.phases.LobbyPhase;
import io.github.mal32.endergames.phases.StartPhase;
import io.github.mal32.endergames.phases.game.GamePhase;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class EnderGames extends JavaPlugin implements Listener {
  private final Map<Phase, AbstractPhase> phases = new HashMap<>();
  private Phase currentPhase;
  private List<AbstractKit> kits;

  public static boolean playerIsIdling(Player player) {
    return player.getGameMode() == GameMode.ADVENTURE;
  }

  public static boolean playerIsPlaying(Player player) {
    return player.getGameMode() == GameMode.SURVIVAL;
  }

  public static boolean playerIsObserving(Player player) {
    return player.getGameMode() == GameMode.SPECTATOR;
  }

  @Override
  public void onEnable() {
    final int PLUGIN_ID = 25844;
    Metrics metrics = new Metrics(this, PLUGIN_ID);

    this.getLifecycleManager()
        .registerEventHandler(
            LifecycleEvents.COMMANDS,
            commands -> commands.registrar().register(endergamesCommand()));

    this.kits = AbstractKit.getKits(this);

    this.phases.put(Phase.IDLE, new LobbyPhase(this));
    this.phases.put(Phase.STARTING, new StartPhase(this));
    this.phases.put(Phase.RUNNING, new GamePhase(this));
    this.phases.put(Phase.STOPPING, new EndPhase(this));

    this.currentPhase = Phase.IDLE;

    Bukkit.getPluginManager().registerEvents(this, this);
  }

  public void nextPhase() {
    getCurrentPhase().stop();
    this.currentPhase = this.currentPhase.next();
    this.getComponentLogger().info("Next phase: {}", this.currentPhase.toString());
    getCurrentPhase().start();
  }

  public List<AbstractKit> getKits() {
    return this.kits;
  }

  public Phase getCurrentPhaseName() {
    return this.currentPhase;
  }

  public AbstractPhase getPhase(Phase phase) {
    return this.phases.get(phase);
  }

  public AbstractPhase getCurrentPhase() {
    return this.phases.get(this.currentPhase);
  }

  @EventHandler
  public void onPlayerJoin(PlayerJoinEvent event) {
    World lobbyWorld = Bukkit.getWorlds().getFirst();
    if (lobbyWorld != event.getPlayer().getWorld()) {
      event.getPlayer().teleport(lobbyWorld.getSpawnLocation());
    }
    ((LobbyPhase) this.phases.get(Phase.IDLE)).initPlayer(event.getPlayer());
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

  public enum Phase {
    IDLE {
      @Override
      public Phase next() {
        return STARTING;
      }
    },
    STARTING {
      @Override
      public Phase next() {
        return RUNNING;
      }
    },
    RUNNING {
      @Override
      public Phase next() {
        return STOPPING;
      }
    },
    STOPPING {
      @Override
      public Phase next() {
        return IDLE;
      }
    };

    protected abstract Phase next();
  }
}
