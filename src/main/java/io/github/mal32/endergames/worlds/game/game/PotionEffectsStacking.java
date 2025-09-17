package io.github.mal32.endergames.worlds.game.game;

import io.github.mal32.endergames.AbstractModule;
import io.github.mal32.endergames.EnderGames;
import java.util.*;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class PotionEffectsStacking extends AbstractModule {
  private static final Comparator<PotionEffect> comparator =
      Comparator.comparingInt(e -> -e.getAmplifier());
  private final HashMap<UUID, HashMap<PotionEffectType, PriorityQueue<PotionEffect>>>
      playerEffects = new HashMap<>();

  public PotionEffectsStacking(EnderGames plugin) {
    super(plugin);
  }

  public void addPotionEffect(Player player, PotionEffect newPotionEffect) {
    var oldPotionEffect = player.getPotionEffect(newPotionEffect.getType());
    if (oldPotionEffect == null) {
      player.addPotionEffect(newPotionEffect);
      return;
    }

    var effectTypes = playerEffects.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>());
    var effectList =
        effectTypes.computeIfAbsent(
            newPotionEffect.getType(), k -> new PriorityQueue<>(comparator));

    if (newPotionEffect.getAmplifier() > oldPotionEffect.getAmplifier()) {
      // if the new effect is stronger, replace the old one
      effectList.add(oldPotionEffect);
      player.addPotionEffect(newPotionEffect);
    } else if (newPotionEffect.getAmplifier() == oldPotionEffect.getAmplifier()) {
      // if the new effect has the same strength, merge durations
      int mergedDuration = oldPotionEffect.getDuration() + newPotionEffect.getDuration();
      player.addPotionEffect(oldPotionEffect.withDuration(mergedDuration));
    } else {
      // if the new effect is weaker, just add it to the list
      effectList.add(newPotionEffect);
    }
  }

  @EventHandler
  private void onPotionEffectChange(EntityPotionEffectEvent event) {
    if (!(event.getEntity() instanceof Player player)) return;
    if (event.getCause() != EntityPotionEffectEvent.Cause.EXPIRATION) return;

    PotionEffectType oldPotionEffectType = event.getOldEffect().getType();

    Bukkit.getScheduler()
        .runTask(
            plugin,
            () -> {
              var effectTypes = playerEffects.get(player.getUniqueId());
              if (effectTypes == null) return;
              var effectList = effectTypes.get(oldPotionEffectType);
              if (effectList == null || effectList.isEmpty()) return;

              var nextStrongestEffect = effectList.remove();
              player.addPotionEffect(nextStrongestEffect);
            });
  }
}
