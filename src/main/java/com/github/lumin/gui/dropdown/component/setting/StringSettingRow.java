package com.github.lumin.gui.dropdown.component.setting;

import com.github.lumin.graphics.renderers.RectRenderer;
import com.github.lumin.graphics.renderers.RoundRectRenderer;
import com.github.lumin.graphics.renderers.TextRenderer;
import com.github.lumin.gui.dropdown.DropdownLayout;
import com.github.lumin.gui.dropdown.DropdownTheme;
import com.github.lumin.gui.dropdown.component.SettingRow;
import com.github.lumin.settings.impl.StringSetting;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class StringSettingRow extends SettingRow<StringSetting> {

    public StringSettingRow(StringSetting setting) {
        super(setting);
    }

    @Override
    public void render(GuiGraphicsExtractor GuiGraphicsExtractor, RoundRectRenderer roundRectRenderer, RectRenderer rectRenderer, TextRenderer textRenderer, DropdownLayout.Rect bounds, float hoverProgress, int mouseX, int mouseY, float partialTick) {
        float labelScale = 0.68f;
        float valueScale = 0.60f;
        float labelY = bounds.y() + (bounds.height() - textRenderer.getHeight(labelScale)) / 2.0f - 1.0f;
        float valueY = bounds.y() + (bounds.height() - textRenderer.getHeight(valueScale)) / 2.0f - 1.0f;
        roundRectRenderer.addRoundRect(bounds.x(), bounds.y(), bounds.width(), bounds.height(), DropdownTheme.CARD_RADIUS, DropdownTheme.lerp(DropdownTheme.SURFACE_CONTAINER, DropdownTheme.SURFACE_CONTAINER_HIGH, hoverProgress));
        textRenderer.addText(setting.getDisplayName(), bounds.x() + DropdownTheme.ROW_CONTENT_INSET, labelY, labelScale, DropdownTheme.TEXT_PRIMARY);
        String value = setting.getValue();
        String shown = value == null ? "" : value;
        if (shown.length() > 22) {
            shown = shown.substring(0, 19) + "...";
        }
        textRenderer.addText(shown, bounds.right() - DropdownTheme.ROW_TRAILING_INSET - 92.0f, valueY, valueScale, DropdownTheme.TEXT_SECONDARY);
    }
}
