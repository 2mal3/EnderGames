package io.github.mal32.endergames.worlds.game.game;

import io.github.mal32.endergames.AbstractModule;
import io.github.mal32.endergames.EnderGames;
import io.github.mal32.endergames.worlds.game.GameWorld;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.LodestoneTracker;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public class Tracker extends AbstractModule {
  public Tracker(EnderGames plugin) {
    super(plugin);
  }

  @EventHandler
  private void onTrackerClick(PlayerInteractEvent event) {
    Player player = event.getPlayer();
    if (!GameWorld.playerIsInGame(player)) return;
    ItemStack item = event.getItem();
    if (item == null || item.getType() != Material.COMPASS) {
      return;
    }

    Player nearestPlayer = getNearestValidPlayer(player);
    if (nearestPlayer == null) return;

    Location targetLocation = nearestPlayer.getLocation();
    Location currentLocation = player.getLocation();
    int distance = (int) currentLocation.distance(targetLocation);
    Component actionBarMessage =
        Component.text()
            .append(Component.text("Tracking ", NamedTextColor.YELLOW))
            .append(Component.text(nearestPlayer.getName(), TextColor.fromHexString("#FFBA43")))
            .append(Component.text(": ", NamedTextColor.YELLOW))
            .append(Component.text(distance + " blocks", NamedTextColor.GREEN))
            .build();
    player.sendActionBar(actionBarMessage);
    item.setData(
        DataComponentTypes.LODESTONE_TRACKER,
        LodestoneTracker.lodestoneTracker().tracked(false).location(targetLocation).build());
  }

  @Nullable
  private Player getNearestValidPlayer(Player executor) {
    Player nearest = null;
    double nearestDistance = Double.MAX_VALUE;
    Location executorLocation = executor.getLocation();

    for (Player other : GameWorld.getPlayersInGame()) {
      if (other.equals(executor)) continue;

      double distance = executorLocation.distance(other.getLocation());
      if (distance < nearestDistance) {
        nearestDistance = distance;
        nearest = other;
      }
    }
    return nearest;
  }
}
