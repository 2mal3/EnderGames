package io.github.mal32.endergames.kits;

import java.util.Random;
import org.bukkit.*;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.loot.LootTables;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class Slime extends AbstractKit {
  public Slime(JavaPlugin plugin) {
    super(plugin);
  }

  @Override
  public void start(Player player) {
    super.start(player);
    player.addPotionEffect(
        new PotionEffect(
            PotionEffectType.JUMP_BOOST, PotionEffect.INFINITE_DURATION, 1, true, false));
  }

  @EventHandler
  public void onEntityDamage(EntityDamageByEntityEvent event) {
    if (!(event.getEntity() instanceof Player)) {
      return;
    }
    Player player = (Player) event.getEntity();
    if (!playerHasKit(player)) {
      return;
    }
    if (event.getDamager() instanceof EnderPearl) {
      return;
    }
    Location location = player.getLocation();
    World world = player.getWorld();
    Random random = new Random();
    int loops = random.nextInt(3) + 1; // 1 bis 3
    for (int x = 0; x < loops; x++) {
      Location editLoc = location.clone();
      editLoc.add(1 - Math.random() * 2, 0.1, 1 - Math.random() * 2);
      if (world.getBlockAt(editLoc).getType() == Material.AIR
          || world.getBlockAt(editLoc).getType() == Material.WATER) {
        org.bukkit.entity.Slime slime =
            (org.bukkit.entity.Slime) world.spawnEntity(editLoc, EntityType.SLIME);
        Bukkit.getScheduler()
            .runTaskLater(
                plugin,
                () -> {
                  if (slime.isValid()) {
                    slime.remove();
                  }
                },
                20 * 4);
        slime.setLootTable(LootTables.ENDERMITE.getLootTable());

        slime.setSize(0);
        slime
            .getPersistentDataContainer()
            .set(new NamespacedKey(plugin, "slimekitslime"), PersistentDataType.INTEGER, 1);
      }
    }
  }

  @EventHandler
  public void onSlimeDeath(EntityDeathEvent event) {
    if (event.getEntity() instanceof org.bukkit.entity.Slime slime) {
      if (slime
          .getPersistentDataContainer()
          .has(new NamespacedKey(plugin, "slimekitslime"), PersistentDataType.INTEGER)) {
        event.getDrops().clear();
        event.setDroppedExp(0);
      }
    }
  }

  @Override
  public String getName() {
    return "slime";
  }
}
