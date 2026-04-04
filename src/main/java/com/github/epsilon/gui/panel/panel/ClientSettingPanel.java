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
import com.github.epsilon.gui.panel.adapter.SettingListController;
import com.github.epsilon.gui.panel.component.setting.KeybindSettingRow;
import com.github.epsilon.gui.panel.popup.PanelPopupHost;
import com.github.epsilon.gui.panel.util.PanelContentBuffer;
import com.github.epsilon.gui.panel.util.PanelContentInvalidationState;
import com.github.epsilon.gui.panel.util.ScrollBarUtil;
import com.github.epsilon.modules.impl.ClientSetting;
import com.github.epsilon.settings.Setting;
import com.github.epsilon.settings.impl.KeybindSetting;
import com.github.epsilon.utils.render.animation.Animation;
import com.github.epsilon.utils.render.animation.Easing;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ClientSettingPanel {

    protected final PanelState state;
    private final RoundRectRenderer roundRectRenderer;
    private final RectRenderer rectRenderer;
    private final TextRenderer textRenderer;
    private final SettingListController settingListController;
    private final PanelContentBuffer contentBuffer = new PanelContentBuffer();
    private final PanelContentInvalidationState contentState = new PanelContentInvalidationState();
    private PanelLayout.Rect bounds;
    private int guiHeight;
    private final Map<Setting<?>, Animation> hoverAnimations = new HashMap<>();
    private float lastScroll = Float.NaN;
    private List<String> lastVisibleSettings = List.of();
    private String lastListeningKey = "";

    private static final TranslateComponent titleComponent = TranslateComponent.create("gui", "clientsettings");

    public ClientSettingPanel(PanelState state, RoundRectRenderer roundRectRenderer, RectRenderer rectRenderer, ShadowRenderer shadowRenderer, TextRenderer textRenderer, PanelPopupHost popupHost) {
        this.state = state;
        this.roundRectRenderer = roundRectRenderer;
        this.rectRenderer = rectRenderer;
        this.textRenderer = textRenderer;
        this.settingListController = new SettingListController(popupHost);
    }

    public void render(GuiGraphicsExtractor GuiGraphicsExtractor, PanelLayout.Rect bounds, int mouseX, int mouseY, float partialTick) {
        this.bounds = bounds;
        this.guiHeight = GuiGraphicsExtractor.guiHeight();
        boolean popupConsumesHover = settingListController.isPopupHovered(mouseX, mouseY);
        int effectiveMouseX = popupConsumesHover ? Integer.MIN_VALUE : mouseX;
        int effectiveMouseY = popupConsumesHover ? Integer.MIN_VALUE : mouseY;

        textRenderer.addText(titleComponent.getTranslatedName(), bounds.x() + MD3Theme.PANEL_TITLE_INSET, bounds.y() + 10.0f, 0.78f, MD3Theme.TEXT_PRIMARY, StaticFontLoader.DUCKSANS);

        PanelLayout.Rect viewport = getViewport();
        List<Setting<?>> settings = ClientSetting.INSTANCE.getSettings().stream().filter(Setting::isAvailable).toList();
        float contentHeight = settings.size() * (28.0f + MD3Theme.ROW_GAP);
        state.setMaxClientSettingScroll(contentHeight - viewport.height());
        float maxClientScroll = Math.max(0, contentHeight - viewport.height());
        boolean hasScrollBar = maxClientScroll > 0;
        float rowWidth = hasScrollBar ? viewport.width() - ScrollBarUtil.TOTAL_WIDTH : viewport.width();

        if (shouldRebuildContent(bounds, mouseX, mouseY, settings, GuiGraphicsExtractor.guiHeight())) {
            contentBuffer.clear();
            contentState.beginRebuild();

            settingListController.layoutRows(settings, viewport, state.getClientSettingScroll(), rowWidth, (setting, row, rowBounds) -> {
                if (row instanceof KeybindSettingRow keybindRow) {
                    keybindRow.setListening(state.getListeningKeybindSetting() == keybindRow.getSetting());
                }
                Animation hoverAnimation = hoverAnimations.computeIfAbsent(setting, ignored -> new Animation(Easing.EASE_OUT_CUBIC, 120L));
                hoverAnimation.run(rowBounds.contains(effectiveMouseX, effectiveMouseY) ? 1.0f : 0.0f);
                row.render(GuiGraphicsExtractor, contentBuffer.roundRectRenderer(), contentBuffer.rectRenderer(), contentBuffer.textRenderer(), rowBounds, hoverAnimation.getValue(), effectiveMouseX, effectiveMouseY, partialTick);
                contentState.noteAnimation(!hoverAnimation.isFinished() || row.hasActiveAnimation());
            });

            rememberSnapshot(bounds, mouseX, mouseY, settings, GuiGraphicsExtractor.guiHeight());
        }

        contentBuffer.queueViewport(viewport, guiHeight, state.getClientSettingScroll(), maxClientScroll, contentHeight);
    }

    public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
        if (bounds == null || event.button() != 0) {
            return false;
        }

        if (state.getListeningKeybindSetting() != null) {
            state.setListeningKeybindSetting(null);
            markDirty();
        }

        if (settingListController.mouseClicked(event, isDoubleClick, bounds, (row, rowBounds, clickEvent, doubleClick) -> {
            if (row instanceof KeybindSettingRow keybindRow && row.mouseClicked(rowBounds, clickEvent, doubleClick)) {
                state.setListeningKeybindSetting(keybindRow.getSetting());
                return true;
            }
            return false;
        })) {
            markDirty();
            return true;
        }
        return false;
    }

    public boolean mouseReleased(MouseButtonEvent event) {
        if (settingListController.mouseReleased(event)) {
            markDirty();
            return true;
        }
        return false;
    }

    public boolean mouseDragged(MouseButtonEvent event, double mouseX, double mouseY) {
        if (settingListController.mouseDragged(event, mouseX, mouseY)) {
            markDirty();
            return true;
        }
        return false;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        PanelLayout.Rect viewport = getViewport();
        if (bounds != null && viewport.contains(mouseX, mouseY)) {
            state.scrollClientSetting(-scrollY * 20.0f);
            markDirty();
            return true;
        }
        return false;
    }

    public boolean keyPressed(KeyEvent event) {
        KeybindSetting listening = state.getListeningKeybindSetting();
        if (listening != null) {
            if (event.key() == 256) {
                state.setListeningKeybindSetting(null);
                markDirty();
                return true;
            }
            if (event.key() == 259 || event.key() == 261) {
                listening.setValue(-1);
                state.setListeningKeybindSetting(null);
                markDirty();
                return true;
            }
            listening.setValue(event.key());
            state.setListeningKeybindSetting(null);
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
        return new PanelLayout.Rect(bounds.x() + MD3Theme.PANEL_VIEWPORT_INSET, bounds.y() + 34.0f, bounds.width() - MD3Theme.PANEL_VIEWPORT_INSET * 2.0f, bounds.height() - 40.0f);
    }

    public void flushContent() {
        contentBuffer.flush();
    }

    public void markDirty() {
        contentState.markDirty();
    }

    public boolean hasActiveAnimations() {
        return contentState.hasActiveAnimations();
    }

    private boolean shouldRebuildContent(PanelLayout.Rect bounds, int mouseX, int mouseY, List<Setting<?>> settings, int currentGuiHeight) {
        if (contentState.needsRebuild(bounds, mouseX, mouseY, currentGuiHeight)) {
            return true;
        }
        if (Float.compare(lastScroll, state.getClientSettingScroll()) != 0) {
            return true;
        }
        String listeningKey = state.getListeningKeybindSetting() == null ? "" : state.getListeningKeybindSetting().getName();
        if (!Objects.equals(lastListeningKey, listeningKey)) {
            return true;
        }
        List<String> visibleSettings = settings.stream().map(Setting::getName).toList();
        return !Objects.equals(lastVisibleSettings, visibleSettings);
    }

    private void rememberSnapshot(PanelLayout.Rect bounds, int mouseX, int mouseY, List<Setting<?>> settings, int currentGuiHeight) {
        contentState.rememberSnapshot(bounds, mouseX, mouseY, currentGuiHeight);
        lastScroll = state.getClientSettingScroll();
        lastListeningKey = state.getListeningKeybindSetting() == null ? "" : state.getListeningKeybindSetting().getName();
        lastVisibleSettings = settings.stream().map(Setting::getName).toList();
    }

}


