package com.github.epsilon.gui.panel.panel;

import com.github.epsilon.assets.i18n.EpsilonTranslateComponent;
import com.github.epsilon.assets.i18n.TranslateComponent;
import com.github.epsilon.graphics.renderers.RectRenderer;
import com.github.epsilon.graphics.renderers.RoundRectRenderer;
import com.github.epsilon.graphics.renderers.ShadowRenderer;
import com.github.epsilon.graphics.renderers.TextRenderer;
import com.github.epsilon.graphics.text.StaticFontLoader;
import com.github.epsilon.gui.panel.MD3Theme;
import com.github.epsilon.gui.panel.PanelLayout;
import com.github.epsilon.gui.panel.PanelState;
import com.github.epsilon.gui.panel.adapter.SettingListController;
import com.github.epsilon.gui.panel.popup.PanelPopupHost;
import com.github.epsilon.gui.panel.util.PanelContentBuffer;
import com.github.epsilon.gui.panel.util.PanelContentInvalidationState;
import com.github.epsilon.gui.panel.util.ScrollBarDragState;
import com.github.epsilon.gui.panel.util.ScrollBarUtil;
import com.github.epsilon.modules.Module;
import com.github.epsilon.settings.Setting;
import com.github.epsilon.utils.render.animation.Animation;
import com.github.epsilon.utils.render.animation.Easing;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;

import java.awt.*;
import java.util.*;
import java.util.List;

public class ModuleDetailPanel {

    protected final PanelState state;
    private final RoundRectRenderer roundRectRenderer;
    private final RectRenderer rectRenderer;
    private final TextRenderer textRenderer;
    private final SettingListController settingListController;
    private final PanelContentBuffer contentBuffer = new PanelContentBuffer();
    private final PanelContentInvalidationState contentState = new PanelContentInvalidationState();
    private PanelLayout.Rect bounds;
    private int guiHeight;
    private PanelLayout.Rect headerBounds;
    private final Map<Setting<?>, Animation> hoverAnimations = new HashMap<>();
    private float lastDetailScroll = Float.NaN;
    private String lastModuleKey = "";
    private List<String> lastVisibleSettings = List.of();
    private final ScrollBarDragState scrollBarDrag = new ScrollBarDragState();
    private final Animation bindModeAnimation = new Animation(Easing.EASE_OUT_CUBIC, 180L);
    private final Animation bindModeHoverAnimation = new Animation(Easing.EASE_OUT_CUBIC, 120L);
    private final Animation keybindHoverAnimation = new Animation(Easing.EASE_OUT_CUBIC, 120L);
    private final Animation keybindFocusAnimation = new Animation(Easing.EASE_OUT_CUBIC, 150L);

    private static final TranslateComponent toggleComponent = EpsilonTranslateComponent.create("keybind", "toggle");
    private static final TranslateComponent holdComponent = EpsilonTranslateComponent.create("keybind", "hold");
    private static final TranslateComponent noneComponent = EpsilonTranslateComponent.create("keybind", "none");

    public ModuleDetailPanel(PanelState state, RoundRectRenderer roundRectRenderer, RectRenderer rectRenderer, ShadowRenderer shadowRenderer, TextRenderer textRenderer, PanelPopupHost popupHost) {
        this.state = state;
        this.roundRectRenderer = roundRectRenderer;
        this.rectRenderer = rectRenderer;
        this.textRenderer = textRenderer;
        this.settingListController = new SettingListController(popupHost);
        this.bindModeAnimation.setStartValue(0.0f);
        this.bindModeHoverAnimation.setStartValue(0.0f);
        this.keybindHoverAnimation.setStartValue(0.0f);
        this.keybindFocusAnimation.setStartValue(0.0f);
    }

