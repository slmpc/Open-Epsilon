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
import com.github.epsilon.gui.panel.adapter.ModuleViewModel;
import com.github.epsilon.gui.panel.component.ModuleRow;
import com.github.epsilon.gui.panel.util.PanelContentBuffer;
import com.github.epsilon.gui.panel.util.PanelContentInvalidationState;
import com.github.epsilon.gui.panel.util.ScrollBarDragState;
import com.github.epsilon.gui.panel.util.ScrollBarUtil;
import com.github.epsilon.modules.Module;
import com.github.epsilon.utils.render.animation.Animation;
import com.github.epsilon.utils.render.animation.Easing;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;

import java.util.*;

public class ModuleListPanel {

    protected final PanelState state;
    private final RoundRectRenderer roundRectRenderer;
    private final RectRenderer rectRenderer;
    private final TextRenderer textRenderer;
    private final PanelContentBuffer contentBuffer = new PanelContentBuffer();
    private final PanelContentInvalidationState contentState = new PanelContentInvalidationState();
    private PanelLayout.Rect bounds;
    private int guiHeight;
    private final List<ModuleRow> rows = new ArrayList<>();
    private final Map<Module, Animation> hoverAnimations = new HashMap<>();
    private final Map<Module, Animation> selectionAnimations = new HashMap<>();
    private final Map<Module, Animation> toggleAnimations = new HashMap<>();
    private final Map<Module, Animation> toggleHoverAnimations = new HashMap<>();
    private float lastModuleScroll = Float.NaN;
    private String lastSearchQuery = "";
    private boolean lastSearchFocused;
    private CategorySnapshot lastCategorySnapshot;
    private String lastSelectedModuleName = "";
    private final Animation searchHoverAnimation = new Animation(Easing.EASE_OUT_CUBIC, 120L);
    private final Animation searchFocusAnimation = new Animation(Easing.EASE_OUT_CUBIC, 120L);
    private final ScrollBarDragState scrollBarDrag = new ScrollBarDragState();
    private boolean searchFocused;
    private int searchCursorIndex;

    private static final TranslateComponent searchComponent = EpsilonTranslateComponent.create("gui", "search");

    public ModuleListPanel(PanelState state, RoundRectRenderer roundRectRenderer, RectRenderer rectRenderer, ShadowRenderer shadowRenderer, TextRenderer textRenderer) {
        this.state = state;
        this.roundRectRenderer = roundRectRenderer;
        this.rectRenderer = rectRenderer;
        this.textRenderer = textRenderer;
        this.searchHoverAnimation.setStartValue(0.0f);
        this.searchFocusAnimation.setStartValue(0.0f);
    }

