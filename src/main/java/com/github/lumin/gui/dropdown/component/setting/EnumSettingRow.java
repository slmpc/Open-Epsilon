package com.github.lumin.gui.dropdown.component.setting;

import com.github.lumin.graphics.renderers.RectRenderer;
import com.github.lumin.graphics.renderers.RoundRectRenderer;
import com.github.lumin.graphics.renderers.TextRenderer;
import com.github.lumin.graphics.text.StaticFontLoader;
import com.github.lumin.gui.dropdown.DropdownLayout;
import com.github.lumin.gui.dropdown.DropdownTheme;
import com.github.lumin.gui.dropdown.component.SettingRow;
import com.github.lumin.settings.impl.EnumSetting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;

public class EnumSettingRow extends SettingRow<EnumSetting<?>> {

    private static final String DROPDOWN_ICON = "v";

    public EnumSettingRow(EnumSetting<?> setting) {
        super(setting);
    }

    @Override
    public void render(GuiGraphicsExtractor GuiGraphicsExtractor, RoundRectRenderer roundRectRenderer, RectRenderer rectRenderer, TextRenderer textRenderer, DropdownLayout.Rect bounds, float hoverProgress, int mouseX, int mouseY, float partialTick) {
        float labelScale = 0.68f;
        float labelY = bounds.y() + (bounds.height() - textRenderer.getHeight(labelScale)) / 2.0f - 1.0f;
        float chipTextScale = 0.60f;
        roundRectRenderer.addRoundRect(bounds.x(), bounds.y(), bounds.width(), bounds.height(), DropdownTheme.CARD_RADIUS, DropdownTheme.lerp(DropdownTheme.SURFACE_CONTAINER, DropdownTheme.SURFACE_CONTAINER_HIGH, hoverProgress));
        textRenderer.addText(setting.getDisplayName(), bounds.x() + DropdownTheme.ROW_CONTENT_INSET, labelY, labelScale, DropdownTheme.TEXT_PRIMARY);
        DropdownLayout.Rect chipBounds = getChipBounds(textRenderer, bounds);
        float chipX = chipBounds.x();
        float chipWidth = chipBounds.width();
        roundRectRenderer.addRoundRect(chipX, chipBounds.y(), chipWidth, chipBounds.height(), 7.0f, DropdownTheme.SECONDARY_CONTAINER);
        float chipTextY = chipBounds.y() + (chipBounds.height() - textRenderer.getHeight(chipTextScale)) / 2.0f - 1.0f;
        textRenderer.addText(setting.getTranslatedValue(), chipX + 8.0f, chipTextY, chipTextScale, DropdownTheme.ON_SECONDARY_CONTAINER);
        float iconWidth = textRenderer.getWidth(DROPDOWN_ICON, 0.58f, StaticFontLoader.ICONS);
        float iconY = chipBounds.y() + (chipBounds.height() - textRenderer.getHeight(0.58f, StaticFontLoader.ICONS)) / 2.0f - 1.0f;
        textRenderer.addText(DROPDOWN_ICON, chipBounds.right() - 8.0f - iconWidth, iconY, 0.58f, DropdownTheme.ON_SECONDARY_CONTAINER, StaticFontLoader.ICONS);
    }

    public DropdownLayout.Rect getChipBounds(TextRenderer textRenderer, DropdownLayout.Rect bounds) {
        String value = setting.getTranslatedValue();
        float chipWidth = Math.min(96.0f, textRenderer.getWidth(value, 0.60f) + 26.0f);
        float chipX = bounds.right() - DropdownTheme.ROW_TRAILING_INSET - chipWidth;
        return new DropdownLayout.Rect(chipX, bounds.y() + 5.0f, chipWidth, 16.0f);
    }

    @Override
    public boolean mouseClicked(DropdownLayout.Rect bounds, MouseButtonEvent event, boolean isDoubleClick) {
        return bounds.contains(event.x(), event.y()) && event.button() == 0;
    }

}
