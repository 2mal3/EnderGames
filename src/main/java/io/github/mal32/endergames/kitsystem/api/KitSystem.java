package io.github.mal32.endergames.kitsystem.api;

import io.github.mal32.endergames.game.phases.GameEndEvent;
import io.github.mal32.endergames.game.phases.GameStartEvent;
import io.github.mal32.endergames.kitsystem.kits.Lumberjack;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;

public final class KitSystem implements Listener {
  private final Plugin plugin;
  private final KitManager kitManager;
  private final KitService kitService;

  public KitSystem(Plugin plugin) {
    this.plugin = Objects.requireNonNull(plugin);
    this.kitManager = new KitManager(plugin);
    this.kitService = new KitService(plugin, kitManager);
  }

  public KitManager kitManager() {
    return kitManager;
  }

  public KitService kitService() {
    return kitService;
  }

  public void enable() {
    plugin.getServer().getPluginManager().registerEvents(this, plugin);
  }

  public void disable() {
    kitManager.disableAll();
    HandlerList.unregisterAll(this);
  }

  @EventHandler
  public void onGameStart(GameStartEvent event) {
    Set<AbstractKit> toActivate = new LinkedHashSet<>();
    for (Player player : event.getPlayers()) {
      final AbstractKit kit = kitService().get(player);
      if (kit != null) {
        toActivate.add(kit);
        kit.initPlayer(player);
      }
    }

    toActivate.forEach(kitManager::enableKit);
  }

  @EventHandler
  public void onGameEnd(GameEndEvent event) {
    kitManager.disableAll();
  }

  @EventHandler
  public void onPlayerJoin(PlayerJoinEvent event) {
    final Player player = event.getPlayer();
    if (kitService.playerHasKit(player)) return;
    kitService.set(player, kitManager.get(Lumberjack.id));
  }
}
