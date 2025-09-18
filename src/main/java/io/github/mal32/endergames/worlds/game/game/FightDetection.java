package io.github.mal32.endergames.worlds.game.game;

import io.github.mal32.endergames.AbstractModule;
import io.github.mal32.endergames.EnderGames;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.jetbrains.annotations.Nullable;

public class FightDetection extends AbstractModule {
  private static final Map<UUID, FightInfo> fights = new ConcurrentHashMap<>();
  private static final long FIGHT_THRESHOLD_SECONDS = 10;

  public FightDetection(EnderGames plugin) {
    super(plugin);
  }

  public static boolean playerIsInFight(Player player) {
    FightInfo info = fights.get(player.getUniqueId());
    if (info == null) {
      return false;
    }
    int now = (int) (System.currentTimeMillis() / 1000);
    return (now - info.lastHitTimestamp()) <= FIGHT_THRESHOLD_SECONDS;
  }

  @EventHandler
  private void onPlayerDamage(EntityDamageByEntityEvent event) {
    if (!(event.getEntity() instanceof Player target)
        || !(event.getDamager() instanceof Player damager)) {
      return;
    }
    if (!EnderGames.playerIsInGameWorld(target) || !EnderGames.playerIsInGameWorld(damager)) {
      return;
    }

    int now = (int) (System.currentTimeMillis() / 1000);
    UUID targetId = target.getUniqueId();
    UUID damagerId = damager.getUniqueId();
    fights.put(targetId, new FightInfo(now, damagerId));
    fights.put(damagerId, new FightInfo(now, null));
  }

  @Nullable
  public static UUID getLastAttacker(Player player) {
    FightInfo info = fights.get(player.getUniqueId());
    return info == null ? null : info.attacker();
  }
}

record FightInfo(int lastHitTimestamp, @Nullable UUID attacker) {}
