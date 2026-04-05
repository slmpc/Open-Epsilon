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
import com.github.epsilon.gui.panel.component.setting.KeybindSettingRow;
import com.github.epsilon.gui.panel.popup.PanelPopupHost;
import com.github.epsilon.gui.panel.util.PanelContentBuffer;
import com.github.epsilon.gui.panel.util.PanelContentInvalidationState;
import com.github.epsilon.gui.panel.util.ScrollBarDragState;
import com.github.epsilon.gui.panel.util.ScrollBarUtil;
import com.github.epsilon.managers.ConfigManager;
import com.github.epsilon.managers.FriendManager;
import com.github.epsilon.modules.impl.ClientSetting;
import com.github.epsilon.settings.Setting;
import com.github.epsilon.settings.impl.KeybindSetting;
import com.github.epsilon.utils.render.animation.Animation;
import com.github.epsilon.utils.render.animation.Easing;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.util.*;
import java.util.List;

public class ClientSettingPanel {

    protected final PanelState state;
    private final RoundRectRenderer roundRectRenderer;
    private final RectRenderer rectRenderer;
    private final TextRenderer textRenderer;
    private final SettingListController settingListController;
    private final PanelContentBuffer contentBuffer = new PanelContentBuffer();
    private final PanelContentInvalidationState contentState = new PanelContentInvalidationState();
    private final PanelContentBuffer friendContentBuffer = new PanelContentBuffer();
    private final PanelContentInvalidationState friendContentState = new PanelContentInvalidationState();
    private PanelLayout.Rect bounds;
    private int guiHeight;

    // General tab state
    private final Map<Setting<?>, Animation> hoverAnimations = new HashMap<>();
    private float lastScroll = Float.NaN;
    private List<String> lastVisibleSettings = List.of();
    private String lastListeningKey = "";

    // Tab animations
    private final Animation tabIndicatorAnimation = new Animation(Easing.EASE_OUT_CUBIC, 200L);
    private final Animation generalTabHoverAnimation = new Animation(Easing.EASE_OUT_CUBIC, 120L);
    private final Animation friendTabHoverAnimation = new Animation(Easing.EASE_OUT_CUBIC, 120L);

    // Friend tab state
    private final Map<String, Animation> friendHoverAnimations = new HashMap<>();
    private final Map<String, Animation> friendRemoveHoverAnimations = new HashMap<>();
    private float lastFriendScroll = Float.NaN;
    private List<String> lastFriendList = List.of();
    private boolean friendInputFocused;
    private String friendInputBuffer = "";
    private int friendInputCursor;
    private final Animation friendInputHoverAnimation = new Animation(Easing.EASE_OUT_CUBIC, 120L);
    private final Animation friendInputFocusAnimation = new Animation(Easing.EASE_OUT_CUBIC, 120L);
    private PanelState.ClientSettingTab lastTab = PanelState.ClientSettingTab.GENERAL;

    // Scrollbar drag state
    private final ScrollBarDragState generalScrollBarDrag = new ScrollBarDragState();
    private final ScrollBarDragState friendScrollBarDrag = new ScrollBarDragState();

    // Friend list row layout cache
    private final List<FriendRowEntry> friendRowEntries = new ArrayList<>();

    private static final TranslateComponent titleComponent = EpsilonTranslateComponent.create("gui", "clientsettings");
    private static final TranslateComponent generalTabComponent = EpsilonTranslateComponent.create("gui", "tab.general");
    private static final TranslateComponent friendTabComponent = EpsilonTranslateComponent.create("gui", "tab.friend");
    private static final TranslateComponent noFriendsComponent = EpsilonTranslateComponent.create("gui", "friend.empty");
    private static final TranslateComponent addFriendPlaceholderComponent = EpsilonTranslateComponent.create("gui", "friend.input.placeholder");

    private static final float TAB_BAR_HEIGHT = 26.0f;
    private static final float TAB_INDICATOR_HEIGHT = 2.5f;
    private static final float FRIEND_ROW_HEIGHT = 30.0f;
    private static final float FRIEND_INPUT_HEIGHT = 28.0f;
    private static final float FRIEND_INPUT_BOTTOM_MARGIN = 4.0f;
    private static final float FRIEND_INPUT_FIELD_SCALE = 0.60f;
    private static final int MAX_FRIEND_NAME_LENGTH = 32;

    public ClientSettingPanel(PanelState state, RoundRectRenderer roundRectRenderer, RectRenderer rectRenderer, ShadowRenderer shadowRenderer, TextRenderer textRenderer, PanelPopupHost popupHost) {
        this.state = state;
        this.roundRectRenderer = roundRectRenderer;
        this.rectRenderer = rectRenderer;
        this.textRenderer = textRenderer;
        this.settingListController = new SettingListController(popupHost);
        tabIndicatorAnimation.setStartValue(0.0f);
        generalTabHoverAnimation.setStartValue(0.0f);
        friendTabHoverAnimation.setStartValue(0.0f);
        friendInputHoverAnimation.setStartValue(0.0f);
        friendInputFocusAnimation.setStartValue(0.0f);
    }

