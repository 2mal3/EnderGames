package io.github.mal32.endergames.worlds.game.game;

import io.github.mal32.endergames.AbstractModule;
import io.github.mal32.endergames.EnderGames;
import java.util.HashMap;
import java.util.UUID;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class SwapperItem extends AbstractModule {
  private final HashMap<UUID, UUID> thrownSwappers = new HashMap<>();

  public SwapperItem(EnderGames plugin) {
    super(plugin);
  }

  @EventHandler
  public void onPlayerSnowballThrow(ProjectileLaunchEvent event) {
    if (!(event.getEntity() instanceof Snowball snowball)) return;
    if (!(snowball.getShooter() instanceof Player player)) return;

    ItemStack item = player.getInventory().getItemInMainHand();
    String itemName = "";
    if (item.getItemMeta().hasItemName()) {
      itemName = ((TextComponent) item.getItemMeta().itemName()).content();
    }
    if (!itemName.equals("Swapper")) return;

    thrownSwappers.put(snowball.getUniqueId(), player.getUniqueId());
  }

  @EventHandler
  public void onSnowBallHit(ProjectileHitEvent event) {
    if (!(event.getEntity() instanceof Snowball snowball)) return;
    if (!thrownSwappers.containsKey(snowball.getUniqueId())) return;

    if (!(event.getHitEntity() instanceof LivingEntity hitEntity)) return;
    if (!(snowball.getShooter() instanceof Player shooter)) return;

    thrownSwappers.remove(snowball.getUniqueId());

    swap(shooter, hitEntity);
  }

  private void swap(Player player, LivingEntity entity) {
    var playerLocation = player.getLocation().clone();
    var entityLocation = entity.getLocation().clone();

    player.teleport(entityLocation);
    entity.teleport(playerLocation);

    swapEffects(player);
    swapEffects(entity);
  }

  private void swapEffects(LivingEntity player) {
    Location location = player.getLocation();
    location.getWorld().playSound(location, Sound.ENTITY_PLAYER_TELEPORT, 1, 0.5f);
    location.getWorld().spawnParticle(Particle.PORTAL, location, 50, 0, 0, 0);

    player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 10, 0, true));
  }
}
