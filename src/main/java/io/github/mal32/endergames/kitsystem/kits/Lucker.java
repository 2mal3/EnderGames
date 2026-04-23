package io.github.mal32.endergames.kitsystem.kits;

import static io.github.mal32.endergames.kitsystem.KitUtils.colorLeatherArmor;

import io.github.mal32.endergames.kitsystem.api.AbstractKit;
import io.github.mal32.endergames.kitsystem.api.Difficulty;
import io.github.mal32.endergames.kitsystem.api.KitDescription;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

public class Lucker extends AbstractKit {

  private static final Set<Material> SEEDS =
      EnumSet.of(
          Material.WHEAT_SEEDS,
          Material.BEETROOT_SEEDS,
          Material.CARROT,
          Material.POTATO,
          Material.MELON_SEEDS,
          Material.PUMPKIN_SEEDS);

  public Lucker(JavaPlugin plugin) {
    super(
        new KitDescription(
            "Lucker",
            Material.DIAMOND,
            "Blessed with extraordinary luck. (Better chest loot, more luck when fishing, mining,"
                + " existing,...)",
            "Light-Green Leather Chestplate",
            Difficulty.EASY),
        plugin);
  }

  @Override
  public void initPlayer(Player player) {
    // Give a light-green leather chestplate as starting item
    ItemStack chestplate = ItemStack.of(Material.LEATHER_CHESTPLATE);
    colorLeatherArmor(chestplate, Color.LIME);
    player.getInventory().setChestplate(chestplate);

    player.addPotionEffect(
        new PotionEffect(
            PotionEffectType.LUCK, PotionEffect.INFINITE_DURATION, 0, false, false, true));
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
          extra.setAmount(1);
          loc.getWorld().dropItemNaturally(loc, extra);
        }
        break;

