package com.github.epsilon.gui.panel.panel;

import com.github.epsilon.Epsilon;
import com.github.epsilon.assets.i18n.EpsilonTranslateComponent;
import com.github.epsilon.assets.i18n.TranslateComponent;
import com.github.epsilon.graphics.renderers.RectRenderer;
import com.github.epsilon.graphics.renderers.RoundRectRenderer;
import com.github.epsilon.graphics.renderers.TextRenderer;
import com.github.epsilon.graphics.text.StaticFontLoader;
import com.github.epsilon.gui.panel.MD3Theme;
import com.github.epsilon.gui.panel.PanelLayout;
import com.github.epsilon.gui.panel.PanelState;
import com.github.epsilon.managers.ModuleManager;
import com.github.epsilon.modules.Category;
import com.github.epsilon.utils.render.animation.Animation;
import com.github.epsilon.utils.render.animation.Easing;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;

import java.awt.*;

public class CategoryRailPanel {

    private static final float CATEGORY_ITEM_HEIGHT = 34.0f;
    private static final float CATEGORY_ITEM_SPACING = 38.0f;
    private static final float CATEGORY_START_Y = 40.0f;
    private static final float RAIL_ICON_CENTER_X_OFFSET = 2.0f;
    private static final String SETTINGS_ICON = "7";

    protected final PanelState state;
    private final RectRenderer rectRenderer;
    private final RoundRectRenderer roundRectRenderer;
    private final TextRenderer textRenderer;
    private final TextRenderer clippedTextRenderer = TextRenderer.create();
    private final Animation expandAnimation = new Animation(Easing.EASE_OUT_CUBIC, 240L);
    private final Animation contentAnimation = new Animation(Easing.EASE_OUT_CUBIC, 180L);
    private final Animation menuHoverAnimation = new Animation(Easing.EASE_OUT_CUBIC, 120L);
    private final Animation headerTitleAnimation = new Animation(Easing.EASE_OUT_CUBIC, 220L);
    private final Animation headerSubtitleAnimation = new Animation(Easing.EASE_OUT_CUBIC, 260L);
    private final Animation headerDividerAnimation = new Animation(Easing.EASE_OUT_CUBIC, 220L);
    private final Animation selectionYAnimation = new Animation(Easing.EASE_OUT_CUBIC, 180L);
    private final Animation selectionHeightAnimation = new Animation(Easing.EASE_OUT_CUBIC, 180L);
    private final Animation hoverYAnimation = new Animation(Easing.EASE_OUT_CUBIC, 160L);
    private final Animation hoverAlphaAnimation = new Animation(Easing.EASE_OUT_CUBIC, 100L);
    private final Animation settingsHoverAnimation = new Animation(Easing.EASE_OUT_CUBIC, 120L);
    private PanelLayout.Rect bounds;
    private boolean clippedTextPending;

    private static final TranslateComponent settingsLabelComponent = EpsilonTranslateComponent.create("gui", "clientsettings");

    public CategoryRailPanel(PanelState state, RectRenderer rectRenderer, RoundRectRenderer roundRectRenderer, TextRenderer textRenderer) {
        this.state = state;
        this.rectRenderer = rectRenderer;
        this.roundRectRenderer = roundRectRenderer;
        this.textRenderer = textRenderer;
        this.expandAnimation.setStartValue(MD3Theme.RAIL_COLLAPSED_WIDTH);
        this.contentAnimation.setStartValue(0.0f);
        this.menuHoverAnimation.setStartValue(0.0f);
        this.headerTitleAnimation.setStartValue(0.0f);
        this.headerSubtitleAnimation.setStartValue(0.0f);
        this.headerDividerAnimation.setStartValue(0.0f);
        this.selectionYAnimation.setStartValue(0.0f);
        this.selectionHeightAnimation.setStartValue(32.0f);
        this.hoverYAnimation.setStartValue(0.0f);
        this.hoverAlphaAnimation.setStartValue(0.0f);
        this.settingsHoverAnimation.setStartValue(0.0f);
    }

    public float getAnimatedWidth() {
        expandAnimation.run(state.isSidebarExpanded() ? MD3Theme.RAIL_EXPANDED_WIDTH : MD3Theme.RAIL_COLLAPSED_WIDTH);
        return expandAnimation.getValue();
    }

