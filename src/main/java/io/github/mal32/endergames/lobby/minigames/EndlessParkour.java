package io.github.mal32.endergames.lobby.minigames;

import io.github.mal32.endergames.AbstractModule;
import io.github.mal32.endergames.BlockLocation;
import io.github.mal32.endergames.EnderGames;
import io.github.mal32.endergames.services.PlayerInWorld;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.jetbrains.annotations.Nullable;

public class EndlessParkour extends AbstractModule {
  private Map<UUID, ParkourSession> players = new HashMap<>();

  public EndlessParkour(EnderGames plugin) {
    super(plugin);
  }

  @EventHandler
  private void onStart(PlayerInteractEvent event) {
    Player player = event.getPlayer();
    if (!PlayerInWorld.LOBBY.is(player)) return;
    if (event.getAction() != Action.PHYSICAL) return;
    if (players.containsKey(player.getUniqueId())) return;
    Block block = event.getClickedBlock();
    if (block.getType() != Material.HEAVY_WEIGHTED_PRESSURE_PLATE) return;
    if (block.getLocation().add(0, -1, 0).getBlock().getType() != Material.WHITE_WOOL) return;

    DyeColor randomColor = DyeColor.values()[(int) (Math.random() * DyeColor.values().length)];

    BlockLocation startLocation = getRandomJumpLocation(new BlockLocation(player.getLocation()));
    startLocation.getBlock().setType(getWoolForColor(randomColor));

    Location teleportLocation = startLocation.toLocation();
    teleportLocation.setYaw(player.getYaw());
    teleportLocation.setPitch(player.getPitch());
    teleportLocation.add(0.5, 1, 0.5);
    player.teleport(teleportLocation, TeleportCause.COMMAND);

    players.put(player.getUniqueId(), new ParkourSession(randomColor, null, startLocation));

    nextBlock(player);
  }

  @EventHandler
  private void onDisconnect(PlayerQuitEvent event) {
    Player player = event.getPlayer();
    if (players.containsKey(player.getUniqueId())) {
      leave(player);
    }
  }

  @EventHandler
  private void onPlayerMove(PlayerMoveEvent event) {
    Player player = event.getPlayer();
    ParkourSession session = players.get(player.getUniqueId());
    if (session == null) return;

    Block groundBlock = player.getLocation().add(0, -0.1, 0).getBlock();
    if (groundBlock.isPassable()) return; // ground check
    if (session.nextBlock.equals(groundBlock.getLocation())) {
      nextBlock(player);
    } else if (((double) (session.currentBlock.getY()) - player.getLocation().getY()) > 0.5) {
      fail(player);
    }
  }

  private void nextBlock(Player player) {
    ParkourSession session = players.get(player.getUniqueId());
    if (session == null) return;

    player.playSound(player, Sound.BLOCK_NOTE_BLOCK_PLING, SoundCategory.UI, 1.0f, 2.0f);
    session.jumps++;
    Component actionBarText =
        Component.text("")
            .append(Component.text(session.jumps, NamedTextColor.GOLD))
            .append(Component.text(" Jumps", NamedTextColor.YELLOW));
    player.sendActionBar(actionBarText);

    if (session.currentBlock != null) {
      Block currentBlock = session.currentBlock.getBlock();
      if (Tag.WOOL.isTagged(currentBlock.getType())
          || Tag.TERRACOTTA.isTagged(currentBlock.getType())) {
        currentBlock.setType(Material.AIR);
      }
    }

    BlockLocation nextLocation = getRandomJumpLocation(session.nextBlock);
    nextLocation.getBlock().setType(getWoolForColor(session.color));

    session.currentBlock = session.nextBlock;
    session.nextBlock = nextLocation;

    session.currentBlock.getBlock().setType(getTerracotaForColor(session.color));
  }

  private BlockLocation getRandomJumpLocation(BlockLocation startLocation) {
    BlockLocation randomLocation = startLocation.clone();
    while (randomLocation.getBlock().getType() != Material.AIR) {
      randomLocation = startLocation.clone();

      if (Math.random() > 0.80) {
        // one higher
        double angle = Math.random() * 2 * Math.PI;
        double distance = 2.5 + Math.random() * 1.9;
        randomLocation.setX((int) (startLocation.getX() + 0.5 + Math.cos(angle) * distance));
        randomLocation.setZ((int) (startLocation.getZ() + 0.5 + Math.sin(angle) * distance));
        randomLocation.add(0, 1, 0);
      } else {
        // same level
        double angle = Math.random() * 2 * Math.PI;
        double distance = 2 + Math.random() * 3.5;
        randomLocation.setX((int) (startLocation.getX() + 0.5 + Math.cos(angle) * distance));
        randomLocation.setZ((int) (startLocation.getZ() + 0.5 + Math.sin(angle) * distance));
      }
    }

    return randomLocation;
  }

