package io.github.mal32.endergames.phases;

import io.github.mal32.endergames.EnderGames;
import java.time.Duration;
import java.util.List;
import java.util.Random;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.block.Biome;
import org.bukkit.block.structure.Mirror;
import org.bukkit.block.structure.StructureRotation;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.structure.Structure;
import org.bukkit.structure.StructureManager;
import org.bukkit.util.BlockVector;

public class StartPhase extends AbstractPhase {
  private final NamespacedKey spawnLocationKey;
  private final Location center;

  public StartPhase(EnderGames plugin) {
    super(plugin);

    this.spawnLocationKey = new NamespacedKey(plugin, "spawn");

    World world = Bukkit.getWorld("world_enga_world");
    this.center = new Location(world, -1000, 151, 0);
    this.reloadSpawnPosition();
    this.placeSpawnPlatform();
  }

  public Location getCenter() {
    return this.center;
  }

  //  Why doesnt BiomeTagKeys.IS_OCEAN work?
  // using directly:
  // https://github.com/misode/mcmeta/blob/data/data/minecraft/tags/worldgen/biome/is_ocean.json
  private boolean isOcean(Biome biome) {
    return biome.equals(Biome.DEEP_FROZEN_OCEAN)
        || biome.equals(Biome.DEEP_COLD_OCEAN)
        || biome.equals(Biome.DEEP_OCEAN)
        || biome.equals(Biome.DEEP_LUKEWARM_OCEAN)
        || biome.equals(Biome.FROZEN_OCEAN)
        || biome.equals(Biome.OCEAN)
        || biome.equals(Biome.COLD_OCEAN)
        || biome.equals(Biome.LUKEWARM_OCEAN)
        || biome.equals(Biome.WARM_OCEAN);
  }

  private void findNewSpawnLocation() {
    Location spawnLocationCandidate = this.center.clone();

    do {
      spawnLocationCandidate.add(1000, 0, 0);
      spawnLocationCandidate.getChunk().load(true);
    } while (isOcean(spawnLocationCandidate.getBlock().getBiome()));

    this.center.setX(spawnLocationCandidate.getX());
  }

  private void updateSpawn() {
    World world = this.center.getWorld();

    world
        .getPersistentDataContainer()
        .set(
            this.spawnLocationKey,
            PersistentDataType.LIST.listTypeFrom(PersistentDataType.INTEGER),
            List.of((int) this.center.getX(), (int) this.center.getZ()));

    world.setSpawnLocation(this.center);
    world.getWorldBorder().setCenter(this.center);
  }

  private void reloadSpawnPosition() {
    World world = this.center.getWorld();

    if (!world.getPersistentDataContainer().has(this.spawnLocationKey)) {
      Bukkit.getServer().sendMessage(Component.text("First EnderGames server start"));
      this.findNewSpawnLocation();
      this.updateSpawn();
    }

    List<Integer> rawSpawn =
        world
            .getPersistentDataContainer()
            .get(
                this.spawnLocationKey,
                PersistentDataType.LIST.listTypeFrom(PersistentDataType.INTEGER));
    this.center.setX(rawSpawn.get(0));
    this.center.setZ(rawSpawn.get(1));
  }

  @Override
  public void start() {
    Bukkit.getPluginManager().registerEvents(this, plugin);

    runCountdown();

    int playerindex = 0;
    final int totalPlayers = Bukkit.getServer().getOnlinePlayers().size();
    for (Player player : Bukkit.getServer().getOnlinePlayers()) { // TODO: playing players
      player.setGameMode(GameMode.SURVIVAL);
      player.getInventory().clear();

      teleportToPlayerSpawns(player, playerindex, totalPlayers);
      playerindex += 1;
    }
  }

  @Override
  public void stop() {
    HandlerList.unregisterAll(this);

    for (Player player : Bukkit.getServer().getOnlinePlayers()) {
      if (!EnderGames.playerIsPlaying(player)) return;
      player.clearActivePotionEffects();
    }
  }

  private void runCountdown() {
    BukkitScheduler scheduler = plugin.getServer().getScheduler();

    final int totalCountdownTimeSeconds = 30;
    var titleTimes = new Integer[] {1, 2, 3, 4, 5, 10, 15, 20, 25, 30};

    for (Integer titleTime : titleTimes) {
      final int scheduleTicks = (totalCountdownTimeSeconds - titleTime) * 20;
      scheduler.runTaskLater(
          plugin,
          () -> {
            for (Player player : Bukkit.getServer().getOnlinePlayers()) {
              if (EnderGames.playerIsIdeling(player)) continue;

              showTitleToPlayerWithSound(
                  player,
                  Component.text(titleTime + "").color(NamedTextColor.YELLOW),
                  Sound.BLOCK_NOTE_BLOCK_HARP);
            }
          },
          scheduleTicks);
    }

    // Game Start
    scheduler.runTaskLater(
        plugin,
        () -> {
          for (Player player : Bukkit.getServer().getOnlinePlayers()) {
            if (EnderGames.playerIsIdeling(player)) continue;
            showTitleToPlayerWithSound(
                player,
                Component.text("Start").color(NamedTextColor.GOLD),
                Sound.BLOCK_NOTE_BLOCK_FLUTE);
          }
        },
        totalCountdownTimeSeconds * 20);
    scheduler.runTaskLater(plugin, plugin::nextPhase, totalCountdownTimeSeconds * 20);
  }

