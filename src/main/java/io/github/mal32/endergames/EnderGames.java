package io.github.mal32.endergames;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.github.lambdaphoenix.advancementLib.AdvancementAPI;
import io.github.mal32.endergames.game.phases.PhaseController;
import io.github.mal32.endergames.kits.KitRegistry;
import io.github.mal32.endergames.lobby.MapManager;
import io.github.mal32.endergames.lobby.PlayerDifficulty;
import io.github.mal32.endergames.lobby.items.MenuManager;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bstats.bukkit.Metrics;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class EnderGames extends JavaPlugin {
  private final MapManager mapManager = new MapManager();
  private WorldManager worldManager;
  private PhaseController phaseController;
  private MenuManager menuManager;

  public static boolean isInDebugMode() {
    String debugEnv = System.getenv("EG_DEBUG");
    return debugEnv != null
        && (debugEnv.equalsIgnoreCase("true") || debugEnv.equalsIgnoreCase("1"));
  }

  public void changeMapPixelsInLobby(
      ArrayList<MapPixel> changedMapPixels, boolean forceFullUpdate) {
    mapManager.addToMapWall(changedMapPixels, forceFullUpdate);
  }

  public WorldManager getWorldManager() {
    return worldManager;
  }

  public PhaseController getPhaseController() {
    return phaseController;
  }

  public MenuManager getMenuManager() {
    return menuManager;
  }

  public void setMenuManager(MenuManager menuManager) {
    this.menuManager = menuManager;
  }

  @Override
  public void onEnable() {
    saveDefaultConfig();

    if (isInDebugMode()) {
      this.getComponentLogger().warn("Debug mode is enabled.");
      this.getComponentLogger().info(Component.text("Loaded Version ").append(getExactVersion()));
    } else {
      final int PLUGIN_ID = 25844;
      var metrics = new Metrics(this, PLUGIN_ID);
    }

    this.worldManager = new WorldManager(this);
    this.phaseController = new PhaseController(this, worldManager.getGameWorld());

    // TODO: move?
    this.menuManager = new MenuManager(this);
    var modules = List.of(new PlayerDifficulty(this));
    for (AbstractModule module : modules) {
      module.enable();
    }

    KitRegistry.registerKits(this);

    KDScoreboard kdScoreboard = new KDScoreboard(this);

    this.getLifecycleManager()
        .registerEventHandler(
            LifecycleEvents.COMMANDS,
            commands -> commands.registrar().register(endergamesCommand()));

    this.registerKitAdvancements();
  }

  private void registerKitAdvancements() {
    AdvancementAPI advancementAPI = new AdvancementAPI(this);
    for (var kit : KitRegistry.getKits()) {
      kit.registerAdvancement(advancementAPI);
    }
  }

  private LiteralCommandNode<CommandSourceStack> endergamesCommand() {
    return Commands.literal("endergames")
        .then(Commands.literal("version").executes(this::getVersionCommant))
        .build();
  }

  private int getVersionCommant(CommandContext<CommandSourceStack> ctx) {
    if (!(ctx.getSource().getSender() instanceof Player player)) return -1;

    Component versionText = getExactVersion();
    Component text =
        Component.text("EnderGames Version: ", NamedTextColor.YELLOW)
            .append(versionText.color(NamedTextColor.GOLD));

    player.sendMessage(text);

    return Command.SINGLE_SUCCESS;
  }

  private Component getExactVersion() {
    Properties props = new Properties();
    try {
      props.load(getClass().getClassLoader().getResourceAsStream("git.properties"));
    } catch (IOException e) {
      return Component.text("Could not load version information", NamedTextColor.RED);
    }

    String commitHash = props.getProperty("git.commit.id.abbrev");
    String commitBranch = props.getProperty("git.branch");
    String currentTag = props.getProperty("git.closest.tag.name");
    boolean isDirty = Boolean.parseBoolean(props.getProperty("git.dirty"));

    Component text = Component.text(currentTag + "-" + commitHash + "-" + commitBranch);
    if (isDirty) {
      text = text.append(Component.text("-dirty"));
    }

    return text;
  }

  @Override
  public void onDisable() {
    worldManager.disable();
  }
}
