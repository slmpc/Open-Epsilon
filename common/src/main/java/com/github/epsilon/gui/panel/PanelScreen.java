package com.github.epsilon.gui.panel;

import com.github.epsilon.graphics.LuminRenderSystem;
import com.github.epsilon.graphics.renderers.RectRenderer;
import com.github.epsilon.graphics.renderers.RoundRectRenderer;
import com.github.epsilon.graphics.renderers.ShadowRenderer;
import com.github.epsilon.graphics.renderers.TextRenderer;
import com.github.epsilon.gui.panel.input.PanelInputRouter;
import com.github.epsilon.gui.panel.panel.CategoryRailPanel;
import com.github.epsilon.gui.panel.panel.ClientSettingPanel;
import com.github.epsilon.gui.panel.panel.ModuleDetailPanel;
import com.github.epsilon.gui.panel.panel.ModuleListPanel;
import com.github.epsilon.gui.panel.popup.PanelPopupHost;
import com.github.epsilon.gui.panel.util.IMEFocusHelper;
import com.github.epsilon.managers.RenderManager;
import com.github.epsilon.modules.impl.ClientSetting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.IMEPreeditOverlay;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.PreeditEvent;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;

import javax.annotation.Nullable;

public class PanelScreen extends Screen {

    public static final PanelScreen INSTANCE = new PanelScreen();

    private final PanelState state = new PanelState();
    private final PanelDirtyState dirtyState = new PanelDirtyState();
    private final TextRenderer textRenderer = TextRenderer.create();
    private final RectRenderer rectRenderer = RectRenderer.create();
    private final RoundRectRenderer roundRectRenderer = RoundRectRenderer.create();
    private final ShadowRenderer shadowRenderer = ShadowRenderer.create();
    private final PanelPopupHost popupHost = new PanelPopupHost();
    private final PanelInputRouter inputRouter = new PanelInputRouter();
    private final CategoryRailPanel categoryRailPanel = new CategoryRailPanel(state, rectRenderer, roundRectRenderer, textRenderer);
    private final ModuleListPanel moduleListPanel = new ModuleListPanel(state, roundRectRenderer, rectRenderer, shadowRenderer, textRenderer);
    private final ModuleDetailPanel moduleDetailPanel = new ModuleDetailPanel(state, roundRectRenderer, rectRenderer, shadowRenderer, textRenderer, popupHost);
    private final ClientSettingPanel clientSettingPanel = new ClientSettingPanel(state, roundRectRenderer, rectRenderer, shadowRenderer, textRenderer, popupHost);
    private int lastWidth = -1;
    private int lastHeight = -1;
    private String lastSelectedCategory = "";
    private String lastSelectedModule = "";
    private String lastSearchQuery = "";
    private boolean lastSidebarExpanded;
    private boolean lastClientSettingMode;

    private @Nullable IMEPreeditOverlay preeditOverlay;

    private @Nullable LuminRenderSystem.LuminRenderTarget renderTarget;

