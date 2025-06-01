package io.github.mal32.endergames.kits;

import io.github.mal32.endergames.EnderGames;
import io.github.mal32.endergames.kits.AbstractKit;
import io.github.mal32.endergames.kits.KitDescriptionItem;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Collection;
import java.util.Random;

public class Lucker extends AbstractKit {

    private final Random random = new Random();

    public Lucker(EnderGames plugin) {
        super(plugin);
    }

    @Override
    public void start(Player player) {
        // Give a light-green leather chestplate as starting item
        ItemStack chestplate = new ItemStack(Material.LEATHER_CHESTPLATE);
        colorLeatherArmor(chestplate, Color.LIME);
        player.getInventory().setChestplate(chestplate);

        player.addPotionEffect(new PotionEffect(PotionEffectType.LUCK,PotionEffect.INFINITE_DURATION, 0, false,false,true));
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {

        Player player = event.getPlayer();
        if (!playerCanUseThisKit(player)) return;

        Block block = event.getBlock();
        Material type = block.getType();
        Location loc = block.getLocation();

        // More drops from ores
        switch (type) {
            case COAL_ORE:
            case IRON_ORE:
            case GOLD_ORE:
            case REDSTONE_ORE:
            case LAPIS_ORE:
            case DIAMOND_ORE:
            case EMERALD_ORE:
            case COPPER_ORE:
            case NETHER_QUARTZ_ORE:
                // Get the default drops for this block (with the player's current tool, if any)
                Collection<ItemStack> drops = block.getDrops(player.getInventory().getItemInMainHand());
                for (ItemStack drop : drops) {
                    // Drop one extra copy of each default drop
                    ItemStack extra = drop.clone();
                    extra.setAmount(2);
                    loc.getWorld().dropItemNaturally(loc, extra);
                }
                break;

            default:
                break;
        }

        // 2) Increased apple drop chance from specific leaves
        if (type == Material.OAK_LEAVES || type == Material.DARK_OAK_LEAVES) {
            // 50% chance to drop an apple
            if (random.nextDouble() > 0.5) {
                ItemStack apple = new ItemStack(Material.APPLE, 1);
                loc.getWorld().dropItemNaturally(loc, apple);
            }
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity dead = event.getEntity();
        Player killer = dead.getKiller();
        if (killer == null) return;
        if (!playerCanUseThisKit(killer)) return;

        // Clone and multiply regular drops
        for (ItemStack drop : event.getDrops().toArray(new ItemStack[0])) {
            ItemStack extra = drop.clone();
            extra.setAmount(2);
            event.getDrops().add(extra);
        }

        // Increase chance for simulated rare drops
        switch (dead.getType()) {
            case ZOMBIE, HUSK, DROWNED -> {
                if (Math.random() > 0.4) {
                    event.getDrops().add(new ItemStack(Material.IRON_INGOT));
                }
            }
            case SKELETON -> {
                if (Math.random() > 0.5) { // 5% chance for rare bow
                    event.getDrops().add(new ItemStack(Material.BOW));
                }
            }
            case WITCH -> {
                    event.getDrops().add(new ItemStack(Material.GLOWSTONE));
            }
            default -> {
            }
        }
    }


    /**
     * Always get treasure (or a lot of fish) when fishing.
     * (Not implemented yet – method head only.)
     */
    public void onFishCatchEvent(/* e.g. PlayerFishEvent event */) {
        // TODO: Guarantee treasure catches or extra fish.
    }

    /**
     * Plants you place grow faster.
     * (Not implemented yet – method head only.)
     */
    @EventHandler
    public void onPlantPlace(BlockPlaceEvent event) {
        // TODO: Speed up crop growth for plants placed by the player.
    }

    /**
     * Get better enchantments.
     * (Not implemented yet – method head only.)
     */
    @EventHandler
    public void onEnchantItem(EnchantItemEvent event) {
        // TODO: Modify enchanting table behavior to yield higher-level enchantments.
    }

    @Override
    public KitDescriptionItem getDescriptionItem() {
        return new KitDescriptionItem(
                Material.AZALEA,
                "Lucker",
                "Has good luck at everything:"
                        + "- More loot from Ender Chests (coming soon)"
                        + "- Extra drops when mining ores; higher apple chance from leaves"
                        + "- Always treasures or tons of fish when fishing (coming soon)"
                        + "- More loot from killing mobs"
                        + "- Faster plant growth (coming soon)"
                        + "- Better enchantments (coming soon)",
                "Light-Green Leather Chestplate",
                Difficulty.MEDIUM
        );
    }
}
