package com.github.lumin.modules.impl.render;

import com.github.lumin.modules.Category;
import com.github.lumin.modules.Module;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

/**
 * @author tRollaURa_
 * @since 2026/03/22 13:15
 */

public class Mascot extends Module {
    public static final Mascot INSTANCE = new Mascot();
    private float dvdX = 100;
    private float dvdY = 100;

    private float velX = 1.5f;
    private float velY = 1.2f;

    private final int imgW = 100;
    private final int imgH = 100;


    private static final Identifier GUNMU =
            Identifier.fromNamespaceAndPath(
                    "lumin",
                    "textures/gunmu.png"
            );
    public Mascot() {
        super("Mascot",Category.RENDER);
    }


    @SubscribeEvent
    private void onRenderGui(RenderGuiEvent.Post event) {
        if (nullCheck()) return;
        //还没日出来
    }
}
