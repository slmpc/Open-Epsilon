package com.github.epsilon.gui.panel.component.setting;

import com.github.epsilon.graphics.renderers.RectRenderer;
import com.github.epsilon.graphics.renderers.RoundRectRenderer;
import com.github.epsilon.graphics.renderers.TextRenderer;
import com.github.epsilon.gui.panel.MD3Theme;
import com.github.epsilon.gui.panel.PanelLayout;
import com.github.epsilon.gui.panel.component.SettingRow;
import com.github.epsilon.settings.impl.ButtonSetting;
import com.github.epsilon.utils.render.animation.Animation;
import com.github.epsilon.utils.render.animation.Easing;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;

public class ButtonSettingRow extends SettingRow<ButtonSetting> {

    private final Animation hoverAnimation = new Animation(Easing.EASE_OUT_CUBIC, 160L);

    public ButtonSettingRow(ButtonSetting setting) {
        super(setting);
        hoverAnimation.setStartValue(0.0f);
    }

    @Override
    public void render(GuiGraphicsExtractor GuiGraphicsExtractor, RoundRectRenderer roundRectRenderer, RectRenderer rectRenderer, TextRenderer textRenderer, PanelLayout.Rect bounds, float hoverProgress, int mouseX, int mouseY, float partialTick) {
        float labelScale = 0.68f;
        float labelY = bounds.y() + (bounds.height() - textRenderer.getHeight(labelScale)) / 2.0f - 1.0f;
        hoverAnimation.run(hoverProgress);

        float animatedHover = hoverAnimation.getValue();

        roundRectRenderer.addRoundRect(bounds.x(), bounds.y(), bounds.width(), bounds.height(), MD3Theme.CARD_RADIUS, MD3Theme.lerp(MD3Theme.SURFACE_CONTAINER, MD3Theme.SURFACE_CONTAINER_HIGH, animatedHover));
        textRenderer.addText(setting.getDisplayName(), bounds.x() + MD3Theme.ROW_CONTENT_INSET, labelY, labelScale, MD3Theme.TEXT_PRIMARY);
    }

    @Override
    public boolean mouseClicked(PanelLayout.Rect bounds, MouseButtonEvent event, boolean isDoubleClick) {
        if (!bounds.contains(event.x(), event.y()) || event.button() != 0) {
            return false;
        }
        setting.getValue().run();
        return true;
    }

    @Override
    public boolean hasActiveAnimation() {
        return !hoverAnimation.isFinished();
    }
}
