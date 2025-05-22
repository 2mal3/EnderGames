package io.github.mal32.endergames.worlds.lobby;

import io.github.mal32.endergames.EnderGames;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

class OperatorItem implements Listener {
  private final EnderGames plugin;

  public OperatorItem(EnderGames plugin) {
    this.plugin = plugin;
    Bukkit.getPluginManager().registerEvents(this, plugin);
  }

  public void giveStartItem(Player player) {
    ItemStack startItem = new ItemStack(Material.NETHER_STAR);
    ItemMeta meta = startItem.getItemMeta();
    if (meta == null) return;
    meta.displayName(Component.text("§6Start Game"));
    startItem.setItemMeta(meta);
    player.getInventory().setItem(8, startItem);
  }

  @EventHandler
  public void onPlayerInteract(PlayerInteractEvent event) {
    if (!EnderGames.playerIsInLobbyWorld(event.getPlayer())) return;
    if (event.getItem() == null || event.getItem().getType() != Material.NETHER_STAR) return;
    this.plugin.getGameWorld().startGame();
  }
}