    public void render(GuiGraphicsExtractor GuiGraphicsExtractor, PanelLayout.Rect bounds, int mouseX, int mouseY, float partialTick) {
        this.bounds = bounds;
        this.guiHeight = GuiGraphicsExtractor.guiHeight();
        boolean popupConsumesHover = settingListController.isPopupHovered(mouseX, mouseY);
        int effectiveMouseX = popupConsumesHover ? Integer.MIN_VALUE : mouseX;
        int effectiveMouseY = popupConsumesHover ? Integer.MIN_VALUE : mouseY;

        // Title
        textRenderer.addText(titleComponent.getTranslatedName(), bounds.x() + MD3Theme.PANEL_TITLE_INSET, bounds.y() + 10.0f, 0.78f, MD3Theme.TEXT_PRIMARY, StaticFontLoader.DUCKSANS);

        // Secondary Tabs
        renderTabs(effectiveMouseX, effectiveMouseY);

        PanelState.ClientSettingTab currentTab = state.getClientSettingTab();
        if (currentTab == PanelState.ClientSettingTab.GENERAL) {
            renderGeneralTab(GuiGraphicsExtractor, effectiveMouseX, effectiveMouseY, partialTick);
        } else {
            renderFriendTab(GuiGraphicsExtractor, effectiveMouseX, effectiveMouseY, partialTick);
        }
    }

    // ─── MD3 Secondary Tabs ─────────────────────────────────────────────

    private void renderTabs(int mouseX, int mouseY) {
        PanelLayout.Rect tabBar = getTabBarRect();
        float halfWidth = tabBar.width() / 2.0f;
        PanelLayout.Rect generalTab = new PanelLayout.Rect(tabBar.x(), tabBar.y(), halfWidth, tabBar.height());
        PanelLayout.Rect friendTab = new PanelLayout.Rect(tabBar.x() + halfWidth, tabBar.y(), halfWidth, tabBar.height());

        boolean isGeneral = state.getClientSettingTab() == PanelState.ClientSettingTab.GENERAL;
        tabIndicatorAnimation.run(isGeneral ? 0.0f : 1.0f);
        generalTabHoverAnimation.run(generalTab.contains(mouseX, mouseY) ? 1.0f : 0.0f);
        friendTabHoverAnimation.run(friendTab.contains(mouseX, mouseY) ? 1.0f : 0.0f);

        float generalHover = generalTabHoverAnimation.getValue();
        float friendHover = friendTabHoverAnimation.getValue();

        // Tab hover ripple backgrounds
        if (generalHover > 0.01f) {
            roundRectRenderer.addRoundRect(generalTab.x(), generalTab.y(), generalTab.width(), generalTab.height(), 6.0f,
                    MD3Theme.withAlpha(MD3Theme.TEXT_PRIMARY, (int) (8 * generalHover)));
        }
        if (friendHover > 0.01f) {
            roundRectRenderer.addRoundRect(friendTab.x(), friendTab.y(), friendTab.width(), friendTab.height(), 6.0f,
                    MD3Theme.withAlpha(MD3Theme.TEXT_PRIMARY, (int) (8 * friendHover)));
        }

        // Tab labels
        float labelScale = 0.62f;
        String generalLabel = generalTabComponent.getTranslatedName();
        String friendLabel = friendTabComponent.getTranslatedName();

        Color generalColor = isGeneral ? MD3Theme.PRIMARY : MD3Theme.TEXT_MUTED;
        Color friendColor = !isGeneral ? MD3Theme.PRIMARY : MD3Theme.TEXT_MUTED;

        float generalTextWidth = textRenderer.getWidth(generalLabel, labelScale);
        float friendTextWidth = textRenderer.getWidth(friendLabel, labelScale);
        float textHeight = textRenderer.getHeight(labelScale);
        float generalTextX = generalTab.x() + (generalTab.width() - generalTextWidth) / 2.0f;
        float friendTextX = friendTab.x() + (friendTab.width() - friendTextWidth) / 2.0f;
        float textY = tabBar.y() + (tabBar.height() - TAB_INDICATOR_HEIGHT - textHeight) / 2.0f - 1.0f;

        textRenderer.addText(generalLabel, generalTextX, textY, labelScale, generalColor);
        textRenderer.addText(friendLabel, friendTextX, textY, labelScale, friendColor);

        // Bottom divider line
        float dividerY = tabBar.bottom() - 1.0f;
        rectRenderer.addRect(tabBar.x(), dividerY, tabBar.width(), 1.0f, MD3Theme.withAlpha(MD3Theme.OUTLINE, 40));

        // Active indicator (animated slide)
        float indicatorProgress = tabIndicatorAnimation.getValue();
        float indicatorWidth = 48.0f;
        float generalIndicatorX = generalTab.x() + (generalTab.width() - indicatorWidth) / 2.0f;
        float friendIndicatorX = friendTab.x() + (friendTab.width() - indicatorWidth) / 2.0f;
        float indicatorX = generalIndicatorX + (friendIndicatorX - generalIndicatorX) * indicatorProgress;
        float indicatorY = tabBar.bottom() - TAB_INDICATOR_HEIGHT;
        roundRectRenderer.addRoundRect(indicatorX, indicatorY, indicatorWidth, TAB_INDICATOR_HEIGHT,
                TAB_INDICATOR_HEIGHT / 2.0f, MD3Theme.PRIMARY);
    }

