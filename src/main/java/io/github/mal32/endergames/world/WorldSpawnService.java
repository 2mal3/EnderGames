package io.github.mal32.endergames.world;

import java.util.HashSet;
import java.util.Set;
import org.bukkit.Location;
import org.bukkit.block.Biome;

public class WorldSpawnService {
  private static final Set<Biome> VALID_BIOMES = new HashSet<>(Set.of(
      Biome.DEEP_FROZEN_OCEAN,
      Biome.DEEP_COLD_OCEAN,
      Biome.DEEP_OCEAN,
      Biome.DEEP_LUKEWARM_OCEAN,
      Biome.FROZEN_OCEAN,
      Biome.OCEAN,
      Biome.COLD_OCEAN,
      Biome.LUKEWARM_OCEAN,
      Biome.WARM_OCEAN,
      Biome.JUNGLE,
      Biome.SPARSE_JUNGLE,
      Biome.BAMBOO_JUNGLE,
      Biome.RIVER,
      Biome.STONY_SHORE
  ));

  public void registerInvalidBiome(Biome biome) {
    VALID_BIOMES.add(biome);
  }

  // Why doesn't BiomeTagKeys.IS_OCEAN work?
  // using directly:
  // https://github.com/misode/mcmeta/blob/data/data/minecraft/tags/worldgen/biome/is_ocean.json
  public boolean isInvalidBiome(Biome biome) {
    return VALID_BIOMES.contains(biome);
  }

  public Location findNextValidSpawn(Location start) {
    Location loc = start.clone();
    do {
      loc.add(1000, 0, 0);
      loc.getChunk().load(true);
    } while (isInvalidBiome(loc.getBlock().getBiome()));
    return loc;
  }
}
