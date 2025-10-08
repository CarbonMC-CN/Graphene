package net.carbonmc.graphene.optimization.chunk;

import net.carbonmc.graphene.config.CoolConfig;
import net.carbonmc.graphene.util.LogThrottler;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;

import static net.carbonmc.graphene.config.CoolConfig.CTU;

public final class ChunkOptif {
    private static final ThreadLocal<Deque<Long>> REGION_STACK = ThreadLocal.withInitial(java.util.ArrayDeque::new);
    private static final ConcurrentHashMap<Long, Boolean> ACTIVE_REGIONS = new ConcurrentHashMap<>();
    private static final int REGION_SIZE_SHIFT = 4;
    private static final long INVALID_REGION_ID = 0L;
    private static final long COORDINATE_MASK = 0x00000000FFFFFFFFL;
    private static final int MAX_COORDINATE = 0x7FFF;

    private ChunkOptif() {
        if (!CTU.get()) return;
        throw new UnsupportedOperationException("Utility class");
    }
    public static long toid(ChunkPos chunkPosition) {
        if (chunkPosition == null) {
            return INVALID_REGION_ID;
        }

        try {
            int chunkX = chunkPosition.x;
            int chunkZ = chunkPosition.z;
            chunkX = Math.max(Math.min(chunkX, MAX_COORDINATE), -MAX_COORDINATE);
            chunkZ = Math.max(Math.min(chunkZ, MAX_COORDINATE), -MAX_COORDINATE);
            int regionX = calculateRegionCoordinate(chunkX);
            int regionZ = calculateRegionCoordinate(chunkZ);
            long regionId = combineCoordinates(regionX, regionZ);
            if (!isValidCalculatedRegionId(regionId)) {
                return fallbackRegionId(chunkX, chunkZ);
            }

            return regionId;

        } catch (Exception e) {
            System.err.println("Region ID calculation error for chunk " + chunkPosition + ": " + e.getMessage());
            return INVALID_REGION_ID;
        }
    }
    private static int calculateRegionCoordinate(int chunkCoord) {
        return chunkCoord >> REGION_SIZE_SHIFT;
    }
    private static long combineCoordinates(int regionX, int regionZ) {

        long xPart = ((long) regionX & COORDINATE_MASK) << 32;
        long zPart = (long) regionZ & COORDINATE_MASK;
        return xPart | zPart;
    }
    private static boolean isValidCalculatedRegionId(long regionId) {
        if (regionId == INVALID_REGION_ID) {
            return false;
        }
        long regionX = (regionId >> 32) & COORDINATE_MASK;
        long regionZ = regionId & COORDINATE_MASK;
        if (regionX == 0x7FFFFFFFL || regionX == 0xFFFFFFFFL ||
                regionZ == 0x7FFFFFFFL || regionZ == 0xFFFFFFFFL) {
            return false;
        }
        return Mth.abs((int)regionX) <= MAX_COORDINATE &&
                Mth.abs((int)regionZ) <= MAX_COORDINATE;
    }
    private static long fallbackRegionId(int chunkX, int chunkZ) {
        int safeX = (chunkX >> REGION_SIZE_SHIFT) & 0x7FFF;
        int safeZ = (chunkZ >> REGION_SIZE_SHIFT) & 0x7FFF;
        return ((long) safeX * 131071L) ^ ((long) safeZ * 524287L);
    }
    private static boolean isValidRegionId(long regionId) {
        if (regionId == INVALID_REGION_ID) {
            return false;
        }
        try {
            long regionX = (regionId >> 32) & COORDINATE_MASK;
            long regionZ = regionId & COORDINATE_MASK;
            if (regionId == 2147483647L || regionId == 9223372032559808512L ||
                    regionId == 9223372034707292159L) {
                return false;
            }
            int x = (int) regionX;
            int z = (int) regionZ;
            return Math.abs(x) <= 1000000 && Math.abs(z) <= 1000000;
        } catch (Exception e) {
            return false;
        }
    }
    public static void go(long regionIdentifier) {
        if (!CTU.get()) {
            return;
        }
        if (!isValidRegionId(regionIdentifier)) {
            String logKey = "invalid_region_" + regionIdentifier;
            if (CoolConfig.DEBUG_LOGGING.get() && LogThrottler.shouldLog(logKey)) {
                System.err.println("Attempted to push invalid region ID: " + regionIdentifier);
            }
            return;
        }

        Deque<Long> stack = REGION_STACK.get();
        if (stack != null) {
            if (stack.size() < 100) {
                stack.addLast(regionIdentifier);
                ACTIVE_REGIONS.put(regionIdentifier, true);

                if (CoolConfig.DEBUG_LOGGING.get()) {
                    System.out.println("[ChunkOptif] Pushed region: " + regionIdentifier +
                            " (stack size: " + stack.size() + ")");
                }
            } else if (CoolConfig.DEBUG_LOGGING.get()) {
                System.err.println("[ChunkOptif] Region stack overflow, size: " + stack.size());
            }
        }
    }
    public static void to() {
        if (!CTU.get()) {
            return;
        }

        Deque<Long> regionStack = REGION_STACK.get();
        if (regionStack != null && !regionStack.isEmpty()) {
            Long regionId = regionStack.pollLast();
            if (regionId != null && isValidRegionId(regionId)) {
                ACTIVE_REGIONS.remove(regionId);

                if (CoolConfig.DEBUG_LOGGING.get()) {
                    System.out.println("[ChunkOptif] Popped region: " + regionId +
                            " (stack size: " + regionStack.size() + ")");
                }
            }
        }
    }
    public static Long currentRegion() {
        Deque<Long> regionStack = REGION_STACK.get();
        if (regionStack == null || regionStack.isEmpty()) {
            return null;
        }

        Long regionId = regionStack.peekLast();
        return isValidRegionId(regionId) ? regionId : null;
    }
    public static void cleanup() {
        if (!CTU.get()) {
            return;
        }
        Deque<Long> stack = REGION_STACK.get();
        if (stack != null) {
            int cleared = stack.size();
            stack.clear();
            ACTIVE_REGIONS.clear();

            if (CoolConfig.DEBUG_LOGGING.get()) {
                System.out.println("[ChunkOptif] Cleaned up " + cleared + " region entries");
            }
        }
        REGION_STACK.remove();
    }
}