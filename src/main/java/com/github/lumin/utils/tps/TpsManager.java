package com.github.lumin.utils.tps;

import com.github.lumin.Lumin;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = Lumin.MODID, value = Dist.CLIENT)
public final class TpsManager {

    private static long lastTick = System.nanoTime();
    private static float tps = 20.0f;

    private TpsManager() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Pre event) {
        long now = System.nanoTime();
        long diff = now - lastTick;
        lastTick = now;
        if (diff <= 0) return;
        float tickTime = diff / 1_000_000_000.0f;
        if (tickTime <= 0) return;
        float current = 1.0f / tickTime;
        if (current > 100.0f) current = 100.0f;
        if (current < 0.1f) current = 0.1f;
        tps = tps * 0.9f + current * 0.1f;
    }

    public static float getTps() {
        return tps;
    }

    public static float getTpsFactor() {
        if (tps <= 0.0f) return 1.0f;
        return 20.0f / tps;
    }
}
