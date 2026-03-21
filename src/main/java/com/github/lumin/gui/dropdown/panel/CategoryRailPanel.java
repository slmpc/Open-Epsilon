package com.github.lumin.gui.dropdown.panel;

import com.github.lumin.graphics.renderers.RectRenderer;
import com.github.lumin.graphics.renderers.RoundRectRenderer;
import com.github.lumin.graphics.renderers.TextRenderer;
import com.github.lumin.graphics.text.StaticFontLoader;
import com.github.lumin.gui.dropdown.DropdownLayout;
import com.github.lumin.gui.dropdown.DropdownState;
import com.github.lumin.gui.dropdown.DropdownTheme;
import com.github.lumin.managers.ModuleManager;
import com.github.lumin.modules.Category;
import com.github.lumin.utils.render.animation.Animation;
import com.github.lumin.utils.render.animation.Easing;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.MouseButtonEvent;

import java.awt.*;

public class CategoryRailPanel {

    protected final DropdownState state;
    private final RectRenderer rectRenderer;
    private final RoundRectRenderer roundRectRenderer;
    private final TextRenderer textRenderer;
    private final Animation expandAnimation = new Animation(Easing.EASE_OUT_CUBIC, 240L);
    private DropdownLayout.Rect bounds;

    public CategoryRailPanel(DropdownState state, RectRenderer rectRenderer, RoundRectRenderer roundRectRenderer, TextRenderer textRenderer) {
        this.state = state;
        this.rectRenderer = rectRenderer;
        this.roundRectRenderer = roundRectRenderer;
        this.textRenderer = textRenderer;
        this.expandAnimation.setStartValue(DropdownTheme.RAIL_COLLAPSED_WIDTH);
    }

    public float getAnimatedWidth() {
        expandAnimation.run(state.isSidebarExpanded() ? DropdownTheme.RAIL_EXPANDED_WIDTH : DropdownTheme.RAIL_COLLAPSED_WIDTH);
        return expandAnimation.getValue();
    }

