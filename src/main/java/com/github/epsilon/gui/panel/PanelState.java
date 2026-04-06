package com.github.epsilon.gui.panel;

import com.github.epsilon.gui.panel.util.SmoothScrollAnimation;
import com.github.epsilon.managers.ModuleManager;
import com.github.epsilon.modules.Category;
import com.github.epsilon.modules.Module;
import com.github.epsilon.settings.impl.KeybindSetting;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class PanelState {

    public enum SortMode {
        NAME,
        ENABLED_FIRST
    }

    public enum ActivePopup {
        NONE,
        ENUM_SELECT,
        KEY_BIND,
        COLOR_PICKER
    }

    public enum ClientSettingTab {
        GENERAL,
        FRIEND
    }

    private Category selectedCategory = Category.COMBAT;
    private Module selectedModule;
    private String searchQuery = "";
    private SortMode sortMode = SortMode.NAME;
    private ActivePopup activePopup = ActivePopup.NONE;
    private Module listeningKeyBindModule;
    private boolean sidebarExpanded;
    private float moduleScroll;
    private float detailScroll;
    private float maxModuleScroll;
    private float maxDetailScroll;

    private boolean clientSettingMode;
    private ClientSettingTab clientSettingTab = ClientSettingTab.GENERAL;
    private KeybindSetting listeningKeybindSetting;
    private float clientSettingScroll;
    private float maxClientSettingScroll;
    private float friendScroll;
    private float maxFriendScroll;

    private final SmoothScrollAnimation moduleScrollAnimation = new SmoothScrollAnimation();
    private final SmoothScrollAnimation detailScrollAnimation = new SmoothScrollAnimation();
    private final SmoothScrollAnimation clientSettingScrollAnimation = new SmoothScrollAnimation();
    private final SmoothScrollAnimation friendScrollAnimation = new SmoothScrollAnimation();

    public PanelState() {
        ensureValidSelection();
    }

    public Category getSelectedCategory() {
        return selectedCategory;
    }

    public void setSelectedCategory(Category category) {
        selectedCategory = category;
        moduleScroll = 0.0f;
        moduleScrollAnimation.reset();
        detailScrollAnimation.reset();
        ensureValidSelection();
    }

    public Module getSelectedModule() {
        ensureValidSelection();
        return selectedModule;
    }

    public void setSelectedModule(Module module) {
        selectedModule = module;
    }

    public String getSearchQuery() {
        return searchQuery;
    }

    public void setSearchQuery(String searchQuery) {
        this.searchQuery = searchQuery == null ? "" : searchQuery;
        moduleScroll = 0.0f;
        moduleScrollAnimation.reset();
        ensureValidSelection();
    }

    public SortMode getSortMode() {
        return sortMode;
    }

    public void setSortMode(SortMode sortMode) {
        this.sortMode = sortMode == null ? SortMode.NAME : sortMode;
    }

    public ActivePopup getActivePopup() {
        return activePopup;
    }

    public void setActivePopup(ActivePopup activePopup) {
        this.activePopup = activePopup == null ? ActivePopup.NONE : activePopup;
    }

    public Module getListeningKeyBindModule() {
        return listeningKeyBindModule;
    }

    public void setListeningKeyBindModule(Module listeningKeyBindModule) {
        this.listeningKeyBindModule = listeningKeyBindModule;
    }

    public boolean isSidebarExpanded() {
        return sidebarExpanded;
    }

    public void setSidebarExpanded(boolean sidebarExpanded) {
        this.sidebarExpanded = sidebarExpanded;
    }

    public void toggleSidebarExpanded() {
        sidebarExpanded = !sidebarExpanded;
    }

    public List<Module> getVisibleModules() {
        String loweredSearch = searchQuery.toLowerCase();
        List<Module> modules = new ArrayList<>(ModuleManager.INSTANCE.getModules().stream()
                .filter(module -> module.category == selectedCategory)
                .filter(module -> loweredSearch.isBlank() || matchesSearch(module, loweredSearch))
                .sorted(getComparator())
                .toList());

        if (!modules.isEmpty() && (selectedModule == null || !modules.contains(selectedModule))) {
            selectedModule = modules.getFirst();
        }

        return modules;
    }

    public float getModuleScroll() {
        return moduleScrollAnimation.getCurrentScroll();
    }

    public void scrollModules(double amount) {
        moduleScrollAnimation.addImpulse((float) amount, maxModuleScroll);
    }

    public float getDetailScroll() {
        return detailScrollAnimation.getCurrentScroll();
    }

    public void scrollDetail(double amount) {
        detailScrollAnimation.addImpulse((float) amount, maxDetailScroll);
    }

    public float getMaxModuleScroll() {
        return maxModuleScroll;
    }

    public void setModuleScroll(float scroll) {
        moduleScrollAnimation.setCurrentScroll(scroll);
        moduleScroll = scroll;
    }

    public void setMaxModuleScroll(float maxModuleScroll) {
        this.maxModuleScroll = Math.max(0.0f, maxModuleScroll);
        moduleScroll = clampScroll(moduleScroll, this.maxModuleScroll);
    }

    public float getMaxDetailScroll() {
        return maxDetailScroll;
    }

    public void setDetailScroll(float scroll) {
        detailScrollAnimation.setCurrentScroll(scroll);
        detailScroll = scroll;
    }

    public void setMaxDetailScroll(float maxDetailScroll) {
        this.maxDetailScroll = Math.max(0.0f, maxDetailScroll);
        detailScroll = clampScroll(detailScroll, this.maxDetailScroll);
    }

    public SmoothScrollAnimation getModuleScrollAnimation() {
        return moduleScrollAnimation;
    }

    public SmoothScrollAnimation getDetailScrollAnimation() {
        return detailScrollAnimation;
    }

    public boolean updateScrollAnimations() {
        boolean moduleChanged = moduleScrollAnimation.update(maxModuleScroll);
        boolean detailChanged = detailScrollAnimation.update(maxDetailScroll);
        boolean clientSettingChanged = clientSettingScrollAnimation.update(maxClientSettingScroll);
        boolean friendChanged = friendScrollAnimation.update(maxFriendScroll);
        return moduleChanged || detailChanged || clientSettingChanged || friendChanged;
    }

    public boolean hasActiveScrollAnimations() {
        return moduleScrollAnimation.isAnimating()
                || detailScrollAnimation.isAnimating()
                || clientSettingScrollAnimation.isAnimating()
                || friendScrollAnimation.isAnimating();
    }

    private void ensureValidSelection() {
        String loweredSearch = searchQuery.toLowerCase();
        List<Module> modules = ModuleManager.INSTANCE.getModules().stream()
                .filter(module -> module.category == selectedCategory)
                .filter(module -> loweredSearch.isBlank() || matchesSearch(module, loweredSearch))
                .sorted(getComparator())
                .toList();
        if (!modules.isEmpty() && (selectedModule == null || !modules.contains(selectedModule))) {
            selectedModule = modules.getFirst();
        }
    }

    private Comparator<Module> getComparator() {
        Comparator<Module> comparator = Comparator.comparing(Module::getName);
        if (sortMode == SortMode.ENABLED_FIRST) {
            comparator = Comparator.comparing(Module::isEnabled).reversed().thenComparing(Module::getName);
        }
        return comparator;
    }

    private boolean matchesSearch(Module module, String loweredSearch) {
        return module.getName().toLowerCase().contains(loweredSearch)
                || module.getTranslatedName().toLowerCase().contains(loweredSearch)
                || (module.category != null && module.category.getName().toLowerCase().contains(loweredSearch));
    }

    public boolean isClientSettingMode() {
        return clientSettingMode;
    }

    public void setClientSettingMode(boolean clientSettingMode) {
        if (this.clientSettingMode != clientSettingMode) {
            this.clientSettingMode = clientSettingMode;
            if (clientSettingMode) {
                listeningKeyBindModule = null;
            } else {
                listeningKeybindSetting = null;
                clientSettingScroll = 0.0f;
                friendScroll = 0.0f;
                clientSettingTab = ClientSettingTab.GENERAL;
            }
        }
    }

    public KeybindSetting getListeningKeybindSetting() {
        return listeningKeybindSetting;
    }

    public void setListeningKeybindSetting(KeybindSetting listeningKeybindSetting) {
        this.listeningKeybindSetting = listeningKeybindSetting;
    }

    public float getClientSettingScroll() {
        return clientSettingScrollAnimation.getCurrentScroll();
    }

    public void scrollClientSetting(double amount) {
        clientSettingScrollAnimation.addImpulse((float) amount, maxClientSettingScroll);
    }

    public float getMaxClientSettingScroll() {
        return maxClientSettingScroll;
    }

    public void setClientSettingScroll(float scroll) {
        clientSettingScrollAnimation.setCurrentScroll(scroll);
        clientSettingScroll = scroll;
    }

    public void setMaxClientSettingScroll(float maxClientSettingScroll) {
        this.maxClientSettingScroll = Math.max(0.0f, maxClientSettingScroll);
        clientSettingScroll = clampScroll(clientSettingScroll, this.maxClientSettingScroll);
    }

    public ClientSettingTab getClientSettingTab() {
        return clientSettingTab;
    }

    public void setClientSettingTab(ClientSettingTab tab) {
        if (this.clientSettingTab != tab) {
            this.clientSettingTab = tab;
            friendScroll = 0.0f;
            clientSettingScroll = 0.0f;
            friendScrollAnimation.reset();
            clientSettingScrollAnimation.reset();
        }
    }

    public float getFriendScroll() {
        return friendScrollAnimation.getCurrentScroll();
    }

    public void scrollFriend(double amount) {
        friendScrollAnimation.addImpulse((float) amount, maxFriendScroll);
    }

    public float getMaxFriendScroll() {
        return maxFriendScroll;
    }

    public void setFriendScroll(float scroll) {
        friendScrollAnimation.setCurrentScroll(scroll);
        friendScroll = scroll;
    }

    public void setMaxFriendScroll(float maxFriendScroll) {
        this.maxFriendScroll = Math.max(0.0f, maxFriendScroll);
        friendScroll = clampScroll(friendScroll, this.maxFriendScroll);
    }

    public SmoothScrollAnimation getClientSettingScrollAnimation() {
        return clientSettingScrollAnimation;
    }

    public SmoothScrollAnimation getFriendScrollAnimation() {
        return friendScrollAnimation;
    }

    private float clampScroll(float scroll, float maxScroll) {
        return Math.clamp(scroll, 0, maxScroll);
    }

}
