package com.github.epsilon.modules.impl.render;

import com.github.epsilon.modules.Category;
import com.github.epsilon.modules.Module;
import com.github.epsilon.settings.impl.IntSetting;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

public class FOV extends Module {

    public static final FOV INSTANCE = new FOV();

    private final IntSetting fovModifier = intSetting("FOV Modifier", 120, 0, 358, 1);

    private FOV() {
        super("FOV", Category.RENDER);
    }

    @SubscribeEvent
    private void onComputeFov(ViewportEvent.ComputeFov event) {
        event.setFOV(fovModifier.getValue());
    }

}
