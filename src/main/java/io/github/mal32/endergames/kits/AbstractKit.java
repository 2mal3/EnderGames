package io.github.mal32.endergames.kits;

import io.github.mal32.endergames.phases.game.AbstractModule;
import java.util.Objects;
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

  public abstract void start(Player player);

  public String getName() {
    return this.getClass().getSimpleName().toLowerCase();
  }

  public abstract ItemStack getDescriptionItem();
}
