package io.github.mal32.endergames.kits;

import io.github.mal32.endergames.EnderGames;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class ForestSpirit extends AbstractKit {

    private static final Color DEFAULT_DARK_GREEN = Color.fromRGB(0x1B5E20);

    // --- ability tuning ---
    private static final int GROWTH_COOLDOWN_SECONDS = 25;
    private static final double GROWTH_RADIUS = 6.0;
    private static final int TREE_PRISON_DURATION_TICKS = 20 * 4; // 4s

    // --- stillness / rooted tree tuning ---
    private static final int ROOTS_TRIGGER_TICKS = 20 * 10; // 10s standing still
    private static final int ROOTED_REGEN_LEVEL = 2; // Regen III (0=I,1=II,2=III)
    private static final int ROOTED_REGEN_DURATION_TICKS = 20 * 60 * 15; // long, removed on free

    // --- vulnerabilities ---
    private static final double FIRE_DAMAGE_MULTIPLIER = 1.35D;
    private static final double AXE_DAMAGE_MULTIPLIER = 1.25D;

    private final Map<UUID, Long> growthCooldownUntil = new HashMap<>();
    private final Map<UUID, Integer> standStillTicks = new HashMap<>();
    private final Map<UUID, BlockKey> lastKnownBlockPos = new HashMap<>();

    // rooted tree state
    private final Map<UUID, RootedTreeState> rootedTrees = new HashMap<>();
    private final Map<BlockKey, UUID> rootLogOwner = new HashMap<>();

    public ForestSpirit(EnderGames plugin) {
        super(plugin);

        // Tick-based stillness tracker:
        // - works even if player doesn't move their mouse
        // - validates roots each tick so ANY destruction cause frees the player
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::tickStillnessAndRoots, 1L, 1L);
    }

    // ---------------------------------------------------------------------------
    // STARTING KIT
    // ---------------------------------------------------------------------------

    @Override
    public void start(Player player) {
        Color biomeColor = getArmorColorForBiome(player.getLocation().getBlock().getBiome());

        player.getInventory().setHelmet(createSpiritArmorPiece(Material.LEATHER_HELMET, biomeColor));
        player.getInventory().setChestplate(createSpiritArmorPiece(Material.LEATHER_CHESTPLATE, biomeColor));
        player.getInventory().setLeggings(createSpiritArmorPiece(Material.LEATHER_LEGGINGS, biomeColor));
        player.getInventory().setBoots(createSpiritArmorPiece(Material.LEATHER_BOOTS, biomeColor));

        player.getInventory().addItem(new ItemStack(Material.OAK_SAPLING, 20));

        // Init stillness tracking
        UUID id = player.getUniqueId();
        standStillTicks.put(id, 0);
        lastKnownBlockPos.put(id, BlockKey.of(player.getLocation()));
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
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        if (!playerCanUseThisKit(player)) return;
        if (isOnGrowthCooldown(player)) return;

        activateGrowth(player);
        setGrowthCooldown(player);
    }

    private void activateGrowth(Player caster) {
        List<LivingEntity> targets = new ArrayList<>();
        for (Entity nearby :
                caster.getWorld().getNearbyEntities(caster.getLocation(), GROWTH_RADIUS, GROWTH_RADIUS, GROWTH_RADIUS)) {
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
        //TODO
        return;
    }

    private void createSmallForest(Location center) {
        //TODO
        return;
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

            BlockKey currentPos = BlockKey.of(player.getLocation());
            BlockKey lastPos = lastKnownBlockPos.get(id);

            if (lastPos == null || !lastPos.equals(currentPos)) {
                lastKnownBlockPos.put(id, currentPos);
                standStillTicks.put(id, 0);
            } else {
                int ticks = standStillTicks.getOrDefault(id, 0) + 1;
                standStillTicks.put(id, ticks);

                if (ticks >= ROOTS_TRIGGER_TICKS) {
                    rootPlayerIntoTree(player);
                    standStillTicks.put(id, 0);
                    lastKnownBlockPos.put(id, currentPos);
                }
            }
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

        rootedTrees.put(id, state);

        player.addPotionEffect(
                new PotionEffect(PotionEffectType.REGENERATION, ROOTED_REGEN_DURATION_TICKS, ROOTED_REGEN_LEVEL, true, true));
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
        if (!rootedTrees.containsKey(player.getUniqueId())) return;

        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) return;

        boolean changedBlock =
                from.getBlockX() != to.getBlockX()
                        || from.getBlockY() != to.getBlockY()
                        || from.getBlockZ() != to.getBlockZ();

        if (changedBlock) {
            Location locked = from.clone();
            locked.setYaw(to.getYaw());
            locked.setPitch(to.getPitch());
            event.setTo(locked);
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

        // TODO: Instantly grow planted saplings.
    }

    @EventHandler
    private void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();

        // If rooted when dying, clean up rooted state instantly.
        if (rootedTrees.containsKey(player.getUniqueId())) {
            freeRootedPlayer(player.getUniqueId(), true);
        }

        if (!playerCanUseThisKit(player)) return;

        // TODO: grow a tree at death location as separate passive.
    }

    @EventHandler
    private void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        if (rootedTrees.containsKey(id)) freeRootedPlayer(id, true);
        standStillTicks.remove(id);
        lastKnownBlockPos.remove(id);
        growthCooldownUntil.remove(id);
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

        event.setCancelled(true);

        // TODO: buff nearby Creakings strongly.
    }

    private boolean isCreaking(Entity entity) {
        return entity.getType().name().equalsIgnoreCase("CREAKING");
    }

    // ---------------------------------------------------------------------------
    // Placement helpers
    // ---------------------------------------------------------------------------

    private void placeTemporary(TemporaryStructure structure, Location location, Material type) {
        Block block = location.getBlock();
        if (!canReplaceDecorationBlock(block)) return;

        BlockKey key = BlockKey.of(block.getLocation());
        structure.originalBlocks.putIfAbsent(key, block.getType());
        block.setType(type);
    }

    private void restoreTemporaryStructure(TemporaryStructure structure) {
        for (Map.Entry<BlockKey, Material> entry : structure.originalBlocks.entrySet()) {
            Block block = entry.getKey().toBlock();
            if (block == null) continue;
            block.setType(entry.getValue());
        }
    }

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

    private Material getLogForBiome(Biome biome) {
        String name = biome.name();

        if (name.contains("JUNGLE")) return Material.JUNGLE_LOG;
        if (name.contains("BIRCH")) return Material.BIRCH_LOG;
        if (name.contains("DARK_FOREST")) return Material.DARK_OAK_LOG;
        if (name.contains("MANGROVE")) return materialOrDefault("MANGROVE_LOG", Material.OAK_LOG);
        if (name.contains("CHERRY")) return materialOrDefault("CHERRY_LOG", Material.OAK_LOG);
        if (name.contains("TAIGA")
                || name.contains("SPRUCE")
                || name.contains("OLD_GROWTH_PINE")
                || name.contains("OLD_GROWTH_SPRUCE")) {
            return Material.SPRUCE_LOG;
        }

        return Material.OAK_LOG;
    }

    private Material getLeavesForBiome(Biome biome) {
        String name = biome.name();

        if (name.contains("JUNGLE")) return Material.JUNGLE_LEAVES;
        if (name.contains("BIRCH")) return Material.BIRCH_LEAVES;
        if (name.contains("DARK_FOREST")) return Material.DARK_OAK_LEAVES;
        if (name.contains("MANGROVE")) return materialOrDefault("MANGROVE_LEAVES", Material.OAK_LEAVES);
        if (name.contains("CHERRY")) return materialOrDefault("CHERRY_LEAVES", Material.OAK_LEAVES);
        if (name.contains("TAIGA")
                || name.contains("SPRUCE")
                || name.contains("OLD_GROWTH_PINE")
                || name.contains("OLD_GROWTH_SPRUCE")) {
            return Material.SPRUCE_LEAVES;
        }

        return Material.OAK_LEAVES;
    }

    private Material materialOrDefault(String materialName, Material fallback) {
        try {
            return Material.valueOf(materialName);
        } catch (IllegalArgumentException ex) {
            return fallback;
        }
    }

    private Color getArmorColorForBiome(Biome biome) {
        String name = biome.name();
        if (name.contains("DARK_FOREST")) return Color.fromRGB(0x0B3D0B);
        if (name.contains("BIRCH")) return Color.fromRGB(0x3F7F3F);
        if (name.contains("TAIGA")
                || name.contains("OLD_GROWTH_PINE")
                || name.contains("OLD_GROWTH_SPRUCE")) return Color.fromRGB(0x2E5D34);
        if (name.contains("JUNGLE")) return Color.fromRGB(0x1E7A3A);
        return DEFAULT_DARK_GREEN;
    }

    // ---------------------------------------------------------------------------
    // Cooldown
    // ---------------------------------------------------------------------------

    private boolean isOnGrowthCooldown(Player player) {
        long now = System.currentTimeMillis();
        return growthCooldownUntil.getOrDefault(player.getUniqueId(), 0L) > now;
    }

    private void setGrowthCooldown(Player player) {
        long until = System.currentTimeMillis() + (GROWTH_COOLDOWN_SECONDS * 1000L);
        growthCooldownUntil.put(player.getUniqueId(), until);
    }

    // ---------------------------------------------------------------------------
    // Description
    // ---------------------------------------------------------------------------

    @Override
    public KitDescription getDescription() {
        return new KitDescription(
                Material.OAK_SAPLING,
                "Forest Spirit",
                "Growth: turns nearby entities into trees and deals suffocating damage. "
                        + "If no enemy is nearby, creates a small forest instead.",
                "Full dark-green leather armor with Thorns II, 20 adaptive saplings.",
                Difficulty.MEDIUM);
    }

    // ---------------------------------------------------------------------------
    // Internal structs
    // ---------------------------------------------------------------------------

    private static final class TemporaryStructure {
        private final Map<BlockKey, Material> originalBlocks = new HashMap<>();
    }

    private static final class RootedTreeState {
        private final UUID playerId;
        private final Map<BlockKey, Material> originalBlocks = new HashMap<>();
        private final Set<BlockKey> rootLogs = new HashSet<>();

        private RootedTreeState(UUID playerId) {
            this.playerId = playerId;
        }
    }

    private static final class BlockKey {
        private final UUID worldId;
        private final int x;
        private final int y;
        private final int z;

        private BlockKey(UUID worldId, int x, int y, int z) {
            this.worldId = worldId;
            this.x = x;
            this.y = y;
            this.z = z;
        }

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

        @Override
        public int hashCode() {
            return Objects.hash(worldId, x, y, z);
        }
    }
}
