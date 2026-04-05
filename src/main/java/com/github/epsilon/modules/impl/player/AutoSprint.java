package com.github.epsilon.modules.impl.player;

import com.github.epsilon.modules.Category;
import com.github.epsilon.modules.Module;
import com.github.epsilon.modules.impl.combat.KillAura;
import com.github.epsilon.settings.impl.BoolSetting;
import com.github.epsilon.settings.impl.DoubleSetting;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

public class AutoSprint extends Module {

    public static final AutoSprint INSTANCE = new AutoSprint();

    private AutoSprint() {
        super("Auto Sprint", Category.PLAYER);
    }

    public final BoolSetting keepSprint = boolSetting("Keep Sprint", true);
    public final DoubleSetting motion = doubleSetting("Motion", 1.0, 0.0, 1.0, 0.1, keepSprint::getValue);
    private final BoolSetting stopWhileUsing = boolSetting("Stop While Using", false);
    private final BoolSetting pauseWhileAura = boolSetting("Pause While Aura", false);

    @SubscribeEvent
    private void onClientTick(ClientTickEvent.Pre event) {
        if (nullCheck()) return;
        mc.player.setSprinting(
                mc.player.getFoodData().getFoodLevel() > 6
                        && !mc.player.horizontalCollision
                        && mc.player.input.hasForwardImpulse()
                        && (!mc.player.isUsingItem() || !stopWhileUsing.getValue())
                        && (!pauseWhileAura.getValue() || !KillAura.INSTANCE.isEnabled() || KillAura.INSTANCE.target == null));
    }

}