  @EventHandler
  private void onTeleport(PlayerTeleportEvent event) {
    if (event.getCause() != TeleportCause.PLUGIN) return;
    Player player = event.getPlayer();
    if (players.containsKey(player.getUniqueId())) leave(player);
  }

  private void fail(Player player) {
    player.playSound(player, Sound.ENTITY_ENDERMAN_TELEPORT, SoundCategory.UI, 1.0f, 0.5f);

    ParkourSession session = players.get(player.getUniqueId());
    if (session == null) return;

    leave(player);
  }

  private void leave(Player player) {
    ParkourSession session = players.get(player.getUniqueId());
    if (session == null) return;

    if (session.currentBlock != null) {
      Block currentBlock = session.currentBlock.getBlock();
      if (currentBlock.getType() != Material.AIR) {
        currentBlock.setType(Material.AIR);
      }
    }
    if (session.nextBlock != null) {
      Block nextBlock = session.nextBlock.getBlock();
      if (nextBlock.getType() != Material.AIR) {
        nextBlock.setType(Material.AIR);
      }
    }

    players.remove(player.getUniqueId());
  }

  private Material getWoolForColor(DyeColor color) {
    switch (color) {
      case BLACK:
        return Material.BLACK_WOOL;
      case BLUE:
        return Material.BLUE_WOOL;
      case BROWN:
        return Material.BROWN_WOOL;
      case CYAN:
        return Material.CYAN_WOOL;
      case GRAY:
        return Material.GRAY_WOOL;
      case GREEN:
        return Material.GREEN_WOOL;
      case LIGHT_BLUE:
        return Material.LIGHT_BLUE_WOOL;
      case LIGHT_GRAY:
        return Material.LIGHT_GRAY_WOOL;
      case LIME:
        return Material.LIME_WOOL;
      case MAGENTA:
        return Material.MAGENTA_WOOL;
      case ORANGE:
        return Material.ORANGE_WOOL;
      case PINK:
        return Material.PINK_WOOL;
      case PURPLE:
        return Material.PURPLE_WOOL;
      case RED:
        return Material.RED_WOOL;
      case WHITE:
        return Material.WHITE_WOOL;
      case YELLOW:
        return Material.YELLOW_WOOL;
      default:
        return Material.WHITE_WOOL;
    }
  }

  private Material getTerracotaForColor(DyeColor color) {
    switch (color) {
      case BLACK:
        return Material.BLACK_TERRACOTTA;
      case BLUE:
        return Material.BLUE_TERRACOTTA;
      case BROWN:
        return Material.BROWN_TERRACOTTA;
      case CYAN:
        return Material.CYAN_TERRACOTTA;
      case GRAY:
        return Material.GRAY_TERRACOTTA;
      case GREEN:
        return Material.GREEN_TERRACOTTA;
      case LIGHT_BLUE:
        return Material.LIGHT_BLUE_TERRACOTTA;
      case LIGHT_GRAY:
        return Material.LIGHT_GRAY_TERRACOTTA;
      case LIME:
        return Material.LIME_TERRACOTTA;
      case MAGENTA:
        return Material.MAGENTA_TERRACOTTA;
      case ORANGE:
        return Material.ORANGE_TERRACOTTA;
      case PINK:
        return Material.PINK_TERRACOTTA;
      case PURPLE:
        return Material.PURPLE_TERRACOTTA;
      case RED:
        return Material.RED_TERRACOTTA;
      case WHITE:
        return Material.WHITE_TERRACOTTA;
      case YELLOW:
        return Material.YELLOW_TERRACOTTA;
      default:
        return Material.WHITE_TERRACOTTA;
    }
  }
}

class ParkourSession {
  public DyeColor color;
  public BlockLocation currentBlock;
  public BlockLocation nextBlock;
  public int jumps = -1;

  public ParkourSession(
      DyeColor color, @Nullable BlockLocation currentBlock, @Nullable BlockLocation nextBlock) {
    this.color = color;
    this.currentBlock = currentBlock;
    this.nextBlock = nextBlock;
  }
}
