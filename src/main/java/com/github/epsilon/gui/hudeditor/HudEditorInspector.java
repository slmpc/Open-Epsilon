package com.github.epsilon.gui.hudeditor;

import com.github.epsilon.graphics.renderers.RectRenderer;
import com.github.epsilon.graphics.renderers.RoundRectRenderer;
import com.github.epsilon.graphics.renderers.ShadowRenderer;
import com.github.epsilon.graphics.renderers.TextRenderer;
import com.github.epsilon.graphics.text.StaticFontLoader;
import com.github.epsilon.gui.panel.MD3Theme;
import com.github.epsilon.gui.panel.PanelLayout;
import com.github.epsilon.gui.panel.adapter.SettingListController;
import com.github.epsilon.gui.panel.popup.PanelPopupHost;
import com.github.epsilon.gui.panel.util.PanelScissor;
import com.github.epsilon.gui.panel.util.ScrollBarUtil;
import com.github.epsilon.modules.HudModule;
import com.github.epsilon.settings.Setting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.util.Mth;

import java.util.List;

final class HudEditorInspector {

    private static final float PANEL_MARGIN = 10.0f;
    private static final float PANEL_WIDTH = 220.0f;
    private static final float PANEL_MIN_HEIGHT = 116.0f;
    private static final float TITLE_SCALE = 0.78f;
    private static final float SUBTITLE_SCALE = 0.56f;
    private static final float HEADER_TOP = 10.0f;
    private static final float HEADER_GAP = 28.0f;
    private static final float CONTENT_TOP = 40.0f;

    private final RoundRectRenderer roundRectRenderer = new RoundRectRenderer();
    private final RectRenderer rectRenderer = new RectRenderer();
    private final ShadowRenderer shadowRenderer = new ShadowRenderer();
    private final TextRenderer textRenderer = new TextRenderer();
    private final RoundRectRenderer contentRoundRectRenderer = new RoundRectRenderer();
    private final RectRenderer contentRectRenderer = new RectRenderer();
    private final ShadowRenderer contentShadowRenderer = new ShadowRenderer();
    private final TextRenderer contentTextRenderer = new TextRenderer();
    private final RoundRectRenderer scrollBarRenderer = new RoundRectRenderer();
    private final PanelPopupHost popupHost = new PanelPopupHost();
    private final SettingListController settingList = new SettingListController(popupHost);

    private PanelLayout.Rect bounds;
    private PanelLayout.Rect viewport;
    private HudModule selectedModule;
    private float scroll;
    private float maxScroll;
    private String selectedModuleName = "";

    void queueRender(GuiGraphicsExtractor graphics, HudModule selectedModule, int screenWidth, int screenHeight, int mouseX, int mouseY, float partialTick, int guiHeight) {
        this.selectedModule = selectedModule;
        this.bounds = computeBounds(screenWidth, screenHeight);

        shadowRenderer.addShadow(bounds.x(), bounds.y(), bounds.width(), bounds.height(), MD3Theme.SECTION_RADIUS, 16.0f, MD3Theme.SHADOW);
        roundRectRenderer.addRoundRect(bounds.x(), bounds.y(), bounds.width(), bounds.height(), MD3Theme.SECTION_RADIUS, MD3Theme.SURFACE_DIM);
        roundRectRenderer.addRoundRect(bounds.x() + 1.0f, bounds.y() + 1.0f, bounds.width() - 2.0f, bounds.height() - 2.0f, MD3Theme.SECTION_RADIUS - 1.0f, MD3Theme.SURFACE_CONTAINER_LOW);

        float titleX = bounds.x() + MD3Theme.PANEL_TITLE_INSET;
        textRenderer.addText("HUD Inspector", titleX, bounds.y() + HEADER_TOP, TITLE_SCALE, MD3Theme.TEXT_PRIMARY, StaticFontLoader.DUCKSANS);

        if (selectedModule == null) {
            textRenderer.addText("Select a HUD element", titleX, bounds.y() + HEADER_GAP, SUBTITLE_SCALE, MD3Theme.TEXT_SECONDARY);
            flushChrome();
            clearContentState();
            return;
        }

        if (!selectedModule.getName().equals(selectedModuleName)) {
            scroll = 0.0f;
            selectedModuleName = selectedModule.getName();
        }

        textRenderer.addText(selectedModule.getTranslatedName(), titleX, bounds.y() + HEADER_GAP, SUBTITLE_SCALE, MD3Theme.TEXT_SECONDARY);

        viewport = new PanelLayout.Rect(
                bounds.x() + MD3Theme.PANEL_VIEWPORT_INSET,
                bounds.y() + CONTENT_TOP,
                bounds.width() - MD3Theme.PANEL_VIEWPORT_INSET * 2.0f,
                bounds.height() - CONTENT_TOP - MD3Theme.PANEL_VIEWPORT_INSET
        );

        List<Setting<?>> settings = selectedModule.getSettings().stream().filter(Setting::isAvailable).toList();

        if (settings.isEmpty()) {
            textRenderer.addText("This HUD has no settings", titleX, viewport.y(), SUBTITLE_SCALE, MD3Theme.TEXT_MUTED);
            flushChrome();
            clearContentState();
            return;
        }

        float contentHeight = settings.size() * (28.0f + MD3Theme.ROW_GAP);
        maxScroll = Math.max(0.0f, contentHeight - viewport.height());
        scroll = Mth.clamp(scroll, 0.0f, maxScroll);
        boolean hasScrollBar = maxScroll > 0.0f;
        float rowWidth = hasScrollBar ? viewport.width() - ScrollBarUtil.TOTAL_WIDTH : viewport.width();
        boolean popupConsumesHover = settingList.isPopupHovered(mouseX, mouseY);
        int effectiveMouseX = popupConsumesHover ? Integer.MIN_VALUE : mouseX;
        int effectiveMouseY = popupConsumesHover ? Integer.MIN_VALUE : mouseY;

        settingList.layoutRows(settings, viewport, scroll, rowWidth, (setting, row, rowBounds) -> {
            float hover = rowBounds.contains(effectiveMouseX, effectiveMouseY) ? 1.0f : 0.0f;
            row.render(graphics, contentRoundRectRenderer, contentRectRenderer, contentTextRenderer, rowBounds, hover, effectiveMouseX, effectiveMouseY, partialTick);
        });

        flushChrome();

        PanelScissor.apply(viewport, contentRectRenderer, contentRoundRectRenderer, contentShadowRenderer, contentTextRenderer, guiHeight);
        contentShadowRenderer.draw();
        contentRoundRectRenderer.draw();
        contentRectRenderer.draw();
        contentTextRenderer.draw();
        PanelScissor.clear(contentRectRenderer, contentRoundRectRenderer, contentShadowRenderer, contentTextRenderer);
        contentShadowRenderer.clear();
        contentRoundRectRenderer.clear();
        contentRectRenderer.clear();
        contentTextRenderer.clear();

        ScrollBarUtil.draw(scrollBarRenderer, viewport, scroll, maxScroll, contentHeight);
        scrollBarRenderer.drawAndClear();
    }

