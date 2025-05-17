package io.github.mal32.endergames.kits;

import io.github.mal32.endergames.EnderGames;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.FoodProperties;
import java.util.Arrays;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
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
  }

  @EventHandler
  private void onFallDamage(EntityDamageEvent event) {
    if (!(event.getEntity() instanceof Player) || !playerCanUseThisKit((Player) event.getEntity()))
      return;

    if (event.getCause() != EntityDamageEvent.DamageCause.FALL) {
      return;
    }

    event.setDamage(event.getDamage() * 0.5);
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
  public ItemStack getDescriptionItem() {
    ItemStack item = new ItemStack(Material.COD, 1);
    ItemMeta meta = item.getItemMeta();
    meta.displayName(
        Component.text("Cat").color(NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
    meta.lore(
        Arrays.asList(
            Component.text("Abilities:")
                .decorate(TextDecoration.UNDERLINED)
                .color(NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false),
            Component.text("Gains Speed III for 30 seconds when")
                .color(NamedTextColor.WHITE)
                .decoration(TextDecoration.ITALIC, false),
            Component.text("eating raw fish.")
                .color(NamedTextColor.WHITE)
                .decoration(TextDecoration.ITALIC, false),
            Component.text("It deals +1 damage with bare hands")
                .color(NamedTextColor.WHITE)
                .decoration(TextDecoration.ITALIC, false),
            Component.text("and takes 50% less fall damage.")
                .color(NamedTextColor.WHITE)
                .decoration(TextDecoration.ITALIC, false),
            Component.text(" "),
            Component.text("Equipment:")
                .decorate(TextDecoration.UNDERLINED)
                .color(NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false),
            Component.text("20 raw fish.")
                .color(NamedTextColor.WHITE)
                .decoration(TextDecoration.ITALIC, false)));
    item.setItemMeta(meta);

    return item;
  }
}
