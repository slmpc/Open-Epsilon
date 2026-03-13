package com.github.lumin.managers.impl;

import com.github.lumin.assets.i18n.TranslateComponent;

import java.util.ArrayList;
import java.util.List;

public class TranslateManager {

    private static TranslateManager INSTANCE = null;
    private List<TranslateComponent> components = new ArrayList<>();

    private TranslateManager() {

    }

    public static TranslateManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new TranslateManager();
        }
        return INSTANCE;
    }

    public void refresh() {
        for (TranslateComponent component : components) {
            component.refresh();
        }
    }

    public void registerTranslateComponent(TranslateComponent component) {
        components.add(component);
    }

}