    public void render(GuiGraphicsExtractor GuiGraphicsExtractor, PanelLayout.Rect bounds, int mouseX, int mouseY, float partialTick) {
        this.bounds = bounds;
        this.guiHeight = GuiGraphicsExtractor.guiHeight();
        boolean popupConsumesHover = settingListController.isPopupHovered(mouseX, mouseY);
        int effectiveMouseX = popupConsumesHover ? Integer.MIN_VALUE : mouseX;
        int effectiveMouseY = popupConsumesHover ? Integer.MIN_VALUE : mouseY;

        Module module = state.getSelectedModule();
        String detailTitle = module == null ? "No Module" : module.getTranslatedName();
        textRenderer.addText(detailTitle, bounds.x() + MD3Theme.PANEL_TITLE_INSET, bounds.y() + 10.0f, 0.78f, MD3Theme.TEXT_PRIMARY, StaticFontLoader.DUCKSANS);
        textRenderer.addText("Settings", bounds.x() + MD3Theme.PANEL_TITLE_INSET, bounds.y() + 21.0f, 0.56f, MD3Theme.TEXT_SECONDARY);

        if (module == null) {
            return;
        }

        headerBounds = new PanelLayout.Rect(bounds.x() + MD3Theme.PANEL_VIEWPORT_INSET, bounds.y() + 34.0f, bounds.width() - MD3Theme.PANEL_VIEWPORT_INSET * 2.0f, 36.0f);
        roundRectRenderer.addRoundRect(headerBounds.x(), headerBounds.y(), headerBounds.width(), headerBounds.height(), MD3Theme.CARD_RADIUS, MD3Theme.SURFACE_CONTAINER);

        float titleScale = 0.72f;
        float headerTextX = getHeaderContentInsetX();
        float titleY = headerBounds.y() + (headerBounds.height() - textRenderer.getHeight(titleScale)) / 2.0f - 1.0f;
        textRenderer.addText(module.getTranslatedName(), headerTextX, titleY, titleScale, MD3Theme.TEXT_PRIMARY, StaticFontLoader.DUCKSANS);

        drawKeybindControl(module, mouseX, mouseY);
        drawBindModeControl(module, mouseX, mouseY);

        PanelLayout.Rect viewport = getViewport();
        List<Setting<?>> settings = module.getSettings().stream().filter(Setting::isAvailable).toList();
        float contentHeight = settings.size() * (28.0f + MD3Theme.ROW_GAP);
        state.setMaxDetailScroll(contentHeight - viewport.height());
        float maxDetailScroll = Math.max(0, contentHeight - viewport.height());
        boolean hasScrollBar = maxDetailScroll > 0;
        float rowWidth = hasScrollBar ? viewport.width() - ScrollBarUtil.TOTAL_WIDTH : viewport.width();

        if (shouldRebuildContent(bounds, mouseX, mouseY, module, settings, GuiGraphicsExtractor.guiHeight())) {
            contentBuffer.clear();
            contentState.beginRebuild();

            settingListController.layoutRows(settings, viewport, state.getDetailScroll(), rowWidth, (setting, row, rowBounds) -> {
                Animation hoverAnimation = hoverAnimations.computeIfAbsent(setting, ignored -> new Animation(Easing.EASE_OUT_CUBIC, 120L));
                hoverAnimation.run(rowBounds.contains(effectiveMouseX, effectiveMouseY) ? 1.0f : 0.0f);
                row.render(GuiGraphicsExtractor, contentBuffer.roundRectRenderer(), contentBuffer.rectRenderer(), contentBuffer.textRenderer(), rowBounds, hoverAnimation.getValue(), effectiveMouseX, effectiveMouseY, partialTick);
                contentState.noteAnimation(!hoverAnimation.isFinished() || row.hasActiveAnimation());
            });

            rememberSnapshot(bounds, mouseX, mouseY, module, settings, GuiGraphicsExtractor.guiHeight());
        }

        contentBuffer.queueViewport(viewport, guiHeight, state.getDetailScroll(), maxDetailScroll, contentHeight);
    }

    public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
        if (bounds == null || event.button() != 0) {
            return false;
        }
        Module module = state.getSelectedModule();
        if (module == null || headerBounds == null) {
            return false;
        }

        // Scrollbar drag
        PanelLayout.Rect viewport = getViewport();
        float maxScroll = state.getMaxDetailScroll();
        if (scrollBarDrag.mouseClicked(event.x(), event.y(), viewport, state.getDetailScroll(), maxScroll)) {
            float newScroll = scrollBarDrag.mouseDragged(event.y(), viewport, maxScroll);
            if (newScroll >= 0) {
                state.setDetailScroll(newScroll);
            }
            markDirty();
            return true;
        }

        PanelLayout.Rect keybindBounds = getKeybindBounds();
        if (keybindBounds.contains(event.x(), event.y())) {
            state.setListeningKeyBindModule(module);
            markDirty();
            return true;
        } else if (state.getListeningKeyBindModule() == module) {
            state.setListeningKeyBindModule(null);
            markDirty();
        }

