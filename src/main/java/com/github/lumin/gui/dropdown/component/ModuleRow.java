package com.github.lumin.gui.dropdown.component;

import com.github.lumin.graphics.renderers.RectRenderer;
import com.github.lumin.graphics.renderers.RoundRectRenderer;
import com.github.lumin.graphics.renderers.TextRenderer;
import com.github.lumin.graphics.text.StaticFontLoader;
import com.github.lumin.gui.dropdown.DropdownLayout;
import com.github.lumin.gui.dropdown.DropdownTheme;
import com.github.lumin.gui.dropdown.adapter.ModuleViewModel;

import java.awt.*;

public class ModuleRow {

    public static final float HEIGHT = 34.0f;

    private final ModuleViewModel module;
    private final DropdownLayout.Rect bounds;

    public ModuleRow(ModuleViewModel module, DropdownLayout.Rect bounds) {
        this.module = module;
        this.bounds = bounds;
    }

    public ModuleViewModel getModule() {
        return module;
    }

    public DropdownLayout.Rect getBounds() {
        return bounds;
    }

    public DropdownLayout.Rect getToggleBounds() {
        return new DropdownLayout.Rect(bounds.right() - DropdownTheme.ROW_TRAILING_INSET - 24.0f, bounds.y() + 9.0f, 24.0f, 14.0f);
    }

    public void render(RoundRectRenderer roundRectRenderer, RectRenderer rectRenderer, TextRenderer textRenderer, float hoverProgress, float selectedProgress) {
        float titleScale = 0.70f;
        float subScale = 0.60f;
        float keyScale = 0.58f;
        float titleHeight = textRenderer.getHeight(titleScale, StaticFontLoader.DUCKSANS);
        float subHeight = textRenderer.getHeight(subScale);
        float lineGap = 2.0f;
        float totalTextHeight = titleHeight + lineGap + subHeight;
        float titleY = bounds.y() + (bounds.height() - totalTextHeight) / 2.0f - 1.0f;
        float subY = titleY + titleHeight + lineGap - 1.0f;
        float keyY = bounds.y() + (bounds.height() - textRenderer.getHeight(keyScale)) / 2.0f - 1.0f;
        Color hoverBackground = DropdownTheme.lerp(DropdownTheme.SURFACE_CONTAINER, DropdownTheme.SURFACE_CONTAINER_HIGH, hoverProgress);
        Color background = DropdownTheme.lerp(hoverBackground, DropdownTheme.PRIMARY_CONTAINER, selectedProgress);
        Color titleColor = DropdownTheme.lerp(DropdownTheme.TEXT_PRIMARY, DropdownTheme.ON_PRIMARY_CONTAINER, selectedProgress);
        Color subColor = DropdownTheme.lerp(DropdownTheme.TEXT_SECONDARY, DropdownTheme.withAlpha(DropdownTheme.ON_PRIMARY_CONTAINER, 180), selectedProgress);

        roundRectRenderer.addRoundRect(bounds.x(), bounds.y(), bounds.width(), bounds.height(), DropdownTheme.CARD_RADIUS, background);

        textRenderer.addText(module.displayName(), bounds.x() + DropdownTheme.ROW_CONTENT_INSET + 4.0f, titleY, titleScale, titleColor, StaticFontLoader.DUCKSANS);
        textRenderer.addText(module.category().getName(), bounds.x() + DropdownTheme.ROW_CONTENT_INSET + 4.0f, subY, subScale, subColor);
        drawSwitch(roundRectRenderer, getToggleBounds(), module.enabled());
        textRenderer.addText(module.module().getKeyBind() == -1 ? "-" : Integer.toString(module.module().getKeyBind()), getToggleBounds().x() - 16.0f, keyY, keyScale, DropdownTheme.TEXT_MUTED);
    }

    private void drawSwitch(RoundRectRenderer roundRectRenderer, DropdownLayout.Rect rect, boolean enabled) {
        Color track = enabled ? DropdownTheme.PRIMARY : DropdownTheme.SURFACE_CONTAINER_HIGHEST;
        Color knob = enabled ? DropdownTheme.ON_PRIMARY_CONTAINER : DropdownTheme.TEXT_SECONDARY;
        roundRectRenderer.addRoundRect(rect.x(), rect.y(), rect.width(), rect.height(), DropdownTheme.CHIP_RADIUS, track);
        float knobSize = rect.height() - 6.0f;
        float knobX = enabled ? rect.right() - knobSize - 3.0f : rect.x() + 3.0f;
        roundRectRenderer.addRoundRect(knobX, rect.y() + 3.0f, knobSize, knobSize, DropdownTheme.CHIP_RADIUS, knob);
    }

}
