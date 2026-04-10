package com.github.epsilon.gui.panel.component;

import com.github.epsilon.assets.i18n.EpsilonTranslateComponent;
import com.github.epsilon.assets.i18n.TranslateComponent;
import com.github.epsilon.graphics.renderers.RectRenderer;
import com.github.epsilon.graphics.renderers.RoundRectRenderer;
import com.github.epsilon.graphics.renderers.TextRenderer;
import com.github.epsilon.graphics.text.StaticFontLoader;
import com.github.epsilon.gui.panel.MD3Theme;
import com.github.epsilon.gui.panel.PanelLayout;
import com.github.epsilon.gui.panel.adapter.ModuleViewModel;
import com.mojang.blaze3d.platform.InputConstants;

import java.awt.*;

public class ModuleRow {

    public static final float HEIGHT = 34.0f;

    private final ModuleViewModel module;
    private final PanelLayout.Rect bounds;

    private static final TranslateComponent noneComponent = EpsilonTranslateComponent.create("keybind", "none");

    public ModuleRow(ModuleViewModel module, PanelLayout.Rect bounds) {
        this.module = module;
        this.bounds = bounds;
    }

    public ModuleViewModel getModule() {
        return module;
    }

    public PanelLayout.Rect getBounds() {
        return bounds;
    }

    public PanelLayout.Rect getToggleBounds() {
        return new PanelLayout.Rect(bounds.right() - MD3Theme.ROW_TRAILING_INSET - 30.0f, bounds.y() + 8.0f, 30.0f, 16.0f);
    }

    public void render(RoundRectRenderer roundRectRenderer, RectRenderer rectRenderer, TextRenderer textRenderer, float hoverProgress, float selectedProgress, float toggleProgress, float toggleHoverProgress) {
        float titleScale = 0.70f;
        float subScale = 0.60f;
        float keyScale = 0.6f;
        float titleHeight = textRenderer.getHeight(titleScale, StaticFontLoader.DUCKSANS);
        float subHeight = textRenderer.getHeight(subScale);
        float lineGap = 3.0f;
        float totalTextHeight = titleHeight + lineGap + subHeight;
        float titleY = bounds.y() + (bounds.height() - totalTextHeight) / 2.0f - 1.0f;
        float subY = titleY + titleHeight + lineGap - 1.0f;
        float keyY = bounds.y() + (bounds.height() - textRenderer.getHeight(keyScale)) / 2.0f - 1.0f;
        Color hoverBackground = MD3Theme.lerp(MD3Theme.SURFACE_CONTAINER, MD3Theme.SURFACE_CONTAINER_HIGH, hoverProgress);
        Color background = MD3Theme.lerp(hoverBackground, MD3Theme.PRIMARY_CONTAINER, selectedProgress);
        Color titleColor = MD3Theme.lerp(MD3Theme.TEXT_PRIMARY, MD3Theme.ON_PRIMARY_CONTAINER, selectedProgress);
        Color subColor = MD3Theme.lerp(MD3Theme.TEXT_SECONDARY, MD3Theme.withAlpha(MD3Theme.ON_PRIMARY_CONTAINER, 180), selectedProgress);
        Color keyColor = MD3Theme.isLightTheme() ? MD3Theme.TEXT_SECONDARY : MD3Theme.TEXT_MUTED;
        String keybindText = formatKeybind(module.module().getKeyBind());
        float keyWidth = textRenderer.getWidth(keybindText, keyScale);
        float keyX = getToggleBounds().x() - 8.0f - keyWidth;

        roundRectRenderer.addRoundRect(bounds.x(), bounds.y(), bounds.width(), bounds.height(), MD3Theme.CARD_RADIUS, background);

        textRenderer.addText(module.displayName(), bounds.x() + MD3Theme.ROW_CONTENT_INSET + 4.0f, titleY, titleScale, titleColor, StaticFontLoader.DUCKSANS);
        String addonText = module.module().getAddonId() != null ? module.module().getAddonId() : "unknown";
        textRenderer.addText(addonText, bounds.x() + MD3Theme.ROW_CONTENT_INSET + 4.0f, subY, subScale, subColor);
        drawSwitch(roundRectRenderer, getToggleBounds(), toggleProgress, toggleHoverProgress);
        textRenderer.addText(keybindText, keyX, keyY, keyScale, keyColor);
    }

    private void drawSwitch(RoundRectRenderer roundRectRenderer, PanelLayout.Rect rect, float toggleProgress, float toggleHoverProgress) {
        Color track = MD3Theme.lerp(MD3Theme.SURFACE_CONTAINER_HIGHEST, MD3Theme.PRIMARY, toggleProgress);
        Color knob = MD3Theme.lerp(MD3Theme.OUTLINE, MD3Theme.ON_PRIMARY, toggleProgress);

        float knobSize = 8.0f + 3.0f * toggleProgress;
        float knobTravel = rect.width() - 10.0f - knobSize;
        float knobX = rect.x() + 5.0f + knobTravel * toggleProgress;
        float knobY = rect.centerY() - knobSize / 2.0f;

        roundRectRenderer.addRoundRect(rect.x(), rect.y(), rect.width(), rect.height(), rect.height() / 2.0f, track);

        if (toggleHoverProgress > 0.02f) {
            float haloSize = 16.0f;
            float haloX = knobX + knobSize / 2.0f - haloSize / 2.0f;
            float haloY = rect.centerY() - haloSize / 2.0f;
            roundRectRenderer.addRoundRect(haloX, haloY, haloSize, haloSize, haloSize / 2.0f,
                    MD3Theme.withAlpha(MD3Theme.TEXT_PRIMARY, (int) (18 * toggleHoverProgress)));
        }

        roundRectRenderer.addRoundRect(knobX, knobY, knobSize, knobSize, knobSize / 2.0f, knob);
    }

    private String formatKeybind(int keyCode) {
        if (keyCode < 0) {
            return noneComponent.getTranslatedName();
        }
        return InputConstants.Type.KEYSYM.getOrCreate(keyCode).getDisplayName().getString().toUpperCase();
    }

}
