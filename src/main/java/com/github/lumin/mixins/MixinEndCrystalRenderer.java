package com.github.lumin.mixins;

import com.github.lumin.modules.impl.combat.AutoCrystal;
import net.minecraft.client.renderer.entity.EndCrystalRenderer;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EndCrystalRenderer.class)
public class MixinEndCrystalRenderer {

    @Inject(method = {"render", "renderCrystal"}, at = @At("HEAD"), cancellable = true, require = 0)
    private void lumin$renderHook(EndCrystal crystal, float entityYaw, float partialTicks, com.mojang.blaze3d.vertex.PoseStack poseStack, net.minecraft.client.renderer.MultiBufferSource buffer, int packedLight, CallbackInfo ci) {
        if (AutoCrystal.INSTANCE.shouldInhibitRender(crystal)) {
            ci.cancel();
        }
    }
}