    void renderPopups(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        settingList.getPopupHost().render(graphics, mouseX, mouseY, partialTick);
    }

    boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
        if (settingList.getPopupHost().mouseClicked(event, isDoubleClick)) {
            return true;
        }
        if (bounds == null || !bounds.contains(event.x(), event.y())) {
            settingList.clearFocus();
            return false;
        }
        if (selectedModule == null || event.button() != 0) {
            return true;
        }

        settingList.mouseClicked(event, isDoubleClick, bounds);
        return true;
    }

    boolean mouseReleased(MouseButtonEvent event) {
        if (settingList.getPopupHost().mouseReleased(event)) {
            return true;
        }
        return settingList.mouseReleased(event);
    }

    boolean mouseDragged(MouseButtonEvent event, double mouseX, double mouseY) {
        if (settingList.getPopupHost().mouseDragged(event, mouseX, mouseY)) {
            return true;
        }
        return settingList.mouseDragged(event, mouseX, mouseY);
    }

    boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (settingList.getPopupHost().mouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
            return true;
        }
        if (viewport != null && viewport.contains(mouseX, mouseY) && maxScroll > 0.0f) {
            scroll = Mth.clamp(scroll - (float) scrollY * 20.0f, 0.0f, maxScroll);
            return true;
        }
        return bounds != null && bounds.contains(mouseX, mouseY);
    }

    boolean keyPressed(KeyEvent event) {
        if (settingList.getPopupHost().keyPressed(event)) {
            return true;
        }
        return settingList.keyPressed(event);
    }

    boolean charTyped(CharacterEvent event) {
        if (settingList.getPopupHost().charTyped(event)) {
            return true;
        }
        return settingList.charTyped(event);
    }

    void clearFocus() {
        settingList.clearFocus();
    }

    private PanelLayout.Rect computeBounds(int screenWidth, int screenHeight) {
        float width = Math.min(PANEL_WIDTH, Math.max(164.0f, screenWidth - PANEL_MARGIN * 2.0f));
        float height = Math.max(PANEL_MIN_HEIGHT, screenHeight - PANEL_MARGIN * 2.0f);
        float x = screenWidth - width - PANEL_MARGIN;
        return new PanelLayout.Rect(x, PANEL_MARGIN, width, height);
    }

    private void flushChrome() {
        shadowRenderer.drawAndClear();
        roundRectRenderer.drawAndClear();
        rectRenderer.drawAndClear();
        textRenderer.drawAndClear();
    }

    private void clearContentState() {
        viewport = null;
        maxScroll = 0.0f;
        settingList.clearAll();
        contentShadowRenderer.clear();
        contentRoundRectRenderer.clear();
        contentRectRenderer.clear();
        contentTextRenderer.clear();
        scrollBarRenderer.clear();
    }
}
