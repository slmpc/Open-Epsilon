package com.github.epsilon.mixins.level;

import com.github.epsilon.managers.RotationManager;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Item.class)
public class MixinItem {

    @WrapOperation(method = "getPlayerPOVHitResult", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getYRot()F"))
    private static float modifyPlayerPOVHitResultYaw(Player player, Operation<Float> original) {
        if (player == Minecraft.getInstance().player) {
            return RotationManager.INSTANCE.getYaw();
        }
        return original.call(player);
    }

    @WrapOperation(method = "getPlayerPOVHitResult", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getXRot()F"))
    private static float modifyPlayerPOVHitResultPitch(Player player, Operation<Float> original) {
        if (player == Minecraft.getInstance().player) {
            return RotationManager.INSTANCE.getPitch();
        }
        return original.call(player);
    }

}
