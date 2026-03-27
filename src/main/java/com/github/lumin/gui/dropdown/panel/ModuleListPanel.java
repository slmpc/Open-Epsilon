package com.github.lumin.gui.dropdown.panel;

import com.github.lumin.graphics.renderers.RectRenderer;
import com.github.lumin.graphics.renderers.RoundRectRenderer;
import com.github.lumin.graphics.renderers.ShadowRenderer;
import com.github.lumin.graphics.renderers.TextRenderer;
import com.github.lumin.graphics.text.StaticFontLoader;
import com.github.lumin.gui.dropdown.DropdownLayout;
import com.github.lumin.gui.dropdown.DropdownState;
import com.github.lumin.gui.dropdown.DropdownTheme;
import com.github.lumin.gui.dropdown.adapter.ModuleViewModel;
import com.github.lumin.gui.dropdown.component.ModuleRow;
import com.github.lumin.gui.dropdown.util.DropdownScissor;
import com.github.lumin.modules.Module;
import com.github.lumin.utils.render.animation.Animation;
import com.github.lumin.utils.render.animation.Easing;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.language.I18n;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModuleListPanel {

    protected final DropdownState state;
    private final RoundRectRenderer roundRectRenderer;
    private final RectRenderer rectRenderer;
    private final ShadowRenderer shadowRenderer;
    private final TextRenderer textRenderer;
    private final RoundRectRenderer contentRoundRectRenderer = new RoundRectRenderer();
    private final RectRenderer contentRectRenderer = new RectRenderer();
    private final ShadowRenderer contentShadowRenderer = new ShadowRenderer();
    private final TextRenderer contentTextRenderer = new TextRenderer();
    private DropdownLayout.Rect bounds;
    private int guiHeight;
    private final List<ModuleRow> rows = new ArrayList<>();
    private final Map<Module, Animation> hoverAnimations = new HashMap<>();
    private final Map<Module, Animation> selectionAnimations = new HashMap<>();
    private final Map<Module, Animation> toggleAnimations = new HashMap<>();
    private final Map<Module, Animation> toggleHoverAnimations = new HashMap<>();
    private boolean contentPending;
    private final Animation searchHoverAnimation = new Animation(Easing.EASE_OUT_CUBIC, 120L);
    private final Animation searchFocusAnimation = new Animation(Easing.EASE_OUT_CUBIC, 120L);
    private boolean searchFocused;
    private int searchCursorIndex;

    public ModuleListPanel(DropdownState state, RoundRectRenderer roundRectRenderer, RectRenderer rectRenderer, ShadowRenderer shadowRenderer, TextRenderer textRenderer) {
        this.state = state;
        this.roundRectRenderer = roundRectRenderer;
        this.rectRenderer = rectRenderer;
        this.shadowRenderer = shadowRenderer;
        this.textRenderer = textRenderer;
        this.searchHoverAnimation.setStartValue(0.0f);
        this.searchFocusAnimation.setStartValue(0.0f);
    }

    public void render(GuiGraphics guiGraphics, DropdownLayout.Rect bounds, int mouseX, int mouseY, float partialTick) {
        this.bounds = bounds;
        this.guiHeight = guiGraphics.guiHeight();
        rows.clear();

        textRenderer.addText(state.getSelectedCategory().getName(), bounds.x() + DropdownTheme.PANEL_TITLE_INSET, bounds.y() + 10.0f, 0.78f, DropdownTheme.TEXT_PRIMARY, StaticFontLoader.DUCKSANS);
        textRenderer.addText("Modules", bounds.x() + DropdownTheme.PANEL_TITLE_INSET, bounds.y() + 21.0f, 0.56f, DropdownTheme.TEXT_SECONDARY);
        drawSearchField(mouseX, mouseY);

        DropdownLayout.Rect viewport = getViewport();
        List<Module> modules = state.getVisibleModules();
        float contentHeight = modules.size() * (ModuleRow.HEIGHT + DropdownTheme.ROW_GAP);
        state.setMaxModuleScroll(contentHeight - viewport.height());

        DropdownScissor.apply(viewport, contentRectRenderer, contentRoundRectRenderer, contentShadowRenderer, contentTextRenderer, guiHeight);
        float y = viewport.y() - state.getModuleScroll();
        for (Module module : modules) {
            ModuleRow row = new ModuleRow(ModuleViewModel.from(module), new DropdownLayout.Rect(viewport.x(), y, viewport.width(), ModuleRow.HEIGHT));
            rows.add(row);
            Animation hoverAnimation = hoverAnimations.computeIfAbsent(module, ignored -> new Animation(Easing.EASE_OUT_CUBIC, 120L));
            Animation selectionAnimation = selectionAnimations.computeIfAbsent(module, ignored -> new Animation(Easing.EASE_OUT_CUBIC, 160L));
            Animation toggleAnimation = toggleAnimations.computeIfAbsent(module, ignored -> new Animation(Easing.DYNAMIC_ISLAND, 220L));
            Animation toggleHoverAnimation = toggleHoverAnimations.computeIfAbsent(module, ignored -> new Animation(Easing.EASE_OUT_CUBIC, 120L));
            hoverAnimation.run(row.getBounds().contains(mouseX, mouseY) ? 1.0f : 0.0f);
            selectionAnimation.run(state.getSelectedModule() == module ? 1.0f : 0.0f);
            toggleAnimation.run(module.isEnabled() ? 1.0f : 0.0f);
            toggleHoverAnimation.run(row.getToggleBounds().contains(mouseX, mouseY) ? 1.0f : 0.0f);
            row.render(contentRoundRectRenderer, contentRectRenderer, contentTextRenderer, hoverAnimation.getValue(), selectionAnimation.getValue(), toggleAnimation.getValue(), toggleHoverAnimation.getValue());
            y += ModuleRow.HEIGHT + DropdownTheme.ROW_GAP;
        }
        contentPending = true;
    }

    public void flushContent() {
        if (!contentPending) {
            return;
        }
        contentShadowRenderer.drawAndClear();
        contentRoundRectRenderer.drawAndClear();
        contentRectRenderer.drawAndClear();
        contentTextRenderer.drawAndClear();
        DropdownScissor.clear(contentRectRenderer, contentRoundRectRenderer, contentShadowRenderer, contentTextRenderer);
        contentPending = false;
    }

    public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
        if (bounds == null || event.button() != 0) {
            return false;
        }
        DropdownLayout.Rect searchBounds = getSearchBounds();
        if (searchBounds.contains(event.x(), event.y())) {
            searchFocused = true;
            searchCursorIndex = state.getSearchQuery().length();
            return true;
        }
        for (ModuleRow row : rows) {
            if (!row.getBounds().contains(event.x(), event.y())) {
                continue;
            }
            if (row.getToggleBounds().contains(event.x(), event.y())) {
                row.getModule().module().toggle();
            } else {
                state.setSelectedModule(row.getModule().module());
            }
            return true;
        }
        return false;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        DropdownLayout.Rect viewport = getViewport();
        if (bounds != null && viewport.contains(mouseX, mouseY)) {
            state.scrollModules(-scrollY * 20.0f);
            return true;
        }
        return false;
    }

    public boolean keyPressed(KeyEvent event) {
        if (!searchFocused) {
            return false;
        }
        String query = state.getSearchQuery();
        return switch (event.key()) {
            case 257, 335 -> true;
            case 256 -> {
                searchFocused = false;
                yield true;
            }
            case 259 -> {
                if (searchCursorIndex > 0 && !query.isEmpty()) {
                    state.setSearchQuery(query.substring(0, searchCursorIndex - 1) + query.substring(searchCursorIndex));
                    searchCursorIndex--;
                }
                yield true;
            }
            case 261 -> {
                if (searchCursorIndex < query.length()) {
                    state.setSearchQuery(query.substring(0, searchCursorIndex) + query.substring(searchCursorIndex + 1));
                }
                yield true;
            }
            case 263 -> {
                searchCursorIndex = Math.max(0, searchCursorIndex - 1);
                yield true;
            }
            case 262 -> {
                searchCursorIndex = Math.min(state.getSearchQuery().length(), searchCursorIndex + 1);
                yield true;
            }
            default -> false;
        };
    }

    public boolean charTyped(CharacterEvent event) {
        if (!searchFocused || !event.isAllowedChatCharacter()) {
            return false;
        }
        String query = state.getSearchQuery();
        String typed = event.codepointAsString();
        state.setSearchQuery(query.substring(0, searchCursorIndex) + typed + query.substring(searchCursorIndex));
        searchCursorIndex++;
        return true;
    }

    public void handleGlobalClick(double mouseX, double mouseY) {
        if (bounds == null) {
            return;
        }
        if (!getSearchBounds().contains(mouseX, mouseY)) {
            searchFocused = false;
        }
    }

    private DropdownLayout.Rect getViewport() {
        return new DropdownLayout.Rect(bounds.x() + DropdownTheme.PANEL_VIEWPORT_INSET, bounds.y() + 34.0f, bounds.width() - DropdownTheme.PANEL_VIEWPORT_INSET * 2.0f, bounds.height() - 40.0f);
    }

    private DropdownLayout.Rect getSearchBounds() {
        return new DropdownLayout.Rect(bounds.right() - DropdownTheme.PANEL_TITLE_INSET - 76.0f, bounds.y() + 8.0f, 76.0f, 18.0f);
    }

    private void drawSearchField(int mouseX, int mouseY) {
        DropdownLayout.Rect searchBounds = getSearchBounds();
        searchHoverAnimation.run(searchBounds.contains(mouseX, mouseY) ? 1.0f : 0.0f);
        searchFocusAnimation.run(searchFocused ? 1.0f : 0.0f);
        float hoverProgress = searchHoverAnimation.getValue();
        float focusProgress = searchFocusAnimation.getValue();
        roundRectRenderer.addRoundRect(searchBounds.x(), searchBounds.y(), searchBounds.width(), searchBounds.height(), 9.0f, DropdownTheme.lerp(DropdownTheme.SURFACE_CONTAINER, DropdownTheme.SURFACE_CONTAINER_HIGH, hoverProgress));
        if (focusProgress > 0.01f) {
            roundRectRenderer.addRoundRect(searchBounds.x(), searchBounds.y(), searchBounds.width(), searchBounds.height(), 9.0f, DropdownTheme.withAlpha(DropdownTheme.PRIMARY, (int) (12 * focusProgress)));
        }

        String query = state.getSearchQuery();
        boolean showPlaceholder = query.isEmpty() && !searchFocused;
        String display = showPlaceholder ? I18n.get("lumin.gui.search") : query;
        float scale = 0.52f;
        float textY = searchBounds.y() + (searchBounds.height() - textRenderer.getHeight(scale)) / 2.0f - 1.0f;
        float textX = searchBounds.x() + 8.0f;
        textRenderer.addText(display, textX, textY, scale, showPlaceholder ? DropdownTheme.TEXT_MUTED : DropdownTheme.TEXT_PRIMARY);

        if (searchFocused) {
            float caretX = textX + textRenderer.getWidth(query.substring(0, Math.min(searchCursorIndex, query.length())), scale);
            rectRenderer.addRect(caretX, searchBounds.y() + 4.0f, 1.0f, searchBounds.height() - 8.0f, DropdownTheme.TEXT_PRIMARY);
        }
    }
}
