package com.github.lumin.modules.impl.client;

import com.github.lumin.gui.dropdown.DropdownScreen;
import com.github.lumin.modules.Category;
import com.github.lumin.modules.Module;
import com.github.lumin.settings.impl.BoolSetting;
import com.github.lumin.settings.impl.DoubleSetting;
import com.github.lumin.settings.impl.EnumSetting;

public class ClickGui extends Module {

    public static final ClickGui INSTANCE = new ClickGui();

    private ClickGui() {
        super("ClickGui", Category.CLIENT);
    }

    private enum BlurMode {
        FullScreen,
        OnlyCategory,
    }

    public final DoubleSetting scale = doubleSetting("Scale", 1.0, 0.5, 2.0, 0.05);
    private final BoolSetting backgroundBlur = boolSetting("BackgroundBlur", true);
    private final DoubleSetting blurStrength = doubleSetting("BlurStrength", 5, 1.0, 15, 0.5, backgroundBlur::getValue);
    private final EnumSetting<BlurMode> blurMode = enumSetting("BlurMode", BlurMode.OnlyCategory, backgroundBlur::getValue);

    @Override
    protected void onEnable() {
        if (nullCheck()) return;
        mc.setScreen(new DropdownScreen());
    }

    @Override
    protected void onDisable() {
        if (mc.screen instanceof DropdownScreen) {
            mc.setScreen(null);
        }
    }

    public boolean shouldBlur() {
        return backgroundBlur.getValue();
    }

    public boolean isFullScreenBlur() {
        return shouldBlur() && blurMode.is(BlurMode.FullScreen);
    }

    public boolean isCategoryBlur() {
        return shouldBlur() && blurMode.is(BlurMode.OnlyCategory);
    }

    public float getBlurStrength() {
        return blurStrength.getValue().floatValue();
    }

}
