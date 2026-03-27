package com.github.lumin.mixins;

import com.github.lumin.modules.impl.player.Velocity;
import net.minecraft.core.Direction;
import net.minecraft.world.level.material.FlowingFluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Iterator;

@Mixin(FlowingFluid.class)
public class MixinFlowingFluid {

    @Redirect(method = "getFlow", at = @At(value = "INVOKE", target = "Ljava/util/Iterator;hasNext()Z", ordinal = 0))
    private boolean hookGetFlow(Iterator<Direction> iterator) {
        if (Velocity.INSTANCE.isEnabled() && Velocity.INSTANCE.waterPush.getValue()) {
            return false;
        }
        return iterator.hasNext();
    }

}