    public void render(GuiGraphicsExtractor GuiGraphicsExtractor, PanelLayout.Rect bounds, int mouseX, int mouseY, float partialTick) {
        this.bounds = bounds;
        applyTextScissor(bounds, GuiGraphicsExtractor.guiHeight());
        contentAnimation.run(state.isSidebarExpanded() ? 1.0f : 0.0f);
        float contentProgress = contentAnimation.getValue();
        headerTitleAnimation.run(contentProgress);
        headerSubtitleAnimation.run(contentProgress > 0.08f ? 1.0f : 0.0f);
        headerDividerAnimation.run(contentProgress > 0.12f ? 1.0f : 0.0f);
        float titleScale = 0.78f;
        float subtitleScale = 0.52f;
        float itemIconScale = 1.02f;
        float itemLabelScale = 0.62f;
        float itemCountScale = 0.58f;

        PanelLayout.Rect menuButton = getMenuButtonBounds();
        menuHoverAnimation.run(mouseOver(menuButton, mouseX, mouseY) ? 1.0f : 0.0f);
        Color buttonColor = MD3Theme.lerp(MD3Theme.withAlpha(MD3Theme.SURFACE_CONTAINER, 0), MD3Theme.SURFACE_CONTAINER_HIGH, menuHoverAnimation.getValue());
        roundRectRenderer.addRoundRect(menuButton.x(), menuButton.y(), menuButton.width(), menuButton.height(), 12.0f, buttonColor);
        drawMenuGlyph(menuButton);

        float titleProgress = headerTitleAnimation.getValue();
        float subtitleProgress = headerSubtitleAnimation.getValue();
        float dividerProgress = headerDividerAnimation.getValue();
        float categoryStartY = getCategoryStartY(bounds);
        if (titleProgress > 0.02f) {
            Color brandColor = MD3Theme.withAlpha(MD3Theme.TEXT_PRIMARY, (int) (255 * titleProgress));
            Color subColor = MD3Theme.withAlpha(MD3Theme.TEXT_SECONDARY, (int) (210 * subtitleProgress));
            float titleY = bounds.y() + 7.0f;
            float titleHeight = clippedTextRenderer.getHeight(titleScale, StaticFontLoader.DUCKSANS);
            float pad = 3.0f;
            float subtitleY = titleY + titleHeight + pad;
            float titleOffset = (1.0f - titleProgress) * 8.0f;
            float subtitleOffset = (1.0f - subtitleProgress) * 10.0f;
            clippedTextRenderer.addText("Epsilon " + Epsilon.VERSION, bounds.x() + 38.0f + titleOffset, titleY, titleScale, brandColor, StaticFontLoader.DUCKSANS);
            if (subtitleProgress > 0.02f) {
                clippedTextRenderer.addText("Open Source", bounds.x() + 38.0f + subtitleOffset, subtitleY, subtitleScale, subColor);
            }
            if (dividerProgress > 0.02f) {
                float dividerY = subtitleY + clippedTextRenderer.getHeight(subtitleScale) + 4.0f;
                float dividerBaseX = bounds.x() + 7.0f;
                float dividerTargetWidth = bounds.width() - 14.0f;
                float dividerWidth = dividerTargetWidth * dividerProgress;
                float dividerX = dividerBaseX + (1.0f - dividerProgress) * 6.0f;
                int dividerAlpha = (int) (120 * dividerProgress);
                rectRenderer.addRect(dividerX, dividerY, dividerWidth, 1.0f, MD3Theme.withAlpha(MD3Theme.OUTLINE_SOFT, dividerAlpha));
                rectRenderer.addRect(dividerX, dividerY, Math.min(18.0f, dividerWidth), 1.0f, MD3Theme.withAlpha(MD3Theme.TEXT_SECONDARY, (int) (52 * dividerProgress)));
            }
        }

        float selectedItemY = categoryStartY;
        float selectedItemHeight = CATEGORY_ITEM_HEIGHT;
        if (state.isClientSettingMode()) {
            selectedItemY = getSettingsButtonY();
        } else {
            float lookupY = categoryStartY;
            for (Category category : Category.values()) {
                if (state.getSelectedCategory() == category) {
                    selectedItemY = lookupY;
                    break;
                }
                lookupY += CATEGORY_ITEM_SPACING;
            }
        }

        float hoveredY = -1.0f;
        float scanY = categoryStartY;
        for (Category category : Category.values()) {
            PanelLayout.Rect scanRect = new PanelLayout.Rect(bounds.x() + 5.0f, scanY, bounds.width() - 10.0f, CATEGORY_ITEM_HEIGHT);
            if (scanRect.contains(mouseX, mouseY)) {
                hoveredY = scanY;
                break;
            }
            scanY += CATEGORY_ITEM_SPACING;
        }
        if (hoveredY < 0) {
            PanelLayout.Rect settingsScanRect = new PanelLayout.Rect(bounds.x() + 5.0f, getSettingsButtonY(), bounds.width() - 10.0f, CATEGORY_ITEM_HEIGHT);
            if (settingsScanRect.contains(mouseX, mouseY)) hoveredY = getSettingsButtonY();
        }
        hoverAlphaAnimation.run(hoveredY >= 0 ? 1.0f : 0.0f);
        if (hoveredY >= 0) hoverYAnimation.run(hoveredY);
        float hoverAlpha = hoverAlphaAnimation.getValue();
        if (hoverAlpha > 0.01f) {
            roundRectRenderer.addRoundRect(bounds.x() + 5.0f, hoverYAnimation.getValue(), bounds.width() - 10.0f, CATEGORY_ITEM_HEIGHT, MD3Theme.CARD_RADIUS,
                    MD3Theme.withAlpha(MD3Theme.SURFACE_CONTAINER_HIGH, (int) (200 * hoverAlpha)));
        }

        selectionYAnimation.run(selectedItemY);
        selectionHeightAnimation.run(selectedItemHeight);
        float animatedSelectionY = selectionYAnimation.getValue();
        float animatedSelectionHeight = selectionHeightAnimation.getValue();
        roundRectRenderer.addRoundRect(bounds.x() + 5.0f, animatedSelectionY, bounds.width() - 10.0f, animatedSelectionHeight, MD3Theme.CARD_RADIUS, MD3Theme.SECONDARY_CONTAINER);

        float itemY = categoryStartY;
        for (Category category : Category.values()) {
            float itemHeight = CATEGORY_ITEM_HEIGHT;
            PanelLayout.Rect itemRect = new PanelLayout.Rect(bounds.x() + 5.0f, itemY, bounds.width() - 10.0f, itemHeight);
            boolean hovered = itemRect.contains(mouseX, mouseY);
            boolean selected = !state.isClientSettingMode() && state.getSelectedCategory() == category;

            Color background = selected ? MD3Theme.withAlpha(MD3Theme.SECONDARY_CONTAINER, 0) :
                    (hovered ? MD3Theme.SURFACE_CONTAINER : MD3Theme.withAlpha(MD3Theme.SURFACE_CONTAINER, 0));
            Color iconColor = selected ? MD3Theme.ON_SECONDARY_CONTAINER : (hovered ? MD3Theme.TEXT_PRIMARY : MD3Theme.TEXT_SECONDARY);
            Color labelColor = selected ? MD3Theme.ON_SECONDARY_CONTAINER : MD3Theme.TEXT_PRIMARY;
            Color countColor = selected ? MD3Theme.ON_SECONDARY_CONTAINER : MD3Theme.TEXT_SECONDARY;
            float iconHeight = clippedTextRenderer.getHeight(itemIconScale, StaticFontLoader.ICONS);
            float labelHeight = clippedTextRenderer.getHeight(itemLabelScale, StaticFontLoader.DUCKSANS);
            float countHeight = clippedTextRenderer.getHeight(itemCountScale);
            float iconY = itemRect.y() + (itemRect.height() - iconHeight) / 2.0f - 2.0f;
            float labelY = itemRect.y() + (itemRect.height() - labelHeight) / 2.0f - 1.0f;
            float countY = itemRect.y() + (itemRect.height() - countHeight) / 2.0f - 1.0f;

            roundRectRenderer.addRoundRect(itemRect.x(), itemRect.y(), itemRect.width(), itemRect.height(), MD3Theme.CARD_RADIUS, background);
            float iconWidth = clippedTextRenderer.getWidth(category.icon, itemIconScale, StaticFontLoader.ICONS);
            float iconX = getRailIconCenterX(menuButton) - iconWidth / 2.0f;
            clippedTextRenderer.addText(category.icon, iconX, iconY, itemIconScale, iconColor, StaticFontLoader.ICONS);

            if (contentProgress > 0.02f) {
                int count = getCategoryCount(category);
                float textOffset = (1.0f - contentProgress) * 5.0f;
                Color animatedLabel = MD3Theme.withAlpha(labelColor, (int) (255 * contentProgress));
                Color animatedCount = MD3Theme.withAlpha(countColor, (int) (220 * contentProgress));
                clippedTextRenderer.addText(category.getName(), itemRect.x() + 30.0f + textOffset, labelY, itemLabelScale, animatedLabel, StaticFontLoader.DUCKSANS);
                float countWidth = clippedTextRenderer.getWidth(Integer.toString(count), itemCountScale);
                clippedTextRenderer.addText(Integer.toString(count), itemRect.right() - 12.0f - countWidth, countY, itemCountScale, animatedCount);
            }

            itemY += CATEGORY_ITEM_SPACING;
        }

        // Settings button at the bottom
        float settingsBtnY = getSettingsButtonY();
        PanelLayout.Rect settingsRect = new PanelLayout.Rect(bounds.x() + 5.0f, settingsBtnY, bounds.width() - 10.0f, CATEGORY_ITEM_HEIGHT);
        boolean settingsHovered = settingsRect.contains(mouseX, mouseY);
        boolean settingsSelected = state.isClientSettingMode();
        settingsHoverAnimation.run(settingsHovered ? 1.0f : 0.0f);

        Color settingsBg = settingsSelected ? MD3Theme.withAlpha(MD3Theme.SECONDARY_CONTAINER, 0) : (settingsHovered ? MD3Theme.SURFACE_CONTAINER : MD3Theme.withAlpha(MD3Theme.SURFACE_CONTAINER, 0));
        Color settingsIconColor = settingsSelected ? MD3Theme.ON_SECONDARY_CONTAINER : (settingsHovered ? MD3Theme.TEXT_PRIMARY : MD3Theme.TEXT_SECONDARY);
        Color settingsLabelColor = settingsSelected ? MD3Theme.ON_SECONDARY_CONTAINER : MD3Theme.TEXT_PRIMARY;
        roundRectRenderer.addRoundRect(settingsRect.x(), settingsRect.y(), settingsRect.width(), settingsRect.height(), MD3Theme.CARD_RADIUS, settingsBg);
        float settingsIconWidth = clippedTextRenderer.getWidth(SETTINGS_ICON, itemIconScale, StaticFontLoader.ICONS);
        float settingsIconX = getRailIconCenterX(menuButton) - settingsIconWidth / 2.0f;
        float settingsIconHeight = clippedTextRenderer.getHeight(itemIconScale, StaticFontLoader.ICONS);
        float settingsIconY = settingsRect.y() + (settingsRect.height() - settingsIconHeight) / 2.0f - 2f;
        clippedTextRenderer.addText(SETTINGS_ICON, settingsIconX, settingsIconY, itemIconScale, settingsIconColor, StaticFontLoader.ICONS);

        if (contentProgress > 0.02f) {
            float textOffset = (1.0f - contentProgress) * 5.0f;
            Color animatedLabel = MD3Theme.withAlpha(settingsLabelColor, (int) (255 * contentProgress));
            float settingsLabelHeight = clippedTextRenderer.getHeight(itemLabelScale, StaticFontLoader.DUCKSANS);
            float settingsLabelY = settingsRect.y() + (settingsRect.height() - settingsLabelHeight) / 2.0f - 1.0f;
            clippedTextRenderer.addText(settingsLabelComponent.getTranslatedName(), settingsRect.x() + 30.0f + textOffset, settingsLabelY, itemLabelScale, animatedLabel, StaticFontLoader.DUCKSANS);
        }

        clippedTextPending = true;
    }