      default:
        break;
    }

    // 2) Increased apple drop chance from specific leaves (50% chance to drop an apple)
    if ((type == Material.OAK_LEAVES || type == Material.DARK_OAK_LEAVES)
        && ThreadLocalRandom.current().nextDouble() > 0.5) {
      ItemStack apple = ItemStack.of(Material.APPLE, 1);
      Location dropLoc = block.getLocation().add(0.5, 0.5, 0.5);
      dropLoc.getWorld().dropItemNaturally(dropLoc, apple);
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
          event.getDrops().add(ItemStack.of(Material.IRON_INGOT));
        }
      }
      case SKELETON -> {
        if (Math.random() > 0.5) { // 5% chance for rare bow
          event.getDrops().add(ItemStack.of(Material.BOW));
        }
      }
      case WITCH -> event.getDrops().add(ItemStack.of(Material.GLOWSTONE));
      case PLAYER -> {
        ItemStack bad_luck_potions = ItemStack.of(Material.SPLASH_POTION);
        PotionMeta meta = (PotionMeta) bad_luck_potions.getItemMeta();
        meta.addCustomEffect(new PotionEffect(PotionEffectType.UNLUCK, 20 * 160, 4), true);
        meta.displayName(
            Component.text("Splash Potion of Bad Luck").decoration(TextDecoration.ITALIC, false));
        bad_luck_potions.setItemMeta(meta);
        for (int i = 0; i < 3; i++) {
          killer
              .getInventory()
              .addItem(bad_luck_potions.clone())
              .forEach(
                  (slot, leftover) ->
                      killer.getWorld().dropItemNaturally(killer.getLocation(), leftover));
        }
      }
      default -> {}
    }
  }

  /**
   * Always get treasure (or a lot of fish) when fishing. (Not implemented yet – method head only.)
   */
  @EventHandler
  public void onFishCatchEvent(PlayerFishEvent event) {
    if (!playerCanUseThisKit(event.getPlayer())) return;
    // Only handle actual catches
    if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;

    Player player = event.getPlayer();
    Random rng = new Random();
    ItemStack catchItem;

    // 10% branch: 25 random fish species
    if (rng.nextDouble() < 0.10) {
      Material[] fishTypes = {
        Material.COD, Material.SALMON,
        Material.PUFFERFISH, Material.TROPICAL_FISH
      };
      for (int i = 0; i < 25; i++) {
        Material species = fishTypes[rng.nextInt(fishTypes.length)];
        ItemStack fish = ItemStack.of(species, 1);
        player
            .getInventory()
            .addItem(fish)
            .values()
            .forEach(
                overflow -> player.getWorld().dropItemNaturally(player.getLocation(), overflow));
      }
    }
  }

  @EventHandler
  private void onCraftItem(CraftItemEvent event) {
    Player player = (Player) event.getWhoClicked();
    if (!playerCanUseThisKit(player)) return;

    ItemStack result = event.getRecipe().getResult();
    if (result.getType() != Material.FISHING_ROD) return;

    // Clone result so you don’t modify the original recipe’s ItemStack
    ItemStack enchantedRod = result.clone();
    enchantedRod.addUnsafeEnchantment(Enchantment.LURE, 5);
    enchantedRod.addUnsafeEnchantment(Enchantment.LUCK_OF_THE_SEA, 99);
    ItemMeta meta = enchantedRod.getItemMeta();
    if (meta != null) {
      meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
      enchantedRod.setItemMeta(meta);
    }

    event.getInventory().setResult(enchantedRod);
  }

  /** Plants you place grow faster. */
  @EventHandler
  public void onPlayerPlant(PlayerInteractEvent event) {
    Player player = event.getPlayer();
    if (!playerCanUseThisKit(player)) return;
    if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

    ItemStack inHand = event.getItem();
    if (inHand == null) return;

    Material seed = inHand.getType();
    if (!SEEDS.contains(seed)) return;

    Block clicked = event.getClickedBlock();
    if (clicked == null || clicked.getType() != Material.FARMLAND) return;

    new BukkitRunnable() {
      @Override
      public void run() {
        Block crop = clicked.getRelative(event.getBlockFace());
        BlockData data = crop.getBlockData();
        if (data instanceof Ageable) {
          startFastGrowth(crop);
        }
      }
    }.runTaskLater(plugin, 1L);
  }

  private void startFastGrowth(Block crop) {
    new BukkitRunnable() {
      @Override
      public void run() {
        BlockData data = crop.getBlockData();
        if (!(data instanceof Ageable ageable)) {
          cancel();
          return;
        }

        int age = ageable.getAge();
        int max = ageable.getMaximumAge();
        if (age < max) {
          ageable.setAge(age + 1);
          crop.setBlockData(ageable, true);
        } else {
          cancel();
        }
      }
    }.runTaskTimer(plugin, 1L, 1L);
  }

  /** Get better enchantments. */
  @EventHandler
  public void onEnchantItem(EnchantItemEvent event) {
    Player player = event.getEnchanter();
    if (!playerCanUseThisKit(player)) return;

    event.getEnchantsToAdd().clear();
    int paid = event.getExpLevelCost();

    ItemStack item = event.getItem();
    applyLvL30Enchants(item);

    // 4) Cancel the rest so no vanilla logic runs

    event.setCancelled(true);

    player.setLevel(player.getLevel() - paid);
    // 6) Put the enchanted item back into the table slot
    player.getOpenInventory().getTopInventory().setItem(0, item);
  }

  /**
   * Choose 1–3 non‑conflicting enchants that CAN go on this item, and give them all at (or near)
   * their max level.
   */
  private void applyLvL30Enchants(ItemStack item) {
    // gather all enchants that can apply
    List<Enchantment> pool =
        RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT).stream()
            .filter(e -> e.canEnchantItem(item))
            .filter(e -> !e.isCursed())
            .collect(Collectors.toList());
    if (pool.isEmpty()) return;

    // give between 1 and 4 enchants
    ThreadLocalRandom rng = ThreadLocalRandom.current();
    int count = 1 + rng.nextInt(4);
    Set<Enchantment> chosen = new HashSet<>();

    for (int i = 0; i < count && !pool.isEmpty(); i++) {
      // pick one at random
      Enchantment pick = pool.remove(rng.nextInt(pool.size()));
      chosen.add(pick);
      // remove any that conflict with it
      pool.removeIf(other -> other.conflictsWith(pick));
    }

    for (Enchantment e : chosen) {
      int lvl = 1 + rng.nextInt(e.getMaxLevel());
      item.addUnsafeEnchantment(e, lvl);
    }
  }
}
