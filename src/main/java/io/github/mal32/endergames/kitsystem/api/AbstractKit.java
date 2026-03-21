package io.github.mal32.endergames.kitsystem.api;

import io.github.lambdaphoenix.advancementLib.AdvancementAPI;
import io.github.mal32.endergames.game.phases.PhaseController;
import java.util.Objects;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public abstract class AbstractKit implements Listener {
  protected final JavaPlugin plugin;
  private final KitDescription kitDescription;
  private final KitService kitService;

  public AbstractKit(KitDescription kitDescription, KitService kitService, JavaPlugin plugin) {
    this.kitDescription = Objects.requireNonNull(kitDescription);
    this.kitService = Objects.requireNonNull(kitService);
    this.plugin = Objects.requireNonNull(plugin);
  }

  public void onEnable() {}

  public void onDisable() {}

  protected boolean playerCanUseThisKit(Player player) {
    return player != null
        && PhaseController.playerIsInGame(player)
        && kitService.get(player).equals(this);
  }

  public abstract void initPlayer(Player player);

  public String id() {
    return kitDescription.displayName();
  }

  public KitDescription description() {
    return kitDescription;
  }

  public void registerAdvancement(AdvancementAPI api) {}
}
