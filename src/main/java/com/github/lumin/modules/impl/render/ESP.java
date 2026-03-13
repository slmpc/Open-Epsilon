package com.github.lumin.modules.impl.render;

import com.github.lumin.modules.Category;
import com.github.lumin.modules.Module;
import com.github.lumin.settings.impl.BoolSetting;
import com.github.lumin.settings.impl.DoubleSetting;
import com.github.lumin.utils.render.Render3DUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.awt.*;

public class ESP extends Module {

    public static final ESP INSTANCE = new ESP();

    private ESP() {
        super("ESP", Category.RENDER);
    }

    private final BoolSetting chests = boolSetting("Chests", true);
    private final DoubleSetting range = doubleSetting("Range", 64.0, 1.0, 128.0, 1.0);

    @SubscribeEvent
    public void onRender3D(RenderLevelStageEvent.AfterEntities event) {
        if (nullCheck()) return;

        if (chests.getValue()) {
            double maxRange = range.getValue();
            int renderDistance = mc.options.renderDistance().get();

            BlockPos playerPos = mc.player.blockPosition();
            ChunkPos playerChunk = mc.player.chunkPosition();

            for (int x = -renderDistance; x <= renderDistance; x++) {
                for (int z = -renderDistance; z <= renderDistance; z++) {
                    for (BlockEntity entity : mc.level.getChunk(playerChunk.x + x, playerChunk.z + z).getBlockEntities().values()) {
                        if (!(entity instanceof RandomizableContainerBlockEntity)) continue;

                        BlockPos blockPos = entity.getBlockPos();
                        if (blockPos.distSqr(playerPos) > maxRange * maxRange) continue;

                        Render3DUtils.drawFilledBox(event.getPoseStack(), getAABB(blockPos), getColor(entity));
                    }
                }
            }
        }

    }

    private AABB getAABB(BlockPos blockPos) {
        BlockState state = mc.level.getBlockState(blockPos);
        return state.getShape(mc.level, blockPos).bounds().move(blockPos);
    }

    public int getColor(BlockEntity blockEntity) {
        if (blockEntity instanceof RandomizableContainerBlockEntity) {
            return new Color(0, 255, 0, 100).getRGB();
        }
        return 0xFFFFFFFF;
    }

}
