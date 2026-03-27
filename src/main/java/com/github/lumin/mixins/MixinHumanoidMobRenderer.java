package com.github.lumin.mixins;

import com.github.lumin.modules.impl.player.ElytraFly;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(HumanoidMobRenderer.class)
public abstract class MixinHumanoidMobRenderer {

    @ModifyExpressionValue(method = "extractHumanoidRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;isFallFlying()Z"))
    private static boolean spoofFallFlyingVisualState(boolean original, LivingEntity entity, HumanoidRenderState reusedState, float partialTick, ItemModelResolver itemModelResolver) {
        if (entity == Minecraft.getInstance().player && ElytraFly.INSTANCE.shouldVisualSpoof()) {
            return false;
        }
        return original;
    }

}
