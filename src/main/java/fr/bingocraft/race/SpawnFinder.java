package fr.bingocraft.race;

import fr.bingocraft.BingoCraft;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.BiomeTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeCoords;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.noise.NoiseConfig;

import java.util.Random;

/**
 * Picks a random landing spot for a team: within {@code radius} blocks of the world spawn, never
 * over an ocean, river or beach, and never over water (lakes, swamps). The returned position is
 * at the top of the world: players fall from the build limit.
 */
public final class SpawnFinder {

    private static final int ATTEMPTS = 40;

    private SpawnFinder() { }

    public static BlockPos find(ServerWorld world, Random random, int radius) {
        BlockPos center = world.getSpawnPos();
        ChunkGenerator generator = world.getChunkManager().getChunkGenerator();
        NoiseConfig noise = world.getChunkManager().getNoiseConfig();
        int topY = world.getTopY() - 2;

        for (int attempt = 0; attempt < ATTEMPTS; attempt++) {
            int x = center.getX() + random.nextInt(radius * 2 + 1) - radius;
            int z = center.getZ() + random.nextInt(radius * 2 + 1) - radius;

            // Cheap check first, from the biome source: no chunk is loaded for rejected spots.
            RegistryEntry<Biome> biome = generator.getBiomeSource().getBiome(BiomeCoords.fromBlock(x),
                    BiomeCoords.fromBlock(generator.getSeaLevel()), BiomeCoords.fromBlock(z), noise.getMultiNoiseSampler());
            if (biome.isIn(BiomeTags.IS_OCEAN) || biome.isIn(BiomeTags.IS_DEEP_OCEAN)
                    || biome.isIn(BiomeTags.IS_RIVER) || biome.isIn(BiomeTags.IS_BEACH)) {
                continue;
            }

            // Then the real surface (generates the chunk): no landing in a lake.
            world.getChunk(x >> 4, z >> 4);
            int surface = world.getTopY(Heightmap.Type.MOTION_BLOCKING, x, z);
            if (!world.getFluidState(new BlockPos(x, surface - 1, z)).isEmpty()) continue;

            return new BlockPos(x, topY, z);
        }
        BingoCraft.LOGGER.warn("[Race] No dry land found in {} attempts, using the world spawn", ATTEMPTS);
        return new BlockPos(center.getX(), topY, center.getZ());
    }
}