    private PanelLayout.Rect getTabBarRect() {
        return new PanelLayout.Rect(
                bounds.x() + MD3Theme.PANEL_VIEWPORT_INSET,
                bounds.y() + 28.0f,
                bounds.width() - MD3Theme.PANEL_VIEWPORT_INSET * 2.0f,
                TAB_BAR_HEIGHT
        );
    }

    // ─── General Tab (existing client settings) ─────────────────────────

    private void renderGeneralTab(GuiGraphicsExtractor GuiGraphicsExtractor, int mouseX, int mouseY, float partialTick) {
        PanelLayout.Rect viewport = getGeneralViewport();
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
                hoverAnimation.run(rowBounds.contains(mouseX, mouseY) ? 1.0f : 0.0f);
                row.render(GuiGraphicsExtractor, contentBuffer.roundRectRenderer(), contentBuffer.rectRenderer(), contentBuffer.textRenderer(), rowBounds, hoverAnimation.getValue(), mouseX, mouseY, partialTick);
                contentState.noteAnimation(!hoverAnimation.isFinished() || row.hasActiveAnimation());
            });

            rememberSnapshot(bounds, mouseX, mouseY, settings, GuiGraphicsExtractor.guiHeight());
        }

        contentBuffer.queueViewport(viewport, guiHeight, state.getClientSettingScroll(), maxClientScroll, contentHeight);
    }

    // ─── Friend Tab ─────────────────────────────────────────────────────

    private void renderFriendTab(GuiGraphicsExtractor GuiGraphicsExtractor, int mouseX, int mouseY, float partialTick) {
        PanelLayout.Rect fullViewport = getFriendFullViewport();

        // Input field at the bottom
        PanelLayout.Rect inputBounds = getFriendInputBounds(fullViewport);
        renderFriendInput(inputBounds, mouseX, mouseY);

        // Friend list viewport (above the input)
        PanelLayout.Rect listViewport = new PanelLayout.Rect(
                fullViewport.x(), fullViewport.y(),
                fullViewport.width(),
                fullViewport.height() - FRIEND_INPUT_HEIGHT - FRIEND_INPUT_BOTTOM_MARGIN * 2
        );

        List<String> friends = FriendManager.INSTANCE.getFriends().stream().sorted(String.CASE_INSENSITIVE_ORDER).toList();
        float contentHeight = friends.size() * (FRIEND_ROW_HEIGHT + MD3Theme.ROW_GAP);
        state.setMaxFriendScroll(contentHeight - listViewport.height());
        float maxFriendScroll = Math.max(0, contentHeight - listViewport.height());
        boolean hasScrollBar = maxFriendScroll > 0;
        float rowWidth = hasScrollBar ? listViewport.width() - ScrollBarUtil.TOTAL_WIDTH : listViewport.width();

        if (shouldRebuildFriendContent(bounds, mouseX, mouseY, friends, GuiGraphicsExtractor.guiHeight())) {
            friendContentBuffer.clear();
            friendContentState.beginRebuild();
            friendRowEntries.clear();

            // Clean up animations for removed friends
            friendHoverAnimations.keySet().removeIf(name -> !friends.contains(name));
            friendRemoveHoverAnimations.keySet().removeIf(name -> !friends.contains(name));

            float rowY = listViewport.y() - state.getFriendScroll();
            for (String friendName : friends) {
                PanelLayout.Rect rowBounds = new PanelLayout.Rect(listViewport.x(), rowY, rowWidth, FRIEND_ROW_HEIGHT);
                PanelLayout.Rect removeBounds = getRemoveButtonBounds(rowBounds);
                friendRowEntries.add(new FriendRowEntry(friendName, rowBounds, removeBounds));

                Animation hoverAnim = friendHoverAnimations.computeIfAbsent(friendName, k -> new Animation(Easing.EASE_OUT_CUBIC, 120L));
                Animation removeHoverAnim = friendRemoveHoverAnimations.computeIfAbsent(friendName, k -> new Animation(Easing.EASE_OUT_CUBIC, 120L));
                hoverAnim.run(rowBounds.contains(mouseX, mouseY) ? 1.0f : 0.0f);
                removeHoverAnim.run(removeBounds.contains(mouseX, mouseY) ? 1.0f : 0.0f);
                friendContentState.noteAnimation(!hoverAnim.isFinished() || !removeHoverAnim.isFinished());

                renderFriendRow(friendName, rowBounds, removeBounds, hoverAnim.getValue(), removeHoverAnim.getValue());
                rowY += FRIEND_ROW_HEIGHT + MD3Theme.ROW_GAP;
            }

            // Empty state hint
            if (friends.isEmpty()) {
                float hintScale = 0.58f;
                String hint = noFriendsComponent.getTranslatedName();
                float hintWidth = friendContentBuffer.textRenderer().getWidth(hint, hintScale);
                float hintX = listViewport.x() + (listViewport.width() - hintWidth) / 2.0f;
                float hintY = listViewport.y() + listViewport.height() / 2.0f - friendContentBuffer.textRenderer().getHeight(hintScale) / 2.0f;
                friendContentBuffer.textRenderer().addText(hint, hintX, hintY, hintScale, MD3Theme.TEXT_MUTED);
            }

            rememberFriendSnapshot(bounds, mouseX, mouseY, friends, GuiGraphicsExtractor.guiHeight());
        }

        friendContentBuffer.queueViewport(listViewport, guiHeight, state.getFriendScroll(), maxFriendScroll, contentHeight);
    }

    private void renderFriendRow(String name, PanelLayout.Rect bounds, PanelLayout.Rect removeBounds, float hoverProgress, float removeHoverProgress) {
        RoundRectRenderer rr = friendContentBuffer.roundRectRenderer();
        TextRenderer tr = friendContentBuffer.textRenderer();

        // Row background
        rr.addRoundRect(bounds.x(), bounds.y(), bounds.width(), bounds.height(), MD3Theme.CARD_RADIUS,
                MD3Theme.lerp(MD3Theme.SURFACE_CONTAINER, MD3Theme.SURFACE_CONTAINER_HIGH, hoverProgress));

        // Player icon placeholder (circle with first letter)
        float avatarSize = 20.0f;
        float avatarX = bounds.x() + MD3Theme.ROW_CONTENT_INSET + 2.0f;
        float avatarY = bounds.y() + (bounds.height() - avatarSize) / 2.0f;
        rr.addRoundRect(avatarX, avatarY, avatarSize, avatarSize, avatarSize / 2.0f, MD3Theme.SECONDARY_CONTAINER);
        String initial = name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase();
        float initialScale = 0.54f;
        float initialWidth = tr.getWidth(initial, initialScale);
        float initialHeight = tr.getHeight(initialScale);
        tr.addText(initial, avatarX + (avatarSize - initialWidth) / 2.0f, avatarY + (avatarSize - initialHeight) / 2.0f - 1.0f, initialScale, MD3Theme.ON_SECONDARY_CONTAINER);

        // Name
        float nameScale = 0.66f;
        float nameX = avatarX + avatarSize + 8.0f;
        float nameY = bounds.y() + (bounds.height() - tr.getHeight(nameScale)) / 2.0f - 1.0f;
        tr.addText(name, nameX, nameY, nameScale, MD3Theme.TEXT_PRIMARY);

        // Remove button
        rr.addRoundRect(removeBounds.x(), removeBounds.y(), removeBounds.width(), removeBounds.height(),
                removeBounds.height() / 2.0f,
                MD3Theme.lerp(MD3Theme.withAlpha(MD3Theme.ERROR, 0), MD3Theme.withAlpha(MD3Theme.ERROR, 32), removeHoverProgress));
        String removeIcon = "✕";
        float removeScale = 0.50f;
        float removeTextWidth = tr.getWidth(removeIcon, removeScale);
        float removeTextHeight = tr.getHeight(removeScale);
        Color removeColor = MD3Theme.lerp(MD3Theme.TEXT_MUTED, MD3Theme.ERROR, removeHoverProgress);
        tr.addText(removeIcon, removeBounds.x() + (removeBounds.width() - removeTextWidth) / 2.0f,
                removeBounds.y() + (removeBounds.height() - removeTextHeight) / 2.0f - 1.0f, removeScale, removeColor);
    }

    private PanelLayout.Rect getRemoveButtonBounds(PanelLayout.Rect rowBounds) {
        float btnSize = 20.0f;
        return new PanelLayout.Rect(
                rowBounds.right() - MD3Theme.ROW_TRAILING_INSET - btnSize,
                rowBounds.y() + (rowBounds.height() - btnSize) / 2.0f,
                btnSize, btnSize
        );
    }

    private void renderFriendInput(PanelLayout.Rect inputBounds, int mouseX, int mouseY) {
        boolean hovered = inputBounds.contains(mouseX, mouseY);
        friendInputHoverAnimation.run(hovered ? 1.0f : 0.0f);
        friendInputFocusAnimation.run(friendInputFocused ? 1.0f : 0.0f);
        float hoverProgress = friendInputHoverAnimation.getValue();
        float focusProgress = friendInputFocusAnimation.getValue();

        // Background
        Color fieldBase = MD3Theme.isLightTheme() ? MD3Theme.SURFACE_CONTAINER : MD3Theme.SURFACE_CONTAINER_LOW;
        Color fieldHover = MD3Theme.SURFACE_CONTAINER_HIGHEST;
        Color fieldColor = friendInputFocused
                ? MD3Theme.lerp(MD3Theme.SURFACE_CONTAINER_HIGH, MD3Theme.SURFACE_CONTAINER_HIGHEST, 0.5f)
                : MD3Theme.lerp(fieldBase, fieldHover, hoverProgress * 0.6f);
        roundRectRenderer.addRoundRect(inputBounds.x(), inputBounds.y(), inputBounds.width(), inputBounds.height(), 9.0f, fieldColor);

        // Focus outline
        if (focusProgress > 0.01f) {
            roundRectRenderer.addRoundRect(inputBounds.x() - 1, inputBounds.y() - 1,
                    inputBounds.width() + 2, inputBounds.height() + 2, 10.0f,
                    MD3Theme.withAlpha(MD3Theme.PRIMARY, (int) (48 * focusProgress)));
            roundRectRenderer.addRoundRect(inputBounds.x(), inputBounds.y(),
                    inputBounds.width(), inputBounds.height(), 9.0f, fieldColor);
        }

        // Text
        float textInset = 10.0f;
        float textScale = FRIEND_INPUT_FIELD_SCALE;
        float textHeight = textRenderer.getHeight(textScale);
        float textY = inputBounds.y() + (inputBounds.height() - textHeight) / 2.0f - 1.0f;
        float textX = inputBounds.x() + textInset;

        boolean showPlaceholder = friendInputBuffer.isEmpty() && !friendInputFocused;
        String display = showPlaceholder ? addFriendPlaceholderComponent.getTranslatedName() : friendInputBuffer;
        Color textColor = showPlaceholder ? MD3Theme.TEXT_MUTED : MD3Theme.TEXT_PRIMARY;
        textRenderer.addText(display, textX, textY, textScale, textColor);

        // Cursor
        if (friendInputFocused) {
            int safeCursor = Math.min(friendInputCursor, friendInputBuffer.length());
            float caretX = textX + textRenderer.getWidth(friendInputBuffer.substring(0, safeCursor), textScale);
            rectRenderer.addRect(caretX, inputBounds.y() + 6.0f, 1.0f, inputBounds.height() - 12.0f, MD3Theme.TEXT_PRIMARY);
        }

        // Enter hint when focused and has text
        if (friendInputFocused && !friendInputBuffer.isEmpty()) {
            String hint = "↵";
            float hintScale = 0.56f;
            float hintWidth = textRenderer.getWidth(hint, hintScale);
            float hintX = inputBounds.right() - textInset - hintWidth;
            float hintY = inputBounds.y() + (inputBounds.height() - textRenderer.getHeight(hintScale)) / 2.0f - 1.0f;
            textRenderer.addText(hint, hintX, hintY, hintScale, MD3Theme.TEXT_MUTED);
        }
    }

    // ─── Input Handling ─────────────────────────────────────────────────

    public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
        if (bounds == null || event.button() != 0) {
            return false;
        }

        if (state.getListeningKeybindSetting() != null) {
            state.setListeningKeybindSetting(null);
            markDirty();
        }

        // Tab click
        PanelLayout.Rect tabBar = getTabBarRect();
        if (tabBar.contains(event.x(), event.y())) {
            float halfWidth = tabBar.width() / 2.0f;
            PanelState.ClientSettingTab clickedTab = (event.x() < tabBar.x() + halfWidth)
                    ? PanelState.ClientSettingTab.GENERAL
                    : PanelState.ClientSettingTab.FRIEND;
            state.setClientSettingTab(clickedTab);
            if (clickedTab == PanelState.ClientSettingTab.FRIEND) {
                settingListController.clearFocus();
            } else {
                friendInputFocused = false;
            }
            markDirty();
            markFriendDirty();
            return true;
        }

        if (state.getClientSettingTab() == PanelState.ClientSettingTab.GENERAL) {
            return handleGeneralClick(event, isDoubleClick);
        } else {
            return handleFriendClick(event, isDoubleClick);
        }
    }

    private boolean handleGeneralClick(MouseButtonEvent event, boolean isDoubleClick) {
        // Scrollbar drag
        PanelLayout.Rect viewport = getGeneralViewport();
        float maxScroll = state.getMaxClientSettingScroll();
        if (generalScrollBarDrag.mouseClicked(event.x(), event.y(), viewport, state.getClientSettingScroll(), maxScroll)) {
            float newScroll = generalScrollBarDrag.mouseDragged(event.y(), viewport, maxScroll);
            if (newScroll >= 0) {
                state.setClientSettingScroll(newScroll);
            }
            markDirty();
            return true;
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

    private boolean handleFriendClick(MouseButtonEvent event, boolean isDoubleClick) {
        // Scrollbar drag
        PanelLayout.Rect fullViewport = getFriendFullViewport();
        PanelLayout.Rect listViewport = new PanelLayout.Rect(
                fullViewport.x(), fullViewport.y(),
                fullViewport.width(),
                fullViewport.height() - FRIEND_INPUT_HEIGHT - FRIEND_INPUT_BOTTOM_MARGIN * 2
        );
        float maxScroll = state.getMaxFriendScroll();
        if (friendScrollBarDrag.mouseClicked(event.x(), event.y(), listViewport, state.getFriendScroll(), maxScroll)) {
            float newScroll = friendScrollBarDrag.mouseDragged(event.y(), listViewport, maxScroll);
            if (newScroll >= 0) {
                state.setFriendScroll(newScroll);
            }
            markFriendDirty();
            return true;
        }

        // Check friend input field click
        PanelLayout.Rect inputBounds = getFriendInputBounds(fullViewport);
        if (inputBounds.contains(event.x(), event.y())) {
            friendInputFocused = true;
            friendInputCursor = friendInputBuffer.length();
            markFriendDirty();
            return true;
        }

        // Unfocus input if clicked elsewhere
        if (friendInputFocused) {
            friendInputFocused = false;
            markFriendDirty();
        }

        // Check friend row clicks (remove button)
        for (FriendRowEntry entry : friendRowEntries) {
            if (entry.removeBounds().contains(event.x(), event.y())) {
                FriendManager.INSTANCE.removeFriend(entry.name());
                ConfigManager.INSTANCE.saveNow();
                markFriendDirty();
                return true;
            }
        }

        return false;
    }

    public boolean mouseReleased(MouseButtonEvent event) {
        if (state.getClientSettingTab() == PanelState.ClientSettingTab.GENERAL) {
            if (generalScrollBarDrag.mouseReleased()) {
                markDirty();
                return true;
            }
            if (settingListController.mouseReleased(event)) {
                markDirty();
                return true;
            }
        } else {
            if (friendScrollBarDrag.mouseReleased()) {
                markFriendDirty();
                return true;
            }
        }
        return false;
    }

    public boolean mouseDragged(MouseButtonEvent event, double mouseX, double mouseY) {
        if (state.getClientSettingTab() == PanelState.ClientSettingTab.GENERAL) {
            if (generalScrollBarDrag.isDragging()) {
                PanelLayout.Rect viewport = getGeneralViewport();
                float newScroll = generalScrollBarDrag.mouseDragged(event.y(), viewport, state.getMaxClientSettingScroll());
                if (newScroll >= 0) {
                    state.setClientSettingScroll(newScroll);
                }
                markDirty();
                return true;
            }
            if (settingListController.mouseDragged(event, mouseX, mouseY)) {
                markDirty();
                return true;
            }
        } else {
            if (friendScrollBarDrag.isDragging()) {
                PanelLayout.Rect fullViewport = getFriendFullViewport();
                PanelLayout.Rect listViewport = new PanelLayout.Rect(
                        fullViewport.x(), fullViewport.y(),
                        fullViewport.width(),
                        fullViewport.height() - FRIEND_INPUT_HEIGHT - FRIEND_INPUT_BOTTOM_MARGIN * 2
                );
                float newScroll = friendScrollBarDrag.mouseDragged(event.y(), listViewport, state.getMaxFriendScroll());
                if (newScroll >= 0) {
                    state.setFriendScroll(newScroll);
                }
                markFriendDirty();
                return true;
            }
        }
        return false;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (bounds == null) {
            return false;
        }

        if (state.getClientSettingTab() == PanelState.ClientSettingTab.GENERAL) {
            PanelLayout.Rect viewport = getGeneralViewport();
            if (viewport.contains(mouseX, mouseY)) {
                state.scrollClientSetting(-scrollY * 20.0f);
                markDirty();
                return true;
            }
        } else {
            PanelLayout.Rect fullViewport = getFriendFullViewport();
            PanelLayout.Rect listViewport = new PanelLayout.Rect(
                    fullViewport.x(), fullViewport.y(),
                    fullViewport.width(),
                    fullViewport.height() - FRIEND_INPUT_HEIGHT - FRIEND_INPUT_BOTTOM_MARGIN * 2
            );
            if (listViewport.contains(mouseX, mouseY)) {
                state.scrollFriend(-scrollY * 20.0f);
                markFriendDirty();
                return true;
            }
        }
        return false;
    }

    public boolean keyPressed(KeyEvent event) {
        if (state.getClientSettingTab() == PanelState.ClientSettingTab.GENERAL) {
            return handleGeneralKeyPressed(event);
        } else {
            return handleFriendKeyPressed(event);
        }
    }

    private boolean handleGeneralKeyPressed(KeyEvent event) {
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

    private boolean handleFriendKeyPressed(KeyEvent event) {
        if (!friendInputFocused) {
            return false;
        }

        if (isControlDown()) {
            return handleFriendControlShortcut(event.key());
        }

        return switch (event.key()) {
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> { // Enter / Numpad Enter
                addFriendFromInput();
                yield true;
            }
            case GLFW.GLFW_KEY_ESCAPE -> { // Escape
                friendInputFocused = false;
                markFriendDirty();
                yield true;
            }
            case GLFW.GLFW_KEY_BACKSPACE -> { // Backspace
                if (friendInputCursor > 0 && !friendInputBuffer.isEmpty()) {
                    friendInputBuffer = friendInputBuffer.substring(0, friendInputCursor - 1) + friendInputBuffer.substring(friendInputCursor);
                    friendInputCursor--;
                    markFriendDirty();
                }
                yield true;
            }
            case GLFW.GLFW_KEY_DELETE -> { // Delete
                if (friendInputCursor < friendInputBuffer.length()) {
                    friendInputBuffer = friendInputBuffer.substring(0, friendInputCursor) + friendInputBuffer.substring(friendInputCursor + 1);
                    markFriendDirty();
                }
                yield true;
            }
            case GLFW.GLFW_KEY_LEFT -> { // Left
                friendInputCursor = Math.max(0, friendInputCursor - 1);
                markFriendDirty();
                yield true;
            }
            case GLFW.GLFW_KEY_RIGHT -> { // Right
                friendInputCursor = Math.min(friendInputBuffer.length(), friendInputCursor + 1);
                markFriendDirty();
                yield true;
            }
            default -> false;
        };
    }

    private boolean handleFriendControlShortcut(int key) {
        return switch (key) {
            case 65 -> { // Ctrl+A
                friendInputCursor = friendInputBuffer.length();
                markFriendDirty();
                yield true;
            }
            case 86 -> { // Ctrl+V
                Minecraft minecraft = Minecraft.getInstance();
                if (minecraft != null) {
                    String clipboard = minecraft.keyboardHandler.getClipboard();
                    if (clipboard != null && !clipboard.isEmpty()) {
                        String sanitized = clipboard.codePoints()
                                .filter(cp -> cp >= 32 && cp != 127)
                                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                                .toString();
                        int available = MAX_FRIEND_NAME_LENGTH - friendInputBuffer.length();
                        if (available > 0 && !sanitized.isEmpty()) {
                            String toInsert = sanitized.length() > available ? sanitized.substring(0, available) : sanitized;
                            friendInputBuffer = friendInputBuffer.substring(0, friendInputCursor) + toInsert + friendInputBuffer.substring(friendInputCursor);
                            friendInputCursor += toInsert.length();
                            markFriendDirty();
                        }
                    }
                }
                yield true;
            }
            default -> false;
        };
    }

    public boolean charTyped(CharacterEvent event) {
        if (state.getClientSettingTab() == PanelState.ClientSettingTab.GENERAL) {
            if (settingListController.charTyped(event)) {
                markDirty();
                return true;
            }
        } else {
            if (friendInputFocused && event.isAllowedChatCharacter()) {
                String typed = event.codepointAsString();
                if (!typed.isEmpty() && friendInputBuffer.length() < MAX_FRIEND_NAME_LENGTH) {
                    friendInputBuffer = friendInputBuffer.substring(0, friendInputCursor) + typed + friendInputBuffer.substring(friendInputCursor);
                    friendInputCursor++;
                    markFriendDirty();
                    return true;
                }
                return true;
            }
        }
        return false;
    }

    // ─── Friend Add Logic ───────────────────────────────────────────────

    private void addFriendFromInput() {
        String name = friendInputBuffer.trim();
        if (!name.isEmpty() && !FriendManager.INSTANCE.isFriend(name)) {
            FriendManager.INSTANCE.addFriend(name);
            ConfigManager.INSTANCE.saveNow();
        }
        friendInputBuffer = "";
        friendInputCursor = 0;
        markFriendDirty();
    }

    // ─── Viewport helpers ───────────────────────────────────────────────

    private PanelLayout.Rect getGeneralViewport() {
        if (bounds == null) {
            return new PanelLayout.Rect(0, 0, 0, 0);
        }
        float tabBottom = bounds.y() + 28.0f + TAB_BAR_HEIGHT + 4.0f;
        return new PanelLayout.Rect(
                bounds.x() + MD3Theme.PANEL_VIEWPORT_INSET,
                tabBottom,
                bounds.width() - MD3Theme.PANEL_VIEWPORT_INSET * 2.0f,
                bounds.bottom() - tabBottom - 6.0f
        );
    }

    private PanelLayout.Rect getFriendFullViewport() {
        if (bounds == null) {
            return new PanelLayout.Rect(0, 0, 0, 0);
        }
        float tabBottom = bounds.y() + 28.0f + TAB_BAR_HEIGHT + 4.0f;
        return new PanelLayout.Rect(
                bounds.x() + MD3Theme.PANEL_VIEWPORT_INSET,
                tabBottom,
                bounds.width() - MD3Theme.PANEL_VIEWPORT_INSET * 2.0f,
                bounds.bottom() - tabBottom - 6.0f
        );
    }

    private PanelLayout.Rect getFriendInputBounds(PanelLayout.Rect fullViewport) {
        return new PanelLayout.Rect(
                fullViewport.x() + 2.0f,
                fullViewport.bottom() - FRIEND_INPUT_HEIGHT - FRIEND_INPUT_BOTTOM_MARGIN,
                fullViewport.width() - 4.0f,
                FRIEND_INPUT_HEIGHT
        );
    }

    // ─── Flush / Dirty / Animation ─────────────────────────────────────

    public void flushContent() {
        if (state.getClientSettingTab() == PanelState.ClientSettingTab.GENERAL) {
            contentBuffer.flush();
        } else {
            friendContentBuffer.flush();
        }
    }

    public void markDirty() {
        contentState.markDirty();
    }

    public void markFriendDirty() {
        friendContentState.markDirty();
    }

    public boolean hasActiveAnimations() {
        return contentState.hasActiveAnimations()
                || friendContentState.hasActiveAnimations()
                || !tabIndicatorAnimation.isFinished()
                || !generalTabHoverAnimation.isFinished()
                || !friendTabHoverAnimation.isFinished()
                || !friendInputHoverAnimation.isFinished()
                || !friendInputFocusAnimation.isFinished();
    }

    // ─── General tab rebuild checks ─────────────────────────────────────

    private boolean shouldRebuildContent(PanelLayout.Rect bounds, int mouseX, int mouseY, List<Setting<?>> settings, int currentGuiHeight) {
        if (contentState.needsRebuild(bounds, mouseX, mouseY, currentGuiHeight)) {
            return true;
        }
        if (Float.compare(lastScroll, state.getClientSettingScroll()) != 0) {
            return true;
        }
        if (lastTab != state.getClientSettingTab()) {
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
        lastTab = state.getClientSettingTab();
    }

    // ─── Friend tab rebuild checks ──────────────────────────────────────

    private boolean shouldRebuildFriendContent(PanelLayout.Rect bounds, int mouseX, int mouseY, List<String> friends, int currentGuiHeight) {
        if (friendContentState.needsRebuild(bounds, mouseX, mouseY, currentGuiHeight)) {
            return true;
        }
        if (Float.compare(lastFriendScroll, state.getFriendScroll()) != 0) {
            return true;
        }
        return !Objects.equals(lastFriendList, friends);
    }

    private void rememberFriendSnapshot(PanelLayout.Rect bounds, int mouseX, int mouseY, List<String> friends, int currentGuiHeight) {
        friendContentState.rememberSnapshot(bounds, mouseX, mouseY, currentGuiHeight);
        lastFriendScroll = state.getFriendScroll();
        lastFriendList = new ArrayList<>(friends);
    }


    private boolean isControlDown() {
        Minecraft minecraft = Minecraft.getInstance();
        return InputConstants.isKeyDown(minecraft.getWindow(), 341) || InputConstants.isKeyDown(minecraft.getWindow(), 345);
    }

    private record FriendRowEntry(String name, PanelLayout.Rect rowBounds, PanelLayout.Rect removeBounds) {
    }

}
