package io.github.mal32.endergames.kits;

import io.github.mal32.endergames.EnderGames;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemEnchantments;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

public class ForestSpirit extends AbstractKit {

  private static final Color DEFAULT_DARK_GREEN = Color.fromRGB(0x1B5E20);

  // --- ability tuning ---
  private static final int GROWTH_COOLDOWN_SECONDS = 25;
  private static final double GROWTH_RADIUS = 12.0;

  // --- stillness / rooted tree tuning ---
  private static final int ROOTS_TRIGGER_TICKS = 20 * 15; // 15s standing still
  private static final int ROOTED_REGEN_LEVEL = 2; // Regen III (0=I,1=II,2=III)
  private static final int ROOTED_REGEN_DURATION_TICKS = 20 * 60 * 15; // long, removed on free

  // --- vulnerabilities ---
  private static final double FIRE_DAMAGE_MULTIPLIER = 2D;
  private static final double AXE_DAMAGE_MULTIPLIER = 1.4D;
  private static boolean healingTaskScheduled = false;
  private final Map<UUID, Long> growthCooldownUntil = new HashMap<>();
  private final Map<UUID, Integer> standStillTicks = new HashMap<>();
  private final Map<UUID, BlockKey> lastKnownBlockPos = new HashMap<>();
  // per-player kit start time, used to prevent rooting on start platform for first 20 seconds
  private final Map<UUID, Long> kitStartTimeMillis = new HashMap<>();
  // rooted tree state
  private final Map<UUID, RootedTreeState> rootedTrees = new HashMap<>();
  private final Map<BlockKey, UUID> rootLogOwner = new HashMap<>();
  private BukkitTask stillnessTask;
  private BukkitTask healingTask;
  private BukkitTask biomeAdaptTask;

  public ForestSpirit(EnderGames plugin) {
    super(plugin);
  }

  // ---------------------------------------------------------------------------
  // STARTING KIT
  // ---------------------------------------------------------------------------

