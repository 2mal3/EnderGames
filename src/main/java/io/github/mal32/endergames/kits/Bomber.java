package io.github.mal32.endergames.kits;

import io.github.mal32.endergames.EnderGames;
import io.github.mal32.endergames.worlds.game.GameWorld;
import java.util.HashSet;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemRarity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class Bomber extends AbstractKit {
  private final NamespacedKey isMineItemKey = new NamespacedKey(plugin, "isMineItem");
  private final HashSet<String> mineLocations = new HashSet<>();

  public Bomber(EnderGames plugin) {
    super(plugin);
  }

  @Override
  public void start(Player player) {
    ItemStack tntStack = new ItemStack(Material.TNT, 5);
    player.getInventory().addItem(tntStack);

    ItemStack mineStack = new ItemStack(Material.STONE_BUTTON, 10);
    var meta = mineStack.getItemMeta();
    meta.itemName(Component.text("Mine"));
    meta.setRarity(ItemRarity.RARE);
    meta.getPersistentDataContainer().set(isMineItemKey, PersistentDataType.BOOLEAN, true);
    mineStack.setItemMeta(meta);
    player.getInventory().addItem(mineStack);
  }

  @EventHandler
  private void onExplosionDamage(EntityDamageEvent event) {
    if (!(event.getEntity() instanceof Player player)) return;
    if (!playerCanUseThisKit(player)) return;

    if (event.getCause() == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION
        || event.getCause() == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION) {
      event.setDamage(0);
    }
  }

  @EventHandler
  private void onEntityDeath(EntityDeathEvent event) {
    // When a player or entity is killed by a Bomber, create an explosion at the death location
    Player killer = event.getEntity().getKiller();
    if (killer == null || !playerCanUseThisKit(killer)) return;

    Location location = event.getEntity().getLocation();
    location.createExplosion(killer, 4f, false, true);
  }

  @EventHandler
  private void onMinePlace(BlockPlaceEvent event) {
    if (event.getBlock().getType() != Material.STONE_BUTTON) return;
    Player player = event.getPlayer();
    if (!playerCanUseThisKit(player)) return;

    ItemStack item = player.getInventory().getItemInMainHand();
    var isMineItem =
        item.getItemMeta()
            .getPersistentDataContainer()
            .get(isMineItemKey, PersistentDataType.BOOLEAN);
    if (isMineItem == null || !isMineItem) return;

    var blockLocation = event.getBlockPlaced().getLocation();
    var key =
        blockLocation.getBlockX()
            + ","
            + blockLocation.getBlockY()
            + ","
            + blockLocation.getBlockZ();
    mineLocations.add(key);

    // Effects
    blockLocation.getWorld().playSound(blockLocation, Sound.BLOCK_VAULT_ACTIVATE, 1, 1);
    blockLocation
        .getWorld()
        .spawnParticle(Particle.SMOKE, blockLocation.clone().add(0.5, 0, 0.5), 5, 0.1, 0.1, 0.1, 0);
  }

  @EventHandler
  private void onStepOnMine(PlayerMoveEvent event) {
    if (!event.hasChangedBlock()) return;
    if (!GameWorld.playerIsInGame(event.getPlayer())) return;

    var blockLocation = event.getTo().getBlock().getLocation();
    var key =
        blockLocation.getBlockX()
            + ","
            + blockLocation.getBlockY()
            + ","
            + blockLocation.getBlockZ();
    if (!mineLocations.contains(key)) return;

    event.getTo().getBlock().setType(Material.AIR);

    event.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 10, 2));

    mineLocations.remove(key);
    blockLocation.createExplosion(4f, true, true);
  }

  @Override
  public KitDescription getDescription() {
    return new KitDescription(
        Material.TNT,
        "Bomber",
        "Takes no explosion damage. Killed entities explode. TNT placed explodes faster.",
        "5 TNT, 10 Mines",
        Difficulty.MEDIUM);
  }
}
