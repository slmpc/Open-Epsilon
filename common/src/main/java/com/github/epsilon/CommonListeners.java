package com.github.epsilon;

import com.github.epsilon.events.bus.EpsilonEventBus;
import com.github.epsilon.events.bus.EventHandler;
import com.github.epsilon.events.input.KeyInputEvent;
import com.github.epsilon.events.render.Render2DEvent;
import com.github.epsilon.events.render.RenderFrameEvent;
import com.github.epsilon.managers.ModuleManager;
import com.github.epsilon.managers.RenderManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

/**
 * Common event listeners that are registered to the custom EpsilonEventBus.
 * These are loader-independent.
 */
public class CommonListeners {

    public static final CommonListeners INSTANCE = new CommonListeners();

    private CommonListeners() {}

    public static void register() {
        EpsilonEventBus.INSTANCE.subscribe(INSTANCE);
    }

    @EventHandler
    public void onKeyPress(KeyInputEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.gui.screen() != null || event.getKey() == GLFW.GLFW_KEY_UNKNOWN) return;
        ModuleManager.INSTANCE.onKeyEvent(event.getKey(), event.getAction());
    }

    @EventHandler
    public void onRenderFramePost(RenderFrameEvent.Post event) {
        RenderSystem.backupProjectionMatrix();
        RenderManager.INSTANCE.callAfterFrame(Minecraft.getInstance().getDeltaTracker());
        RenderSystem.restoreProjectionMatrix();
        RenderManager.INSTANCE.clear();
    }

    @EventHandler
    public void onRenderInGameGuiPre(Render2DEvent.BeforeInGameGui event) {
        RenderSystem.backupProjectionMatrix();
        RenderManager.INSTANCE.callInGameGui(Minecraft.getInstance().getDeltaTracker());
        RenderSystem.restoreProjectionMatrix();
    }
}

