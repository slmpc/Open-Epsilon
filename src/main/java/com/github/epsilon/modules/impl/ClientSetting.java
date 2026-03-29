package com.github.epsilon.modules.impl;

import com.github.epsilon.gui.panel.PanelScreen;
import com.github.epsilon.modules.Module;
import com.github.epsilon.settings.impl.BoolSetting;
import com.github.epsilon.settings.impl.EnumSetting;
import com.github.epsilon.settings.impl.KeybindSetting;

public class ClientSetting extends Module {

    public enum ThemePreset {
        TonalSpot,
        Neutral,
        Vibrant,
        Expressive,
        Fidelity,
        Content,
        Rainbow,
        FruitSalad,
        Monochrome
    }

    public enum ThemeMode {
        Dark,
        Light
    }

    private ClientSetting() {
        super("ClientSetting", null);
    }

    public static final ClientSetting INSTANCE = new ClientSetting();

    public final KeybindSetting guiKeybind = addKeybindSetting(new KeybindSetting("Gui Keybind", this, -1) {
        @Override
        public void setValue(Integer value) {
            super.setValue(value);
            syncKeyBind(value != null ? value : -1);
        }
    });

    private final BoolSetting backgroundBlur = boolSetting("BackgroundBlur", true);
    public final EnumSetting<ThemeMode> themeMode = enumSetting("ThemeMode", ThemeMode.Dark);
    public final EnumSetting<ThemePreset> themePreset = enumSetting("ThemePreset", ThemePreset.TonalSpot);

    @Override
    public void setKeyBind(int keyBind) {
        super.setKeyBind(keyBind);
        if (guiKeybind != null && guiKeybind.getValue() != keyBind) {
            guiKeybind.setValueDirect(keyBind);
        }
    }

    private void syncKeyBind(int keyBind) {
        if (getKeyBind() != keyBind) {
            super.setKeyBind(keyBind);
        }
    }

    private KeybindSetting addKeybindSetting(KeybindSetting setting) {
        settings.add(setting);
        return setting;
    }

    @Override
    protected void onEnable() {
        if (nullCheck()) return;
        mc.setScreen(PanelScreen.INSTANCE);
    }

    @Override
    protected void onDisable() {
        if (mc.screen instanceof PanelScreen) {
            mc.setScreen(null);
        }
    }

    public boolean shouldBlur() {
        return backgroundBlur.getValue();
    }

    public ThemePreset getThemePreset() {
        return themePreset.getValue();
    }

    public ThemeMode getThemeMode() {
        return themeMode.getValue();
    }

}
