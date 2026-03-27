package com.github.epsilon.gui.panel;

import com.github.epsilon.graphics.renderers.RectRenderer;
import com.github.epsilon.graphics.renderers.RoundRectRenderer;
import com.github.epsilon.graphics.renderers.ShadowRenderer;
import com.github.epsilon.graphics.renderers.TextRenderer;
import com.github.epsilon.gui.panel.input.PanelInputRouter;
import com.github.epsilon.gui.panel.panel.CategoryRailPanel;
import com.github.epsilon.gui.panel.panel.ModuleDetailPanel;
import com.github.epsilon.gui.panel.panel.ModuleListPanel;
import com.github.epsilon.gui.panel.popup.PanelPopupHost;
import com.github.epsilon.managers.RenderManager;
import com.github.epsilon.modules.impl.client.ClickGui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;

public class PanelScreen extends Screen {

    public static final PanelScreen INSTANCE = new PanelScreen();

    private final PanelState state = new PanelState();
    private final TextRenderer textRenderer = new TextRenderer();
    private final RectRenderer backgroundRectRenderer = new RectRenderer();
    private final RectRenderer rectRenderer = new RectRenderer();
    private final RoundRectRenderer roundRectRenderer = new RoundRectRenderer();
    private final ShadowRenderer shadowRenderer = new ShadowRenderer();
    private final PanelPopupHost popupHost = new PanelPopupHost();
    private final PanelInputRouter inputRouter = new PanelInputRouter();
    private final CategoryRailPanel categoryRailPanel = new CategoryRailPanel(state, rectRenderer, roundRectRenderer, textRenderer);
    private final ModuleListPanel moduleListPanel = new ModuleListPanel(state, roundRectRenderer, rectRenderer, shadowRenderer, textRenderer);
    private final ModuleDetailPanel moduleDetailPanel = new ModuleDetailPanel(state, roundRectRenderer, rectRenderer, shadowRenderer, textRenderer, popupHost);

    private PanelScreen() {
        super(Component.literal("PanelGui"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void extractBackground(@NonNull GuiGraphicsExtractor GuiGraphicsExtractor, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor GuiGraphicsExtractor, int mouseX, int mouseY, float partialTick) {
        MD3Theme.syncFromSettings();
        float railWidth = categoryRailPanel.getAnimatedWidth();
        PanelLayout.Layout layout = PanelLayout.compute(width, height, railWidth);

        drawBackgroundScrim();
        drawChrome(layout);
        categoryRailPanel.render(GuiGraphicsExtractor, layout.rail(), mouseX, mouseY, partialTick);
        moduleListPanel.render(GuiGraphicsExtractor, layout.modules(), mouseX, mouseY, partialTick);
        moduleDetailPanel.render(GuiGraphicsExtractor, layout.detail(), mouseX, mouseY, partialTick);

        RenderManager.INSTANCE.applyRenderAfterFrame(this::flushQueuedRenderers);

        popupHost.render(GuiGraphicsExtractor, mouseX, mouseY, partialTick);
    }

    private void drawBackgroundScrim() {
        backgroundRectRenderer.addRect(0, 0, width, height, MD3Theme.SCRIM);
    }

    private void drawChrome(PanelLayout.Layout layout) {
        if (ClickGui.INSTANCE.shouldBlur()) {
            // 我感觉blur没必要好吧因为几乎看不到捏
            //BlurShader.INSTANCE.drawBlur(layout.panel().x(), layout.panel().y(), layout.panel().width(), layout.panel().height(), MD3Theme.PANEL_RADIUS, clickGui.getBlurStrength());
        }

        shadowRenderer.addShadow(layout.panel().x(), layout.panel().y(), layout.panel().width(), layout.panel().height(), MD3Theme.PANEL_RADIUS, 18.0f, MD3Theme.SHADOW);
        roundRectRenderer.addRoundRect(layout.panel().x(), layout.panel().y(), layout.panel().width(), layout.panel().height(), MD3Theme.PANEL_RADIUS, MD3Theme.SURFACE);

        roundRectRenderer.addRoundRect(layout.rail().x(), layout.rail().y(), layout.rail().width(), layout.rail().height(), MD3Theme.SECTION_RADIUS, MD3Theme.SURFACE_DIM);
        roundRectRenderer.addRoundRect(layout.modules().x(), layout.modules().y(), layout.modules().width(), layout.modules().height(), MD3Theme.SECTION_RADIUS, MD3Theme.SURFACE_DIM);
        roundRectRenderer.addRoundRect(layout.detail().x(), layout.detail().y(), layout.detail().width(), layout.detail().height(), MD3Theme.SECTION_RADIUS, MD3Theme.SURFACE_DIM);
    }

    private void flushQueuedRenderers() {
        backgroundRectRenderer.drawAndClear();
        shadowRenderer.drawAndClear();
        roundRectRenderer.drawAndClear();
        rectRenderer.drawAndClear();
        textRenderer.drawAndClear();
        moduleListPanel.flushContent();
        moduleDetailPanel.flushContent();
        categoryRailPanel.flushClippedText();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        if (event.button() != 0) {
            return super.mouseClicked(event, isDoubleClick);
        }

        if (popupHost.getActivePopup() != null) {
            return inputRouter.routeMouseClicked(event, isDoubleClick, popupHost, moduleDetailPanel, moduleListPanel, categoryRailPanel)
                    || super.mouseClicked(event, isDoubleClick);
        }

        PanelLayout.Layout layout = PanelLayout.compute(width, height, categoryRailPanel.getAnimatedWidth());
        if (!layout.panel().contains(mouseX, mouseY)) {
            onClose();
            return true;
        }
        moduleListPanel.handleGlobalClick(mouseX, mouseY);
        return inputRouter.routeMouseClicked(event, isDoubleClick, popupHost, moduleDetailPanel, moduleListPanel, categoryRailPanel) || super.mouseClicked(event, isDoubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (moduleListPanel.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
            return true;
        }
        if (moduleDetailPanel.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (inputRouter.routeMouseReleased(event, popupHost, moduleDetailPanel)) {
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double mouseX, double mouseY) {
        if (inputRouter.routeMouseDragged(event, mouseX, mouseY, popupHost, moduleDetailPanel)) {
            return true;
        }
        return super.mouseDragged(event, mouseX, mouseY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == 256) {
            onClose();
            return true;
        }
        if (inputRouter.routeKeyPressed(event, popupHost, moduleDetailPanel, moduleListPanel)) {
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (inputRouter.routeCharTyped(event, popupHost, moduleDetailPanel, moduleListPanel)) {
            return true;
        }
        return super.charTyped(event);
    }

    @Override
    public void onClose() {
        ClickGui.INSTANCE.setEnabled(false);
        super.onClose();
    }

}
