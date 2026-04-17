package com.github.epsilon;

import com.github.epsilon.addon.EpsilonAddon;
import com.github.epsilon.addon.EpsilonAddonSetupEvent;
import com.github.epsilon.events.bus.EpsilonEventBus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

@Mod(value = Epsilon.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = Epsilon.MODID, value = Dist.CLIENT)
public class EpsilonNeo {

    @SubscribeEvent
    private static void onClientSetup(FMLClientSetupEvent event) {
        Epsilon.VERSION = event.getContainer().getModInfo().getVersion().toString();
        Epsilon.platform = new NeoForgePlatformCompat();

        // Common initialization
        Epsilon.init();
        CommonListeners.register();

        // 发送 Addon 注册事件，允许第三方 Addon 注册 Module（仅NeoForge可用）
        EpsilonAddonSetupEvent addonEvent = EpsilonEventBus.INSTANCE.post(new EpsilonAddonSetupEvent());
        for (EpsilonAddon addon : addonEvent.addons) {
            addon.onSetup();
            Epsilon.LOGGER.info("Loaded Epsilon addon: {}", addon.addonId);
        }
    }

}
