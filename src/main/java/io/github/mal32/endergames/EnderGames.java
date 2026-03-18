package io.github.mal32.endergames;

import io.github.lambdaphoenix.advancementLib.AdvancementAPI;
import io.github.mal32.endergames.game.phases.PhaseController;
import io.github.mal32.endergames.kits.KitRegistry;
import io.github.mal32.endergames.lobby.MapManager;
import io.github.mal32.endergames.lobby.PlayerDifficulty;
import io.github.mal32.endergames.lobby.items.MenuManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
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

  // TODO: remove dev setup
  /* run this async */
  public void sendAnalyticsAsync(String event, Map<String, String> props) {
    Bukkit.getScheduler().runTaskAsynchronously(this, () -> sendAnalytics(event, props));
  }

  private void sendAnalytics(String event, Map<String, String> props) {
    if (isInDebugMode()) return;
    if (!this.getConfig().getBoolean("analytics.enabled")) return;

    String id = this.getConfig().getString("analytics.id");
    if (id == null) {
      id = UUID.randomUUID().toString();
      this.getConfig().set("analytics.id", id);
      Bukkit.getScheduler().runTaskAsynchronously(this, () -> this.saveConfig());
    }

    final String POSTHOG_API_KEY = "phc_410WhGEu4C930lfm2TAFRmucTFYJsUXyYxjOLzYCUNA";
    final String POSTHOG_HOST = "eu.i.posthog.com";
    String iso8601Time = Instant.now().toString();

    // I know its dirty but this frees us from a new dependency
    String jsonBody =
        String.format(
            "{\"api_key\": \"%s\", \"event\": \"%s\", \"timestamp\": \"%s\", \"properties\":"
                + " {\"distinct_id\": \"%s\"",
            POSTHOG_API_KEY, event, iso8601Time, id);
    if (props != null) {
      for (Map.Entry<String, String> entry : props.entrySet()) {
        jsonBody += String.format(", \"%s\": \"%s\"", entry.getKey(), entry.getValue());
      }
    }
    jsonBody += "}}";

    HttpClient client = HttpClient.newHttpClient();
    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create("https://" + POSTHOG_HOST + "/capture"))
            .header("Content-Type", "application/json")
            .POST(BodyPublishers.ofString(jsonBody))
            .build();

    try {
      client.send(request, HttpResponse.BodyHandlers.ofString());
    } catch (Exception ex) {
      this.getComponentLogger().warn("Could not send analytics: " + ex.toString());
    }
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
    sendAnalytics("startup", null);

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
  }
}
