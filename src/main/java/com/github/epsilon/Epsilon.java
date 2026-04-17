package com.github.epsilon;

import com.github.epsilon.addon.EpsilonAddon;
import com.github.epsilon.addon.EpsilonAddonSetupEvent;
import com.github.epsilon.assets.i18n.I18NFileGenerator;
import com.github.epsilon.managers.ConfigManager;
import com.github.epsilon.managers.ModuleManager;
import com.github.epsilon.managers.TargetManager;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(value = Epsilon.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = Epsilon.MODID, value = Dist.CLIENT)
public class Epsilon {

    public static final String MODID = "open_epsilon";
    public static String VERSION = "Loading ...";

    public static final Logger LOGGER = LogManager.getLogger("Open Epsilon");

    public static int skipTicks;
    public static Minecraft mc;

    @SubscribeEvent
    private static void onClientSetup(FMLClientSetupEvent event) {
        LOGGER.info("Welcome to Epsilon, Meow~");

        mc = Minecraft.getInstance();
        VERSION = event.getContainer().getModInfo().getVersion().toString();

        // 初始化 Managers
        ModuleManager.INSTANCE.initModules();

        // 发送 Addon 注册事件，允许第三方 Addon 注册 Module
        EpsilonAddonSetupEvent addonEvent = NeoForge.EVENT_BUS.post(new EpsilonAddonSetupEvent());
        for (EpsilonAddon addon : addonEvent.addons) {
            addon.onSetup();
            LOGGER.info("Loaded Epsilon addon: {}", addon.addonId);
        }

        TargetManager.INSTANCE.clearSharedTarget();
        ConfigManager.INSTANCE.initConfig();

        // 生成空的 i18n 文件
        I18NFileGenerator.generate("epsilon-config/empty-i18n.json");

        // 添加一个退出游戏时候的钩子
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            ConfigManager.INSTANCE.saveNow();
            Epsilon.LOGGER.info("お兄ちゃん、私はあなたを一番愛しています~");
        }));

        Epsilon.LOGGER.info("Epsilon has loaded successfully, Meow~");
    }

}
