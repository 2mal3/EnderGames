package io.github.mal32.endergames.kits;

import io.github.mal32.endergames.phases.game.AbstractModule;

import java.util.List;
import java.util.Objects;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
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
    Bukkit.dispatchCommand(
        Bukkit.getConsoleSender(),
        "loot give " + player.getName() + " loot enga:kits/" + getName());
  }

  public String getName() {
    return this.getClass().getSimpleName().toLowerCase();
  }

  public abstract ItemStack getDescriptionItem();

  public static List<AbstractKit> getKits(JavaPlugin plugin) {
    return List.of(
            new Lumberjack(plugin),
            new Cat(plugin),
            new Cactus(plugin),
            new Barbarian(plugin),
            new Blaze(plugin),
            new Slime(plugin),
            new Dolphin(plugin));
  }
}
