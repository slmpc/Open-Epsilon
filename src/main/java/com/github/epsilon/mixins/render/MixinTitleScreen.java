package com.github.epsilon.mixins.render;

import com.github.epsilon.modules.impl.ClientSetting;
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
        ClientSetting clientSetting = ClientSetting.INSTANCE;
        if (clientSetting.getKeyBind() == -1) {
            clientSetting.setKeyBind(GLFW.GLFW_KEY_RIGHT_SHIFT);
        }
    }

}