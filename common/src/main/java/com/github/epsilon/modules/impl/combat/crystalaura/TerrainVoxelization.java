package com.github.epsilon.modules.impl.combat.crystalaura;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 地形体素化工具 —— 将玩家周围的方块存在信息编码为紧凑的位图，
 * 供 GPU Compute Shader 做爆炸射线遮挡查询。
 *
 * <h3>设计思路</h3>
 * 参考 VXGI 的 Voxelization，但不需要存储球谐 (SH) 系数，
 * 只需存储每个体素是否为实心方块（1 bit / voxel）。
 *
 * <h3>滚动窗口</h3>
 * 体素网格以玩家当前位置为中心，当玩家移动时网格整体滚动：
 * 仅需更新新进入区域的列，避免每帧全量重建。
 *
 * <h3>数据布局（std430 SSBO）</h3>
 * <pre>
 *   Header (16 bytes, vec4 对齐):
 *     ivec3 gridOrigin   — 网格世界空间起始坐标
 *     int   gridSize     — 单轴体素数量（gridSize^3 个体素）
 *
 *   Body:
 *     uint voxelBits[]   — 每 uint 存 32 个体素的位图
 *                          总长 = ceil(gridSize^3 / 32)
 * </pre>
 */
public class TerrainVoxelization {

    private static final Minecraft mc = Minecraft.getInstance();

    /** 单轴体素数量，必须为 32 的倍数以便位对齐 */
    public static final int GRID_SIZE = 64;

    /** 体素总数 */
    public static final int TOTAL_VOXELS = GRID_SIZE * GRID_SIZE * GRID_SIZE;

    /** 位图 uint 数量 */
    public static final int BITMAP_UINT_COUNT = TOTAL_VOXELS / 32;

    /** Header 大小：ivec3 gridOrigin (3×4B) + int gridSize (4B) = 16B */
    public static final int HEADER_BYTES = 16;

    /** Body 大小 */
    public static final int BODY_BYTES = BITMAP_UINT_COUNT * Integer.BYTES;

    /** SSBO 总大小 */
    public static final int BUFFER_SIZE = HEADER_BYTES + BODY_BYTES;

    // ── 滚动状态 ──

    private int originX, originY, originZ;
    private final int[] voxelBits = new int[BITMAP_UINT_COUNT];
    private boolean needsFullRebuild = true;

    /**
     * 每帧调用：以玩家位置为中心更新体素网格。
     * 返回 true 表示数据已变更，需重新上传 SSBO。
     */
    public boolean update() {
        if (mc.player == null || mc.level == null) return false;

        int half = GRID_SIZE / 2;
        int cx = (int) Math.floor(mc.player.getX()) - half;
        int cy = (int) Math.floor(mc.player.getY()) - half;
        int cz = (int) Math.floor(mc.player.getZ()) - half;

        if (needsFullRebuild || cx != originX || cy != originY || cz != originZ) {
            originX = cx;
            originY = cy;
            originZ = cz;
            rebuildFull();
            needsFullRebuild = false;
            return true;
        }
        return false;
    }

    /**
     * 完整重建体素网格。
     */
    private void rebuildFull() {
        java.util.Arrays.fill(voxelBits, 0);

        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        for (int lx = 0; lx < GRID_SIZE; lx++) {
            for (int ly = 0; ly < GRID_SIZE; ly++) {
                for (int lz = 0; lz < GRID_SIZE; lz++) {
                    mutable.set(originX + lx, originY + ly, originZ + lz);
                    BlockState state = mc.level.getBlockState(mutable);
                    if (isSolid(state)) {
                        int idx = flatIndex(lx, ly, lz);
                        voxelBits[idx >> 5] |= (1 << (idx & 31));
                    }
                }
            }
        }
    }

    /**
     * 判断方块是否应视为实心（遮挡射线）。
     */
    private static boolean isSolid(BlockState state) {
        return !state.isAir() && !state.getCollisionShape(mc.level, BlockPos.ZERO).isEmpty();
    }

    /**
     * 三维坐标 → 一维索引（Z-major 排列以利于 GPU 缓存）。
     */
    private static int flatIndex(int x, int y, int z) {
        return (z * GRID_SIZE + y) * GRID_SIZE + x;
    }

    // ── 数据写入 ──

    /**
     * 将体素数据写入 Std430Writer（header + body）。
     */
    public void writeTo(com.github.epsilon.graphics.vulkan.buffer.Std430Writer writer) {
        writer.putInt(originX);
        writer.putInt(originY);
        writer.putInt(originZ);
        writer.putInt(GRID_SIZE);

        for (int bits : voxelBits) {
            writer.putUInt(bits);
        }
    }

    /**
     * 标记需要在下一帧进行完整重建。
     */
    public void invalidate() {
        needsFullRebuild = true;
    }

    public int getOriginX() { return originX; }
    public int getOriginY() { return originY; }
    public int getOriginZ() { return originZ; }
}
