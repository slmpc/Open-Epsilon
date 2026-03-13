package com.github.lumin.modules.impl.player;

import com.github.lumin.modules.Category;
import com.github.lumin.modules.Module;
import com.github.lumin.settings.impl.IntSetting;

public class BreakCooldown extends Module {

    public static final BreakCooldown INSTANCE = new BreakCooldown();

    private BreakCooldown() {
        super("挖掘延迟", "BreakCooldown", Category.PLAYER);
    }

    public final IntSetting cooldown = intSetting("冷却时间", 0, 0, 5, 1);

}
