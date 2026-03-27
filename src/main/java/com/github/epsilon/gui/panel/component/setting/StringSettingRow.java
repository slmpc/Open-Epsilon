package com.github.epsilon.gui.panel.component.setting;

import com.github.epsilon.graphics.renderers.RectRenderer;
import com.github.epsilon.graphics.renderers.RoundRectRenderer;
import com.github.epsilon.graphics.renderers.TextRenderer;
import com.github.epsilon.gui.panel.PanelLayout;
import com.github.epsilon.gui.panel.MD3Theme;
import com.github.epsilon.gui.panel.component.SettingRow;
import com.github.epsilon.settings.impl.StringSetting;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class StringSettingRow extends SettingRow<StringSetting> {

    public StringSettingRow(StringSetting setting) {
        super(setting);
    }

    @Override
    public void render(GuiGraphicsExtractor GuiGraphicsExtractor, RoundRectRenderer roundRectRenderer, RectRenderer rectRenderer, TextRenderer textRenderer, PanelLayout.Rect bounds, float hoverProgress, int mouseX, int mouseY, float partialTick) {
        float labelScale = 0.68f;
        float valueScale = 0.60f;
        float labelY = bounds.y() + (bounds.height() - textRenderer.getHeight(labelScale)) / 2.0f - 1.0f;
        float valueY = bounds.y() + (bounds.height() - textRenderer.getHeight(valueScale)) / 2.0f - 1.0f;
        roundRectRenderer.addRoundRect(bounds.x(), bounds.y(), bounds.width(), bounds.height(), MD3Theme.CARD_RADIUS, MD3Theme.lerp(MD3Theme.SURFACE_CONTAINER, MD3Theme.SURFACE_CONTAINER_HIGH, hoverProgress));
        textRenderer.addText(setting.getDisplayName(), bounds.x() + MD3Theme.ROW_CONTENT_INSET, labelY, labelScale, MD3Theme.TEXT_PRIMARY);
        String value = setting.getValue();
        String shown = value == null ? "" : value;
        if (shown.length() > 22) {
            shown = shown.substring(0, 19) + "...";
        }
        textRenderer.addText(shown, bounds.right() - MD3Theme.ROW_TRAILING_INSET - 92.0f, valueY, valueScale, MD3Theme.TEXT_SECONDARY);
    }
}
