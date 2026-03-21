package com.github.lumin.modules.impl.render;

import com.github.lumin.modules.Category;
import com.github.lumin.modules.Module;
import com.github.lumin.settings.impl.BoolSetting;

public class NoRender extends Module {

    public static final NoRender INSTANCE = new NoRender();

    private NoRender() {
        super("NoRender", Category.RENDER);
    }

    public final BoolSetting vignette = boolSetting("Vignette", true);
    public final BoolSetting potionEffects = boolSetting("PotionEffects", true);
    public final BoolSetting playerNameTags = boolSetting("PlayerNameTags", true);
    public final BoolSetting blockOverlay = boolSetting("BlockOverlay", true);
    public final BoolSetting explosions = boolSetting("Explosions", true);
    public final BoolSetting totems = boolSetting("Totems", true);
    public final BoolSetting totemAnimation = boolSetting("TotemAnimation", true);
    public final BoolSetting portal = boolSetting("Portal", true);
    public final BoolSetting fireworks = boolSetting("Fireworks", true);
    public final BoolSetting fireOverlay = boolSetting("FireOverlay", true);
    public final BoolSetting negativeEffects = boolSetting("NegativeEffects", true);
    public final BoolSetting potionParticles = boolSetting("PotionParticles", true);

    public boolean noVignette() {
        return isEnabled() && vignette.getValue();
    }

    public boolean noPotionEffects() {
        return isEnabled() && potionEffects.getValue();
    }

}
