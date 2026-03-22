package com.github.lumin.gui.dropdown.util;

import com.github.lumin.graphics.renderers.RectRenderer;
import com.github.lumin.graphics.renderers.RoundRectRenderer;
import com.github.lumin.graphics.renderers.ShadowRenderer;
import com.github.lumin.graphics.renderers.TextRenderer;
import com.github.lumin.gui.dropdown.DropdownLayout;
import net.minecraft.client.Minecraft;

public final class DropdownScissor {

    private DropdownScissor() {
    }

    public static void apply(DropdownLayout.Rect rect, RectRenderer rectRenderer, RoundRectRenderer roundRectRenderer, ShadowRenderer shadowRenderer, TextRenderer textRenderer, int guiHeight) {
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
