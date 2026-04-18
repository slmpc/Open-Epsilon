package com.github.epsilon.mixins.entity;

import com.github.epsilon.managers.RotationManager;
import com.github.epsilon.modules.impl.player.AutoSprint;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public class MixinPlayer {

    @WrapOperation(method = "causeExtraKnockback", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getYRot()F"))
    private float modifyExtraKnockbackYaw(Player player, Operation<Float> original) {
        if (player == Minecraft.getInstance().player) {
            return RotationManager.INSTANCE.getYaw();
        }
        return original.call(player);
    }

    @Inject(method = "causeExtraKnockback", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;setSprinting(Z)V", shift = At.Shift.AFTER))
    private void hookCauseExtraKnockback(CallbackInfo callbackInfo) {
        if (AutoSprint.INSTANCE.isEnabled() && AutoSprint.INSTANCE.keepSprint.getValue()) {
            Minecraft mc = Minecraft.getInstance();
            float multiplier = 0.6f + 0.4f * AutoSprint.INSTANCE.motion.getValue().floatValue();
            mc.player.setDeltaMovement(mc.player.getDeltaMovement().x / 0.6 * multiplier, mc.player.getDeltaMovement().y, mc.player.getDeltaMovement().z / 0.6 * multiplier);
            mc.player.setSprinting(true);
        }
    }

}
