package com.github.epsilon.gui.panel.popup;

import com.github.epsilon.graphics.renderers.RoundRectRenderer;
import com.github.epsilon.graphics.renderers.ShadowRenderer;
import com.github.epsilon.graphics.renderers.TextRenderer;
import com.github.epsilon.graphics.text.StaticFontLoader;
import com.github.epsilon.gui.panel.MD3Theme;
import com.github.epsilon.gui.panel.PanelLayout;
import com.github.epsilon.managers.RenderManager;
import com.github.epsilon.settings.impl.EnumSetting;
import com.github.epsilon.utils.render.animation.Animation;
import com.github.epsilon.utils.render.animation.Easing;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;

import java.awt.*;

public class EnumSelectPopup implements PanelPopupHost.Popup {

    private final PanelLayout.Rect bounds;
    private final EnumSetting<?> setting;
    private final PanelLayout.Rect anchorBounds;
    private final RoundRectRenderer roundRectRenderer = new RoundRectRenderer();
    private final ShadowRenderer shadowRenderer = new ShadowRenderer();
    private final TextRenderer textRenderer = new TextRenderer();
    private final Animation openAnimation = new Animation(Easing.EASE_OUT_CUBIC, 140L);
    private int hoveredIndex = -1;

    public EnumSelectPopup(PanelLayout.Rect bounds, PanelLayout.Rect anchorBounds, EnumSetting<?> setting) {
        this.bounds = bounds;
        this.anchorBounds = anchorBounds;
        this.setting = setting;
        this.openAnimation.setStartValue(0.0f);
    }

    public EnumSetting<?> getSetting() {
        return setting;
    }

    @Override
    public PanelLayout.Rect getBounds() {
        return bounds;
    }

    @Override
    public void extractGui(GuiGraphicsExtractor GuiGraphicsExtractor, int mouseX, int mouseY, float partialTick) {
        openAnimation.run(1.0f);
        float progress = openAnimation.getValue();
        float popupY = bounds.y() - (1.0f - progress) * 6.0f;
        int alpha = (int) (245 * progress);

        shadowRenderer.addShadow(bounds.x(), popupY, bounds.width(), bounds.height(), MD3Theme.CARD_RADIUS, POPUP_SHADOW_RADIUS, MD3Theme.withAlpha(MD3Theme.SHADOW, (int) (120 * progress)));
        roundRectRenderer.addRoundRect(bounds.x(), popupY, bounds.width(), bounds.height(), MD3Theme.CARD_RADIUS, MD3Theme.withAlpha(MD3Theme.SURFACE_CONTAINER_LOW, alpha));
        roundRectRenderer.addRoundRect(anchorBounds.x(), anchorBounds.y(), anchorBounds.width(), anchorBounds.height(), MD3Theme.CARD_RADIUS, MD3Theme.withAlpha(MD3Theme.SECONDARY_CONTAINER, (int) (130 * progress)));

        Enum<?>[] modes = setting.getModes();
        float itemY = popupY + 6.0f;
        hoveredIndex = -1;
        for (int i = 0; i < modes.length; i++) {
            PanelLayout.Rect itemBounds = new PanelLayout.Rect(bounds.x() + 6.0f, itemY, bounds.width() - 12.0f, 22.0f);
            boolean hovered = itemBounds.contains(mouseX, mouseY);
            if (hovered) {
                hoveredIndex = i;
            }
            boolean selected = i == setting.getModeIndex();
            Color baseBackground = MD3Theme.withAlpha(MD3Theme.SURFACE_CONTAINER_HIGHEST, 0);
            Color hoverBackground = MD3Theme.lerp(MD3Theme.SURFACE_CONTAINER_HIGH, MD3Theme.SURFACE_CONTAINER_HIGHEST, 0.55f);
            Color selectedBackground = MD3Theme.SECONDARY_CONTAINER;
            Color background = selected ? selectedBackground : hovered ? hoverBackground : baseBackground;
            Color textColor = selected ? MD3Theme.ON_SECONDARY_CONTAINER : hovered ? MD3Theme.withAlpha(MD3Theme.TEXT_PRIMARY, 255) : MD3Theme.TEXT_SECONDARY;
            roundRectRenderer.addRoundRect(itemBounds.x(), itemY, itemBounds.width(), itemBounds.height(), 8.0f, MD3Theme.withAlpha(background, alpha));
            if (selected) {
                textRenderer.addText("V", itemBounds.x() + 8.0f, itemY + 6.5f, 0.72f, MD3Theme.withAlpha(MD3Theme.ON_SECONDARY_CONTAINER, alpha), StaticFontLoader.ICONS);
            }
            textRenderer.addText(setting.getTranslatedValueByIndex(i), itemBounds.x() + (selected ? 22.0f : 10.0f), itemY + 7.0f, 0.62f, MD3Theme.withAlpha(textColor, alpha), StaticFontLoader.DUCKSANS);
            itemY += 24.0f;
        }

        RenderManager.INSTANCE.applyRenderAfterFrame(() -> {
            shadowRenderer.drawAndClear();
            roundRectRenderer.drawAndClear();
            textRenderer.drawAndClear();
        });
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
        if (!bounds.contains(event.x(), event.y()) || event.button() != 0) {
            return false;
        }
        Enum[] modes = setting.getModes();
        if (hoveredIndex < 0 || hoveredIndex >= modes.length) {
            return false;
        }
        ((EnumSetting) setting).setMode(modes[hoveredIndex]);
        return true;
    }

    @Override
    public boolean shouldCloseAfterClick() {
        return true;
    }

}