    public void render(GuiGraphics guiGraphics, DropdownLayout.Rect bounds, int mouseX, int mouseY, float partialTick) {
        this.bounds = bounds;
        float progress = Math.max(0.0f, Math.min(1.0f, (getAnimatedWidth() - DropdownTheme.RAIL_COLLAPSED_WIDTH) / (DropdownTheme.RAIL_EXPANDED_WIDTH - DropdownTheme.RAIL_COLLAPSED_WIDTH)));
        float titleScale = 0.78f;
        float subtitleScale = 0.52f;
        float itemIconScale = 0.78f;
        float itemLabelScale = 0.62f;
        float itemCountScale = 0.58f;

        DropdownLayout.Rect menuButton = getMenuButtonBounds();
        roundRectRenderer.addRoundRect(menuButton.x(), menuButton.y(), menuButton.width(), menuButton.height(), 12.0f, mouseOver(menuButton, mouseX, mouseY) ? DropdownTheme.SURFACE_CONTAINER : DropdownTheme.withAlpha(DropdownTheme.SURFACE_CONTAINER, 0));
        drawMenuGlyph(menuButton.x() + 8.0f, menuButton.y() + 9.0f);

        if (progress > 0.15f) {
            Color brandColor = DropdownTheme.withAlpha(DropdownTheme.TEXT_PRIMARY, (int) (255 * progress));
            Color subColor = DropdownTheme.withAlpha(DropdownTheme.TEXT_SECONDARY, (int) (210 * progress));
            float titleY = bounds.y() + 8.0f;
            float titleHeight = textRenderer.getHeight(titleScale, StaticFontLoader.DUCKSANS);
            float pad = 2.0f;
            float subtitleY = titleY + titleHeight + pad;
            textRenderer.addText("Lumin", bounds.x() + 38.0f, titleY, titleScale, brandColor, StaticFontLoader.DUCKSANS);
            textRenderer.addText("史莱姆嫖娼", bounds.x() + 38.0f, subtitleY, subtitleScale, subColor);
        }

        float itemY = bounds.y() + 40.0f;
        for (Category category : Category.values()) {
            float itemHeight = progress > 0.35f ? 34.0f : 32.0f;
            DropdownLayout.Rect itemRect = new DropdownLayout.Rect(bounds.x() + 5.0f, itemY, bounds.width() - 10.0f, itemHeight);
            boolean hovered = itemRect.contains(mouseX, mouseY);
            boolean selected = state.getSelectedCategory() == category;

            Color background = selected ? DropdownTheme.SECONDARY_CONTAINER : hovered ? DropdownTheme.SURFACE_CONTAINER : DropdownTheme.withAlpha(DropdownTheme.SURFACE_CONTAINER, 0);
            Color iconColor = selected ? DropdownTheme.ON_SECONDARY_CONTAINER : hovered ? DropdownTheme.TEXT_PRIMARY : DropdownTheme.TEXT_SECONDARY;
            Color labelColor = selected ? DropdownTheme.ON_SECONDARY_CONTAINER : DropdownTheme.TEXT_PRIMARY;
            Color countColor = selected ? DropdownTheme.ON_SECONDARY_CONTAINER : DropdownTheme.TEXT_SECONDARY;
            float iconWidth = textRenderer.getWidth(category.icon, itemIconScale, StaticFontLoader.ICONS);
            float iconHeight = textRenderer.getHeight(itemIconScale, StaticFontLoader.ICONS);
            float labelHeight = textRenderer.getHeight(itemLabelScale, StaticFontLoader.DUCKSANS);
            float countHeight = textRenderer.getHeight(itemCountScale);
            float iconX = progress > 0.2f ? itemRect.x() + 10.0f : itemRect.x() + (itemRect.width() - iconWidth) / 2.0f;
            float iconY = itemRect.y() + (itemRect.height() - iconHeight) / 2.0f;
            float labelY = itemRect.y() + (itemRect.height() - labelHeight) / 2.0f - 1.0f;
            float countY = itemRect.y() + (itemRect.height() - countHeight) / 2.0f - 1.0f;

            roundRectRenderer.addRoundRect(itemRect.x(), itemRect.y(), itemRect.width(), itemRect.height(), DropdownTheme.CARD_RADIUS, background);
            textRenderer.addText(category.icon, iconX, iconY, itemIconScale, iconColor, StaticFontLoader.ICONS);

            if (progress > 0.2f) {
                int count = getCategoryCount(category);
                Color animatedLabel = DropdownTheme.withAlpha(labelColor, (int) (255 * progress));
                Color animatedCount = DropdownTheme.withAlpha(countColor, (int) (220 * progress));
                textRenderer.addText(category.getName(), itemRect.x() + 30.0f, labelY, itemLabelScale, animatedLabel, StaticFontLoader.DUCKSANS);
                float countWidth = textRenderer.getWidth(Integer.toString(count), itemCountScale);
                textRenderer.addText(Integer.toString(count), itemRect.right() - 12.0f - countWidth, countY, itemCountScale, animatedCount);
            }

            itemY += progress > 0.35f ? 38.0f : 36.0f;
        }
    }

    public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
        if (bounds == null || event.button() != 0) {
            return false;
        }
        if (getMenuButtonBounds().contains(event.x(), event.y())) {
            state.toggleSidebarExpanded();
            return true;
        }

        float progress = Math.max(0.0f, Math.min(1.0f, (getAnimatedWidth() - DropdownTheme.RAIL_COLLAPSED_WIDTH) / (DropdownTheme.RAIL_EXPANDED_WIDTH - DropdownTheme.RAIL_COLLAPSED_WIDTH)));
        float itemY = bounds.y() + 40.0f;
        for (Category category : Category.values()) {
            DropdownLayout.Rect itemRect = new DropdownLayout.Rect(bounds.x() + 5.0f, itemY, bounds.width() - 10.0f, progress > 0.35f ? 34.0f : 32.0f);
            if (itemRect.contains(event.x(), event.y())) {
                state.setSelectedCategory(category);
                return true;
            }
            itemY += progress > 0.35f ? 38.0f : 36.0f;
        }
        return false;
    }

    private DropdownLayout.Rect getMenuButtonBounds() {
        return new DropdownLayout.Rect(bounds.x() + 4.0f, bounds.y() + 4.0f, bounds.width() - 8.0f, 28.0f);
    }

    private boolean mouseOver(DropdownLayout.Rect rect, int mouseX, int mouseY) {
        return rect.contains(mouseX, mouseY);
    }

    private void drawMenuGlyph(float x, float y) {
        Color lineColor = DropdownTheme.TEXT_PRIMARY;
        rectRenderer.addRect(x, y, 12.0f, 1.6f, lineColor);
        rectRenderer.addRect(x, y + 4.0f, 12.0f, 1.6f, lineColor);
        rectRenderer.addRect(x, y + 8.0f, 12.0f, 1.6f, lineColor);
    }

    private int getCategoryCount(Category category) {
        return (int) ModuleManager.INSTANCE.getModules().stream().filter(module -> module.category == category).count();
    }
}
