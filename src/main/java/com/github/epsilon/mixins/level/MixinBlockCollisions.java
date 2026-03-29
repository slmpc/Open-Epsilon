package com.github.epsilon.mixins.level;

import com.github.epsilon.events.CollisionEvent;
import com.github.epsilon.modules.impl.player.Phase;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockCollisions;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(BlockCollisions.class)
public class MixinBlockCollisions {

    @Redirect(method = "computeNext", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/BlockGetter;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"))
    private BlockState hookComputeNext(BlockGetter instance, BlockPos blockPos) {
        if (!Phase.INSTANCE.isEnabled()) {
            return instance.getBlockState(blockPos);
        }
        CollisionEvent event = NeoForge.EVENT_BUS.post(new CollisionEvent(instance.getBlockState(blockPos), blockPos));
        return event.getState();
    }

}
