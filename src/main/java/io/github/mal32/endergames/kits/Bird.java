package io.github.mal32.endergames.kits;

import io.github.mal32.endergames.EnderGames;
import java.util.Arrays;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class Bird extends AbstractKit {

  public Bird(EnderGames plugin) {
    super(plugin);
  }

  @Override
  public void start(Player player) {
    // Give the player an Elytra
    player.getInventory().setChestplate(new ItemStack(Material.ELYTRA));
    // Give the player 5 rockets (firework rockets)
    player.getInventory().addItem(new ItemStack(Material.FIREWORK_ROCKET, 5));
  }

  @EventHandler
  public void onPlayerKill(EntityDeathEvent event) {
    if (!(event.getEntity() instanceof Player victim)) return;
    Player killer = victim.getKiller();
    if (killer == null) return;
    if (!playerCanUseThisKit(killer)) return;

    killer.getInventory().addItem(new ItemStack(Material.FIREWORK_ROCKET, 2));
  }

  @Override
  public ItemStack getDescriptionItem() {
    ItemStack item = new ItemStack(Material.ELYTRA, 1);

    ItemMeta meta = item.getItemMeta();

    meta.displayName(
        Component.text("Bird").color(NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));

    meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

    meta.lore(
        Arrays.asList(
            Component.text("Abilities:")
                .decorate(TextDecoration.UNDERLINED)
                .color(NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false),
            Component.text("Starts with an Elytra and 5 rockets")
                .color(NamedTextColor.WHITE)
                .decoration(TextDecoration.ITALIC, false),
            Component.text("Gains 2 rocket per player kill")
                .color(NamedTextColor.WHITE)
                .decoration(TextDecoration.ITALIC, false),
            Component.text("Fly like a bird!")
                .color(NamedTextColor.WHITE)
                .decoration(TextDecoration.ITALIC, false),
            Component.text(" "),
            Component.text("Equipment:")
                .decorate(TextDecoration.UNDERLINED)
                .color(NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false),
            Component.text("1 Elytra, 5 Firework Rockets")
                .color(NamedTextColor.WHITE)
                .decoration(TextDecoration.ITALIC, false)));

    item.setItemMeta(meta);

    return item;
  }
}
