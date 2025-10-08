package net.carbonmc.graphene.engine.cull;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.SectionPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;

import java.util.HashSet;
import java.util.Set;

public class CullingEngine {
    private static final double MAX_ENTITY_CHECK_VOLUME = 16 * 16 * 16 * 15;
    private final Minecraft client;
    private final Set<ChunkPos> visibleChunks = new HashSet<>();
    private boolean enabled = true;

    public CullingEngine(Minecraft client) {
        this.client = client;
    }

    public void updateVisibleChunks(ClientLevel world, int renderDistance) {
        visibleChunks.clear();

        if (client.player == null) return;

        SectionPos center = SectionPos.of(client.player.blockPosition());
        int radius = Math.min(renderDistance, 32);

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                if (x * x + z * z <= radius * radius) {
                    visibleChunks.add(new ChunkPos(center.x() + x, center.z() + z));
                }
            }
        }
    }

    public boolean shouldRenderEntity(Entity entity) {
        if (!enabled) return true;

        if (client.shouldEntityAppearGlowing(entity) || entity.shouldShowName()) {
            return true;
        }

        AABB box = entity.getBoundingBoxForCulling();
        if (isInfiniteExtentsBox(box) || isVeryLargeBox(box)) {
            return true;
        }

        return isBoxVisible(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
    }

    public boolean shouldRenderBlockEntity(BlockEntity blockEntity) {
        if (!enabled) return true;
        if (blockEntity.hasCustomOutlineRendering(client.player)) {
            return true;
        }

        AABB box = blockEntity.getRenderBoundingBox();
        return isBoxVisible(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
    }

    private boolean isBoxVisible(double x1, double y1, double z1,
                                 double x2, double y2, double z2) {
        int minX = SectionPos.posToSectionCoord(x1 - 0.5D);
        int minZ = SectionPos.posToSectionCoord(z1 - 0.5D);
        int maxX = SectionPos.posToSectionCoord(x2 + 0.5D);
        int maxZ = SectionPos.posToSectionCoord(z2 + 0.5D);

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                if (visibleChunks.contains(new ChunkPos(x, z))) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isInfiniteExtentsBox(AABB box) {
        return Double.isInfinite(box.minX) || Double.isInfinite(box.minY) || Double.isInfinite(box.minZ)
                || Double.isInfinite(box.maxX) || Double.isInfinite(box.maxY) || Double.isInfinite(box.maxZ);
    }

    private boolean isVeryLargeBox(AABB box) {
        double volume = (box.maxX - box.minX) * (box.maxY - box.minY) * (box.maxZ - box.minZ);
        return volume > MAX_ENTITY_CHECK_VOLUME;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}