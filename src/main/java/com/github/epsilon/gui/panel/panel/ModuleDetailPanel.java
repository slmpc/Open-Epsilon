package com.github.epsilon.gui.panel.panel;

import com.github.epsilon.assets.i18n.TranslateComponent;
import com.github.epsilon.graphics.renderers.RectRenderer;
import com.github.epsilon.graphics.renderers.RoundRectRenderer;
import com.github.epsilon.graphics.renderers.ShadowRenderer;
import com.github.epsilon.graphics.renderers.TextRenderer;
import com.github.epsilon.graphics.text.StaticFontLoader;
import com.github.epsilon.gui.panel.MD3Theme;
import com.github.epsilon.gui.panel.PanelLayout;
import com.github.epsilon.gui.panel.PanelState;
import com.github.epsilon.gui.panel.adapter.SettingViewFactory;
import com.github.epsilon.gui.panel.component.SettingRow;
import com.github.epsilon.gui.panel.component.setting.ColorSettingRow;
import com.github.epsilon.gui.panel.component.setting.DoubleSettingRow;
import com.github.epsilon.gui.panel.component.setting.EnumSettingRow;
import com.github.epsilon.gui.panel.component.setting.IntSettingRow;
import com.github.epsilon.gui.panel.popup.ColorPickerPopup;
import com.github.epsilon.gui.panel.popup.EnumSelectPopup;
import com.github.epsilon.gui.panel.popup.PanelPopupHost;
import com.github.epsilon.gui.panel.util.PanelScissor;
import com.github.epsilon.modules.Module;
import com.github.epsilon.settings.Setting;
import com.github.epsilon.utils.render.animation.Animation;
import com.github.epsilon.utils.render.animation.Easing;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.language.I18n;

import java.awt.*;
import java.util.*;
import java.util.List;

public class ModuleDetailPanel {

    protected final PanelState state;
    private final RoundRectRenderer roundRectRenderer;
    private final RectRenderer rectRenderer;
    private final TextRenderer textRenderer;
    private final PanelPopupHost popupHost;
    private final RoundRectRenderer contentRoundRectRenderer = new RoundRectRenderer();
    private final RectRenderer contentRectRenderer = new RectRenderer();
    private final ShadowRenderer contentShadowRenderer = new ShadowRenderer();
    private final TextRenderer contentTextRenderer = new TextRenderer();
    private PanelLayout.Rect bounds;
    private int guiHeight;
    private PanelLayout.Rect headerBounds;
    private final List<SettingEntry> settingEntries = new ArrayList<>();
    private final Map<Setting<?>, Animation> hoverAnimations = new HashMap<>();
    private final Map<Setting<?>, SettingRow<?>> rowCache = new HashMap<>();
    private SettingEntry draggingSliderEntry;
    private boolean contentPending;
    private boolean contentDirty = true;
    private int lastMouseX = Integer.MIN_VALUE;
    private int lastMouseY = Integer.MIN_VALUE;
    private float lastDetailScroll = Float.NaN;
    private String lastModuleKey = "";
    private int lastGuiHeight = -1;
    private PanelLayout.Rect lastBounds;
    private List<String> lastVisibleSettings = List.of();
    private boolean hasActiveContentAnimations;
    private final Animation bindModeAnimation = new Animation(Easing.EASE_OUT_CUBIC, 180L);
    private final Animation bindModeHoverAnimation = new Animation(Easing.EASE_OUT_CUBIC, 120L);
    private final Animation keybindHoverAnimation = new Animation(Easing.EASE_OUT_CUBIC, 120L);
    private final Animation keybindFocusAnimation = new Animation(Easing.EASE_OUT_CUBIC, 150L);

    private static final TranslateComponent toggleComponent = TranslateComponent.create("keybind", "toggle");
    private static final TranslateComponent holdComponent = TranslateComponent.create("keybind", "hold");
    private static final TranslateComponent noneComponent = TranslateComponent.create("keybind", "none");

    public ModuleDetailPanel(PanelState state, RoundRectRenderer roundRectRenderer, RectRenderer rectRenderer, ShadowRenderer shadowRenderer, TextRenderer textRenderer, PanelPopupHost popupHost) {
        this.state = state;
        this.roundRectRenderer = roundRectRenderer;
        this.rectRenderer = rectRenderer;
        this.textRenderer = textRenderer;
        this.popupHost = popupHost;
        this.bindModeAnimation.setStartValue(0.0f);
        this.bindModeHoverAnimation.setStartValue(0.0f);
        this.keybindHoverAnimation.setStartValue(0.0f);
        this.keybindFocusAnimation.setStartValue(0.0f);
    }

