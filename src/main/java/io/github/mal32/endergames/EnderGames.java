package io.github.mal32.endergames;

import io.github.lambdaphoenix.advancementLib.AdvancementAPI;
import io.github.mal32.endergames.game.phases.PhaseController;
import io.github.mal32.endergames.kits.KitRegistry;
import io.github.mal32.endergames.lobby.MapManager;
import io.github.mal32.endergames.lobby.PlayerDifficulty;
import io.github.mal32.endergames.lobby.items.MenuManager;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.plugin.java.JavaPlugin;

public class EnderGames extends JavaPlugin {
  private final MapManager mapManager = new MapManager();
  private WorldManager worldManager;
  private PhaseController phaseController;
  private MenuManager menuManager;
  public final Analytics analytics = new Analytics(this);

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
    }
    analytics.sendAsync("startup", null);

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
    this.registerKitAdvancements();
    analytics.enable();
  }

  private void registerKitAdvancements() {
    AdvancementAPI advancementAPI = new AdvancementAPI(this);
    for (var kit : KitRegistry.getKits()) {
      kit.registerAdvancement(advancementAPI);
    }
  }

  @Override
  public void onDisable() {
    worldManager.disable();
    analytics.disable();
  }
}