  @Override
  public void enable() {
    super.enable();

    // - validates roots each tick so ANY destruction cause frees the player
    stillnessTask =
        plugin
            .getServer()
            .getScheduler()
            // delay 800 ticks (~40 seconds) so roots cannot trigger during the first 40s
            .runTaskTimer(plugin, this::tickStillnessAndRoots, 800L, 1L);

    // Passive: every 5 seconds, check for nearby logs and heal if surrounded by enough
    if (!healingTaskScheduled) {
      healingTask =
          plugin
              .getServer()
              .getScheduler()
              .runTaskTimer(plugin, this::tickLogAuraHealing, 60L, 60L);
      healingTaskScheduled = true;
    }

    // Every 3 seconds, adapt saplings and armor to the player's current biome
    biomeAdaptTask =
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::tickBiomeAdaptation, 40L, 40L);
  }

  @Override
  public void initPlayer(Player player) {
    Color biomeColor = getArmorColorForBiome(player.getLocation().getBlock().getBiome());

    ItemStack green_dye = new ItemStack(Material.GREEN_DYE);
    ItemMeta meta = green_dye.getItemMeta();
    meta.displayName(
        Component.text("Growth")
            .color(NamedTextColor.DARK_GREEN)
            .decoration(TextDecoration.ITALIC, false));
    green_dye.setItemMeta(meta);
    green_dye.setData(
        DataComponentTypes.ENCHANTMENTS,
        ItemEnchantments.itemEnchantments().add(Enchantment.VANISHING_CURSE, 1).build());
    player.getInventory().addItem(green_dye);

    player.getInventory().setHelmet(createSpiritArmorPiece(Material.LEATHER_HELMET, biomeColor));
    player
        .getInventory()
        .setChestplate(createSpiritArmorPiece(Material.LEATHER_CHESTPLATE, biomeColor));
    player
        .getInventory()
        .setLeggings(createSpiritArmorPiece(Material.LEATHER_LEGGINGS, biomeColor));
    player.getInventory().setBoots(createSpiritArmorPiece(Material.LEATHER_BOOTS, biomeColor));

    // give saplings adapted to the current biome
    Material saplingType = getSaplingForBiome(player.getLocation().getBlock().getBiome());
    player.getInventory().addItem(new ItemStack(saplingType, 20));

    // Init stillness tracking
    UUID id = player.getUniqueId();
    standStillTicks.put(id, 0);
    lastKnownBlockPos.put(id, BlockKey.of(player.getLocation()));
    // remember when this kit started for this player (used as a grace period)
    kitStartTimeMillis.put(id, System.currentTimeMillis());
  }

  private ItemStack createSpiritArmorPiece(Material type, Color color) {
    ItemStack item = colorLeatherArmor(new ItemStack(type), color);
    return enchantItem(item, Enchantment.THORNS, 2);
  }

  // ---------------------------------------------------------------------------
  // ACTIVE ABILITY: Growth (turn entities into trees)
  // ---------------------------------------------------------------------------

  @EventHandler
  private void onUseGrowth(PlayerInteractEvent event) {
    Player player = event.getPlayer();
    if (!playerCanUseThisKit(player)) return;
    ItemStack item = event.getItem();
    if (item == null || item.getType() != Material.GREEN_DYE) return;

    if (player.hasCooldown(Material.GREEN_DYE)) return;
    player.setCooldown(Material.GREEN_DYE, GROWTH_COOLDOWN_SECONDS * 20);
    activateGrowth(player);
  }

  private void activateGrowth(Player caster) {
    List<LivingEntity> targets = new ArrayList<>();
    for (Entity nearby :
        caster
            .getWorld()
            .getNearbyEntities(caster.getLocation(), GROWTH_RADIUS, GROWTH_RADIUS, GROWTH_RADIUS)) {
      if (!(nearby instanceof LivingEntity living)) continue;
      if (living.equals(caster)) continue;
      if (living.isDead()) continue;
      targets.add(living);
    }

    if (targets.isEmpty()) {
      createSmallForest(caster.getLocation());
      return;
    }

    for (LivingEntity target : targets) {
      turnEntityIntoTree(target, caster);
    }
  }

  private void turnEntityIntoTree(LivingEntity target, Player caster) {
    Location baseLoc = target.getLocation().getBlock().getLocation();
    World world = baseLoc.getWorld();
    if (world == null) return;

    // Determine biome and corresponding materials
    Biome biome = baseLoc.getBlock().getBiome();
    Material logType = getLogForBiome(biome);
    Material leavesType = getLeavesForBiome(biome);

    // 1) Try to generate a jungle tree at the entity's position
    Random rng = ThreadLocalRandom.current();
    boolean generated = world.generateTree(baseLoc, rng, TreeType.JUNGLE);

    int bX = baseLoc.getBlockX();
    int bY = baseLoc.getBlockY();
    int bZ = baseLoc.getBlockZ();

    // Center position we want to keep the target at (for later, if needed)
    Location trapCenter = new Location(world, bX + 0.5, bY, bZ + 0.5);

    if (!generated) {
      // Fallback: manually create a simple 2x2, 6-block-high log trunk with a leaf crown on top

      // Build 2x2 trunk, 6 blocks high
      for (int dy = 0; dy < 6; dy++) {
        for (int dx = 0; dx < 2; dx++) {
          for (int dz = 0; dz < 2; dz++) {
            Block trunkBlock = world.getBlockAt(bX + dx, bY + dy, bZ + dz);
            trunkBlock.setType(logType, false);
          }
        }
      }

      // Simple leaf crown on top of the trunk (one layer around and one on top center)
      int crownY = bY + 6;
      for (int dx = -2; dx <= 3; dx++) {
        for (int dz = -2; dz <= 3; dz++) {
          // Avoid making an enormous cube; keep it to a rough radius
          double dist = Math.sqrt(Math.pow(dx - 0.5, 2) + Math.pow(dz - 0.5, 2));
          if (dist > 3.0) continue;

          Block leafBlock = world.getBlockAt(bX + dx, crownY, bZ + dz);
          if (leafBlock.getType().isAir() || Tag.LEAVES.isTagged(leafBlock.getType())) {
            leafBlock.setType(leavesType, false);
          }
        }
      }

      // One extra leaf block as crown tip above the center of the trunk
      Block crownTip = world.getBlockAt(bX + 1, crownY + 1, bZ + 1);
      if (crownTip.getType().isAir() || Tag.LEAVES.isTagged(crownTip.getType())) {
        crownTip.setType(leavesType, false);
      }

      // Build a leaf wall around the base 2x2 trunk
      buildBaseLeafWall(world, bX, bY, bZ, leavesType);

      // Teleport the target into the center of the 2x2 trunk so it suffocates inside the tree
      Location center =
          new Location(
              world,
              bX + 1.0, // middle of the 2x2 in X
              bY,
              bZ + 1.0, // middle of the 2x2 in Z
              target.getLocation().getYaw(),
              target.getLocation().getPitch());
      target.teleport(center);

      // Nothing more to adapt in this fallback case
      return;
    }

    // 2) Adapt the mega jungle tree to the current biome

    // Define a rough bounding box where the mega jungle tree could exist
    // Mega jungle trees can be tall and wide; this is a conservative box
    int radiusX = 10;
    int radiusZ = 10;
    int height = 40; // from baseLoc.y up to baseLoc.y + height

    int baseX = baseLoc.getBlockX();
    int baseY = baseLoc.getBlockY();
    int baseZ = baseLoc.getBlockZ();

    String biomeKey = biome.getKey().value().toLowerCase(Locale.ROOT);
    boolean isJungleOrSwamp =
        biomeKey.contains("jungle") || biomeKey.contains("swamp") || biomeKey.contains("mangrove");

    for (int x = baseX - radiusX; x <= baseX + radiusX; x++) {
      for (int y = baseY; y <= baseY + height; y++) {
        for (int z = baseZ - radiusZ; z <= baseZ + radiusZ; z++) {
          Block block = world.getBlockAt(x, y, z);
          Material type = block.getType();

          // Optionally remove vines when not in jungle/swamp biomes
          if (!isJungleOrSwamp && type == Material.VINE) {
            block.setType(Material.AIR, false);
            continue;
          }

          // Replace logs with biome-appropriate log type
          if (Tag.LOGS.isTagged(type)) {
            block.setType(logType, false);
            continue;
          }

          // Replace leaves with biome-appropriate leaves type
          if (Tag.LEAVES.isTagged(type)) {
            block.setType(leavesType, false);
          }
        }
      }
    }

    // Build a leaf wall around the base of the (generated) 2x2 trunk area
    buildBaseLeafWall(world, baseX, baseY, baseZ, leavesType);

    // After adaptation, also put the target roughly in the center of the trunk area
    Location center =
        new Location(
            world,
            trapCenter.getX(),
            trapCenter.getY(),
            trapCenter.getZ(),
            target.getLocation().getYaw(),
            target.getLocation().getPitch());
    target.teleport(center);
  }

  /**
   * Builds a ring of leaves around a 2x2 trunk at (baseX..baseX+1, baseZ..baseZ+1). Pattern (top
   * view, one layer): . L L . L T T L L T T L . L L . The walls are 2 blocks high, with a 50%
   * chance to add a 3rd layer. Additionally, with a small chance extra leaves are placed one block
   * further out.
   */
  private void buildBaseLeafWall(
      World world, int baseX, int baseY, int baseZ, Material leavesType) {
    Random rng = ThreadLocalRandom.current();

    // Positions around the 2x2 trunk (offsets relative to baseX/baseZ)
    int[][] ringOffsets = {
      // north row
      {0, -1},
      {1, -1},
      {2, -1},
      // middle rows
      {-1, 0},
      {-1, 1},
      {2, 0},
      {2, 1},
      // south row
      {0, 2},
      {1, 2},
      {2, 2}
    };

    // Build walls 2 blocks high, with 50% chance for a 3rd block
    for (int[] off : ringOffsets) {
      int x = baseX + off[0];
      int z = baseZ + off[1];

      int maxHeight = 2 + (rng.nextBoolean() ? 1 : 0); // 2 or 3 high
      for (int dy = 0; dy < maxHeight; dy++) {
        int y = baseY + dy;
        Block b = world.getBlockAt(x, y, z);
        Material type = b.getType();
        if (type.isAir() || Tag.LEAVES.isTagged(type)) {
          b.setType(leavesType, false);
        }
      }
    }

    // Small chance to add extra leaves one block further out for a natural look
    int[][] outerOffsets = {
      {0, -2}, {1, -2}, {2, -2}, {-2, 0}, {-2, 1}, {3, 0}, {3, 1}, {0, 3}, {1, 3}, {2, 3}
    };

    for (int[] off : outerOffsets) {
      if (rng.nextDouble() > 0.35) continue; // ~35% chance to place an outer leaf column

      int x = baseX + off[0];
      int z = baseZ + off[1];

      int maxHeight = 1 + (rng.nextBoolean() ? 1 : 0); // 1 or 2 blocks high
      for (int dy = 0; dy < maxHeight; dy++) {
        int y = baseY + dy + 1;
        Block b = world.getBlockAt(x, y, z);
        Material type = b.getType();
        if (type.isAir() || Tag.LEAVES.isTagged(type)) {
          b.setType(leavesType, false);
        }
      }
    }
  }

  private void createSmallForest(Location center) {
    World world = center.getWorld();
    if (world == null) return;

    Random rng = ThreadLocalRandom.current();
    int radius = 15;

    int centerX = center.getBlockX();
    int centerZ = center.getBlockZ();

    // Collect all valid ground blocks inside the radius
    List<Location> candidateGround = new ArrayList<>();
    for (int dx = -radius; dx <= radius; dx++) {
      for (int dz = -radius; dz <= radius; dz++) {
        if (dx * dx + dz * dz > radius * radius) continue;

        int x = centerX + dx;
        int z = centerZ + dz;

        int topY = world.getHighestBlockYAt(x, z);
        if (topY <= world.getMinHeight()) continue;

        // Ground block is the block directly below the top exposed block
        Block ground = world.getBlockAt(x, topY, z);
        if (isValidTreeGround(ground.getType())) {
          candidateGround.add(ground.getLocation());
        }
      }
    }

    int minDesiredGround = 18;
    if (candidateGround.size() < minDesiredGround) {
      candidateGround.clear();
      for (int dx = -radius; dx <= radius; dx++) {
        for (int dz = -radius; dz <= radius; dz++) {
          if (dx * dx + dz * dz > radius * radius) continue;

          int x = centerX + dx;
          int z = centerZ + dz;

          int topY = world.getHighestBlockYAt(x, z);
          if (topY <= world.getMinHeight()) continue;

          Block surface = world.getBlockAt(x, topY, z);

          // Don't replace important/indestructible blocks
          Material current = surface.getType();
          if (current == Material.ENDER_CHEST
              || current == Material.OBSIDIAN
              || current == Material.ENCHANTING_TABLE
              || current == Material.BEDROCK
              || Tag.LEAVES.isTagged(current)) {
            continue;
          }

          // Randomly choose between moss, coarse dirt and dirt
          double r = rng.nextDouble();
          Material newType;
          if (r < 0.62) {
            newType = Material.MOSS_BLOCK;
          } else if (r < 0.85) {
            newType = Material.COARSE_DIRT;
          } else {
            newType = Material.DIRT;
          }
          surface.setType(newType, false);
          candidateGround.add(surface.getLocation());
          // set biome to forest
          int minBiomeY = Math.max(world.getMinHeight(), topY - 2);
          int maxBiomeY = Math.min(world.getMaxHeight() - 1, topY + 14);
          for (int y = minBiomeY; y <= maxBiomeY; y++) {
            world.setBiome(x, y, z, Biome.FOREST);
          }
        }
      }
    }

    // Shuffle and take up to 18 positions
    Collections.shuffle(candidateGround, rng);
    int saplingsToPlace = Math.min(18, candidateGround.size());

    for (int i = 0; i < saplingsToPlace; i++) {
      Location groundLoc = candidateGround.get(i);

      int baseX = groundLoc.getBlockX();
      int baseY = groundLoc.getBlockY() + 1; // tree base is one block above the ground
      int baseZ = groundLoc.getBlockZ();

      Location treeBase = new Location(world, baseX, baseY, baseZ);

      // Optionally, set a sapling block at the tree base first (not strictly required for
      // generateTree, but keeps the world consistent if you inspect it between ticks):
      Block baseBlock = treeBase.getBlock();
      // set the block to air before tree generation in case there is snow etc
      baseBlock.setType(Material.AIR, false);

      // Grow a tree using the same biome-based tree selection as sapling placement
      Biome biomeAtSpot = baseBlock.getBiome();
      TreeType type = getTreeTypeforBiome(biomeAtSpot);

      world.generateTree(treeBase, rng, type);
    }
  }

  private boolean isValidTreeGround(Material type) {
    return type == Material.DIRT
        || type == Material.COARSE_DIRT
        || type == Material.GRASS_BLOCK
        || type == Material.PODZOL
        || type == Material.ROOTED_DIRT
        || type == Material.MOSS_BLOCK;
  }

  @EventHandler
  private void onInteractEntityWithGrowthItem(
      org.bukkit.event.player.PlayerInteractEntityEvent event) {
    Player player = event.getPlayer();
    if (!playerCanUseThisKit(player)) return;

    ItemStack item =
        event.getHand() == org.bukkit.inventory.EquipmentSlot.HAND
            ? player.getInventory().getItemInMainHand()
            : player.getInventory().getItemInOffHand();

    if (!isGrowthItem(item)) return;

    // Block vanilla interaction such as dyeing sheep
    event.setCancelled(true);
  }

  private boolean isGrowthItem(ItemStack stack) {
    if (stack == null || stack.getType() != Material.GREEN_DYE) return false;
    ItemMeta meta = stack.getItemMeta();
    if (meta == null || !meta.hasDisplayName()) return false;
    Component name = meta.displayName();
    if (name == null) return false;
    // Simple textual check; you can tighten this if needed
    return Component.text("Growth")
        .color(NamedTextColor.DARK_GREEN)
        .decoration(TextDecoration.ITALIC, false)
        .equals(name);
  }

  // ---------------------------------------------------------------------------
  // PASSIVE: Stillness -> roots + full tree state
  // ---------------------------------------------------------------------------

  private void tickStillnessAndRoots() {
    for (Player player : plugin.getServer().getOnlinePlayers()) {
      if (!playerCanUseThisKit(player)) continue;
      if (player.isDead()) continue;

      UUID id = player.getUniqueId();

      // rooted players: validate roots every tick so ANY break cause frees immediately
      RootedTreeState rooted = rootedTrees.get(id);
      if (rooted != null) {
        if (!areRootsIntact(rooted)) {
          freeRootedPlayer(id, true);
        }
        continue;
      }

      Location loc = player.getLocation();
      BlockKey currentPos = BlockKey.of(loc);
      BlockKey lastPos = lastKnownBlockPos.get(id);

      if (lastPos == null || !lastPos.equals(currentPos)) {
        lastKnownBlockPos.put(id, currentPos);
        standStillTicks.put(id, 0);
      } else {
        int ticks = standStillTicks.getOrDefault(id, 0) + 1;
        standStillTicks.put(id, ticks);

        if (ticks >= ROOTS_TRIGGER_TICKS) {
          Biome biomeHere = loc.getBlock().getBiome();

          if (isDesertLikeBiome(biomeHere)) {
            growDesertShrubAtFeet(player);
          } else {
            rootPlayerIntoTree(player);
          }

          // Reset timers / position tracking after the trigger, regardless of biome.
          standStillTicks.put(id, 0);
          lastKnownBlockPos.put(id, currentPos);
        }
      }
    }
  }

  /** Returns true for any biome where the Forest Spirit should not root, only grow shrubs. */
  private boolean isDesertLikeBiome(Biome biome) {
    String key = biome.getKey().value().toUpperCase(Locale.ROOT);
    if (key.contains("DESERT")) return true;
    if (key.contains("BADLANDS")) return true;
    return false;
  }

  /** Places a dead bush at the player's feet in desert-like biomes, if terrain allows it. */
  private void growDesertShrubAtFeet(Player player) {
    Location base = player.getLocation().getBlock().getLocation();
    World world = base.getWorld();
    if (world == null) return;

    Block feet = base.getBlock();
    Block below = feet.getRelative(0, -1, 0);

    Material belowType = below.getType();
    boolean validSoil =
        belowType == Material.SAND
            || belowType == Material.RED_SAND
            || belowType == Material.TERRACOTTA
            || belowType == Material.WHITE_TERRACOTTA
            || belowType == Material.ORANGE_TERRACOTTA
            || belowType == Material.MAGENTA_TERRACOTTA
            || belowType == Material.LIGHT_BLUE_TERRACOTTA
            || belowType == Material.YELLOW_TERRACOTTA
            || belowType == Material.LIME_TERRACOTTA
            || belowType == Material.PINK_TERRACOTTA
            || belowType == Material.GRAY_TERRACOTTA
            || belowType == Material.LIGHT_GRAY_TERRACOTTA
            || belowType == Material.CYAN_TERRACOTTA
            || belowType == Material.PURPLE_TERRACOTTA
            || belowType == Material.BLUE_TERRACOTTA
            || belowType == Material.BROWN_TERRACOTTA
            || belowType == Material.GREEN_TERRACOTTA
            || belowType == Material.RED_TERRACOTTA
            || belowType == Material.BLACK_TERRACOTTA
            || belowType == Material.DIRT
            || belowType == Material.COARSE_DIRT
            || belowType == Material.GRASS_BLOCK
            || belowType == Material.PODZOL;

    if (validSoil && (feet.getType().isAir() || feet.isPassable())) {
      feet.setType(Material.DEAD_BUSH, false);
    }
  }

  private void rootPlayerIntoTree(Player player) {
    UUID id = player.getUniqueId();
    if (rootedTrees.containsKey(id)) return;

    Location base = player.getLocation().getBlock().getLocation();
    Biome biome = base.getBlock().getBiome();

    Material log = getLogForBiome(biome);
    Material leaves = getLeavesForBiome(biome);

    RootedTreeState state = new RootedTreeState(id);
    // remember where the player was rooted (block position)
    state.rootOrigin = BlockKey.of(base);

    // roots: straight down 3 blocks
    placeRooted(state, base.clone().add(0, -1, 0), log, true, true);
    placeRooted(state, base.clone().add(0, -2, 0), log, true, true);
    placeRooted(state, base.clone().add(0, -3, 0), log, true, true);

    // one side branch (only one direction, little branch)
    int side = ThreadLocalRandom.current().nextInt(4);
    int dx = 0;
    int dz = 0;
    if (side == 0) dx = 1;
    if (side == 1) dx = -1;
    if (side == 2) dz = 1;
    if (side == 3) dz = -1;

    placeRooted(state, base.clone().add(dx, -2, dz), log, true, true);
    placeRooted(state, base.clone().add(dx, -3, dz), log, true, true);

    // trunk above player (starts above head so player is not inside solid block)
    placeRooted(state, base.clone().add(0, 2, 0), log, false, false);
    placeRooted(state, base.clone().add(0, 3, 0), log, false, false);

    // leaves: 2 layers, circular-ish / random / not square
    buildNaturalCanopy(state, base, leaves);

    Location feetLoc = base.clone();
    Block baseBlock = feetLoc.getBlock();
    state.originalBlocks.putIfAbsent(BlockKey.of(baseBlock.getLocation()), baseBlock.getType());
    baseBlock.setType(Material.AIR, false);

    Block eyeLoc = baseBlock.getRelative(0, 1, 0);
    state.originalBlocks.putIfAbsent(BlockKey.of(eyeLoc.getLocation()), eyeLoc.getType());
    eyeLoc.setType(Material.AIR, false);

    rootedTrees.put(id, state);

    player.addPotionEffect(
        new PotionEffect(
            PotionEffectType.REGENERATION,
            ROOTED_REGEN_DURATION_TICKS,
            ROOTED_REGEN_LEVEL,
            true,
            true));
  }

  private void buildNaturalCanopy(RootedTreeState state, Location base, Material leaves) {
    ThreadLocalRandom rng = ThreadLocalRandom.current();

    // Lower canopy layer (y + 2): wider
    for (int dx = -2; dx <= 2; dx++) {
      for (int dz = -2; dz <= 2; dz++) {
        if (dx == 0 && dz == 0) continue; // trunk center
        double dist = Math.sqrt(dx * dx + dz * dz);
        if (dist > 2.2) continue;

        // random shaping so it's not a perfect disk/square
        double chance = dist <= 1.3 ? 0.95 : 0.70;
        if (rng.nextDouble() <= chance) {
          placeRooted(state, base.clone().add(dx, 2, dz), leaves, false, false);
        }
      }
    }

    // Upper canopy layer (y + 3): tighter half-dome
    for (int dx = -2; dx <= 2; dx++) {
      for (int dz = -2; dz <= 2; dz++) {
        double dist = Math.sqrt(dx * dx + dz * dz);
        if (dist > 1.8) continue;

        double chance = dist <= 1.0 ? 0.95 : 0.65;
        if (ThreadLocalRandom.current().nextDouble() <= chance) {
          placeRooted(state, base.clone().add(dx, 3, dz), leaves, false, false);
        }
      }
    }

    // crown tip
    placeRooted(state, base.clone().add(0, 4, 0), leaves, false, false);
  }

  private boolean areRootsIntact(RootedTreeState state) {
    for (BlockKey key : state.rootLogs) {
      Block block = key.toBlock();
      if (block == null) continue; // world unloaded -> ignore this block
      if (!Tag.LOGS.isTagged(block.getType())) {
        return false;
      }
    }
    return true;
  }

  // Prevent moving while rooted (can still rotate camera)
  @EventHandler
  private void onMoveWhileRooted(PlayerMoveEvent event) {
    Player player = event.getPlayer();
    if (!playerCanUseThisKit(player)) return;

    UUID id = player.getUniqueId();
    RootedTreeState state = rootedTrees.get(id);

    // Biome-based adaptation is now handled by a periodic task.

    if (state == null) return;

    Location from = event.getFrom();
    Location to = event.getTo();
    if (to == null) return;

    // If the player has been teleported / moved more than 1 block away from
    // the original rooted position, free them completely.
    if (state.rootOrigin != null) {
      Block originBlock = state.rootOrigin.toBlock();
      if (originBlock != null) {
        Location originLoc = originBlock.getLocation().add(0.5, 0, 0.5);
        if (originLoc.getWorld() != null
            && to.getWorld() != null
            && originLoc.getWorld().equals(to.getWorld())
            && originLoc.distanceSquared(to) > 1.0) {
          freeRootedPlayer(id, true);
          return;
        }
      }
    }

    boolean changedBlock =
        from.getBlockX() != to.getBlockX()
            || from.getBlockY() != to.getBlockY()
            || from.getBlockZ() != to.getBlockZ();

    if (changedBlock) {
      Location locked = from.clone();
      locked.setYaw(to.getYaw());
      locked.setPitch(to.getPitch());
      event.setTo(locked);

      // Inform the player via actionbar how to free themselves
      player.sendActionBar(
          Component.text("You grew roots. Remove the wood beneath you to free yourself.")
              .color(TextColor.color(0x6B4F2A))); // greenish-brown
    }
  }

  // If ANY root log breaks (player, TNT, etc.) -> free instantly
  @EventHandler
  private void onRootLogBreak(BlockBreakEvent event) {
    UUID owner = rootLogOwner.get(BlockKey.of(event.getBlock().getLocation()));
    if (owner != null) {
      freeRootedPlayer(owner, true);
    }
  }

  @EventHandler
  private void onRootLogEntityExplode(EntityExplodeEvent event) {
    Set<UUID> owners = new HashSet<>();
    for (Block block : event.blockList()) {
      UUID owner = rootLogOwner.get(BlockKey.of(block.getLocation()));
      if (owner != null) owners.add(owner);
    }
    for (UUID owner : owners) freeRootedPlayer(owner, true);
  }

  @EventHandler
  private void onRootLogBlockExplode(BlockExplodeEvent event) {
    Set<UUID> owners = new HashSet<>();
    for (Block block : event.blockList()) {
      UUID owner = rootLogOwner.get(BlockKey.of(block.getLocation()));
      if (owner != null) owners.add(owner);
    }
    for (UUID owner : owners) freeRootedPlayer(owner, true);
  }

  private void freeRootedPlayer(UUID playerId, boolean restoreBlocks) {
    RootedTreeState state = rootedTrees.remove(playerId);
    if (state == null) return;

    // remove ownership map first
    for (BlockKey key : state.rootLogs) {
      rootLogOwner.remove(key);
    }

    if (restoreBlocks) {
      for (Map.Entry<BlockKey, Material> entry : state.originalBlocks.entrySet()) {
        Block block = entry.getKey().toBlock();
        if (block == null) continue;
        block.setType(entry.getValue());
      }
    }

    Player player = plugin.getServer().getPlayer(playerId);
    if (player != null) {
      player.removePotionEffect(PotionEffectType.REGENERATION);
      standStillTicks.put(playerId, 0);
      lastKnownBlockPos.put(playerId, BlockKey.of(player.getLocation()));
    }
  }

  // ---------------------------------------------------------------------------
  // Existing / other passives (left mostly as-is)
  // ---------------------------------------------------------------------------

  @EventHandler
  private void onSaplingPlaced(BlockPlaceEvent event) {
    Player player = event.getPlayer();
    if (!playerCanUseThisKit(player)) return;
    if (!Tag.SAPLINGS.isTagged(event.getBlockPlaced().getType())) return;

    Block saplingBlock = event.getBlockPlaced();
    Biome biome = saplingBlock.getBiome();
    Material originalSaplingType = saplingBlock.getType();
    UUID playerId = player.getUniqueId();

    // Decide what kind of tree to grow based on the biome's wood family
    TreeType treeType = getTreeTypeforBiome(biome);

    // Let the sapling be placed normally, but on the next tick
    // remove it and grow a full tree at that position.
    Location treeLocation = saplingBlock.getLocation().clone();
    Bukkit.getScheduler()
        .runTask(
            plugin,
            () -> {
              Block block = treeLocation.getBlock();

              // If it's still a sapling, clear it before attempting to grow the tree
              if (Tag.SAPLINGS.isTagged(block.getType())) {
                block.setType(Material.AIR);
              }

              Random rng = ThreadLocalRandom.current();
              boolean success = block.getWorld().generateTree(treeLocation, rng, treeType);

              // If the tree failed to generate (e.g., too little space), refund one sapling
              if (!success) {
                Player current = Bukkit.getPlayer(playerId);
                ItemStack refund = new ItemStack(originalSaplingType, 1);

                if (current != null && current.isOnline()) {
                  var leftover = current.getInventory().addItem(refund);
                  // If inventory is full, drop leftover at the sapling location
                  for (ItemStack remaining : leftover.values()) {
                    if (remaining == null || remaining.getAmount() <= 0) continue;
                    block
                        .getWorld()
                        .dropItemNaturally(treeLocation.clone().add(0.5, 0.1, 0.5), remaining);
                  }
                } else {
                  // Player is offline; drop the sapling at the location
                  block
                      .getWorld()
                      .dropItemNaturally(treeLocation.clone().add(0.5, 0.1, 0.5), refund);
                }
              }
            });
  }

  @EventHandler
  private void onDeath(PlayerDeathEvent event) {
    Player player = event.getEntity();

    // If rooted when dying, clean up rooted state instantly.
    if (rootedTrees.containsKey(player.getUniqueId())) {
      freeRootedPlayer(player.getUniqueId(), true);
    }

    if (!playerCanUseThisKit(player)) return;
    UUID id = event.getPlayer().getUniqueId();
    if (rootedTrees.containsKey(id)) freeRootedPlayer(id, true);
    standStillTicks.remove(id);
    lastKnownBlockPos.remove(id);
    growthCooldownUntil.remove(id);
    kitStartTimeMillis.remove(id);

    // Passive: grow a tree at death location adapted to the biome.
    Location baseLoc = player.getLocation().getBlock().getLocation();
    World world = baseLoc.getWorld();
    if (world != null) {
      Biome biome = baseLoc.getBlock().getBiome();

      if (isDesertLikeBiome(biome)) {
        generateDriedDesertMegatree(world, baseLoc);
        return;
      }

      Material logType = getLogForBiome(biome);
      Material leavesType = getLeavesForBiome(biome);

      // First try to generate a mega spruce-like tree at the death position.
      // Use TreeType.MEGA_REDWOOD as the in-game analog for minecraft:mega_spruce.
      Random rng = ThreadLocalRandom.current();
      boolean generated = world.generateTree(baseLoc, rng, TreeType.MEGA_REDWOOD);

      if (generated) {
        // Adapt the generated tree's logs and leaves to the biome, similar to turnEntityIntoTree.
        int radiusX = 10;
        int radiusZ = 10;
        int height = 45;
        int baseX = baseLoc.getBlockX();
        int baseY = baseLoc.getBlockY();
        int baseZ = baseLoc.getBlockZ();

        int startY = baseY - 5;

        String biomeKey = biome.getKey().value().toLowerCase(Locale.ROOT);
        boolean isSpruceNative =
            biomeKey.contains("taiga")
                || biomeKey.contains("grove")
                || biomeKey.contains("snowy")
                || biomeKey.contains("windswept");

        for (int x = baseX - radiusX; x <= baseX + radiusX; x++) {
          for (int y = startY; y <= startY + height; y++) {
            for (int z = baseZ - radiusZ; z <= baseZ + radiusZ; z++) {
              Block block = world.getBlockAt(x, y, z);
              Material type = block.getType();

              // Clean up podzol ground into fresh grass.
              if (type == Material.PODZOL) {
                block.setType(Material.GRASS_BLOCK, false);
                continue;
              }

              if (!isSpruceNative && type == Material.VINE) {
                block.setType(Material.AIR, false);
                continue;
              }

              if (Tag.LOGS.isTagged(type)) {
                block.setType(logType, false);
                continue;
              }

              if (Tag.LEAVES.isTagged(type)) {
                block.setType(leavesType, false);
              }
            }
          }
        }
      } else {
        // Mega tree generation failed: place sapling
        Material saplingType = getSaplingForBiome(biome);
        Block saplingBlock = baseLoc.getBlock();
        Block belowSapling = saplingBlock.getRelative(0, -1, 0);
        if (saplingBlock.getType().isAir() || Tag.LEAVES.isTagged(saplingBlock.getType())) {
          saplingBlock.setType(saplingType, false);
          belowSapling.setType(Material.COARSE_DIRT, false);
        }
      }
    }
  }

  /**
   * Generates a dried, desert-themed mega tree: a 2x2 pale oak trunk (tall) with many diagonal pale
   * oak wood branches that get shorter towards the top, and a small cluster of oak-wood columns
   * capping the 2x2 top.
   */
  private void generateDriedDesertMegatree(World world, Location baseLoc) {
    Random rng = ThreadLocalRandom.current();

    int baseX = baseLoc.getBlockX();
    int baseY = baseLoc.getBlockY();
    int baseZ = baseLoc.getBlockZ();

    // Make the trunk significantly taller: 14..22 blocks.
    int height = 14 + rng.nextInt(9); // 14..22 inclusive

    Material trunkMat = materialOrDefault("PALE_OAK_LOG", Material.OAK_LOG);
    Material branchMat = materialOrDefault("PALE_OAK_WOOD", trunkMat);

    // Build 2x2 vertical trunk
    for (int dy = 0; dy < height; dy++) {
      int y = baseY + dy;
      world.getBlockAt(baseX, y, baseZ).setType(trunkMat, false);
      world.getBlockAt(baseX + 1, y, baseZ).setType(trunkMat, false);
      world.getBlockAt(baseX, y, baseZ + 1).setType(trunkMat, false);
      world.getBlockAt(baseX + 1, y, baseZ + 1).setType(trunkMat, false);
    }

    // Define potential diagonal/side branch directions (dx,dz) from trunk center.
    int[][] diagonalDirs = {
      {1, 0}, // east
      {-1, 0}, // west
      {0, 1}, // south
      {0, -1}, // north
      {1, 1}, // southeast
      {1, -1}, // northeast
      {-1, 1}, // southwest
      {-1, -1} // northwest
    };

    // Place more branches along the height, lower ones being longer than upper ones.
    int branchCount = 8 + rng.nextInt(5); // 8-12 branches (slightly more than before)
    for (int i = 0; i < branchCount; i++) {
      // Bias branch start towards mid/lower part but allow some high ones.
      int branchBaseY = baseY + 2 + rng.nextInt(Math.max(3, height - 4));

      // Relative height factor (0 at bottom, 1 at top)
      double t = (branchBaseY - baseY) / (double) Math.max(1, height - 1);

      // Longer at bottom (~6), shorter at top (~3)
      int maxLenBottom = 6;
      int minLenTop = 3;
      int branchLen = (int) Math.round(maxLenBottom - t * (maxLenBottom - minLenTop));
      branchLen = Math.max(minLenTop, Math.min(maxLenBottom, branchLen));

      // Pick a random direction
      int[] dir = diagonalDirs[rng.nextInt(diagonalDirs.length)];
      int dx = dir[0];
      int dz = dir[1];

      // Start from side/edge of 2x2 trunk in that direction
      double startX = baseX + 0.5 + 0.7 * dx;
      double startZ = baseZ + 0.5 + 0.7 * dz;

      double x = startX;
      double y = branchBaseY;
      double z = startZ;

      for (int step = 0; step < branchLen; step++) {
        int bx = (int) Math.round(x);
        int by = (int) Math.round(y);
        int bz = (int) Math.round(z);

        Block b = world.getBlockAt(bx, by, bz);
        if (b.getType().isAir() || b.isPassable() || Tag.LEAVES.isTagged(b.getType())) {
          b.setType(branchMat, false);
        }

        // Move outward and slightly upward. Make branches a bit more vertical now:
        // previously ~1.0..0.5; now ~1.1..0.7 for a slightly steeper angle.
        double verticalStep = 1.1 - 0.4 * t; // bottom ~1.1, top ~0.7
        x += dx;
        z += dz;
        y += verticalStep;
      }
    }

    // Cap the top of the 2x2 trunk with 1-3 block high oak_wood columns.
    Material capMat = Material.PALE_OAK_WOOD;
    int topY = baseY + height;

    for (int tx = baseX; tx <= baseX + 1; tx++) {
      for (int tz = baseZ; tz <= baseZ + 1; tz++) {
        int capHeight = 1 + rng.nextInt(3); // 1..3
        for (int dy = 0; dy < capHeight; dy++) {
          Block capBlock = world.getBlockAt(tx, topY + dy, tz);
          if (capBlock.getType().isAir()
              || capBlock.isPassable()
              || Tag.LEAVES.isTagged(capBlock.getType())) {
            capBlock.setType(capMat, false);
          }
        }
      }
    }
  }

  @EventHandler
  private void onQuit(PlayerQuitEvent event) {
    UUID id = event.getPlayer().getUniqueId();
    if (rootedTrees.containsKey(id)) freeRootedPlayer(id, true);
    standStillTicks.remove(id);
    lastKnownBlockPos.remove(id);
    growthCooldownUntil.remove(id);
    kitStartTimeMillis.remove(id);
  }

  @EventHandler
  private void onFireDamage(EntityDamageEvent event) {
    if (!(event.getEntity() instanceof Player player)) return;
    if (!playerCanUseThisKit(player)) return;

    EntityDamageEvent.DamageCause cause = event.getCause();
    if (cause == EntityDamageEvent.DamageCause.FIRE
        || cause == EntityDamageEvent.DamageCause.FIRE_TICK
        || cause == EntityDamageEvent.DamageCause.LAVA
        || cause == EntityDamageEvent.DamageCause.HOT_FLOOR) {
      event.setDamage(event.getDamage() * FIRE_DAMAGE_MULTIPLIER);
    }
  }

  @EventHandler
  private void onAxeDamage(EntityDamageByEntityEvent event) {
    if (!(event.getEntity() instanceof Player victim)) return;
    if (!playerCanUseThisKit(victim)) return;

    if (!(event.getDamager() instanceof Player attacker)) return;
    ItemStack weapon = attacker.getInventory().getItemInMainHand();
    if (weapon == null) return;
    if (!Tag.ITEMS_AXES.isTagged(weapon.getType())) return;

    event.setDamage(event.getDamage() * AXE_DAMAGE_MULTIPLIER);
  }

  @EventHandler
  private void onEntityTargetLivingEntity(EntityTargetLivingEntityEvent event) {
    if (!(event.getTarget() instanceof Player target)) return;
    if (!playerCanUseThisKit(target)) return;
    if (!isCreaking(event.getEntity())) return;

    event.setTarget(null);
    event.setCancelled(true);

    // Buff nearby Creakings strongly: more speed and more damage around the target.
    double radius = 50.0; // search radius around the Forest Spirit
    Location center = target.getLocation();
    World world = center.getWorld();
    if (world == null) return;

    for (Entity nearby : world.getNearbyEntities(center, radius, radius, radius)) {
      if (!isCreaking(nearby)) continue;
      if (!(nearby instanceof LivingEntity living)) continue;

      var speedAttr = living.getAttribute(Attribute.MOVEMENT_SPEED);
      if (speedAttr != null) {
        // Strong speed boost; vanilla Creakings are slow.
        speedAttr.setBaseValue(speedAttr.getBaseValue() * 2.5);
      }

      var damageAttr = living.getAttribute(Attribute.ATTACK_DAMAGE);
      if (damageAttr != null) {
        // Increase attack damage significantly.
        damageAttr.setBaseValue(damageAttr.getBaseValue() * 2.5);
      }
    }
  }

  private boolean isCreaking(Entity entity) {
    return entity.getType().name().equalsIgnoreCase("CREAKING");
  }

  // ---------------------------------------------------------------------------
  // Placement helpers
  // ---------------------------------------------------------------------------

  private void placeRooted(
      RootedTreeState state,
      Location location,
      Material newType,
      boolean isRootLog,
      boolean forceReplace) {
    Block block = location.getBlock();

    if (!forceReplace && !canReplaceDecorationBlock(block)) {
      return;
    }

    BlockKey key = BlockKey.of(block.getLocation());
    state.originalBlocks.putIfAbsent(key, block.getType());
    block.setType(newType);

    if (isRootLog) {
      state.rootLogs.add(key);
      rootLogOwner.put(key, state.playerId);
    }
  }

  private boolean canReplaceDecorationBlock(Block block) {
    Material type = block.getType();
    if (type.isAir()) return true;
    if (Tag.LEAVES.isTagged(type)) return true;
    return block.isPassable();
  }

  // ---------------------------------------------------------------------------
  // Biome material / visuals
  // ---------------------------------------------------------------------------

  // Replace all saplings in the player's inventory with the biome-appropriate sapling
  private void adaptSaplingsToBiome(Player player, Biome biome) {
    Material targetSapling = getSaplingForBiome(biome);
    boolean targetIsDeadBush = (targetSapling == Material.DEAD_BUSH);

    var inventory = player.getInventory();
    for (int slot = 0; slot < inventory.getSize(); slot++) {
      ItemStack stack = inventory.getItem(slot);
      if (stack == null) continue;
      Material type = stack.getType();

      // When entering desert/badlands, convert any saplings to dead bushes.
      if (targetIsDeadBush) {
        if (Tag.SAPLINGS.isTagged(type)) {
          ItemStack newStack = new ItemStack(targetSapling, stack.getAmount());
          ItemMeta oldMeta = stack.getItemMeta();
          if (oldMeta != null) {
            newStack.setItemMeta(oldMeta);
          }
          inventory.setItem(slot, newStack);
        }
        continue;
      }

      if (type == Material.DEAD_BUSH || Tag.SAPLINGS.isTagged(type)) {
        ItemStack newStack = new ItemStack(targetSapling, stack.getAmount());
        ItemMeta oldMeta = stack.getItemMeta();
        if (oldMeta != null) {
          newStack.setItemMeta(oldMeta);
        }
        inventory.setItem(slot, newStack);
      }
    }
  }

  private void adaptArmorToBiome(Player player, Biome biome) {
    Color armorColor = getArmorColorForBiome(biome);
    var inventory = player.getInventory();

    for (int slot = 0; slot < inventory.getSize(); slot++) {
      ItemStack stack = inventory.getItem(slot);
      if (stack == null) continue;

      Material type = stack.getType();
      if (type != Material.LEATHER_HELMET
          && type != Material.LEATHER_CHESTPLATE
          && type != Material.LEATHER_LEGGINGS
          && type != Material.LEATHER_BOOTS) {
        continue;
      }

      // Reuse the existing stack, only change its color and keep other meta
      ItemStack recolored = stack.clone();
      ItemMeta meta = recolored.getItemMeta();
      if (meta instanceof org.bukkit.inventory.meta.LeatherArmorMeta leatherMeta) {
        leatherMeta.setColor(armorColor);
        recolored.setItemMeta(leatherMeta);
      }

      // Ensure Thorns II is still present (or add it if missing)
      recolored = enchantItem(recolored, Enchantment.THORNS, 2);

      inventory.setItem(slot, recolored);
    }
  }

  /** Returns the base wood type (log family) to use for a biome. */
  private Material getWoodTypeForBiome(Biome biome) {
    String name = biome.getKey().value().toUpperCase(Locale.ROOT);
    // Oceans, rivers, beaches
    if (name.contains("OCEAN")) return Material.OAK_LOG;
    if (name.contains("RIVER")) return Material.OAK_LOG;
    if (name.contains("BEACH") || name.contains("STONY_SHORE")) return Material.OAK_LOG;

    // Mushroom / caves / deep dark
    if (name.contains("MUSHROOM")) return Material.OAK_LOG;
    if (name.contains("DEEP_DARK")) return Material.OAK_LOG;
    if (name.contains("DRIPSTONE_CAVES")) return Material.OAK_LOG;
    if (name.contains("LUSH_CAVES")) return Material.OAK_LOG;

    // Mangrove swamps (handle before taiga/spruce matching so they don't fall through)
    if (name.contains("MANGROVE")) return materialOrDefault("MANGROVE_LOG", Material.OAK_LOG);

    // Peaks, slopes, meadows, groves, windswept hills/forest/gravelly
    if (name.contains("JAGGED_PEAKS")) return Material.SPRUCE_LOG;
    if (name.contains("FROZEN_PEAKS")) return Material.SPRUCE_LOG;
    if (name.contains("STONY_PEAKS")) return Material.SPRUCE_LOG;
    if (name.contains("SNOWY_SLOPES")) return Material.SPRUCE_LOG;
    if (name.contains("MEADOW")) return Material.OAK_LOG;
    if (name.contains("WINDSWEPT_HILLS")) return Material.SPRUCE_LOG;
    if (name.contains("WINDSWEPT_GRAVELLY_HILLS")) return Material.SPRUCE_LOG;
    if (name.contains("WINDSWEPT_FOREST")) return Material.SPRUCE_LOG;

    // Cherry grove
    if (name.contains("CHERRY")) return materialOrDefault("CHERRY_LOG", Material.OAK_LOG);

    // Forests
    if (name.equals("FOREST") || name.contains("FLOWER_FOREST")) return Material.OAK_LOG;
    if (name.contains("BIRCH")) return Material.BIRCH_LOG;
    if (name.contains("DARK_FOREST")) return Material.DARK_OAK_LOG;
    if (name.contains("PALE_GARDEN")) return Material.PALE_OAK_LOG;

    // Taiga family
    if (name.contains("TAIGA")
        || name.contains("OLD_GROWTH_PINE_TAIGA")
        || name.contains("OLD_GROWTH_SPRUCE_TAIGA")) {
      return Material.SPRUCE_LOG;
    }

    // Jungles
    if (name.contains("JUNGLE")) return Material.JUNGLE_LOG;

    // Swamps (classic)
    if (name.equals("SWAMP")) return Material.OAK_LOG;

    // Plains / flatlands
    if (name.equals("PLAINS")) return Material.OAK_LOG;
    if (name.contains("SUNFLOWER_PLAINS")) return Material.OAK_LOG;
    if (name.contains("SNOWY_PLAINS") || name.contains("ICE_SPIKES")) return Material.SPRUCE_LOG;

    // Arid biomes
    if (name.contains("DESERT")) return Material.OAK_LOG;
    if (name.contains("SAVANNA")) return Material.ACACIA_LOG;
    if (name.contains("BADLANDS")) return Material.OAK_LOG;

    // Fallback: oak family
    if (name.contains("GROVE")) return Material.SPRUCE_LOG;
    return Material.OAK_LOG;
  }

  // Sapling material derived from the wood type
  private Material getSaplingForBiome(Biome biome) {
    // In desert / badlands-like biomes, always use a dead bush as the sapling type.
    String key = biome.getKey().value().toUpperCase(Locale.ROOT);
    if (key.contains("DESERT") || key.contains("BADLANDS")) {
      return Material.DEAD_BUSH;
    }

    Material wood = getWoodTypeForBiome(biome);
    return switch (wood) {
      case SPRUCE_LOG -> Material.SPRUCE_SAPLING;
      case BIRCH_LOG -> Material.BIRCH_SAPLING;
      case JUNGLE_LOG -> Material.JUNGLE_SAPLING;
      case ACACIA_LOG -> Material.ACACIA_SAPLING;
      case DARK_OAK_LOG -> Material.DARK_OAK_SAPLING;
      case PALE_OAK_LOG -> Material.PALE_OAK_SAPLING;
      case MANGROVE_LOG -> Material.MANGROVE_PROPAGULE;
      default -> {
        // MANGROVE_LOG, CHERRY_LOG or anything else fall back appropriately
        if (wood == materialOrDefault("MANGROVE_LOG", Material.OAK_LOG)) {
          yield materialOrDefault("MANGROVE_PROPAGULE", Material.OAK_SAPLING);
        }
        if (wood == materialOrDefault("CHERRY_LOG", Material.OAK_LOG)) {
          yield materialOrDefault("CHERRY_SAPLING", Material.OAK_SAPLING);
        }
        yield Material.OAK_SAPLING;
      }
    };
  }

  // Leaves material derived from the wood type
  private Material getLeavesForBiome(Biome biome) {
    Material wood = getWoodTypeForBiome(biome);
    return switch (wood) {
      case SPRUCE_LOG -> Material.SPRUCE_LEAVES;
      case BIRCH_LOG -> Material.BIRCH_LEAVES;
      case JUNGLE_LOG -> Material.JUNGLE_LEAVES;
      case ACACIA_LOG -> Material.ACACIA_LEAVES;
      case DARK_OAK_LOG -> Material.DARK_OAK_LEAVES;
      default -> {
        if (wood == materialOrDefault("MANGROVE_LOG", Material.OAK_LOG)) {
          yield materialOrDefault("MANGROVE_LEAVES", Material.OAK_LEAVES);
        }
        if (wood == materialOrDefault("CHERRY_LOG", Material.OAK_LOG)) {
          yield materialOrDefault("CHERRY_LEAVES", Material.OAK_LEAVES);
        }
        yield Material.OAK_LEAVES;
      }
    };
  }

  // Log material is just the wood type itself
  private Material getLogForBiome(Biome biome) {
    return getWoodTypeForBiome(biome);
  }

  private TreeType getTreeTypeforBiome(Biome biome) {
    Material wood = getWoodTypeForBiome(biome);
    TreeType type;
    switch (wood) {
      case BIRCH_LOG -> type = TreeType.BIRCH;
      case SPRUCE_LOG -> type = TreeType.REDWOOD;
      case JUNGLE_LOG -> type = TreeType.SMALL_JUNGLE;
      case ACACIA_LOG -> type = TreeType.ACACIA;
      case DARK_OAK_LOG -> type = TreeType.DARK_OAK;
      case MANGROVE_LOG -> type = TreeType.MANGROVE;
      case CHERRY_LOG -> type = TreeType.CHERRY;
      case PALE_OAK_LOG -> type = TreeType.PALE_OAK;
      default -> type = TreeType.TREE;
    }
    return type;
  }

  private Material materialOrDefault(String materialName, Material fallback) {
    try {
      return Material.valueOf(materialName);
    } catch (IllegalArgumentException ex) {
      return fallback;
    }
  }

  private Color getArmorColorForBiome(Biome biome) {
    String key = biome.getKey().value().toUpperCase(Locale.ROOT);

    // Oceans & rivers: bluish green
    if (key.contains("OCEAN") || key.contains("RIVER")) return Color.fromRGB(0x1BAE7A); // teal-ish

    // Mushroom / caves / deep dark: muted, darker greens
    if (key.contains("MUSHROOM")) return Color.fromRGB(0x3E5A4A);
    if (key.contains("DEEP_DARK")) return Color.fromRGB(0x0B2416);
    if (key.contains("DRIPSTONE_CAVES")) return Color.fromRGB(0x2E4634);
    if (key.contains("LUSH_CAVES")) return Color.fromRGB(0x2E8B57);

    // Mangrove / classic swamps: dirty greens
    if (key.contains("MANGROVE")) return Color.fromRGB(0x2F5F2F);
    if (key.equals("SWAMP")) return Color.fromRGB(0x3A5A2A);

    // Peaks, snowy, meadows, groves, windswept: cool or fresh greens
    if (key.contains("JAGGED_PEAKS")
        || key.contains("FROZEN_PEAKS")
        || key.contains("STONY_PEAKS")
        || key.contains("SNOWY_SLOPES")
        || key.contains("SNOWY_PLAINS")
        || key.contains("ICE_SPIKES")) {
      return Color.fromRGB(0x3F6F5F); // cold, desaturated green
    }
    if (key.contains("MEADOW") || key.contains("GROVE") || key.contains("WINDSWEPT")) {
      return Color.fromRGB(0x4C8F3C); // bright meadow green
    }

    // Cherry grove: keep it green but a bit lighter
    if (key.contains("CHERRY_GROVE")) return Color.fromRGB(0x6FBF8F);

    // Forests: different green shades
    if (key.equals("FOREST") || key.contains("FLOWER_FOREST")) return Color.fromRGB(0x2F6F2F);
    if (key.contains("BIRCH")) return Color.fromRGB(0x4FAF5F);
    if (key.contains("DARK_FOREST")) return Color.fromRGB(0x0B3D0B);
    if (key.contains("PALE_GARDEN")) return Color.fromRGB(0x5F9F7F);

    // Taiga family (incl. snowy/old growth): conifer green
    if (key.contains("TAIGA")
        || key.contains("OLD_GROWTH_PINE_TAIGA")
        || key.contains("OLD_GROWTH_SPRUCE_TAIGA")) {
      return Color.fromRGB(0x2E5D34);
    }

    // Jungles: vibrant deep green
    if (key.contains("JUNGLE")) return Color.fromRGB(0x1E7A3A);

    // Plains / sunflower: softer grass green
    if (key.equals("PLAINS") || key.contains("SUNFLOWER_PLAINS")) return Color.fromRGB(0x6BBF59);

    // Arid / hot biomes
    if (key.contains("DESERT")) return Color.fromRGB(0xD2B48C);
    if (key.contains("SAVANNA")) return Color.fromRGB(0xB5A142);
    if (key.contains("BADLANDS")) return Color.fromRGB(0xB55630);

    // Beaches & shores: pale green with a hint of sand
    if (key.contains("BEACH") || key.contains("STONY_SHORE")) return Color.fromRGB(0x9FFF8A);

    // Fallback: use default dark green
    return DEFAULT_DARK_GREEN;
  }

  // ---------------------------------------------------------------------------
  // Internal structs
  // ---------------------------------------------------------------------------

  /**
   * Passive: every 5 seconds, for each Forest Spirit, count nearby log blocks in a radius and grant
   * half a heart of healing if there are enough logs.
   */
  private void tickLogAuraHealing() {
    final int radius = 10; // search radius in blocks
    final int requiredLogs = 12; // minimum number of log blocks to trigger healing

    for (Player player : plugin.getServer().getOnlinePlayers()) {
      if (!playerCanUseThisKit(player)) continue;
      if (player.isDead()) continue;

      Location loc = player.getLocation();
      World world = loc.getWorld();
      if (world == null) continue;

      int centerX = loc.getBlockX();
      int centerY = loc.getBlockY();
      int centerZ = loc.getBlockZ();

      int logCount = 0;
      int r2 = radius * radius;

      outer:
      for (int x = centerX - radius; x <= centerX + radius; x++) {
        for (int y = centerY - radius; y <= centerY + radius; y++) {
          for (int z = centerZ - radius; z <= centerZ + radius; z++) {
            int dx = x - centerX;
            int dy = y - centerY;
            int dz = z - centerZ;
            if (dx * dx + dy * dy + dz * dz > r2) continue; // spherical radius

            Material type = world.getBlockAt(x, y, z).getType();
            if (Tag.LOGS.isTagged(type)) {
              logCount++;
              if (logCount >= requiredLogs) {
                break outer;
              }
            }
          }
        }
      }

      if (logCount >= requiredLogs) {
        var attr = player.getAttribute(Attribute.MAX_HEALTH);
        if (attr == null) continue;
        double max = attr.getValue();
        if (player.getHealth() >= max) continue; // don't heal if already full

        double newHealth = Math.min(player.getHealth() + 2.0D, max); // +1 heart
        player.setHealth(newHealth);

        World pWorld = player.getWorld();
        Location pLoc = player.getLocation().add(0, 0, 0);
        pWorld.spawnParticle(
            Particle.EGG_CRACK,
            pLoc,
            20, // count
            0.5, // offset X
            0.7, // offset Y
            0.5, // offset Z
            0.01 // extra / speed
            );
      }
    }
  }

  /**
   * Periodic biome adaptation: every few seconds, update saplings and armor colors to match the
   * player's current biome instead of doing it on every movement event.
   */
  private void tickBiomeAdaptation() {
    for (Player player : plugin.getServer().getOnlinePlayers()) {
      if (!playerCanUseThisKit(player)) continue;
      Biome currentBiome = player.getLocation().getBlock().getBiome();
      adaptSaplingsToBiome(player, currentBiome);
      adaptArmorToBiome(player, currentBiome);
    }
  }

  @Override
  public KitDescription getDescription() {
    return new KitDescription(
        Material.OAK_SAPLING,
        "Forest Spirit",
        "Growth: turns nearby entities into trees and deals suffocating damage. "
            + "Gets stronger in forests, turns into a tree when standing still",
        "Full dark-green leather armor with Thorns II, 20 adaptive saplings.",
        Difficulty.MEDIUM);
  }

  @Override
  public void disable() {
    if (stillnessTask != null) {
      stillnessTask.cancel();
      stillnessTask = null;
    }

    if (healingTask != null) {
      healingTask.cancel();
      healingTask = null;
      healingTaskScheduled = false;
    }

    if (biomeAdaptTask != null) {
      biomeAdaptTask.cancel();
      biomeAdaptTask = null;
    }

    super.disable();
  }

  // ---------------------------------------------------------------------------
  // Description
  // ---------------------------------------------------------------------------

  private static final class RootedTreeState {
    private final UUID playerId;
    private final Map<BlockKey, Material> originalBlocks = new HashMap<>();
    private final Set<BlockKey> rootLogs = new HashSet<>();
    // Block position where the player became rooted
    private BlockKey rootOrigin;

    private RootedTreeState(UUID playerId) {
      this.playerId = playerId;
    }
  }

  private record BlockKey(UUID worldId, int x, int y, int z) {

    static BlockKey of(Location location) {
      return new BlockKey(
          Objects.requireNonNull(location.getWorld()).getUID(),
          location.getBlockX(),
          location.getBlockY(),
          location.getBlockZ());
    }

    Block toBlock() {
      World world = Bukkit.getWorld(worldId);
      if (world == null) return null;
      return world.getBlockAt(x, y, z);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof BlockKey other)) return false;
      return x == other.x && y == other.y && z == other.z && worldId.equals(other.worldId);
    }
  }
}
