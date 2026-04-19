package com.github.epsilon.gui.panel.popup;

import com.github.epsilon.graphics.renderers.RoundRectRenderer;
import com.github.epsilon.graphics.renderers.ShadowRenderer;
import com.github.epsilon.graphics.renderers.TextRenderer;
import com.github.epsilon.graphics.text.StaticFontLoader;
import com.github.epsilon.gui.panel.MD3Theme;
import com.github.epsilon.gui.panel.PanelLayout;
import com.github.epsilon.gui.panel.util.ScrollBarUtil;
import com.github.epsilon.managers.RenderManager;
import com.github.epsilon.settings.impl.EnumSetting;
import com.github.epsilon.utils.render.animation.Animation;
import com.github.epsilon.utils.render.animation.Easing;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;

import java.awt.*;

public class EnumSelectPopup implements PanelPopupHost.Popup {

    public static final int MAX_VISIBLE_ITEMS = 5;
    private static final float ITEM_HEIGHT = 24.0f;
    private static final float ITEM_INNER_HEIGHT = 22.0f;
    private static final float CONTENT_PADDING = 6.0f;

    private final PanelLayout.Rect bounds;
    private final EnumSetting<?> setting;
    private final PanelLayout.Rect anchorBounds;
    private final boolean scrollable;
    private final float maxScroll;
    private float scroll;

    private final RoundRectRenderer roundRectRenderer = RoundRectRenderer.create();
    private final ShadowRenderer shadowRenderer = ShadowRenderer.create();
    private final TextRenderer textRenderer = TextRenderer.create();
    private final RoundRectRenderer itemRoundRectRenderer = RoundRectRenderer.create();
    private final TextRenderer itemTextRenderer = TextRenderer.create();
    private final Animation openAnimation = new Animation(Easing.EASE_OUT_CUBIC, 140L);
    private int hoveredIndex = -1;

