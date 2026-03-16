package com.github.lumin;

import com.github.lumin.assets.i18n.LanguageReloadListener;
import com.github.lumin.assets.resources.ResourceLocationUtils;
import com.github.lumin.graphics.LuminRenderPipelines;
import com.github.lumin.gui.clickgui.ClickGuiScreen;
import com.github.lumin.gui.menu.MainMenuScreen;
import com.github.lumin.managers.Managers;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;

@EventBusSubscriber(modid = Lumin.MODID, value = Dist.CLIENT)
public class EventHandler {

    @SubscribeEvent
    private static void onRegisterRenderPipelines(RegisterRenderPipelinesEvent event) {
        LuminRenderPipelines.onRegisterRenderPipelines(event);
    }

    @SubscribeEvent
    private static void onKeyPress(InputEvent.Key event) {
        Managers.MODULE.onKeyEvent(event.getKey(), event.getAction());
    }

    @SubscribeEvent
    public static void onResourcesReload(AddClientReloadListenersEvent event) {
        event.addListener(ResourceLocationUtils.getIdentifier("objects/reload_listener"), new LanguageReloadListener());
    }

    @SubscribeEvent
    public static void onRenderFramePost(RenderFrameEvent.Post event) {
        MainMenuScreen.INSTANCE.luminRender(event.getPartialTick());

        if (Minecraft.getInstance().level != null) {
            ClickGuiScreen.INSTANCE.luminRender(event.getPartialTick());
        }
    }

}