  private void showTitleToPlayerWithSound(Player player, Component text, Sound sound) {
    final Title.Times titleTime =
        Title.Times.times(
            Duration.ofMillis(5 * 50), Duration.ofMillis(10 * 50), Duration.ofMillis(5 * 50));

    Title title = Title.title(text, Component.text(""), titleTime);
    player.showTitle(title);
    player.playSound(player.getLocation(), sound, 1, 1);
  }

  private void placeSpawnPlatform() {
    StructureManager manager = Bukkit.getServer().getStructureManager();
    Structure structure = manager.loadStructure(new NamespacedKey("enga", "spawn_platform"));

    BlockVector structureSize = structure.getSize();
    int posX = (int) (this.center.x() - (structureSize.getBlockX() / 2.0));
    int posZ = (int) (this.center.z() - (structureSize.getBlockZ() / 2.0));
    Location location = new Location(this.center.getWorld(), posX, this.center.getY(), posZ);
    structure.place(location, true, StructureRotation.NONE, Mirror.NONE, 0, 1.0f, new Random());
  }

  protected void replaceSpawn() {
    this.findNewSpawnLocation();
    this.updateSpawn();
    this.placeSpawnPlatform();
  }

  private void teleportToPlayerSpawns(
      Player player, int playerIndex, int totalPlayers) { // TODO: calculate with sin, cos
    if (totalPlayers == 0) return;

    List<BlockVector> offsets = makeSpawnOffsets();
    int offsetIndex = (playerIndex * offsets.size()) / totalPlayers;
    BlockVector offset = offsets.get(offsetIndex);

    World world = this.center.getWorld();
    double x = this.center.getX() + offset.getBlockX();
    double y = this.center.getY();
    double z = this.center.getZ() + offset.getBlockZ();
    Location dest =
        new Location(
            world, x - 0.5, y + 1.5,
            z + 0.5); // Add 0.5 on y and z to center player. Substract one from z because
    // spawnLocation is not the middle    TODO: fix spawnLocation is not the middle

    double dx = this.center.getX() - x;
    double dz = this.center.getZ() - z;
    float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
    dest.setYaw(yaw);
    dest.setPitch(0);

    // Teleport the player
    player.teleport(dest);
    plugin.getLogger().info("Teleporting to " + dest);
  }

  public List<BlockVector> makeSpawnOffsets() {
    // not correct yet
    return List.of(
        new BlockVector(0, 0, -9),
        new BlockVector(2, 0, -9),
        new BlockVector(4, 0, -8),
        new BlockVector(6, 0, -6),
        new BlockVector(8, 0, -4),
        new BlockVector(9, 0, -2),
        new BlockVector(9, 0, 0),
        new BlockVector(9, 0, 2),
        new BlockVector(8, 0, 4),
        new BlockVector(6, 0, 6),
        new BlockVector(4, 0, 8),
        new BlockVector(2, 0, 9),
        new BlockVector(0, 0, 9),
        new BlockVector(-2, 0, 9),
        new BlockVector(-4, 0, 8),
        new BlockVector(-6, 0, 6),
        new BlockVector(-8, 0, 4),
        new BlockVector(-9, 0, 2),
        new BlockVector(-9, 0, 0),
        new BlockVector(-9, 0, -2),
        new BlockVector(-8, 0, -4),
        new BlockVector(-6, 0, -6),
        new BlockVector(-4, 0, -8),
        new BlockVector(-2, 0, -9));
  }

  @EventHandler
  private void onPlayerMove(PlayerMoveEvent event) {
    Player player = event.getPlayer();
    if (!EnderGames.playerIsPlaying(player)) return;

    Location startLocation = event.getFrom();
    Location startLocationBlock = startLocation.getBlock().getLocation();
    Location endLocation = event.getTo();
    Location endLocationBlock = endLocation.getBlock().getLocation();

    if (startLocationBlock.getX() != endLocationBlock.getX()
        || startLocationBlock.getZ() != endLocationBlock.getZ()) {
      event.getTo().setX(startLocation.getX());
      event.getTo().setZ(startLocation.getZ());
    }
  }
}
