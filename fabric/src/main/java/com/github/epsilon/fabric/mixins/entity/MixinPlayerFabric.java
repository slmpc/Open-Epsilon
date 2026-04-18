package com.github.epsilon.fabric.mixins.entity;

import com.github.epsilon.managers.RotationManager;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Player.class)
public class MixinPlayerFabric {

    @WrapOperation(method = "doSweepAttack", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getYRot()F"))
    private float modifySweepAttackYaw(Player player, Operation<Float> original) {
        if (player == Minecraft.getInstance().player) {
            return RotationManager.INSTANCE.getYaw();
        }
        return original.call(player);
    }

}

