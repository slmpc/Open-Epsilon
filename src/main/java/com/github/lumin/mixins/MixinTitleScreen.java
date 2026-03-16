package com.github.lumin.mixins;

import com.github.lumin.gui.menu.MainMenuScreen;
import com.github.lumin.modules.impl.client.ClickGui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public class MixinTitleScreen {

    @Inject(method = "init", at = @At("HEAD"))
    private void onInit(CallbackInfo ci) {
        ClickGui clickGui = ClickGui.INSTANCE;
        if (clickGui.getKeyBind() == -1) {
            clickGui.setKeyBind(GLFW.GLFW_KEY_RIGHT_SHIFT);
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof TitleScreen) {
            mc.setScreen(MainMenuScreen.INSTANCE);
        }
    }

}