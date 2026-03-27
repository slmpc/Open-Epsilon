package com.github.epsilon.gui.panel.component.setting;

import com.github.epsilon.graphics.renderers.RectRenderer;
import com.github.epsilon.graphics.renderers.RoundRectRenderer;
import com.github.epsilon.graphics.renderers.TextRenderer;
import com.github.epsilon.graphics.text.StaticFontLoader;
import com.github.epsilon.gui.panel.PanelLayout;
import com.github.epsilon.gui.panel.MD3Theme;
import com.github.epsilon.gui.panel.component.SettingRow;
import com.github.epsilon.settings.impl.EnumSetting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;

public class EnumSettingRow extends SettingRow<EnumSetting<?>> {

    private static final String DROPDOWN_ICON = "v";

    public EnumSettingRow(EnumSetting<?> setting) {
        super(setting);
    }

    @Override
    public void render(GuiGraphicsExtractor GuiGraphicsExtractor, RoundRectRenderer roundRectRenderer, RectRenderer rectRenderer, TextRenderer textRenderer, PanelLayout.Rect bounds, float hoverProgress, int mouseX, int mouseY, float partialTick) {
        float labelScale = 0.68f;
        float labelY = bounds.y() + (bounds.height() - textRenderer.getHeight(labelScale)) / 2.0f - 1.0f;
        float chipTextScale = 0.60f;
        roundRectRenderer.addRoundRect(bounds.x(), bounds.y(), bounds.width(), bounds.height(), MD3Theme.CARD_RADIUS, MD3Theme.lerp(MD3Theme.SURFACE_CONTAINER, MD3Theme.SURFACE_CONTAINER_HIGH, hoverProgress));
        textRenderer.addText(setting.getDisplayName(), bounds.x() + MD3Theme.ROW_CONTENT_INSET, labelY, labelScale, MD3Theme.TEXT_PRIMARY);
        PanelLayout.Rect chipBounds = getChipBounds(textRenderer, bounds);
        float chipX = chipBounds.x();
        float chipWidth = chipBounds.width();
        roundRectRenderer.addRoundRect(chipX, chipBounds.y(), chipWidth, chipBounds.height(), 7.0f, MD3Theme.SECONDARY_CONTAINER);
        float chipTextY = chipBounds.y() + (chipBounds.height() - textRenderer.getHeight(chipTextScale)) / 2.0f - 1.0f;
        textRenderer.addText(setting.getTranslatedValue(), chipX + 8.0f, chipTextY, chipTextScale, MD3Theme.ON_SECONDARY_CONTAINER);
        float iconWidth = textRenderer.getWidth(DROPDOWN_ICON, 0.58f, StaticFontLoader.ICONS);
        float iconY = chipBounds.y() + (chipBounds.height() - textRenderer.getHeight(0.58f, StaticFontLoader.ICONS)) / 2.0f - 1.0f;
        textRenderer.addText(DROPDOWN_ICON, chipBounds.right() - 8.0f - iconWidth, iconY, 0.58f, MD3Theme.ON_SECONDARY_CONTAINER, StaticFontLoader.ICONS);
    }

    public PanelLayout.Rect getChipBounds(TextRenderer textRenderer, PanelLayout.Rect bounds) {
        String value = setting.getTranslatedValue();
        float chipWidth = Math.min(96.0f, textRenderer.getWidth(value, 0.60f) + 26.0f);
        float chipX = bounds.right() - MD3Theme.ROW_TRAILING_INSET - chipWidth;
        return new PanelLayout.Rect(chipX, bounds.y() + 5.0f, chipWidth, 16.0f);
    }

    @Override
    public boolean mouseClicked(PanelLayout.Rect bounds, MouseButtonEvent event, boolean isDoubleClick) {
        return bounds.contains(event.x(), event.y()) && event.button() == 0;
    }

}