    public void render(GuiGraphicsExtractor GuiGraphicsExtractor, PanelLayout.Rect bounds, int mouseX, int mouseY, float partialTick) {
        this.bounds = bounds;
        this.guiHeight = GuiGraphicsExtractor.guiHeight();
        boolean popupConsumesHover = popupHost.getActivePopup() != null && popupHost.getActivePopup().getBounds().contains(mouseX, mouseY);
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
        rowCache.keySet().removeIf(setting -> !settings.contains(setting));
        float contentHeight = settings.size() * (28.0f + MD3Theme.ROW_GAP);
        state.setMaxDetailScroll(contentHeight - viewport.height());

        if (shouldRebuildContent(bounds, mouseX, mouseY, module, settings, GuiGraphicsExtractor.guiHeight())) {
            settingEntries.clear();
            contentRoundRectRenderer.clear();
            contentRectRenderer.clear();
            contentShadowRenderer.clear();
            contentTextRenderer.clear();
            hasActiveContentAnimations = false;

            float y = viewport.y() - state.getDetailScroll();
            for (Setting<?> setting : settings) {
                SettingRow<?> row = rowCache.computeIfAbsent(setting, SettingViewFactory::create);
                if (row == null) {
                    continue;
                }
                PanelLayout.Rect rowBounds = new PanelLayout.Rect(viewport.x(), y, viewport.width(), row.getHeight());
                settingEntries.add(new SettingEntry(row, rowBounds));
                Animation hoverAnimation = hoverAnimations.computeIfAbsent(setting, ignored -> new Animation(Easing.EASE_OUT_CUBIC, 120L));
                hoverAnimation.run(rowBounds.contains(effectiveMouseX, effectiveMouseY) ? 1.0f : 0.0f);
                row.render(GuiGraphicsExtractor, contentRoundRectRenderer, contentRectRenderer, contentTextRenderer, rowBounds, hoverAnimation.getValue(), effectiveMouseX, effectiveMouseY, partialTick);
                hasActiveContentAnimations = hasActiveContentAnimations || !hoverAnimation.isFinished() || row.hasActiveAnimation();
                y += row.getHeight() + MD3Theme.ROW_GAP;
            }

            rememberSnapshot(bounds, mouseX, mouseY, module, settings, GuiGraphicsExtractor.guiHeight());
            contentDirty = false;
        }

        PanelScissor.apply(viewport, contentRectRenderer, contentRoundRectRenderer, contentShadowRenderer, contentTextRenderer, guiHeight);
        contentPending = true;
    }

    public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
        if (bounds == null || event.button() != 0) {
            return false;
        }
        Module module = state.getSelectedModule();
        if (module == null || headerBounds == null) {
            return false;
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

        clearRowFocus();
        for (SettingEntry entry : settingEntries) {
            if (entry.row instanceof IntSettingRow intRow && intRow.mouseClicked(entry.bounds, event, isDoubleClick)) {
                if (intRow.isDragging()) {
                    draggingSliderEntry = entry;
                    intRow.updateFromMouse(entry.bounds, event.x());
                } else {
                    draggingSliderEntry = null;
                }
                markDirty();
                return true;
            }
            if (entry.row instanceof DoubleSettingRow doubleRow && doubleRow.mouseClicked(entry.bounds, event, isDoubleClick)) {
                if (doubleRow.isDragging()) {
                    draggingSliderEntry = entry;
                    doubleRow.updateFromMouse(entry.bounds, event.x());
                } else {
                    draggingSliderEntry = null;
                }
                markDirty();
                return true;
            }
            if (entry.row instanceof EnumSettingRow enumRow && entry.row.mouseClicked(entry.bounds, event, isDoubleClick)) {
                popupHost.open(createEnumPopup(enumRow, entry.bounds));
                markDirty();
                return true;
            }
            if (entry.row instanceof ColorSettingRow colorRow && entry.row.mouseClicked(entry.bounds, event, isDoubleClick)) {
                popupHost.open(createColorPopup(colorRow, entry.bounds));
                markDirty();
                return true;
            }
            if (entry.row.mouseClicked(entry.bounds, event, isDoubleClick)) {
                markDirty();
                return true;
            }
        }
        return false;
    }

    public boolean mouseReleased(MouseButtonEvent event) {
        if (draggingSliderEntry != null) {
            draggingSliderEntry.row.mouseReleased(draggingSliderEntry.bounds, event);
        }
        draggingSliderEntry = null;
        markDirty();
        return false;
    }

