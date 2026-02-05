package io.github.mal32.endergames.kits;

import io.github.mal32.endergames.EnderGames;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Biome;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class ForestSpirit extends AbstractKit {

    private static final Color DEFAULT_DARK_GREEN = Color.fromRGB(0x1B5E20);

    // --- tuning placeholders ---
    private static final int GROWTH_COOLDOWN_SECONDS = 25;
    private static final int ROOTS_TRIGGER_TICKS = 20 * 5; // 5s standing still
    private static final double FIRE_DAMAGE_MULTIPLIER = 1.35D;
    private static final double AXE_DAMAGE_MULTIPLIER = 1.25D;

    // --- state placeholders ---
    private final Map<UUID, Long> growthCooldownUntil = new HashMap<>();
    private final Map<UUID, Integer> standStillTicks = new HashMap<>();
    private final Map<UUID, Location> lastKnownBlockPos = new HashMap<>();

    public ForestSpirit(EnderGames plugin) {
        super(plugin);
    }

    // ---------------------------------------------------------------------------
    // STARTING KIT (implemented)
    // ---------------------------------------------------------------------------

    @Override
    public void start(Player player) {
        Color biomeColor = getArmorColorForBiome(player.getLocation().getBlock().getBiome());

        player.getInventory().setHelmet(createSpiritArmorPiece(Material.LEATHER_HELMET, biomeColor));
        player.getInventory().setChestplate(createSpiritArmorPiece(Material.LEATHER_CHESTPLATE, biomeColor));
        player.getInventory().setLeggings(createSpiritArmorPiece(Material.LEATHER_LEGGINGS, biomeColor));
        player.getInventory().setBoots(createSpiritArmorPiece(Material.LEATHER_BOOTS, biomeColor));

        // Initial saplings; will be swapped dynamically by biome in TODO logic.
        player.getInventory().addItem(new ItemStack(Material.OAK_SAPLING, 20));
    }

    private ItemStack createSpiritArmorPiece(Material type, Color color) {
        ItemStack item = colorLeatherArmor(new ItemStack(type), color);
        return enchantItem(item, Enchantment.THORNS, 2);
    }

    // ---------------------------------------------------------------------------
    // ACTIVE ABILITY: Growth
    // ---------------------------------------------------------------------------

    @EventHandler
    private void onUseGrowth(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        if (!playerCanUseThisKit(player)) return;

        if (isOnGrowthCooldown(player)) {
            // TODO: Add proper cooldown feedback/actionbar with remaining time.
            return;
        }

        activateGrowth(player);
        setGrowthCooldown(player);
    }

    private void activateGrowth(Player player) {
        // TODO:
        // 1) Find nearby enemy entities in radius.
        // 2) If enemies found: "turn into trees" effect + suffocation-style damage ticks.
        // 3) If none found: generate a small patch of forest around the player.
        // 4) Add particles/sounds.
        // 5) Respect protected regions / map rules.
    }

    private boolean isOnGrowthCooldown(Player player) {
        long now = System.currentTimeMillis();
        return growthCooldownUntil.getOrDefault(player.getUniqueId(), 0L) > now;
    }

    private void setGrowthCooldown(Player player) {
        long until = System.currentTimeMillis() + (GROWTH_COOLDOWN_SECONDS * 1000L);
        growthCooldownUntil.put(player.getUniqueId(), until);
    }

    // ---------------------------------------------------------------------------
    // PASSIVE: Saplings grow instantly
    // ---------------------------------------------------------------------------

    @EventHandler
    private void onSaplingPlaced(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (!playerCanUseThisKit(player)) return;
        if (!Tag.SAPLINGS.isTagged(event.getBlockPlaced().getType())) return;

        // TODO:
        // Instantly grow the sapling:
        // - Either set a generated tree immediately
        // - or trigger a growth API/event depending on your server version.
        // - Handle failure cases (not enough space) gracefully.
    }

    // ---------------------------------------------------------------------------
    // PASSIVE: Forest regen + stillness roots + dynamic visuals
    // ---------------------------------------------------------------------------

    @EventHandler
    private void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!playerCanUseThisKit(player)) return;

        // Dynamic armor/sapling adaptation by biome.
        updateBiomeVisualsAndSaplings(player);

        // Stillness tracking for "becoming a tree".
        trackStillness(player, event);

        // TODO:
        // Apply slight regeneration while in forest biome.
        // Optionally require density check (enough logs/leaves nearby).
        // Increase regeneration when standing still.
    }

    private void trackStillness(Player player, PlayerMoveEvent event) {
        Location to = event.getTo();
        Location from = event.getFrom();
        if (to == null) return;

        // Only consider block-to-block movement.
        boolean changedBlock =
                to.getBlockX() != from.getBlockX()
                        || to.getBlockY() != from.getBlockY()
                        || to.getBlockZ() != from.getBlockZ();

        UUID id = player.getUniqueId();
        if (changedBlock) {
            standStillTicks.put(id, 0);
            lastKnownBlockPos.put(id, to.getBlock().getLocation());
            // TODO: Remove roots/leaves visual state if currently rooted.
            return;
        }

        int ticks = standStillTicks.getOrDefault(id, 0) + 1;
        standStillTicks.put(id, ticks);

        if (ticks >= ROOTS_TRIGGER_TICKS) {
            // TODO:
            // Root the player:
            // - Spawn root blocks below / leaves above (temporary).
            // - Restrict movement until roots are broken.
            // - Add stronger regen while rooted.
        }
    }

    private void updateBiomeVisualsAndSaplings(Player player) {
        Biome biome = player.getLocation().getBlock().getBiome();

        // Armor color update (already lightweight).
        Color target = getArmorColorForBiome(biome);
        recolorArmorIfNeeded(player, target);

        // TODO:
        // Convert held sapling stacks to biome-specific sapling type while in inventory.
        // Keep amount and slots; avoid running every tick if unchanged.
        // Consider cooldown/throttle for performance.
    }

    private void recolorArmorIfNeeded(Player player, Color color) {
        // Simple recolor pass; safe no-op for null/non-leather.
        // TODO: Optimize by caching last applied biome/color per player.
        if (player.getInventory().getHelmet() != null
                && player.getInventory().getHelmet().getType() == Material.LEATHER_HELMET) {
            player.getInventory().setHelmet(createSpiritArmorPiece(Material.LEATHER_HELMET, color));
        }
        if (player.getInventory().getChestplate() != null
                && player.getInventory().getChestplate().getType() == Material.LEATHER_CHESTPLATE) {
            player.getInventory().setChestplate(createSpiritArmorPiece(Material.LEATHER_CHESTPLATE, color));
        }
        if (player.getInventory().getLeggings() != null
                && player.getInventory().getLeggings().getType() == Material.LEATHER_LEGGINGS) {
            player.getInventory().setLeggings(createSpiritArmorPiece(Material.LEATHER_LEGGINGS, color));
        }
        if (player.getInventory().getBoots() != null
                && player.getInventory().getBoots().getType() == Material.LEATHER_BOOTS) {
            player.getInventory().setBoots(createSpiritArmorPiece(Material.LEATHER_BOOTS, color));
        }
    }

    // ---------------------------------------------------------------------------
    // PASSIVE: Death tree
    // ---------------------------------------------------------------------------

    @EventHandler
    private void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (!playerCanUseThisKit(player)) return;

        Location deathLocation = player.getLocation();
        // TODO:
        // Grow/spawn a tree at deathLocation:
        // - pick tree type from biome
        // - avoid protected/invalid spots
        // - keep it performant and grief-safe
    }

    // ---------------------------------------------------------------------------
    // PASSIVE: Vulnerable to fire and axes
    // ---------------------------------------------------------------------------

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

    // ---------------------------------------------------------------------------
    // PASSIVE: Creaking interactions
    // ---------------------------------------------------------------------------

    @EventHandler
    private void onEntityTargetLivingEntity(EntityTargetLivingEntityEvent event) {
        if (!(event.getTarget() instanceof Player target)) return;
        if (!playerCanUseThisKit(target)) return;
        if (!isCreaking(event.getEntity())) return;

        // Creakings don't attack Forest Spirit.
        event.setCancelled(true);

        // TODO:
        // Buff all Creakings in a huge radius heavily:
        // - Strength/Speed/Resistance/etc.
        // - Duration/refresh logic
        // - Performance considerations (spatial queries / cooldown)
    }

    private boolean isCreaking(Entity entity) {
        // Version-safe check by name (avoids compile issues on older enums/APIs).
        return entity.getType().name().equalsIgnoreCase("CREAKING");
    }

    // ---------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------

    private Color getArmorColorForBiome(Biome biome) {
        // Basic palette; tweak as needed.
        String name = biome.name();
        if (name.contains("DARK_FOREST")) return Color.fromRGB(0x0B3D0B);
        if (name.contains("BIRCH")) return Color.fromRGB(0x3F7F3F);
        if (name.contains("TAIGA")
                || name.contains("OLD_GROWTH_PINE")
                || name.contains("OLD_GROWTH_SPRUCE")) return Color.fromRGB(0x2E5D34);
        if (name.contains("JUNGLE")) return Color.fromRGB(0x1E7A3A);
        return DEFAULT_DARK_GREEN;
    }

    @Override
    public KitDescription getDescription() {
        return new KitDescription(
                Material.OAK_SAPLING,
                "Forest Spirit",
                "Regenerates in forests and is more vulnerable to axes and fire."
                        + "Be careful: If you stand still to long you need to break your roots to move again."
                        + "Growth Ability: turns nearby entities into trees and deals suffocating damage.",
                "Full dark-green leather armor with Thorns II, 20 saplings.",
                Difficulty.MEDIUM);
    }
}
