package com.github.lumin;

import com.github.lumin.graphics.LuminRenderPipelines;
import com.github.lumin.managers.Managers;
import com.github.lumin.utils.AuthUtils;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;

@EventBusSubscriber(modid = Lumin.MODID, value = Dist.CLIENT)
public class EventHandler {

    private static int tickCounter = 0;

    @SubscribeEvent
    private static void onRegisterRenderPipelines(RegisterRenderPipelinesEvent event) {
        LuminRenderPipelines.onRegisterRenderPipelines(event);
    }

    @SubscribeEvent
    private static void onKeyPress(InputEvent.Key event) {
        Managers.MODULE.onKeyEvent(event.getKey(), event.getAction());
    }

    @SubscribeEvent
    private static void onClientTick(ClientTickEvent.Post event) {
        tickCounter++;
        if (tickCounter >= 100) { // Check every 100 ticks (approx 5 seconds)
            tickCounter = 0;
            AuthUtils.doSomethingImportant();
        }
    }

}