        PanelLayout.Rect bindModeBounds = getBindModeBounds();
        if (bindModeBounds.contains(event.x(), event.y())) {
            float midpoint = bindModeBounds.centerX();
            module.setBindMode(event.x() < midpoint ? Module.BindMode.Toggle : Module.BindMode.Hold);
            markDirty();
            return true;
        }

        if (settingListController.mouseClicked(event, isDoubleClick, bounds)) {
            markDirty();
            return true;
        }
        return false;
    }

    public boolean mouseReleased(MouseButtonEvent event) {
        if (scrollBarDrag.mouseReleased()) {
            markDirty();
            return true;
        }
        if (settingListController.mouseReleased(event)) {
            markDirty();
            return true;
        }
        return false;
    }

    public boolean mouseDragged(MouseButtonEvent event, double mouseX, double mouseY) {
        if (scrollBarDrag.isDragging()) {
            PanelLayout.Rect viewport = getViewport();
            float newScroll = scrollBarDrag.mouseDragged(event.y(), viewport, state.getMaxDetailScroll());
            if (newScroll >= 0) {
                state.setDetailScroll(newScroll);
            }
            markDirty();
            return true;
        }
        if (settingListController.mouseDragged(event, mouseX, mouseY)) {
            markDirty();
            return true;
        }
        return false;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        PanelLayout.Rect viewport = getViewport();
        if (bounds != null && viewport.contains(mouseX, mouseY)) {
            state.scrollDetail(-scrollY * 20.0f);
            markDirty();
            return true;
        }
        return false;
    }

    public boolean keyPressed(KeyEvent event) {
        Module module = state.getSelectedModule();
        if (module != null && state.getListeningKeyBindModule() == module) {
            if (event.key() == 256) {
                state.setListeningKeyBindModule(null);
                markDirty();
                return true;
            }
            if (event.key() == 259 || event.key() == 261) {
                module.setKeyBind(-1);
                state.setListeningKeyBindModule(null);
                markDirty();
                return true;
            }
            module.setKeyBind(event.key());
            state.setListeningKeyBindModule(null);
            markDirty();
            return true;
        }
        if (settingListController.keyPressed(event)) {
            markDirty();
            return true;
        }
        return false;
    }

    public boolean charTyped(CharacterEvent event) {
        if (settingListController.charTyped(event)) {
            markDirty();
            return true;
        }
        return false;
    }

    private PanelLayout.Rect getViewport() {
        if (bounds == null) {
            return new PanelLayout.Rect(0, 0, 0, 0);
        }
        if (headerBounds == null) {
            return new PanelLayout.Rect(bounds.x(), bounds.y(), bounds.width(), bounds.height());
        }
        return new PanelLayout.Rect(bounds.x() + MD3Theme.PANEL_VIEWPORT_INSET, headerBounds.bottom() + 6.0f, bounds.width() - MD3Theme.PANEL_VIEWPORT_INSET * 2.0f, bounds.bottom() - headerBounds.bottom() - 10.0f);
    }

    private PanelLayout.Rect getBindModeBounds() {
        return new PanelLayout.Rect(getHeaderControlGroupX() + getKeybindControlSize() + getHeaderControlGap(), getHeaderControlsY(), getBindModeControlWidth(), getHeaderControlHeight());
    }

    private PanelLayout.Rect getKeybindBounds() {
        return new PanelLayout.Rect(getHeaderControlGroupX(), getHeaderControlsY(), getKeybindControlSize(), getKeybindControlSize());
    }

    private float getHeaderControlGroupX() {
        return headerBounds.right() - getHeaderContentInset() - getHeaderControlGroupWidth();
    }

    private float getHeaderControlGroupWidth() {
        return getKeybindControlSize() + getHeaderControlGap() + getBindModeControlWidth();
    }

    private float getHeaderContentInset() {
        return MD3Theme.PANEL_TITLE_INSET + 2.0f;
    }

    private float getHeaderContentInsetX() {
        return headerBounds.x() + getHeaderContentInset();
    }

    private float getHeaderControlHeight() {
        return 18.0f;
    }

    private float getKeybindControlSize() {
        return getHeaderControlHeight();
    }

    private float getBindModeControlWidth() {
        return 96.0f;
    }

    private float getHeaderControlGap() {
        return 6.0f;
    }

    private float getHeaderControlRadius() {
        return 7.0f;
    }

    private float getKeybindControlRadius() {
        return 8.0f;
    }

    private float getHeaderControlsY() {
        return headerBounds.y() + (headerBounds.height() - getHeaderControlHeight()) / 2.0f;
    }

    private void drawBindModeControl(Module module, int mouseX, int mouseY) {
        PanelLayout.Rect bindModeBounds = getBindModeBounds();
        bindModeAnimation.run(module.getBindMode() == Module.BindMode.Hold ? 1.0f : 0.0f);
        bindModeHoverAnimation.run(bindModeBounds.contains(mouseX, mouseY) ? 1.0f : 0.0f);
        float progress = bindModeAnimation.getValue();
        float hoverProgress = bindModeHoverAnimation.getValue();
        float outerRadius = getHeaderControlRadius();
        float shellInset = 1.0f;
        float innerX = bindModeBounds.x() + shellInset;
        float innerY = bindModeBounds.y() + shellInset;
        float innerWidth = bindModeBounds.width() - shellInset * 2.0f;
        float innerHeight = bindModeBounds.height() - shellInset * 2.0f;
        float segmentWidth = innerWidth / 2.0f;
        float indicatorInset = 1.5f;
        float indicatorWidth = segmentWidth - indicatorInset * 2.0f;
        float indicatorX = innerX + indicatorInset + segmentWidth * progress;
        float indicatorY = innerY + indicatorInset;
        float indicatorHeight = innerHeight - indicatorInset * 2.0f;
        float indicatorRadius = Math.max(4.0f, outerRadius - 2.0f);

        roundRectRenderer.addRoundRect(bindModeBounds.x(), bindModeBounds.y(), bindModeBounds.width(), bindModeBounds.height(), outerRadius, MD3Theme.OUTLINE_SOFT);
        roundRectRenderer.addRoundRect(innerX, innerY, innerWidth, innerHeight, Math.max(outerRadius - shellInset, 1.0f), MD3Theme.isLightTheme() ? MD3Theme.SURFACE : MD3Theme.SURFACE_CONTAINER_HIGH);
        if (hoverProgress > 0.01f) {
            int hoverAlpha = MD3Theme.isLightTheme() ? 10 : 14;
            roundRectRenderer.addRoundRect(innerX, innerY, innerWidth, innerHeight, Math.max(outerRadius - shellInset, 1.0f), MD3Theme.withAlpha(MD3Theme.TEXT_PRIMARY, (int) (hoverAlpha * hoverProgress)));
        }

        float dividerX = innerX + segmentWidth - 0.5f;
        rectRenderer.addRect(dividerX, innerY + 3.0f, 1.0f, innerHeight - 6.0f, MD3Theme.OUTLINE_SOFT);
        roundRectRenderer.addRoundRect(indicatorX, indicatorY, indicatorWidth, indicatorHeight, indicatorRadius, MD3Theme.SECONDARY_CONTAINER);

        String toggleText = toggleComponent.getTranslatedName();
        String holdText = holdComponent.getTranslatedName();

        float toggleScale = 0.52f;
        float holdScale = 0.52f;
        float toggleWidth = textRenderer.getWidth(toggleText, toggleScale);
        float holdWidth = textRenderer.getWidth(holdText, holdScale);
        float textHeight = textRenderer.getHeight(toggleScale);
        float centerY = innerY + (innerHeight - textHeight) / 2.0f - 1.0f;
        Color inactiveText = MD3Theme.isLightTheme() ? MD3Theme.TEXT_SECONDARY : MD3Theme.TEXT_MUTED;
        textRenderer.addText(toggleText, innerX + (segmentWidth - toggleWidth) / 2.0f, centerY, toggleScale, MD3Theme.lerp(MD3Theme.ON_SECONDARY_CONTAINER, inactiveText, progress));
        textRenderer.addText(holdText, innerX + segmentWidth + (segmentWidth - holdWidth) / 2.0f, centerY, holdScale, MD3Theme.lerp(inactiveText, MD3Theme.ON_SECONDARY_CONTAINER, progress));
    }

    private void drawKeybindControl(Module module, int mouseX, int mouseY) {
        PanelLayout.Rect keybindBounds = getKeybindBounds();
        boolean listening = state.getListeningKeyBindModule() == module;
        keybindHoverAnimation.run(keybindBounds.contains(mouseX, mouseY) ? 1.0f : 0.0f);
        keybindFocusAnimation.run(listening ? 1.0f : 0.0f);
        float hoverProgress = keybindHoverAnimation.getValue();
        float focusProgress = keybindFocusAnimation.getValue();
        float radius = getKeybindControlRadius();
        float haloInset = 1.5f * focusProgress;
        if (haloInset > 0.01f) {
            roundRectRenderer.addRoundRect(keybindBounds.x() - haloInset, keybindBounds.y() - haloInset, keybindBounds.width() + haloInset * 2.0f, keybindBounds.height() + haloInset * 2.0f, radius + haloInset, MD3Theme.withAlpha(MD3Theme.PRIMARY, (int) (28 * focusProgress)));
        }

        Color background = MD3Theme.lerp(MD3Theme.SECONDARY_CONTAINER, MD3Theme.PRIMARY_CONTAINER, focusProgress);
        Color foreground = MD3Theme.lerp(MD3Theme.ON_SECONDARY_CONTAINER, MD3Theme.ON_PRIMARY_CONTAINER, focusProgress);
        roundRectRenderer.addRoundRect(keybindBounds.x(), keybindBounds.y(), keybindBounds.width(), keybindBounds.height(), radius, background);
        if (hoverProgress > 0.01f) {
            int hoverAlpha = listening ? 18 : 12;
            roundRectRenderer.addRoundRect(keybindBounds.x(), keybindBounds.y(), keybindBounds.width(), keybindBounds.height(), radius, MD3Theme.withAlpha(foreground, (int) (hoverAlpha * hoverProgress)));
        }

        String label = listening ? "..." : formatCompactKeybind(module.getKeyBind());
        float scale = label.length() >= 3 ? 0.42f : 0.5f;
        float textWidth = textRenderer.getWidth(label, scale);
        float textHeight = textRenderer.getHeight(scale);
        float textX = keybindBounds.x() + (keybindBounds.width() - textWidth) / 2.0f;
        float textY = keybindBounds.y() + (keybindBounds.height() - textHeight) / 2.0f - 1.0f;
        textRenderer.addText(label, textX, textY, scale, foreground);
    }

    private String formatCompactKeybind(int keyCode) {
        if (keyCode < 0) {
            return noneComponent.getTranslatedName();
        }
        String label = formatKeybind(keyCode).trim();
        if (label.isEmpty()) {
            return "?";
        }

        String[] parts = label.split("[^A-Za-z0-9]+");
        StringBuilder initials = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty() && Character.isLetterOrDigit(part.charAt(0))) {
                initials.append(Character.toUpperCase(part.charAt(0)));
            }
            if (initials.length() == 3) {
                break;
            }
        }
        if (initials.length() >= 2) {
            return initials.toString();
        }

        String compact = label.replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT);
        if (compact.isEmpty()) {
            return "?";
        }
        return compact.length() > 3 ? compact.substring(0, 3) : compact;
    }

    private String formatKeybind(int keyCode) {
        if (keyCode < 0) {
            return "None";
        }
        return InputConstants.Type.KEYSYM.getOrCreate(keyCode).getDisplayName().getString();
    }

    public void flushContent() {
        contentBuffer.flush();
    }

    public void markDirty() {
        contentState.markDirty();
    }

    public boolean hasActiveAnimations() {
        return contentState.hasActiveAnimations()
                || !keybindHoverAnimation.isFinished()
                || !keybindFocusAnimation.isFinished()
                || !bindModeAnimation.isFinished()
                || !bindModeHoverAnimation.isFinished();
    }

    private boolean shouldRebuildContent(PanelLayout.Rect bounds, int mouseX, int mouseY, Module module, List<Setting<?>> settings, int currentGuiHeight) {
        if (contentState.needsRebuild(bounds, mouseX, mouseY, currentGuiHeight)) {
            return true;
        }
        if (Float.compare(lastDetailScroll, state.getDetailScroll()) != 0) {
            return true;
        }
        if (!Objects.equals(lastModuleKey, module.getName() + ":" + module.getBindMode() + ":" + module.getKeyBind())) {
            return true;
        }
        List<String> visibleSettings = settings.stream().map(Setting::getName).toList();
        return !Objects.equals(lastVisibleSettings, visibleSettings);
    }

    private void rememberSnapshot(PanelLayout.Rect bounds, int mouseX, int mouseY, Module module, List<Setting<?>> settings, int currentGuiHeight) {
        contentState.rememberSnapshot(bounds, mouseX, mouseY, currentGuiHeight);
        lastDetailScroll = state.getDetailScroll();
        lastModuleKey = module.getName() + ":" + module.getBindMode() + ":" + module.getKeyBind();
        lastVisibleSettings = settings.stream().map(Setting::getName).toList();
    }

}
