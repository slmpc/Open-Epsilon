package com.github.lumin.mixins;

import com.github.lumin.events.JumpEvent;
import com.github.lumin.managers.RotationManager;
import com.github.lumin.modules.impl.render.NoRender;
import com.github.lumin.ducks.PlayerHurtAccess;
import com.github.lumin.modules.impl.player.JumpCooldown;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.common.NeoForge;
import org.joml.Vector2f;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class MixinLivingEntity implements PlayerHurtAccess {

    @Unique
    private boolean lumin$hurt;

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

    @Redirect(method = "aiStep", at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/LivingEntity;noJumpDelay:I", opcode = Opcodes.PUTFIELD, ordinal = 1))
    private void redirectJumpingCooldown(LivingEntity instance, int value) {
        JumpCooldown module = JumpCooldown.INSTANCE;
        if (instance == Minecraft.getInstance().player && module.isEnabled()) {
            this.noJumpDelay = module.cooldown.getValue();
        } else {
            this.noJumpDelay = value;
        }
    }

    @Inject(method = "handleEntityEvent", at = @At("HEAD"), cancellable = true)
    private void lumin$noTotemEffect(byte status, CallbackInfo ci) {
        if (NoRender.INSTANCE.isEnabled() && NoRender.INSTANCE.totems.getValue() && status == 35) {
            ci.cancel();
        }
    }

    @Override
    public void lumin$setHurt(boolean hurt) {
        this.lumin$hurt = hurt;
    }

    @Override
    public boolean lumin$isHurt() {
        return lumin$hurt;
    }

}
