package com.github.lumin;

import com.github.lumin.assets.i18n.LanguageReloadListener;
import com.github.lumin.assets.resources.ResourceLocationUtils;
import com.github.lumin.graphics.LuminRenderPipelines;
import com.github.lumin.managers.ModuleManager;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;

@EventBusSubscriber(modid = Lumin.MODID, value = Dist.CLIENT)
public class EventHandler {

    @SubscribeEvent
    private static void onRegisterRenderPipelines(RegisterRenderPipelinesEvent event) {
        LuminRenderPipelines.onRegisterRenderPipelines(event);
    }

    @SubscribeEvent
    private static void onKeyPress(InputEvent.Key event) {
        if (Minecraft.getInstance().level == null) return;
        ModuleManager.INSTANCE.onKeyEvent(event.getKey(), event.getAction());
    }

    @SubscribeEvent
    public static void onResourcesReload(AddClientReloadListenersEvent event) {
        event.addListener(ResourceLocationUtils.getIdentifier("objects/reload_listener"), new LanguageReloadListener());
    }

}
