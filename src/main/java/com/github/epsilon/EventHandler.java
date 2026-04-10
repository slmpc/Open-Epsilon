package com.github.epsilon;

import com.github.epsilon.assets.i18n.LanguageReloadListener;
import com.github.epsilon.assets.resources.ResourceLocationUtils;
import com.github.epsilon.events.EpsilonRenderGuiEvent;
import com.github.epsilon.graphics.LuminRenderPipelines;
import com.github.epsilon.managers.ModuleManager;
import com.github.epsilon.managers.RenderManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = Epsilon.MODID, value = Dist.CLIENT)
public class EventHandler {

    @SubscribeEvent
    private static void onRegisterRenderPipelines(RegisterRenderPipelinesEvent event) {
        LuminRenderPipelines.onRegisterRenderPipelines(event);
    }

    @SubscribeEvent
    private static void onKeyPress(InputEvent.Key event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.screen != null || event.getKey() == GLFW.GLFW_KEY_UNKNOWN) return;
        ModuleManager.INSTANCE.onKeyEvent(event.getKey(), event.getAction());
    }

    @SubscribeEvent
    public static void onResourcesReload(AddClientReloadListenersEvent event) {
        event.addListener(ResourceLocationUtils.getIdentifier("objects/reload_listener"), new LanguageReloadListener());
    }

    @SubscribeEvent
    public static void onRenderFramePost(RenderFrameEvent.Post event) {
        RenderSystem.backupProjectionMatrix();
        RenderManager.INSTANCE.callAfterFrame(Minecraft.getInstance().getDeltaTracker());
        RenderSystem.restoreProjectionMatrix();
        RenderManager.INSTANCE.clear();
    }

    @SubscribeEvent
    public static void onRenderInGameGuiPre(EpsilonRenderGuiEvent.BeforeInGameGui event) {
        RenderSystem.backupProjectionMatrix();
        RenderManager.INSTANCE.callInGameGui(Minecraft.getInstance().getDeltaTracker());
        RenderSystem.restoreProjectionMatrix();
    }

}
