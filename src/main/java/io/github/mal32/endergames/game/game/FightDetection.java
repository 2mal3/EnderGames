package io.github.mal32.endergames.game.game;

import io.github.mal32.endergames.AbstractModule;
import io.github.mal32.endergames.EnderGames;
import io.github.mal32.endergames.services.PlayerInWorld;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class FightDetection extends AbstractModule {
  private static final Map<UUID, DamageEvent> damageEvents = new ConcurrentHashMap<>();
  private static final long FIGHT_THRESHOLD_SECONDS = 10;

  public FightDetection(EnderGames plugin) {
    super(plugin);
  }

  public static boolean playerIsInFight(Player player) {
    DamageEvent lastPlayerDamage = damageEvents.get(player.getUniqueId());
    if (lastPlayerDamage == null) {
      return false;
    }
    int now = (int) (System.currentTimeMillis() / 1000);
    return (now - lastPlayerDamage.timestamp()) <= FIGHT_THRESHOLD_SECONDS;
  }

  public static Player getActiveDamager(Player player) {
    if (!playerIsInFight(player)) return null;

    DamageEvent lastPlayerDamage = damageEvents.get(player.getUniqueId());
    if (lastPlayerDamage == null) {
      return null;
    }
    return Bukkit.getPlayer(lastPlayerDamage.damagerUuid());
  }

  public static void fakeDamage(Player target, Player damager) {
    int now = (int) (System.currentTimeMillis() / 1000);
    UUID targetId = target.getUniqueId();
    UUID damagerUuid = damager.getUniqueId();
    damageEvents.put(targetId, new DamageEvent(damagerUuid, now));
  }

  @EventHandler
  private void onPlayerDamage(EntityDamageByEntityEvent event) {
    if (!(event.getEntity() instanceof Player target)) return;

    Player damager = null;
    switch (event.getDamager().getType()) {
      case PLAYER:
        damager = (Player) event.getDamager();
        break;

      case SPECTRAL_ARROW:
      case ARROW:
        Arrow arrow = (Arrow) event.getDamager();
        UUID shooterUuid = arrow.getOwnerUniqueId();
        if (shooterUuid == null) break;
        Player shooter = Bukkit.getPlayer(shooterUuid);
        if (shooter == null) break; // not a player
        damager = shooter;
        break;

      case TNT:
        plugin.getComponentLogger().info("hit by tnt");
        TNTPrimed tnt = (TNTPrimed) event.getDamager();
        if (tnt.getSource() == null) return;
        if (tnt.getSource().getType() != EntityType.PLAYER) return;
        damager = (Player) tnt.getSource();
        break;

      default:
        break;
    }
    if (damager == null) return;

    if (!PlayerInWorld.GAME.is(target)) {
      return;
    }

    int now = (int) (System.currentTimeMillis() / 1000);
    UUID targetId = target.getUniqueId();
    UUID damagerUuid = damager.getUniqueId();
    damageEvents.put(targetId, new DamageEvent(damagerUuid, now));
  }
}

record DamageEvent(UUID damagerUuid, int timestamp) {}
