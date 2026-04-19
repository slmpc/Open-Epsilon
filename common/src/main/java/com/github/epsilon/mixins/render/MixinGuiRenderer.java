package com.github.epsilon.mixins.render;

import com.github.epsilon.events.bus.EpsilonEventBus;
import com.github.epsilon.events.render.Render2DEvent;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import net.minecraft.client.gui.render.GuiRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiRenderer.class)
public class MixinGuiRenderer {

    @Inject(method = "draw", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/render/GuiRenderer;executeDrawRange(Ljava/util/function/Supplier;Lcom/mojang/blaze3d/pipeline/RenderTarget;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;II)V", shift = At.Shift.BEFORE, ordinal = 0))
    public void renderInGameGuiPre(CallbackInfo ci) {
        EpsilonEventBus.INSTANCE.post(new Render2DEvent.BeforeInGameGui());
    }

    @Inject(method = "draw", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/render/GuiRenderer;executeDrawRange(Ljava/util/function/Supplier;Lcom/mojang/blaze3d/pipeline/RenderTarget;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;II)V", shift = At.Shift.AFTER, ordinal = 0))
    public void renderInGameGuiPost(CallbackInfo ci) {
        EpsilonEventBus.INSTANCE.post(new Render2DEvent.AfterInGameGui());
    }

    @Inject(method = "draw", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/render/GuiRenderer;executeDrawRange(Ljava/util/function/Supplier;Lcom/mojang/blaze3d/pipeline/RenderTarget;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;II)V", shift = At.Shift.BEFORE, ordinal = 1))
    public void renderGuiPre(CallbackInfo ci) {
        EpsilonEventBus.INSTANCE.post(new Render2DEvent.BeforeGui());
    }

    @Inject(method = "draw", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/render/GuiRenderer;executeDrawRange(Ljava/util/function/Supplier;Lcom/mojang/blaze3d/pipeline/RenderTarget;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;II)V", shift = At.Shift.AFTER, ordinal = 1))
    public void renderGuiPost(CallbackInfo ci) {
        EpsilonEventBus.INSTANCE.post(new Render2DEvent.AfterGui());
    }

}
