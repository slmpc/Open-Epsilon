package com.github.epsilon.settings.impl;

import com.github.epsilon.modules.Module;
import com.github.epsilon.settings.Setting;

public class ButtonSetting extends Setting<Runnable> {

    public ButtonSetting(String name, Module module, Runnable func, Dependency dependency) {
        super(name, module, dependency);
        this.value = func;
        this.defaultValue = func;
    }

    public ButtonSetting(String name, Module module, Runnable func) {
        this(name, module, func, () -> true);
    }

}