    private PanelScreen() {
        super(Component.literal("PanelGui"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {

        final var window = minecraft.getWindow();
        if (renderTarget == null) {
            renderTarget = LuminRenderSystem.LuminRenderTarget.create("click-gui", window.getWidth(), window.getHeight());
        }
        renderTarget.clear();
        renderTarget.resize(window.getWidth(), window.getHeight());

        LuminRenderSystem.setActiveTarget(renderTarget);

        String currentCategory = state.getSelectedCategory().name();
        String currentModule = state.getSelectedModule() == null ? "" : state.getSelectedModule().getName();
        String currentQuery = state.getSearchQuery();
        boolean sidebarExpanded = state.isSidebarExpanded();
        boolean clientSettingMode = state.isClientSettingMode();
        if (!lastSelectedCategory.equals(currentCategory)
                || !lastSelectedModule.equals(currentModule)
                || !lastSearchQuery.equals(currentQuery)
                || lastSidebarExpanded != sidebarExpanded
                || lastClientSettingMode != clientSettingMode) {
            dirtyState.markAllDirty();
            lastSelectedCategory = currentCategory;
            lastSelectedModule = currentModule;
            lastSearchQuery = currentQuery;
            lastSidebarExpanded = sidebarExpanded;
            lastClientSettingMode = clientSettingMode;
        }

        if (categoryRailPanel.hasActiveAnimations()
                || moduleListPanel.hasActiveAnimations()
                || moduleDetailPanel.hasActiveAnimations()
                || clientSettingPanel.hasActiveAnimations()) {
            dirtyState.markAllDirty();
        }

        if (width != lastWidth || height != lastHeight) {
            dirtyState.markLayoutDirty();
            lastWidth = width;
            lastHeight = height;
        }

        if (dirtyState.consumeModuleListDirty()) {
            moduleListPanel.markDirty();
        }
        if (dirtyState.consumeDetailDirty()) {
            moduleDetailPanel.markDirty();
        }
        if (dirtyState.consumeClientSettingDirty()) {
            clientSettingPanel.markDirty();
        }

        MD3Theme.syncFromSettings();
        float railWidth = categoryRailPanel.getAnimatedWidth();
        PanelLayout.Layout layout = PanelLayout.compute(width, height, railWidth);

        drawChrome(layout);
        categoryRailPanel.render(guiGraphics, layout.rail(), mouseX, mouseY, partialTick);
        if (state.isClientSettingMode()) {
            PanelLayout.Rect clientSettingsBounds = new PanelLayout.Rect(
                    layout.modules().x(), layout.modules().y(),
                    layout.detail().right() - layout.modules().x(),
                    layout.modules().height()
            );
            clientSettingPanel.render(guiGraphics, clientSettingsBounds, mouseX, mouseY, partialTick);
        } else {
            moduleListPanel.render(guiGraphics, layout.modules(), mouseX, mouseY, partialTick);
            moduleDetailPanel.render(guiGraphics, layout.detail(), mouseX, mouseY, partialTick);
        }

        RenderManager.INSTANCE.applyRender(this::flushQueuedRenderers);

        popupHost.render(guiGraphics, mouseX, mouseY, partialTick);

        LuminRenderSystem.setActiveTarget(null);

        if (preeditOverlay != null) {
            this.preeditOverlay.updateInputPosition((int) IMEFocusHelper.activeCursorX, (int) IMEFocusHelper.activeCursorY);
            guiGraphics.setPreeditOverlay(this.preeditOverlay);
        }
        guiGraphics.blit(renderTarget.getIdentifier(), 0, 0, window.getGuiScaledWidth(), window.getGuiScaledHeight(), 0, 1, 1, 0);
    }

    private void drawChrome(PanelLayout.Layout layout) {
        shadowRenderer.addShadow(layout.panel().x(), layout.panel().y(), layout.panel().width(), layout.panel().height(), MD3Theme.PANEL_RADIUS, 18.0f, MD3Theme.SHADOW);
        roundRectRenderer.addRoundRect(layout.panel().x(), layout.panel().y(), layout.panel().width(), layout.panel().height(), MD3Theme.PANEL_RADIUS, MD3Theme.SURFACE);

        roundRectRenderer.addRoundRect(layout.rail().x(), layout.rail().y(), layout.rail().width(), layout.rail().height(), MD3Theme.SECTION_RADIUS, MD3Theme.SURFACE_DIM);
        if (state.isClientSettingMode()) {
            float csX = layout.modules().x();
            float csY = layout.modules().y();
            float csW = layout.detail().right() - layout.modules().x();
            float csH = layout.modules().height();
            roundRectRenderer.addRoundRect(csX, csY, csW, csH, MD3Theme.SECTION_RADIUS, MD3Theme.SURFACE_DIM);
        } else {
            roundRectRenderer.addRoundRect(layout.modules().x(), layout.modules().y(), layout.modules().width(), layout.modules().height(), MD3Theme.SECTION_RADIUS, MD3Theme.SURFACE_DIM);
            roundRectRenderer.addRoundRect(layout.detail().x(), layout.detail().y(), layout.detail().width(), layout.detail().height(), MD3Theme.SECTION_RADIUS, MD3Theme.SURFACE_DIM);
        }
    }

    private void flushQueuedRenderers() {
        shadowRenderer.drawAndClear();
        roundRectRenderer.drawAndClear();
        rectRenderer.drawAndClear();
        textRenderer.drawAndClear();
        if (state.isClientSettingMode()) {
            clientSettingPanel.flushContent();
        } else {
            moduleListPanel.flushContent();
            moduleDetailPanel.flushContent();
        }
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
            return inputRouter.routeMouseClicked(event, isDoubleClick, popupHost, moduleDetailPanel, moduleListPanel, categoryRailPanel, clientSettingPanel, state.isClientSettingMode())
                    || super.mouseClicked(event, isDoubleClick);
        }

        PanelLayout.Layout layout = PanelLayout.compute(width, height, categoryRailPanel.getAnimatedWidth());
        if (!layout.panel().contains(mouseX, mouseY)) {
            if (ClientSetting.INSTANCE.closeOnOutside.getValue()) minecraft.gui.setScreen(null);
            return true;
        }
        if (!state.isClientSettingMode()) {
            moduleListPanel.handleGlobalClick(mouseX, mouseY);
        }
        boolean handled = inputRouter.routeMouseClicked(event, isDoubleClick, popupHost, moduleDetailPanel, moduleListPanel, categoryRailPanel, clientSettingPanel, state.isClientSettingMode());
        if (handled) {
            dirtyState.markAllDirty();
        }
        return handled || super.mouseClicked(event, isDoubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (popupHost.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
            dirtyState.markAllDirty();
            return true;
        }
        if (state.isClientSettingMode()) {
            if (clientSettingPanel.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
                dirtyState.markClientSettingDirty();
                return true;
            }
        } else {
            if (moduleListPanel.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
                dirtyState.markModuleListDirty();
                return true;
            }
            if (moduleDetailPanel.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
                dirtyState.markDetailDirty();
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (inputRouter.routeMouseReleased(event, popupHost, moduleDetailPanel, moduleListPanel, clientSettingPanel, state.isClientSettingMode())) {
            dirtyState.markAllDirty();
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double mouseX, double mouseY) {
        if (inputRouter.routeMouseDragged(event, mouseX, mouseY, popupHost, moduleDetailPanel, moduleListPanel, clientSettingPanel, state.isClientSettingMode())) {
            dirtyState.markAllDirty();
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
        if (inputRouter.routeKeyPressed(event, popupHost, moduleDetailPanel, moduleListPanel, clientSettingPanel, state.isClientSettingMode())) {
            dirtyState.markAllDirty();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (inputRouter.routeCharTyped(event, popupHost, moduleDetailPanel, moduleListPanel, clientSettingPanel, state.isClientSettingMode())) {
            dirtyState.markAllDirty();
            return true;
        }
        return super.charTyped(event);
    }

    @Override
    public boolean preeditUpdated(@Nullable PreeditEvent event) {
        this.preeditOverlay = event != null ? new IMEPreeditOverlay(event, this.font, 10) : null;
        return true;
    }

    @Override
    public void onClose() {
        IMEFocusHelper.deactivate();
        super.onClose();
    }

    public LuminRenderSystem.LuminRenderTarget getRenderTarget() {
        return renderTarget;
    }
}
