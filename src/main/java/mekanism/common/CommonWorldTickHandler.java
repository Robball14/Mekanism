package mekanism.common;

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import java.util.LinkedList;
import java.util.Queue;
import mekanism.common.frequency.FrequencyManager;
import mekanism.common.multiblock.MultiblockManager;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.WorldTickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;

public class CommonWorldTickHandler {

    private static final long maximumDeltaTimeNanoSecs = 16_000_000; // 16 milliseconds

    private Int2ObjectMap<Queue<ChunkPos>> chunkRegenMap;

    public void addRegenChunk(int dimensionId, ChunkPos chunkCoord) {
        if (chunkRegenMap == null) {
            chunkRegenMap = new Int2ObjectArrayMap<>();
        }
        if (!chunkRegenMap.containsKey(dimensionId)) {
            LinkedList<ChunkPos> list = new LinkedList<>();
            list.add(chunkCoord);
            chunkRegenMap.put(dimensionId, list);
        } else if (!chunkRegenMap.get(dimensionId).contains(chunkCoord)) {
            chunkRegenMap.get(dimensionId).add(chunkCoord);
        }
    }

    public void resetRegenChunks() {
        if (chunkRegenMap != null) {
            chunkRegenMap.clear();
        }
    }

    @SubscribeEvent
    public void onTick(WorldTickEvent event) {
        if (event.side == LogicalSide.SERVER) {
            if (event.phase == Phase.START) {
                tickStart(event.world);
            } else if (event.phase == Phase.END) {
                tickEnd(event.world);
            }
        }
    }

    public void tickStart(World world) {
        if (!world.isRemote) {
            if (!FrequencyManager.loaded) {
                FrequencyManager.load(world);
            }
        }
    }

    public void tickEnd(World world) {
        if (!world.isRemote) {
            MultiblockManager.tick(world);
            FrequencyManager.tick(world);
            //TODO: Fix chunk regeneration at some point
            /*if (chunkRegenMap == null) {
                return;
            }

            //TODO: I think this is wrong
            int dimensionId = world.getDimension().getType().getId();
            //Credit to E. Beef
            if (chunkRegenMap.containsKey(dimensionId)) {
                Queue<ChunkPos> chunksToGen = chunkRegenMap.get(dimensionId);
                long startTime = System.nanoTime();

                while (System.nanoTime() - startTime < maximumDeltaTimeNanoSecs && !chunksToGen.isEmpty()) {
                    ChunkPos nextChunk = chunksToGen.poll();
                    if (nextChunk == null) {
                        break;
                    }

                    Random fmlRandom = new Random(world.getSeed());
                    long xSeed = fmlRandom.nextLong() >> 2 + 1L;
                    long zSeed = fmlRandom.nextLong() >> 2 + 1L;
                    fmlRandom.setSeed((xSeed * nextChunk.x + zSeed * nextChunk.z) ^ world.getSeed());
                    Mekanism.genHandler.generate(fmlRandom, nextChunk.x, nextChunk.z, world, ((ServerChunkProvider) world.getChunkProvider()).getChunkGenerator(), world.getChunkProvider());
                    Mekanism.logger.info("Regenerating ores at chunk " + nextChunk);
                }
                if (chunksToGen.isEmpty()) {
                    chunkRegenMap.remove(dimensionId);
                }
            }*/
        }
    }
}