    public void render(GuiGraphicsExtractor GuiGraphicsExtractor, PanelLayout.Rect bounds, int mouseX, int mouseY, float partialTick) {
        this.bounds = bounds;
        this.guiHeight = GuiGraphicsExtractor.guiHeight();

        textRenderer.addText(state.getSelectedCategory().getName(), bounds.x() + MD3Theme.PANEL_TITLE_INSET, bounds.y() + 10.0f, 0.78f, MD3Theme.TEXT_PRIMARY, StaticFontLoader.DUCKSANS);
        textRenderer.addText("Modules", bounds.x() + MD3Theme.PANEL_TITLE_INSET, bounds.y() + 21.0f, 0.56f, MD3Theme.TEXT_SECONDARY);
        drawSearchField(mouseX, mouseY);

        PanelLayout.Rect viewport = getViewport();
        List<Module> modules = state.getVisibleModules();
        float contentHeight = modules.size() * (ModuleRow.HEIGHT + MD3Theme.ROW_GAP);
        state.setMaxModuleScroll(contentHeight - viewport.height());
        float maxModuleScroll = Math.max(0, contentHeight - viewport.height());
        boolean hasScrollBar = maxModuleScroll > 0;
        float rowWidth = hasScrollBar ? viewport.width() - ScrollBarUtil.TOTAL_WIDTH : viewport.width();

        if (shouldRebuildContent(bounds, mouseX, mouseY, modules, GuiGraphicsExtractor.guiHeight())) {
            rows.clear();
            contentBuffer.clear();
            contentState.beginRebuild();

            float y = viewport.y() - state.getModuleScroll();
            for (Module module : modules) {
                ModuleRow row = new ModuleRow(ModuleViewModel.from(module), new PanelLayout.Rect(viewport.x(), y, rowWidth, ModuleRow.HEIGHT));
                rows.add(row);
                Animation hoverAnimation = hoverAnimations.computeIfAbsent(module, ignored -> new Animation(Easing.EASE_OUT_CUBIC, 120L));
                Animation selectionAnimation = selectionAnimations.computeIfAbsent(module, ignored -> new Animation(Easing.EASE_OUT_CUBIC, 160L));
                Animation toggleAnimation = toggleAnimations.computeIfAbsent(module, ignored -> new Animation(Easing.DYNAMIC_ISLAND, 220L));
                Animation toggleHoverAnimation = toggleHoverAnimations.computeIfAbsent(module, ignored -> new Animation(Easing.EASE_OUT_CUBIC, 120L));
                hoverAnimation.run(row.getBounds().contains(mouseX, mouseY) ? 1.0f : 0.0f);
                selectionAnimation.run(state.getSelectedModule() == module ? 1.0f : 0.0f);
                toggleAnimation.run(module.isEnabled() ? 1.0f : 0.0f);
                toggleHoverAnimation.run(row.getToggleBounds().contains(mouseX, mouseY) ? 1.0f : 0.0f);
                contentState.noteAnimation(!hoverAnimation.isFinished()
                        || !selectionAnimation.isFinished()
                        || !toggleAnimation.isFinished()
                        || !toggleHoverAnimation.isFinished());
                row.render(contentBuffer.roundRectRenderer(), contentBuffer.rectRenderer(), contentBuffer.textRenderer(), hoverAnimation.getValue(), selectionAnimation.getValue(), toggleAnimation.getValue(), toggleHoverAnimation.getValue());
                y += ModuleRow.HEIGHT + MD3Theme.ROW_GAP;
            }

            rememberSnapshot(bounds, mouseX, mouseY, modules, GuiGraphicsExtractor.guiHeight());
        }

        contentBuffer.queueViewport(viewport, guiHeight, state.getModuleScroll(), maxModuleScroll, contentHeight);
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

    public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
        if (bounds == null || event.button() != 0) {
            return false;
        }
        // Scrollbar drag
        PanelLayout.Rect viewport = getViewport();
        float maxScroll = state.getMaxModuleScroll();
        if (scrollBarDrag.mouseClicked(event.x(), event.y(), viewport, state.getModuleScroll(), maxScroll)) {
            float newScroll = scrollBarDrag.mouseDragged(event.y(), viewport, maxScroll);
            if (newScroll >= 0) {
                state.setModuleScroll(newScroll);
            }
            markDirty();
            return true;
        }
        PanelLayout.Rect searchBounds = getSearchBounds();
        if (searchBounds.contains(event.x(), event.y())) {
            searchFocused = true;
            searchCursorIndex = state.getSearchQuery().length();
            markDirty();
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
        return false;
    }

    public boolean mouseDragged(MouseButtonEvent event, double mouseX, double mouseY) {
        if (scrollBarDrag.isDragging()) {
            PanelLayout.Rect viewport = getViewport();
            float newScroll = scrollBarDrag.mouseDragged(event.y(), viewport, state.getMaxModuleScroll());
            if (newScroll >= 0) {
                state.setModuleScroll(newScroll);
            }
            markDirty();
            return true;
        }
        return false;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        PanelLayout.Rect viewport = getViewport();
        if (bounds != null && viewport.contains(mouseX, mouseY)) {
            state.scrollModules(-scrollY * 20.0f);
            markDirty();
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
                    markDirty();
                }
                yield true;
            }
            case 261 -> {
                if (searchCursorIndex < query.length()) {
                    state.setSearchQuery(query.substring(0, searchCursorIndex) + query.substring(searchCursorIndex + 1));
                    markDirty();
                }
                yield true;
            }
            case 263 -> {
                searchCursorIndex = Math.max(0, searchCursorIndex - 1);
                markDirty();
                yield true;
            }
            case 262 -> {
                searchCursorIndex = Math.min(state.getSearchQuery().length(), searchCursorIndex + 1);
                markDirty();
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
        markDirty();
        return true;
    }

    public void handleGlobalClick(double mouseX, double mouseY) {
        if (bounds == null) {
            return;
        }
        if (!getSearchBounds().contains(mouseX, mouseY)) {
            searchFocused = false;
            markDirty();
        }
    }

    private boolean shouldRebuildContent(PanelLayout.Rect bounds, int mouseX, int mouseY, List<Module> modules, int currentGuiHeight) {
        if (contentState.needsRebuild(bounds, mouseX, mouseY, currentGuiHeight)) {
            return true;
        }
        if (Float.compare(lastModuleScroll, state.getModuleScroll()) != 0) {
            return true;
        }
        if (!Objects.equals(lastSearchQuery, state.getSearchQuery()) || lastSearchFocused != searchFocused) {
            return true;
        }
        String selectedModuleName = state.getSelectedModule() == null ? "" : state.getSelectedModule().getName();
        if (!Objects.equals(lastSelectedModuleName, selectedModuleName)) {
            return true;
        }
        return !Objects.equals(lastCategorySnapshot, CategorySnapshot.of(state.getSelectedCategory().name(), modules));
    }

    private void rememberSnapshot(PanelLayout.Rect bounds, int mouseX, int mouseY, List<Module> modules, int currentGuiHeight) {
        contentState.rememberSnapshot(bounds, mouseX, mouseY, currentGuiHeight);
        lastModuleScroll = state.getModuleScroll();
        lastSearchQuery = state.getSearchQuery();
        lastSearchFocused = searchFocused;
        lastCategorySnapshot = CategorySnapshot.of(state.getSelectedCategory().name(), modules);
        lastSelectedModuleName = state.getSelectedModule() == null ? "" : state.getSelectedModule().getName();
    }

    private record CategorySnapshot(String categoryName, List<String> moduleIds) {
        private static CategorySnapshot of(String categoryName, List<Module> modules) {
            List<String> ids = modules.stream().map(Module::getName).toList();
            return new CategorySnapshot(categoryName, ids);
        }
    }

    private PanelLayout.Rect getViewport() {
        return new PanelLayout.Rect(bounds.x() + MD3Theme.PANEL_VIEWPORT_INSET, bounds.y() + 34.0f, bounds.width() - MD3Theme.PANEL_VIEWPORT_INSET * 2.0f, bounds.height() - 40.0f);
    }

    private PanelLayout.Rect getSearchBounds() {
        return new PanelLayout.Rect(bounds.right() - MD3Theme.PANEL_TITLE_INSET - 76.0f, bounds.y() + 8.0f, 76.0f, 18.0f);
    }

    private void drawSearchField(int mouseX, int mouseY) {
        PanelLayout.Rect searchBounds = getSearchBounds();
        searchHoverAnimation.run(searchBounds.contains(mouseX, mouseY) ? 1.0f : 0.0f);
        searchFocusAnimation.run(searchFocused ? 1.0f : 0.0f);
        float hoverProgress = searchHoverAnimation.getValue();
        float focusProgress = searchFocusAnimation.getValue();
        roundRectRenderer.addRoundRect(searchBounds.x(), searchBounds.y(), searchBounds.width(), searchBounds.height(), 9.0f, MD3Theme.lerp(MD3Theme.SURFACE_CONTAINER, MD3Theme.SURFACE_CONTAINER_HIGH, hoverProgress));
        if (focusProgress > 0.01f) {
            roundRectRenderer.addRoundRect(searchBounds.x(), searchBounds.y(), searchBounds.width(), searchBounds.height(), 9.0f, MD3Theme.withAlpha(MD3Theme.PRIMARY, (int) (12 * focusProgress)));
        }

        String query = state.getSearchQuery();
        boolean showPlaceholder = query.isEmpty() && !searchFocused;
        String display = showPlaceholder ? searchComponent.getTranslatedName() : query;
        float scale = 0.52f;
        float textY = searchBounds.y() + (searchBounds.height() - textRenderer.getHeight(scale)) / 2.0f - 1.0f;
        float textX = searchBounds.x() + 8.0f;
        textRenderer.addText(display, textX, textY, scale, showPlaceholder ? MD3Theme.TEXT_MUTED : MD3Theme.TEXT_PRIMARY);

        if (searchFocused) {
            float caretX = textX + textRenderer.getWidth(query.substring(0, Math.min(searchCursorIndex, query.length())), scale);
            rectRenderer.addRect(caretX, searchBounds.y() + 4.0f, 1.0f, searchBounds.height() - 8.0f, MD3Theme.TEXT_PRIMARY);
        }
    }
}