    public void flushClippedText() {
        if (!clippedTextPending) {
            return;
        }
        clippedTextRenderer.drawAndClear();
        clearTextScissor();
        clippedTextPending = false;
    }

    public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
        if (bounds == null || event.button() != 0) {
            return false;
        }
        if (getMenuButtonBounds().contains(event.x(), event.y())) {
            state.toggleSidebarExpanded();
            return true;
        }

        float itemY = getCategoryStartY(bounds);
        for (Category category : Category.values()) {
            PanelLayout.Rect itemRect = new PanelLayout.Rect(bounds.x() + 5.0f, itemY, bounds.width() - 10.0f, CATEGORY_ITEM_HEIGHT);
            if (itemRect.contains(event.x(), event.y())) {
                state.setClientSettingMode(false);
                state.setSelectedCategory(category);
                return true;
            }
            itemY += CATEGORY_ITEM_SPACING;
        }

        PanelLayout.Rect settingsRect = new PanelLayout.Rect(bounds.x() + 5.0f, getSettingsButtonY(), bounds.width() - 10.0f, CATEGORY_ITEM_HEIGHT);
        if (settingsRect.contains(event.x(), event.y())) {
            state.setClientSettingMode(true);
            return true;
        }

        return false;
    }

    private PanelLayout.Rect getMenuButtonBounds() {
        return new PanelLayout.Rect(bounds.x() + 4.0f + RAIL_ICON_CENTER_X_OFFSET, bounds.y() + 4.0f, 28.0f, 28.0f);
    }

    public boolean hasActiveAnimations() {
        return !expandAnimation.isFinished()
                || !contentAnimation.isFinished()
                || !menuHoverAnimation.isFinished()
                || !headerTitleAnimation.isFinished()
                || !headerSubtitleAnimation.isFinished()
                || !headerDividerAnimation.isFinished()
                || !selectionYAnimation.isFinished()
                || !selectionHeightAnimation.isFinished()
                || !hoverYAnimation.isFinished()
                || !hoverAlphaAnimation.isFinished()
                || !settingsHoverAnimation.isFinished();
    }

    private boolean mouseOver(PanelLayout.Rect rect, int mouseX, int mouseY) {
        return rect.contains(mouseX, mouseY);
    }

    private float getCategoryStartY(PanelLayout.Rect bounds) {
        // Keep category list vertically stable regardless of sidebar expansion progress.
        return bounds.y() + CATEGORY_START_Y;
    }

    private float getSettingsButtonY() {
        return bounds.bottom() - CATEGORY_ITEM_HEIGHT - 5.0f;
    }

    private void drawMenuGlyph(PanelLayout.Rect button) {
        Color lineColor = MD3Theme.TEXT_PRIMARY;
        float glyphWidth = 12.0f;
        float glyphHeight = 10.0f;
        float x = getRailIconCenterX(button) - glyphWidth / 2.0f;
        float y = button.y() + (button.height() - glyphHeight) / 2.0f;
        rectRenderer.addRect(x, y, 12.0f, 1.6f, lineColor);
        rectRenderer.addRect(x, y + 4.0f, 12.0f, 1.6f, lineColor);
        rectRenderer.addRect(x, y + 8.0f, 12.0f, 1.6f, lineColor);
    }

    private float getRailIconCenterX(PanelLayout.Rect railButton) {
        return railButton.x() + railButton.width() / 2.0f;
    }

    private int getCategoryCount(Category category) {
        return (int) ModuleManager.INSTANCE.getModules().stream().filter(module -> module.category == category).count();
    }

    private void applyTextScissor(PanelLayout.Rect rect, int guiHeight) {
        int scale = Minecraft.getInstance().getWindow().getGuiScale();
        int x = Math.round(rect.x() * scale);
        int y = Math.round((guiHeight - rect.bottom()) * scale);
        int width = Math.round(rect.width() * scale);
        int height = Math.round(rect.height() * scale);
        clippedTextRenderer.setScissor(x, y, width, height);
    }

    private void clearTextScissor() {
        clippedTextRenderer.clearScissor();
    }

}
