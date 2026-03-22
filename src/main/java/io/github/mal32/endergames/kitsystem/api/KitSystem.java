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
  private final KitManager manager;
  private final KitService service;

  public KitSystem(Plugin plugin) {
    this.plugin = Objects.requireNonNull(plugin);
    this.manager = new KitManager(plugin);
    this.service = new KitService(plugin, manager);
  }

  public KitManager manager() {
    return manager;
  }

  public KitService service() {
    return service;
  }

  public void enable() {
    plugin.getServer().getPluginManager().registerEvents(this, plugin);
  }

  public void disable() {
    manager.disableAll();
    HandlerList.unregisterAll(this);
  }

  @EventHandler
  public void onGameStart(GameStartEvent event) {
    Set<AbstractKit> toActivate = new LinkedHashSet<>();
    for (Player player : event.getPlayers()) {
      final AbstractKit kit = service.get(player);
      if (kit != null) {
        toActivate.add(kit);
        kit.initPlayer(player);
      }
    }

    toActivate.forEach(manager::enableKit);
  }

  @EventHandler
  public void onGameEnd(GameEndEvent event) {
    manager.disableAll();
  }

  @EventHandler
  public void onPlayerJoin(PlayerJoinEvent event) {
    final Player player = event.getPlayer();
    if (service.hasValidKit(player)) return;
    assert (manager.get(Lumberjack.id).isPresent());
    service.set(player, manager.get(Lumberjack.id).get());
  }
}
