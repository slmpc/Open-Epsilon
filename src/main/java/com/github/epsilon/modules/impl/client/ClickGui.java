package com.github.epsilon.modules.impl.client;

import com.github.epsilon.gui.panel.PanelScreen;
import com.github.epsilon.modules.Category;
import com.github.epsilon.modules.Module;
import com.github.epsilon.settings.impl.BoolSetting;
import com.github.epsilon.settings.impl.EnumSetting;

public class ClickGui extends Module {

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

    private ClickGui() {
        super("ClickGui", Category.CLIENT);
    }

    public static final ClickGui INSTANCE = new ClickGui();

    private final BoolSetting backgroundBlur = boolSetting("BackgroundBlur", true);
    public final EnumSetting<ThemeMode> themeMode = enumSetting("ThemeMode", ThemeMode.Dark);
    public final EnumSetting<ThemePreset> themePreset = enumSetting("ThemePreset", ThemePreset.TonalSpot);

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
