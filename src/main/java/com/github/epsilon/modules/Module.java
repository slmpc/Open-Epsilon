package com.github.epsilon.modules;

import com.github.epsilon.assets.i18n.TranslateComponent;
import com.github.epsilon.settings.Setting;
import com.github.epsilon.settings.impl.*;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.common.NeoForge;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class Module {

    private final String name;

    public Category category;

    private int keyBind = -1;

    public enum BindMode {Toggle, Hold}

    private BindMode bindMode = BindMode.Toggle;

    private boolean enabled;

    public final List<Setting<?>> settings = new ArrayList<>();

    protected final Minecraft mc;

    public final TranslateComponent translateComponent;

    public Module(String name, Category category) {
        this.name = name;
        this.category = category;

        translateComponent = TranslateComponent.create("modules", name.toLowerCase());
        mc = Minecraft.getInstance();
    }

    protected boolean nullCheck() {
        return mc.player == null || mc.level == null;
    }

    protected void onEnable() {
    }

    protected void onDisable() {
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void toggle() {
        setEnabled(!enabled);
    }

    public void setEnabled(boolean enabled) {
        if (this.enabled != enabled) {
            this.enabled = enabled;
            if (enabled) {
                try {
                    NeoForge.EVENT_BUS.register(this);
                } catch (Exception ignored) {
                }
                onEnable();
            } else {
                try {
                    NeoForge.EVENT_BUS.unregister(this);
                } catch (Exception ignored) {
                }
                onDisable();
            }
        }
    }

    public void reset() {
        setEnabled(false);
        bindMode = BindMode.Toggle;
        for (Setting<?> setting : settings) {
            setting.reset();
        }
    }

    protected <T extends Setting<?>> T addSetting(T setting) {
        settings.add(setting);
        return setting;
    }

    public List<Setting<?>> getSettings() {
        return settings;
    }

    public int getKeyBind() {
        return keyBind;
    }

    public void setKeyBind(int keyBind) {
        this.keyBind = keyBind;
    }

    public BindMode getBindMode() {
        return bindMode;
    }

    public void setBindMode(BindMode bindMode) {
        this.bindMode = bindMode;
    }

    public String getName() {
        return name;
    }

    public String getTranslatedName() {
        return translateComponent.getTranslatedName();
    }

    public String getDescription() {
        return name;
    }

    public String getChineseDescription() {
        return name;
    }

    protected IntSetting intSetting(String name, int defaultValue, int min, int max, int step, Setting.Dependency dependency) {
        return addSetting(new IntSetting(name, this, defaultValue, min, max, step, dependency, false));
    }

    protected IntSetting intSetting(String name, int defaultValue, int min, int max, int step, Setting.Dependency dependency, boolean percentageMode) {
        return addSetting(new IntSetting(name, this, defaultValue, min, max, step, dependency, percentageMode));
    }

    protected IntSetting intSetting(String name, int defaultValue, int min, int max, int step) {
        return addSetting(new IntSetting(name, this, defaultValue, min, max, step));
    }

    protected BoolSetting boolSetting(String name, boolean defaultValue, Setting.Dependency dependency) {
        return addSetting(new BoolSetting(name, this, defaultValue, dependency));
    }

    protected BoolSetting boolSetting(String name, boolean defaultValue) {
        return addSetting(new BoolSetting(name, this, defaultValue));
    }

    protected DoubleSetting doubleSetting(String name, double defaultValue, double min, double max, double step, Setting.Dependency dependency) {
        return addSetting(new DoubleSetting(name, this, defaultValue, min, max, step, dependency, false));
    }

    protected DoubleSetting doubleSetting(String name, double defaultValue, double min, double max, double step, Setting.Dependency dependency, boolean percentageMode) {
        return addSetting(new DoubleSetting(name, this, defaultValue, min, max, step, dependency, percentageMode));
    }

    protected DoubleSetting doubleSetting(String name, double defaultValue, double min, double max, double step) {
        return addSetting(new DoubleSetting(name, this, defaultValue, min, max, step));
    }

    protected StringSetting stringSetting(String name, String defaultValue, Setting.Dependency dependency) {
        return addSetting(new StringSetting(name, this, defaultValue, dependency));
    }

    protected StringSetting stringSetting(String name, String defaultValue) {
        return addSetting(new StringSetting(name, this, defaultValue));
    }

    protected <E extends Enum<E>> EnumSetting<E> enumSetting(String name, E defaultValue, Setting.Dependency dependency) {
        return addSetting(new EnumSetting<>(name, this, defaultValue, dependency));
    }

    protected <E extends Enum<E>> EnumSetting<E> enumSetting(String name, E defaultValue) {
        return addSetting(new EnumSetting<>(name, this, defaultValue, () -> true));
    }

    protected ColorSetting colorSetting(String name, Color defaultValue, Setting.Dependency dependency) {
        return addSetting(new ColorSetting(name, this, defaultValue, dependency));
    }

    protected ColorSetting colorSetting(String name, Color defaultValue) {
        return addSetting(new ColorSetting(name, this, defaultValue));
    }

    protected KeybindSetting keybindSetting(String name, int defaultValue, Setting.Dependency dependency) {
        return addSetting(new KeybindSetting(name, this, defaultValue, dependency));
    }

    protected KeybindSetting keybindSetting(String name, int defaultValue) {
        return addSetting(new KeybindSetting(name, this, defaultValue));
    }

    protected ButtonSetting buttonSetting(String name, Runnable func) {
        return addSetting(new ButtonSetting(name, this, func));
    }

    protected ButtonSetting buttonSetting(String name, Runnable func, Setting.Dependency dependency) {
        return addSetting(new ButtonSetting(name, this, func, dependency));
    }

}