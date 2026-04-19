package com.github.epsilon.modules.impl.combat.crystalaura;

import com.github.epsilon.graphics.vulkan.buffer.Std430Writer;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;

import java.util.Arrays;

/**
 * 地形体素化工具 —— 将玩家周围的方块存在信息编码为紧凑的位图，
 * 供 GPU Compute Shader 做爆炸射线遮挡查询。
 *
 * <h3>数据布局（std430 SSBO）</h3>
 * <pre>
 *   Header (16 bytes):
 *     int gridOriginX
 *     int gridOriginY
 *     int gridOriginZ
 *     int gridSize
 *
 *   Body:
 *     uint voxelBits[BITMAP_UINT_COUNT]
 *     — 每 uint 存 32 个体素
 *     — flatIndex(x,y,z) = (z * GRID_SIZE + y) * GRID_SIZE + x
 * </pre>
 *
 * <h3>写入字节数</h3>
 * Header: 4 × int = 16 bytes（Std430Writer 的 putInt 按 4 字节对齐，连续写无间隙）。
 * Body:   BITMAP_UINT_COUNT × 4 bytes = 32768 bytes。
 * 合计:   {@link #BUFFER_SIZE} = 32784 bytes。
 * <p>
 * {@link #writeTo(Std430Writer)} 写入恰好 {@link #BUFFER_SIZE} 字节。
 */
public class TerrainVoxelization {

    private static final Minecraft mc = Minecraft.getInstance();

    /** 单轴体素数量（必须为 32 的倍数） */
    public static final int GRID_SIZE = 64;

    /** 体素总数 */
    public static final int TOTAL_VOXELS = GRID_SIZE * GRID_SIZE * GRID_SIZE;

    /** 位图 uint 数量 */
    public static final int BITMAP_UINT_COUNT = TOTAL_VOXELS / 32;

    /** Header: 4 × int = 16 bytes */
    public static final int HEADER_BYTES = 4 * Integer.BYTES;

    /** Body: BITMAP_UINT_COUNT × 4 bytes */
    public static final int BODY_BYTES = BITMAP_UINT_COUNT * Integer.BYTES;

    /** SSBO 总大小（writeTo 写入的精确字节数） */
    public static final int BUFFER_SIZE = HEADER_BYTES + BODY_BYTES;

    // ── 状态 ──

    private int originX, originY, originZ;
    private final int[] voxelBits = new int[BITMAP_UINT_COUNT];
    private boolean needsFullRebuild = true;

    /**
     * 每帧调用：以玩家位置为中心更新体素网格。
     * 返回 true 表示数据已变更。
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
        Arrays.fill(voxelBits, 0);

        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        for (int lz = 0; lz < GRID_SIZE; lz++) {
            int wz = originZ + lz;
            for (int ly = 0; ly < GRID_SIZE; ly++) {
                int wy = originY + ly;
                for (int lx = 0; lx < GRID_SIZE; lx++) {
                    int wx = originX + lx;
                    mutable.set(wx, wy, wz);

                    // 用实际坐标查询碰撞体积，而非 BlockPos.ZERO
                    BlockState state = mc.level.getBlockState(mutable);
                    if (!state.isAir()
                            && !state.getCollisionShape(mc.level, mutable, CollisionContext.empty()).isEmpty()) {
                        int idx = flatIndex(lx, ly, lz);
                        voxelBits[idx >> 5] |= (1 << (idx & 31));
                    }
                }
            }
        }
    }

    /**
     * Z-major 线性索引，与 shader 中 sampleVoxel 一致。
     */
    private static int flatIndex(int x, int y, int z) {
        return (z * GRID_SIZE + y) * GRID_SIZE + x;
    }

    // ─── 数据写入 ────────────────────────────────────────────────────────────

    /**
     * 将体素数据写入 {@link Std430Writer}。
     * <p>
     * 写入精确 {@link #BUFFER_SIZE} 字节（16 header + 32768 body）。
     * 使用 {@code putInt} 而非 {@code putUInt}，二者等价（4 字节对齐 + 4 字节写入）。
     * 由于连续写 int 且起始 position 为 0（由调用方 clear），不会产生对齐填充。
     */
    public void writeTo(Std430Writer writer) {
        // Header: 4 × int = 16 bytes
        writer.putInt(originX);
        writer.putInt(originY);
        writer.putInt(originZ);
        writer.putInt(GRID_SIZE);

        // Body: BITMAP_UINT_COUNT × int = 32768 bytes
        for (int i = 0; i < BITMAP_UINT_COUNT; i++) {
            writer.putInt(voxelBits[i]);
        }
        // 此时 writer.writtenBytes() == BUFFER_SIZE
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
