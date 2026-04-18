package com.github.epsilon.neoforge;

import com.github.epsilon.CommonListeners;
import com.github.epsilon.Epsilon;
import com.github.epsilon.addon.AddonBootstrap;
import com.github.epsilon.addon.EpsilonAddonSetupEvent;
import com.github.epsilon.events.bus.EpsilonEventBus;
import com.github.epsilon.neoforge.compat.NeoForgePlatformCompat;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

@Mod(value = Epsilon.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = Epsilon.MODID, value = Dist.CLIENT)
public class EpsilonNeo {

    public EpsilonNeo() {
        // Some NeoForge client events fire before FMLClientSetupEvent.
        if (Epsilon.platform == null) {
            Epsilon.platform = new NeoForgePlatformCompat();
        }
    }

    @SubscribeEvent
    private static void onClientSetup(FMLClientSetupEvent event) {
        Epsilon.VERSION = event.getContainer().getModInfo().getVersion().toString();
        if (Epsilon.platform == null) {
            Epsilon.platform = new NeoForgePlatformCompat();
        }

        // Common initialization
        Epsilon.init();
        CommonListeners.register();

        // 发送 Addon 注册事件，允许第三方 Addon 注册 Module
        EpsilonAddonSetupEvent addonEvent = EpsilonEventBus.INSTANCE.post(new EpsilonAddonSetupEvent());
        AddonBootstrap.setupAddons(addonEvent);
    }

}
