package com.github.lumin.mixins;

import com.github.lumin.modules.impl.player.ElytraFly;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(AvatarRenderer.class)
public abstract class MixinAvatarRenderer {

    @ModifyExpressionValue(method = "extractFlightData", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Avatar;getFallFlyingTicks()I"))
    private int spoofFallFlyingTicks(int original, Avatar entity, AvatarRenderState reusedState, float partialTick) {
        if (entity == Minecraft.getInstance().player && ElytraFly.INSTANCE.shouldVisualSpoof()) {
            return 0;
        }
        return original;
    }

}
