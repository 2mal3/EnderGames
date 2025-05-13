package io.github.mal32.endergames.kits;

import io.github.mal32.endergames.phases.game.AbstractModule;

import java.util.Collection;
import java.util.Objects;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.loot.LootContext;
import org.bukkit.loot.LootTable;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public abstract class AbstractKit extends AbstractModule {
  public AbstractKit(JavaPlugin plugin) {
    super(plugin);
  }

  protected boolean playerHasKit(Player player) {
    return Objects.equals(
        player
            .getPersistentDataContainer()
            .get(new NamespacedKey(plugin, "kit"), PersistentDataType.STRING),
        getName());
  }

  public void start(Player player) {
    LootTable lootTable = Bukkit.getLootTable(new NamespacedKey("enga", "kits/" + getName()));

    Collection<ItemStack> loot = lootTable != null ? lootTable.populateLoot(new Random(), new LootContext.Builder(player.getLocation()).build()) : null;

    if (loot == null) return;

    for (ItemStack item : loot) {
      if (item == null) return;
      String name = item.getType().name();
      if (name.endsWith("_HELMET")) {
        player.getInventory().setHelmet(item);
      } else if (name.endsWith("_CHESTPLATE")) {
        player.getInventory().setChestplate(item);
      } else if (name.endsWith("_LEGGINGS")) {
        player.getInventory().setLeggings(item);
      } else if (name.endsWith("_BOOTS")) {
        player.getInventory().setBoots(item);
      } else {
        player.getInventory().addItem(item);
      }
    }
  }

  public String getName() {
    return this.getClass().getSimpleName().toLowerCase();
  }

  public abstract ItemStack getDescriptionItem();
}
