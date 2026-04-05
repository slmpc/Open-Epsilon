package com.github.epsilon.modules.impl.render;

import com.github.epsilon.modules.Category;
import com.github.epsilon.modules.Module;
import com.github.epsilon.settings.impl.IntSetting;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ViewportEvent;

/*
 * Author Moli
 */

@EventBusSubscriber(Dist.CLIENT)
public class FOV extends Module {

    public static final FOV INSTANCE = new FOV();

    private final IntSetting fovModifier = intSetting("FOV Modifier", 120, 0, 358, 1);

    private FOV() {
        super("FOV", Category.RENDER);
    }

    @SubscribeEvent
    public static void onComputeFov(ViewportEvent.ComputeFov event) {
        if (!INSTANCE.isEnabled()) return;
        event.setFOV(INSTANCE.fovModifier.getValue());
    }
}
