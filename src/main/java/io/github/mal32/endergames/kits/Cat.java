package io.github.mal32.endergames.kits;

import io.github.mal32.endergames.EnderGames;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.FoodProperties;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class Cat extends AbstractKit {
  public Cat(EnderGames plugin) {
    super(plugin);
  }

  @Override
  public void start(Player player) {
    ItemStack fish = new ItemStack(Material.COD, 20);
    FoodProperties fishProperties =
        FoodProperties.food().canAlwaysEat(true).nutrition(3).saturation(0.6f).build();
    fish.setData(DataComponentTypes.FOOD, fishProperties);
    ItemMeta fishMeta = fish.getItemMeta();
    fishMeta.displayName(Component.text("Fish").decoration(TextDecoration.ITALIC, false));
    fishMeta.lore(List.of(Component.text("Can always be eaten").color(NamedTextColor.GRAY)));
    fish.setItemMeta(fishMeta);
    player.getInventory().addItem(fish);

    var safeFallDistanceAttribute = player.getAttribute(Attribute.SAFE_FALL_DISTANCE);
    safeFallDistanceAttribute.setBaseValue(5);
    var fallDamageMultiplierAttribute = player.getAttribute(Attribute.FALL_DAMAGE_MULTIPLIER);
    fallDamageMultiplierAttribute.setBaseValue(0.5);
  }

  @EventHandler(priority = EventPriority.LOW)
  public void onPlayerDeath(PlayerDeathEvent event) {
    var player = event.getPlayer();
    if (!playerCanUseThisKit(player)) return;

    var safeFallDistanceAttribute = player.getAttribute(Attribute.SAFE_FALL_DISTANCE);
    safeFallDistanceAttribute.setBaseValue(safeFallDistanceAttribute.getDefaultValue());
    var fallDamageMultiplierAttribute = player.getAttribute(Attribute.FALL_DAMAGE_MULTIPLIER);
    fallDamageMultiplierAttribute.setBaseValue(fallDamageMultiplierAttribute.getDefaultValue());
  }

  @EventHandler
  private void onPlayerEatFish(PlayerItemConsumeEvent event) {
    if (!playerCanUseThisKit(event.getPlayer())) return;

    if (!Tag.ITEMS_FISHES.isTagged(event.getItem().getType())) return;

    event.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20 * 30, 2, true));
  }

  @EventHandler
  private void onPlayerHit(EntityDamageByEntityEvent event) {
    if (!(event.getDamager() instanceof Player damager) || !playerCanUseThisKit(damager)) return;

    // skip if damage is not with bare hands
    if (!damager.getInventory().getItemInMainHand().getType().isAir()) return;

    event.setDamage(event.getDamage() + 2);
  }

  @Override
  public KitDescriptionItem getDescriptionItem() {
    return new KitDescriptionItem(
        Material.COD,
        "Cat",
        "Gains Speed III for 30 seconds when eating raw fish. It deals +1 damage with bare hands"
            + " and takes 50% less fall damage.",
        "20 raw fish",
        Difficulty.EASY);
  }
}
