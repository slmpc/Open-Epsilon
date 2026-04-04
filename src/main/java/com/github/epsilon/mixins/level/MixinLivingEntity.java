package com.github.epsilon.mixins.level;

import com.github.epsilon.events.FallFlyingEvent;
import com.github.epsilon.events.JumpEvent;
import com.github.epsilon.events.TravelEvent;
import com.github.epsilon.managers.RotationManager;
import com.github.epsilon.modules.impl.player.JumpCooldown;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import org.joml.Vector2f;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class MixinLivingEntity {

    @Shadow
    private int noJumpDelay;

    @Redirect(method = "jumpFromGround", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getYRot()F"))
    private float redirectGetYRotInJumpFromGround(LivingEntity instance) {
        if (instance == Minecraft.getInstance().player) {
            JumpEvent event = NeoForge.EVENT_BUS.post(new JumpEvent(instance.getYRot()));
            return event.getYaw();
        }
        return instance.getYRot();
    }

    @Redirect(method = "tickHeadTurn", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getYRot()F"))
    private float modifyHeadYaw(LivingEntity entity) {
        if (entity == Minecraft.getInstance().player) {
            Vector2f animationRotation = RotationManager.INSTANCE.animationRotation;
            if (animationRotation != null) {
                return animationRotation.x;
            }
        }
        return entity.getYRot();
    }

    @Redirect(method = "updateFallFlyingMovement", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getXRot()F"))
    private float onUpdateFallFlyingMovement(LivingEntity instance) {
        FallFlyingEvent event = NeoForge.EVENT_BUS.post(new FallFlyingEvent(instance.getXRot()));
        return event.getPitch();
    }

    @Redirect(method = "aiStep", at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/LivingEntity;noJumpDelay:I", opcode = Opcodes.PUTFIELD, ordinal = 1))
    private void redirectJumpingCooldown(LivingEntity instance, int value) {
        JumpCooldown module = JumpCooldown.INSTANCE;
        if (instance == Minecraft.getInstance().player && module.isEnabled()) {
            this.noJumpDelay = module.cooldown.getValue();
        } else {
            this.noJumpDelay = value;
        }
    }

    @Inject(method = "travel", at = @At("HEAD"), cancellable = true)
    public void move(Vec3 vec3, CallbackInfo ci) {
        if ((LivingEntity) (Object) this == Minecraft.getInstance().player) {
            if (NeoForge.EVENT_BUS.post(new TravelEvent()).isCanceled()) {
                ci.cancel();
            }
        }
    }

}
