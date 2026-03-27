package com.github.lumin.mixins;

import com.github.lumin.modules.impl.render.NoRender;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class MixinGui {

    @Inject(method = "renderVignette", at = @At("HEAD"), cancellable = true)
    private void onRenderVignette(GuiGraphics guiGraphics, Entity entity, CallbackInfo ci) {
        if (NoRender.INSTANCE.noVignette()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderEffects", at = @At("HEAD"), cancellable = true)
    private void onRenderEffects(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (NoRender.INSTANCE.noPotionEffects()) {
            ci.cancel();
        }
    }

}