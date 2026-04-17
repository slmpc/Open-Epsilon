package com.github.epsilon;

import com.github.epsilon.assets.i18n.LanguageReloadListener;
import com.github.epsilon.assets.resources.ResourceLocationUtils;
import com.github.epsilon.graphics.LuminRenderPipelines;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;

@EventBusSubscriber(modid = Epsilon.MODID, value = Dist.CLIENT)
public class EventHandler {

    @SubscribeEvent
    private static void onRegisterRenderPipelines(RegisterRenderPipelinesEvent event) {
        LuminRenderPipelines.registerAll(event::registerPipeline);
    }

    @SubscribeEvent
    public static void onResourcesReload(AddClientReloadListenersEvent event) {
        event.addListener(ResourceLocationUtils.getIdentifier("objects/reload_listener"), new LanguageReloadListener());
    }

}
