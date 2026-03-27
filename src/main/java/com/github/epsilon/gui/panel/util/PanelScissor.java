package com.github.epsilon.gui.panel.util;

import com.github.epsilon.graphics.renderers.RectRenderer;
import com.github.epsilon.graphics.renderers.RoundRectRenderer;
import com.github.epsilon.graphics.renderers.ShadowRenderer;
import com.github.epsilon.graphics.renderers.TextRenderer;
import com.github.epsilon.gui.panel.PanelLayout;
import net.minecraft.client.Minecraft;

public final class PanelScissor {

    private PanelScissor() {
    }

    public static void apply(PanelLayout.Rect rect, RectRenderer rectRenderer, RoundRectRenderer roundRectRenderer, ShadowRenderer shadowRenderer, TextRenderer textRenderer, int guiHeight) {
        int scale = Minecraft.getInstance().getWindow().getGuiScale();
        int x = Math.round(rect.x() * scale);
        int y = Math.round((guiHeight - rect.bottom()) * scale);
        int width = Math.round(rect.width() * scale);
        int height = Math.round(rect.height() * scale);
        rectRenderer.setScissor(x, y, width, height);
        roundRectRenderer.setScissor(x, y, width, height);
        shadowRenderer.setScissor(x, y, width, height);
        textRenderer.setScissor(x, y, width, height);
    }

    public static void clear(RectRenderer rectRenderer, RoundRectRenderer roundRectRenderer, ShadowRenderer shadowRenderer, TextRenderer textRenderer) {
        rectRenderer.clearScissor();
        roundRectRenderer.clearScissor();
        shadowRenderer.clearScissor();
        textRenderer.clearScissor();
    }

}
