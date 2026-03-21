package io.github.mal32.endergames.kitsystem.api;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class DummyKit extends AbstractKit {
  public boolean enabled = false;
  public boolean disabled = false;

  public DummyKit(String id, KitService kitService, JavaPlugin plugin) {
    super(new KitDescription(id, Material.STONE, "", "", Difficulty.EASY), kitService, plugin);
  }

  @Override
  public void initPlayer(Player player) {}

  @Override
  public void onEnable() {
    enabled = true;
  }

  @Override
  public void onDisable() {
    disabled = true;
  }
}
