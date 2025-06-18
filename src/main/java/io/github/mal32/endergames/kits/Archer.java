package io.github.mal32.endergames.kits;

import io.github.mal32.endergames.EnderGames;
import io.github.mal32.endergames.worlds.game.GameWorld;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;

public class Archer extends AbstractKit {
  private BukkitTask giveArrowTask;
  private BukkitTask tickBowChargingPlayersTask;
  private final HashMap<UUID, Integer> playerBowLoad = new HashMap<>();
  private final int MAX_CHARGE = 10;

  public Archer(EnderGames plugin) {
    super(plugin);
  }

  @Override
  public void enable() {
    super.enable();

    BukkitScheduler scheduler = plugin.getServer().getScheduler();
    giveArrowTask = scheduler.runTaskTimer(plugin, this::giveArrow, 12 * 20, 12 * 20);
    tickBowChargingPlayersTask =
        scheduler.runTaskTimer(plugin, this::tickBowChargingPlayers, 10, 10);
  }

  @Override
  public void disable() {
    super.disable();

    giveArrowTask.cancel();
    tickBowChargingPlayersTask.cancel();
  }

  private void giveArrow() {
    for (Player player : GameWorld.getPlayersInGame()) {
      if (!playerCanUseThisKit(player)) continue;

      player.getInventory().addItem(new ItemStack(Material.ARROW));
    }
  }

  @EventHandler
  public void onPlayerBowChargeStart(PlayerInteractEvent event) {
    var player = event.getPlayer();

    if (!playerCanUseThisKit(player)) return;
    if (!event.getAction().isRightClick()) return;
    if (event.getItem() == null || event.getItem().getType() != Material.BOW) return;

    playerBowLoad.put(player.getUniqueId(), 0);
  }

  private void tickBowChargingPlayers() {
    for (Map.Entry<UUID, Integer> entry : playerBowLoad.entrySet()) {
      Player player = plugin.getServer().getPlayer(entry.getKey());
      if (player == null || !playerCanUseThisKit(player)) {
        playerBowLoad.remove(entry.getKey());
        continue;
      }

      boolean stillUsingBow = player.isHandRaised();

      // Stop charging if the player is not holding a bow anymore
      if (!stillUsingBow) {
        playerBowLoad.remove(entry.getKey());
        continue;
      }

      // Player is still charging the bow
      int charge = entry.getValue();
      if (charge < MAX_CHARGE) {
        charge += 1;
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BANJO, 1, 1);
      }
      playerBowLoad.put(entry.getKey(), charge);

      var message =
          Component.text()
              .append(Component.text("❚".repeat(charge)).color(NamedTextColor.AQUA))
              .append(Component.text("❚".repeat(MAX_CHARGE - charge)).color(NamedTextColor.GRAY));
      player.sendActionBar(message);

      World world = player.getLocation().getWorld();
      Location location = player.getEyeLocation();
      location.add(location.getDirection().multiply(1));

      final int MAX_DUST_SIZE = 4;
      Particle.DustOptions dustOptions =
          new Particle.DustOptions(
              Color.AQUA, (float) Math.min((double) charge / 8 + 0.5, MAX_DUST_SIZE));
      world.spawnParticle(Particle.DUST, location, charge / 2, 0.5, 0.5, 0.5, 0, dustOptions);
    }
  }

  @EventHandler
  public void onBowShoot(EntityShootBowEvent event) {
    if (!(event.getEntity() instanceof Player player)) return;
    if (!playerCanUseThisKit(player)) return;

    if (event.getBow() == null || event.getBow().getType() != Material.BOW) return;

    if (!playerBowLoad.containsKey(player.getUniqueId())) return;

    int charge = playerBowLoad.get(player.getUniqueId());
    playerBowLoad.remove(player.getUniqueId());

    // actions
    Arrow arrow = (Arrow) event.getProjectile();

    final int MAX_DAMAGE = 20;
    arrow.setDamage(2 + ((double) charge / MAX_CHARGE) * MAX_DAMAGE);

    if (charge < MAX_CHARGE / 2) return;

    player.getWorld().playSound(player.getLocation(), Sound.ITEM_TRIDENT_THUNDER, 1, 1);
  }

  @Override
  public void start(Player player) {
    player.getInventory().addItem(new ItemStack(Material.BOW));
    player.getInventory().addItem(new ItemStack(Material.ARROW, 5));

    player.getInventory().setHelmet(new ItemStack(Material.GOLDEN_HELMET));
  }

  @Override
  public KitDescriptionItem getDescriptionItem() {
    return new KitDescriptionItem(
        Material.BOW,
        "Archer",
        "Gets a new Arrow every 12 seconds. Can overcharge the Bow to make more damage.",
        "Bow, 5 Arrows",
        Difficulty.MEDIUM);
  }
}
