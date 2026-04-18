package com.github.epsilon.mixins.entity;

import com.github.epsilon.events.bus.EpsilonEventBus;
import com.github.epsilon.events.movement.FallFlyingEvent;
import com.github.epsilon.events.movement.JumpEvent;
import com.github.epsilon.events.movement.TravelEvent;
import com.github.epsilon.managers.RotationManager;
import com.github.epsilon.modules.impl.player.JumpCooldown;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector2f;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class MixinLivingEntity {

    @WrapOperation(method = "jumpFromGround", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getYRot()F"))
    private float redirectGetYRotInJumpFromGround(LivingEntity instance, Operation<Float> original) {
        if (instance == Minecraft.getInstance().player) {
            JumpEvent event = EpsilonEventBus.INSTANCE.post(new JumpEvent(instance.getYRot()));
            return event.getYaw();
        }
        return original.call(instance);
    }

    @WrapOperation(method = "tickHeadTurn", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getYRot()F"))
    private float modifyHeadYaw(LivingEntity entity, Operation<Float> original) {
        if (entity == Minecraft.getInstance().player) {
            Vector2f animationRotation = RotationManager.INSTANCE.animationRotation;
            if (animationRotation != null) {
                return animationRotation.x;
            }
        }
        return original.call(entity);
    }

    @WrapOperation(method = "updateFallFlyingMovement", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getXRot()F"))
    private float onUpdateFallFlyingMovement(LivingEntity instance, Operation<Float> original) {
        FallFlyingEvent event = EpsilonEventBus.INSTANCE.post(new FallFlyingEvent(original.call(instance)));
        return event.getPitch();
    }

    @WrapOperation(method = "aiStep", at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/LivingEntity;noJumpDelay:I", opcode = Opcodes.PUTFIELD, ordinal = 1))
    private void redirectJumpingCooldown(LivingEntity instance, int value, Operation<Void> original) {
        JumpCooldown module = JumpCooldown.INSTANCE;
        int newValue = value;
        if (instance == Minecraft.getInstance().player && module.isEnabled()) {
            newValue = module.cooldown.getValue();
        }
        original.call(instance, newValue);
    }

    @Inject(method = "travel", at = @At("HEAD"), cancellable = true)
    public void move(Vec3 vec3, CallbackInfo ci) {
        if ((LivingEntity) (Object) this == Minecraft.getInstance().player) {
            if (EpsilonEventBus.INSTANCE.postCancellable(new TravelEvent()).isCancelled()) {
                ci.cancel();
            }
        }
    }
}
