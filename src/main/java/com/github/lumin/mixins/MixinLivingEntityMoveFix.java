package com.github.lumin.mixins;

import com.github.lumin.managers.MioRotationManager;
import com.github.lumin.utils.player.MoveUtils;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LivingEntity.class)
public class MixinLivingEntityMoveFix {

    @ModifyVariable(method = "travel", at = @At("HEAD"), argsOnly = true, require = 0)
    private Vec3 lumin$moveFix(Vec3 input) {
        if (MioRotationManager.INSTANCE.isMovementFixEnabled()) {
            float currentYaw = ((LivingEntity) (Object) this).getYRot();
            return MoveUtils.fixMovement(input, MioRotationManager.INSTANCE.getYaw(), currentYaw);
        }
        return input;
    }
}