    public boolean mouseDragged(MouseButtonEvent event, double mouseX, double mouseY) {
        if (draggingSliderEntry == null || event.button() != 0) {
            return false;
        }
        double currentMouseX = event.x();
        if (draggingSliderEntry.row instanceof IntSettingRow intRow) {
            intRow.updateFromMouse(draggingSliderEntry.bounds, currentMouseX);
            markDirty();
            return true;
        }
        if (draggingSliderEntry.row instanceof DoubleSettingRow doubleRow) {
            doubleRow.updateFromMouse(draggingSliderEntry.bounds, currentMouseX);
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
        for (SettingEntry entry : settingEntries) {
            if (entry.row.keyPressed(event)) {
                markDirty();
                return true;
            }
        }
        return false;
    }

    public boolean charTyped(CharacterEvent event) {
        for (SettingEntry entry : settingEntries) {
            if (entry.row.charTyped(event)) {
                markDirty();
                return true;
            }
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

    private record SettingEntry(SettingRow<?> row, PanelLayout.Rect bounds) {
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

    private EnumSelectPopup createEnumPopup(EnumSettingRow enumRow, PanelLayout.Rect rowBounds) {
        PanelLayout.Rect chipBounds = enumRow.getChipBounds(textRenderer, rowBounds);
        int optionCount = enumRow.getSetting().getModes().length;
        float popupHeight = optionCount * 24.0f + 12.0f;
        float popupWidth = Math.max(108.0f, chipBounds.width() + 24.0f);
        float popupX = Math.max(bounds.x() + MD3Theme.PANEL_VIEWPORT_INSET, chipBounds.right() - popupWidth);
        float popupY = chipBounds.bottom() + 4.0f;
        float maxBottom = bounds.bottom() - MD3Theme.PANEL_VIEWPORT_INSET;
        if (popupY + popupHeight > maxBottom) {
            popupY = chipBounds.y() - popupHeight - 4.0f;
        }
        return new EnumSelectPopup(new PanelLayout.Rect(popupX, popupY, popupWidth, popupHeight), chipBounds, enumRow.getSetting());
    }

    private ColorPickerPopup createColorPopup(ColorSettingRow colorRow, PanelLayout.Rect rowBounds) {
        PanelLayout.Rect swatchBounds = colorRow.getSwatchBounds(rowBounds);
        int channelCount = colorRow.getSetting().isAllowAlpha() ? 4 : 3;
        float popupWidth = 156.0f;
        float popupHeight = 58.0f + channelCount * 24.0f;
        float popupX = Math.max(bounds.x() + MD3Theme.PANEL_VIEWPORT_INSET, swatchBounds.right() - popupWidth);
        float popupY = swatchBounds.bottom() + 4.0f;
        float maxBottom = bounds.bottom() - MD3Theme.PANEL_VIEWPORT_INSET;
        if (popupY + popupHeight > maxBottom) {
            popupY = swatchBounds.y() - popupHeight - 4.0f;
        }
        return new ColorPickerPopup(new PanelLayout.Rect(popupX, popupY, popupWidth, popupHeight), swatchBounds, colorRow.getSetting());
    }

    public void flushContent() {
        if (!contentPending) {
            return;
        }
        contentShadowRenderer.draw();
        contentRoundRectRenderer.draw();
        contentRectRenderer.draw();
        contentTextRenderer.draw();
        PanelScissor.clear(contentRectRenderer, contentRoundRectRenderer, contentShadowRenderer, contentTextRenderer);
        contentPending = false;
    }

    public void markDirty() {
        contentDirty = true;
    }

    public boolean hasActiveAnimations() {
        return hasActiveContentAnimations
                || !keybindHoverAnimation.isFinished()
                || !keybindFocusAnimation.isFinished()
                || !bindModeAnimation.isFinished()
                || !bindModeHoverAnimation.isFinished();
    }

    private void clearRowFocus() {
        for (SettingRow<?> row : rowCache.values()) {
            row.setFocused(false);
        }
    }

    private boolean shouldRebuildContent(PanelLayout.Rect bounds, int mouseX, int mouseY, Module module, List<Setting<?>> settings, int currentGuiHeight) {
        if (contentDirty) {
            return true;
        }
        if (hasActiveContentAnimations) {
            return true;
        }
        if (lastBounds == null || !sameRect(lastBounds, bounds)) {
            return true;
        }
        if (lastGuiHeight != currentGuiHeight) {
            return true;
        }
        if (lastMouseX != mouseX || lastMouseY != mouseY) {
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
        lastBounds = bounds;
        lastMouseX = mouseX;
        lastMouseY = mouseY;
        lastDetailScroll = state.getDetailScroll();
        lastModuleKey = module.getName() + ":" + module.getBindMode() + ":" + module.getKeyBind();
        lastVisibleSettings = settings.stream().map(Setting::getName).toList();
        lastGuiHeight = currentGuiHeight;
    }

    private boolean sameRect(PanelLayout.Rect a, PanelLayout.Rect b) {
        return Float.compare(a.x(), b.x()) == 0
                && Float.compare(a.y(), b.y()) == 0
                && Float.compare(a.width(), b.width()) == 0
                && Float.compare(a.height(), b.height()) == 0;
    }

}
