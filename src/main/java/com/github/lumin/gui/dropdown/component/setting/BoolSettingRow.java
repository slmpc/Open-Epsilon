package com.github.lumin.gui.dropdown.component.setting;

import com.github.lumin.graphics.renderers.RectRenderer;
import com.github.lumin.graphics.renderers.RoundRectRenderer;
import com.github.lumin.graphics.renderers.TextRenderer;
import com.github.lumin.gui.dropdown.DropdownLayout;
import com.github.lumin.gui.dropdown.DropdownTheme;
import com.github.lumin.gui.dropdown.component.SettingRow;
import com.github.lumin.settings.impl.BoolSetting;
import com.github.lumin.utils.render.animation.Animation;
import com.github.lumin.utils.render.animation.Easing;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.MouseButtonEvent;

import java.awt.*;

public class BoolSettingRow extends SettingRow<BoolSetting> {

    private final Animation hoverAnimation = new Animation(Easing.EASE_OUT_CUBIC, 160L);
    private final Animation toggleAnimation = new Animation(Easing.DYNAMIC_ISLAND, 220L);
    private final Animation handleSizeAnimation = new Animation(Easing.EASE_OUT_CUBIC, 140L);

    public BoolSettingRow(BoolSetting setting) {
        super(setting);
        hoverAnimation.setStartValue(0.0f);
        toggleAnimation.setStartValue(setting.getValue() ? 1.0f : 0.0f);
        handleSizeAnimation.setStartValue(setting.getValue() ? 11.0f : 8.0f);
    }

    @Override
    public void render(GuiGraphics guiGraphics, RoundRectRenderer roundRectRenderer, RectRenderer rectRenderer, TextRenderer textRenderer, DropdownLayout.Rect bounds, float hoverProgress, int mouseX, int mouseY, float partialTick) {
        float labelScale = 0.68f;
        float labelY = bounds.y() + (bounds.height() - textRenderer.getHeight(labelScale)) / 2.0f - 1.0f;
        hoverAnimation.run(hoverProgress);
        toggleAnimation.run(setting.getValue() ? 1.0f : 0.0f);
        handleSizeAnimation.run(setting.getValue() ? 11.0f : 8.0f);

        float animatedHover = hoverAnimation.getValue();
        float toggleProgress = toggleAnimation.getValue();
        float handleSize = handleSizeAnimation.getValue();

        roundRectRenderer.addRoundRect(bounds.x(), bounds.y(), bounds.width(), bounds.height(), DropdownTheme.CARD_RADIUS, DropdownTheme.lerp(DropdownTheme.SURFACE_CONTAINER, DropdownTheme.SURFACE_CONTAINER_HIGH, animatedHover));
        textRenderer.addText(setting.getDisplayName(), bounds.x() + DropdownTheme.ROW_CONTENT_INSET, labelY, labelScale, DropdownTheme.TEXT_PRIMARY);

        DropdownLayout.Rect switchBounds = getSwitchBounds(bounds);
        Color track = DropdownTheme.lerp(DropdownTheme.SURFACE_CONTAINER_HIGHEST, DropdownTheme.PRIMARY, toggleProgress);
        Color handleColor = DropdownTheme.lerp(DropdownTheme.OUTLINE, DropdownTheme.ON_PRIMARY, toggleProgress);
        roundRectRenderer.addRoundRect(switchBounds.x(), switchBounds.y(), switchBounds.width(), switchBounds.height(), switchBounds.height() / 2.0f, track);

        float handleTravel = switchBounds.width() - 10.0f - handleSize;
        float handleX = switchBounds.x() + 5.0f + handleTravel * toggleProgress;
        float handleY = switchBounds.centerY() - handleSize / 2.0f;
        roundRectRenderer.addRoundRect(handleX, handleY, handleSize, handleSize, handleSize / 2.0f, handleColor);

        if (animatedHover > 0.02f) {
            float haloSize = 16.0f;
            float haloX = handleX + handleSize / 2.0f - haloSize / 2.0f;
            float haloY = switchBounds.centerY() - haloSize / 2.0f;
            roundRectRenderer.addRoundRect(haloX, haloY, haloSize, haloSize, haloSize / 2.0f, DropdownTheme.withAlpha(DropdownTheme.TEXT_PRIMARY, (int) (18 * animatedHover)));
        }
    }

    private DropdownLayout.Rect getSwitchBounds(DropdownLayout.Rect bounds) {
        return new DropdownLayout.Rect(bounds.right() - DropdownTheme.ROW_TRAILING_INSET - 30.0f, bounds.y() + 6.0f, 30.0f, 16.0f);
    }

    @Override
    public boolean mouseClicked(DropdownLayout.Rect bounds, MouseButtonEvent event, boolean isDoubleClick) {
        if (!bounds.contains(event.x(), event.y()) || event.button() != 0) {
            return false;
        }
        setting.setValue(!setting.getValue());
        return true;
    }
}
