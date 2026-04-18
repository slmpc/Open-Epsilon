package com.github.epsilon.mixins.level;

import com.github.epsilon.events.bus.EpsilonEventBus;
import com.github.epsilon.events.movement.CollisionEvent;
import com.github.epsilon.modules.impl.player.Phase;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockCollisions;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(BlockCollisions.class)
public class MixinBlockCollisions {

    @WrapOperation(method = "computeNext", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/BlockGetter;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"))
    private BlockState hookComputeNext(BlockGetter instance, BlockPos blockPos, Operation<BlockState> original) {
        if (!Phase.INSTANCE.isEnabled()) {
            return original.call(instance, blockPos);
        }
        CollisionEvent event = EpsilonEventBus.INSTANCE.post(new CollisionEvent(original.call(instance, blockPos), blockPos));
        return event.getState();
    }
}
