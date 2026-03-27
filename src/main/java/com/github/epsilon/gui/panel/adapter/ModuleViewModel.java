package com.github.epsilon.gui.panel.adapter;

import com.github.epsilon.modules.Category;
import com.github.epsilon.modules.Module;

public record ModuleViewModel(Module module, String displayName, String description, boolean enabled, Category category,
                              String searchText) {
    public static ModuleViewModel from(Module module) {
        String displayName = module.getTranslatedName();
        String description = module.getDescription();
        String searchText = (displayName + " " + description + " " + module.category.getName()).toLowerCase();
        return new ModuleViewModel(module, displayName, description, module.isEnabled(), module.category, searchText);
    }
}
