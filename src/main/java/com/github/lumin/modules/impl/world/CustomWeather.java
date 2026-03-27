package com.github.lumin.modules.impl.world;

import com.github.lumin.modules.Category;
import com.github.lumin.modules.Module;
import com.github.lumin.settings.impl.EnumSetting;
import net.minecraft.client.multiplayer.ClientLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

public class CustomWeather extends Module {

    public static final CustomWeather INSTANCE = new CustomWeather();

    private enum Mode {
        Clear,
        Rain,
        Thunder,
        Snow
    }

    private final EnumSetting<Mode> mode = enumSetting("Mode", Mode.Clear);

    private ClientLevel appliedLevel;
    private boolean savedRaining;
    private float savedRainLevel;
    private float savedThunderLevel;

    private CustomWeather() {
        super("CustomWeather", Category.WORLD);
    }

    public boolean isSnowMode() {
        return isEnabled() && mode.is(Mode.Snow);
    }

    @SubscribeEvent
    private void onClientTick(ClientTickEvent.Pre event) {
        if (nullCheck()) {
            return;
        }

        if (appliedLevel != mc.level) {
            saveWeather(mc.level);
        }

        switch (mode.getValue()) {
            case Clear -> applyWeather(false, 0.0F, 0.0F);
            case Rain -> applyWeather(true, 1.0F, 0.0F);
            case Thunder -> applyWeather(true, 1.0F, 1.0F);
            case Snow -> applyWeather(true, 1.0F, 0.0F);
        }
    }

    @Override
    protected void onDisable() {
        if (appliedLevel == null) {
            return;
        }

        appliedLevel.getLevelData().setRaining(savedRaining);
        appliedLevel.setRainLevel(savedRainLevel);
        appliedLevel.setThunderLevel(savedThunderLevel);
        appliedLevel = null;
    }

    private void saveWeather(ClientLevel level) {
        appliedLevel = level;
        savedRaining = level.getLevelData().isRaining();
        savedRainLevel = level.getRainLevel(1.0F);
        savedThunderLevel = level.getThunderLevel(1.0F);
    }

    private void applyWeather(boolean raining, float rainLevel, float thunderLevel) {
        mc.level.getLevelData().setRaining(raining);
        mc.level.setRainLevel(rainLevel);
        mc.level.setThunderLevel(thunderLevel);
    }

}
