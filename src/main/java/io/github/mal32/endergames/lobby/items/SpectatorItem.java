package io.github.mal32.endergames.lobby.items;

import io.github.mal32.endergames.EnderGames;
import io.papermc.paper.persistence.PersistentDataContainerView;
import java.util.Map;
import java.util.Objects;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

class SpectatorItem extends MenuItem {

  public SpectatorItem(JavaPlugin plugin) {
    super(
        plugin,
        (byte) 8,
        "spectate_game",
        Map.of(
            "",
            new ItemDisplay(
                Material.ENDER_EYE,
                Component.text("Spectate Game").color(NamedTextColor.GOLD),
                NamespacedKey.minecraft("spyglass"))));
  }

  @Override
  public void onGameStart(Player player) {
    this.giveItem(player);
  }

  @Override
  public void onGameEnd(Player player) {
    @Nullable ItemStack item = player.getInventory().getItem(slot);
    if (item == null) return;
    PersistentDataContainerView pdc = item.getItemMeta().getPersistentDataContainer();
    if (!pdc.has(new NamespacedKey(plugin, "menu"))
        || !Objects.equals(
            pdc.get(new NamespacedKey(plugin, "menu"), PersistentDataType.STRING), "spectate_game"))
      return;
    player.getInventory().clear(slot);
  }

  @Override
  public void playerInteract(PlayerInteractEvent event) {
    Player player = event.getPlayer();
    player.playSound(player, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
    player.setGameMode(GameMode.SPECTATOR);
    ((EnderGames) plugin).sendToGame(player); // TODO: event?
  }
}
