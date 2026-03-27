package com.github.lumin.settings;

import com.github.lumin.assets.i18n.TranslateComponent;
import com.github.lumin.modules.Module;

public abstract class Setting<V> {

    protected final String name;
    protected V value;
    protected V defaultValue;
    protected final Dependency dependency;
    protected final TranslateComponent translateComponent;

    public Setting(String name, Module module, Dependency dependency) {
        this.name = name;
        this.dependency = dependency;
        // Usage: lumin.modules.<module>.<setting>
        String prefix = "modules." + module.getName().toLowerCase();
        this.translateComponent = TranslateComponent.create(prefix, name.toLowerCase());
    }

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return translateComponent.getTranslatedName();
    }

    public V getValue() {
        return value;
    }

    public void setValue(V value) {
        this.value = value;
    }

    public void reset() {
        this.value = this.defaultValue;
    }

    public V getDefaultValue() {
        return defaultValue;
    }

    public boolean isAvailable() {
        return dependency != null && this.dependency.check();
    }

    @FunctionalInterface
    public interface Dependency {
        boolean check();
    }

    public Dependency getDependency() {
        return dependency;
    }
}