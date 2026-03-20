package com.github.lumin.mixins;

import com.github.lumin.modules.impl.render.NoRender;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ScreenEffectRenderer.class)
public class MixinScreenEffectRenderer {

    @Inject(method = "renderTex", at = @At("HEAD"), cancellable = true)
    private static void onRenderFluid(TextureAtlasSprite texture, PoseStack poseStack, MultiBufferSource bufferSource, CallbackInfo ci) {
        if (NoRender.INSTANCE.isEnabled() && NoRender.INSTANCE.blockOverlay.getValue()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderFire", at = @At("HEAD"), cancellable = true)
    private static void onRenderFire(PoseStack poseStack, MultiBufferSource bufferSource, TextureAtlasSprite texture, CallbackInfo ci) {
        if (NoRender.INSTANCE.isEnabled() && NoRender.INSTANCE.fireOverlay.getValue()) {
            ci.cancel();
        }
    }

}
