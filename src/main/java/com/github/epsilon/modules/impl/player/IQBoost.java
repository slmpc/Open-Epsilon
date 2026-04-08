package com.github.epsilon.modules.impl.player;

import com.github.epsilon.modules.Category;
import com.github.epsilon.modules.Module;
import com.github.epsilon.settings.impl.IntSetting;
import net.minecraft.network.chat.Component;

public class IQBoost extends Module {

    public static final IQBoost INSTANCE = new IQBoost();

    private final IntSetting iq = intSetting("IQ", 520, 0, 5201314, 1);

    private IQBoost() {
        super("IQ Boost", Category.PLAYER);
    }

    @Override
    protected void onEnable() {
        if (mc.player != null) {
            mc.player.sendSystemMessage(Component.literal("恭喜！您的IQ已提升至" + iq.getValue() + "！"));
        }
    }
}
