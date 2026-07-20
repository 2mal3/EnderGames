package io.github.mal32.endergames.kitsystem.kits;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.UUID;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

class KnightTest extends KitMockBukkitTest<Knight> {
  @Override
  protected Knight createKit() {
    return new Knight(plugin);
  }

  /**
   * MockBukkit doesn't implement {@code Bukkit.getLootTable} / {@code Horse.setLootTable}, which
   * {@link Knight#spawnHorse} uses, so {@link Knight#initPlayer} can't run fully here. To still
   * verify the mount-protection logic we spawn the horses directly and register them in the kit's
   * private {@code mounts} map (the same state {@code spawnHorse} would produce) via reflection.
   */
  @SuppressWarnings("unchecked")
  private void registerMount(Player owner, Horse horse) throws Exception {
    Field mountsField = Knight.class.getDeclaredField("mounts");
    mountsField.setAccessible(true);
    Map<UUID, UUID> mounts = (Map<UUID, UUID>) mountsField.get(kit);
    mounts.put(owner.getUniqueId(), horse.getUniqueId());
  }

  private Horse spawnOwnedHorse(Player owner) {
    Horse horse = owner.getWorld().spawn(owner.getLocation(), Horse.class);
    horse.setOwner(owner);
    return horse;
  }

  @Test
  void testOtherKnightCannotMountKnightHorse() throws Exception {
    Horse knightHorse = spawnOwnedHorse(player);
    registerMount(player, knightHorse);

    // A different knight with their own horse still must not ride someone else's horse.
    PlayerMock otherKnight = server.addPlayer();
    Horse otherHorse = spawnOwnedHorse(otherKnight);
    registerMount(otherKnight, otherHorse);

    boolean mounted = knightHorse.addPassenger(otherKnight);

    assertFalse(mounted, "Other knights should not be able to mount a knight's horse");
    assertFalse(knightHorse.getPassengers().contains(otherKnight));
    assertNull(otherKnight.getVehicle());
  }

  @Test
  void testNonKnightCannotMountKnightHorse() throws Exception {
    Horse knightHorse = spawnOwnedHorse(player);
    registerMount(player, knightHorse);

    // A plain player who owns no horse at all (the realistic "other player" case).
    PlayerMock otherPlayer = server.addPlayer();

    boolean mounted = knightHorse.addPassenger(otherPlayer);

    assertFalse(mounted, "Non-knights should not be able to mount a knight's horse");
    assertFalse(knightHorse.getPassengers().contains(otherPlayer));
    assertNull(otherPlayer.getVehicle());
  }

  @Test
  void testOwnerCanMountOwnHorse() throws Exception {
    Horse knightHorse = spawnOwnedHorse(player);
    registerMount(player, knightHorse);

    boolean mounted = knightHorse.addPassenger(player);

    assertTrue(mounted, "The owner should be able to mount their own horse");
    assertTrue(knightHorse.getPassengers().contains(player));
    assertSame(knightHorse, player.getVehicle());
  }
}