    public EnumSelectPopup(PanelLayout.Rect bounds, PanelLayout.Rect anchorBounds, EnumSetting<?> setting) {
        this.anchorBounds = anchorBounds;
        this.setting = setting;

        int optionCount = setting.getModes().length;
        this.scrollable = optionCount > MAX_VISIBLE_ITEMS;
        if (scrollable) {
            float cappedHeight = MAX_VISIBLE_ITEMS * ITEM_HEIGHT + CONTENT_PADDING * 2;
            this.bounds = new PanelLayout.Rect(bounds.x(), bounds.y(), bounds.width(), cappedHeight);
        } else {
            this.bounds = bounds;
        }

        float fullContentHeight = optionCount * ITEM_HEIGHT;
        float viewportHeight = this.bounds.height() - CONTENT_PADDING * 2;
        this.maxScroll = Math.max(0, fullContentHeight - viewportHeight);

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

        // Background & shadow (no scissor)
        shadowRenderer.addShadow(bounds.x(), popupY, bounds.width(), bounds.height(), MD3Theme.CARD_RADIUS, POPUP_SHADOW_RADIUS, MD3Theme.withAlpha(MD3Theme.SHADOW, (int) (120 * progress)));
        roundRectRenderer.addRoundRect(bounds.x(), popupY, bounds.width(), bounds.height(), MD3Theme.CARD_RADIUS, MD3Theme.withAlpha(MD3Theme.SURFACE_CONTAINER_LOW, alpha));
        roundRectRenderer.addRoundRect(anchorBounds.x(), anchorBounds.y(), anchorBounds.width(), anchorBounds.height(), MD3Theme.CARD_RADIUS, MD3Theme.withAlpha(MD3Theme.SECONDARY_CONTAINER, (int) (130 * progress)));

        // Scrollbar (rendered with background, before items — no overlap since items are narrower)
        if (scrollable) {
            float viewportHeight = bounds.height() - CONTENT_PADDING * 2;
            float fullContentHeight = setting.getModes().length * ITEM_HEIGHT;
            PanelLayout.Rect scrollViewport = new PanelLayout.Rect(bounds.x() + CONTENT_PADDING, popupY + CONTENT_PADDING, bounds.width() - CONTENT_PADDING * 2, viewportHeight);
            ScrollBarUtil.draw(roundRectRenderer, scrollViewport, scroll, maxScroll, fullContentHeight);
        }

        // Items
        Enum<?>[] modes = setting.getModes();
        float itemStartY = popupY + CONTENT_PADDING - scroll;
        float itemAreaWidth = bounds.width() - CONTENT_PADDING * 2 - (scrollable ? 6.0f : 0);
        hoveredIndex = -1;
        for (int i = 0; i < modes.length; i++) {
            float itemY = itemStartY + i * ITEM_HEIGHT;
            PanelLayout.Rect itemBounds = new PanelLayout.Rect(bounds.x() + CONTENT_PADDING, itemY, itemAreaWidth, ITEM_INNER_HEIGHT);
            boolean visible = itemY + ITEM_INNER_HEIGHT > popupY + CONTENT_PADDING && itemY < popupY + bounds.height() - CONTENT_PADDING;
            boolean hovered = visible && itemBounds.contains(mouseX, mouseY)
                    && mouseY >= popupY + CONTENT_PADDING && mouseY <= popupY + bounds.height() - CONTENT_PADDING;
            if (hovered) {
                hoveredIndex = i;
            }
            boolean selected = i == setting.getModeIndex();
            Color baseBackground = MD3Theme.withAlpha(MD3Theme.SURFACE_CONTAINER_HIGHEST, 0);
            Color hoverBackground = MD3Theme.lerp(MD3Theme.SURFACE_CONTAINER_HIGH, MD3Theme.SURFACE_CONTAINER_HIGHEST, 0.55f);
            Color selectedBackground = MD3Theme.SECONDARY_CONTAINER;
            Color background = selected ? selectedBackground : (hovered ? hoverBackground : baseBackground);
            Color textColor = selected ? MD3Theme.ON_SECONDARY_CONTAINER : (hovered ? MD3Theme.withAlpha(MD3Theme.TEXT_PRIMARY, 255) : MD3Theme.TEXT_SECONDARY);
            itemRoundRectRenderer.addRoundRect(itemBounds.x(), itemY, itemBounds.width(), itemBounds.height(), 8.0f, MD3Theme.withAlpha(background, alpha));
            if (selected) {
                itemTextRenderer.addText("V", itemBounds.x() + 8.0f, itemY + 6.5f, 0.72f, MD3Theme.withAlpha(MD3Theme.ON_SECONDARY_CONTAINER, alpha), StaticFontLoader.ICONS);
            }
            itemTextRenderer.addText(setting.getTranslatedValueByIndex(i), itemBounds.x() + (selected ? 22.0f : 10.0f), itemY + 7.0f, 0.62f, MD3Theme.withAlpha(textColor, alpha), StaticFontLoader.DUCKSANS);
        }

        // Apply scissor on item renderers if scrollable
        if (scrollable) {
            PanelLayout.Rect clipRect = new PanelLayout.Rect(bounds.x() + CONTENT_PADDING, popupY + CONTENT_PADDING, bounds.width() - CONTENT_PADDING * 2, bounds.height() - CONTENT_PADDING * 2);
            int guiHeight = GuiGraphicsExtractor.guiHeight();
            int scale = Minecraft.getInstance().getWindow().getGuiScale();
            int sx = Math.round(clipRect.x() * scale);
            int sy = Math.round((guiHeight - clipRect.bottom()) * scale);
            int sw = Math.round(clipRect.width() * scale);
            int sh = Math.round(clipRect.height() * scale);
            itemRoundRectRenderer.setScissor(sx, sy, sw, sh);
            itemTextRenderer.setScissor(sx, sy, sw, sh);
        }

        RenderManager.INSTANCE.applyRender(() -> {
            shadowRenderer.drawAndClear();
            roundRectRenderer.drawAndClear();
            itemRoundRectRenderer.drawAndClear();
            itemTextRenderer.drawAndClear();
            if (scrollable) {
                itemRoundRectRenderer.clearScissor();
                itemTextRenderer.clearScissor();
            }
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
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!scrollable) {
            return false;
        }
        scroll = Math.max(0, Math.min(maxScroll, scroll - (float) scrollY * 20.0f));
        return true;
    }

    @Override
    public boolean shouldCloseAfterClick() {
        return true;
    }

}
