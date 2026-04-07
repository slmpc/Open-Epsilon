package com.github.epsilon.modules.impl.player;

import com.github.epsilon.modules.Category;
import com.github.epsilon.modules.Module;
import com.github.epsilon.modules.impl.combat.KillAura;
import com.github.epsilon.settings.impl.BoolSetting;
import com.github.epsilon.settings.impl.DoubleSetting;
import com.github.epsilon.settings.impl.EnumSetting;
import com.github.epsilon.utils.player.MoveUtils;
import com.mojang.blaze3d.platform.InputConstants;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

public class AutoSprint extends Module {

    public static final AutoSprint INSTANCE = new AutoSprint();

    private AutoSprint() {
        super("Auto Sprint", Category.PLAYER);
    }

    private enum Mode {
        Normal,
        Strict
    }

    private final EnumSetting<Mode> mode = enumSetting("Mode", Mode.Normal);
    public final BoolSetting keepSprint = boolSetting("Keep Sprint", true, () -> mode.is(Mode.Normal));
    public final DoubleSetting motion = doubleSetting("Motion", 1.0, 0.0, 1.0, 0.1, () -> mode.is(Mode.Normal) && keepSprint.getValue());
    private final BoolSetting allDirections = boolSetting("All Directions", false, () -> mode.is(Mode.Normal));
    private final BoolSetting stopWhileUsing = boolSetting("Stop While Using", false, () -> mode.is(Mode.Normal));
    private final BoolSetting pauseWhileAura = boolSetting("Pause While Aura", false, () -> mode.is(Mode.Strict));

    @Override
    public void onDisable() {
        syncSprintKeyState();
    }

    @SubscribeEvent
    private void onClientTick(ClientTickEvent.Pre event) {
        if (nullCheck()) {
            syncSprintKeyState();
            return;
        }

        switch (mode.getValue()) {
            case Normal -> handleNormalMode();
            case Strict -> handleStrictMode();
        }
    }

    private void handleNormalMode() {
        syncSprintKeyState();
        if (mc.player != null) {
            mc.player.setSprinting(
                    mc.player.getFoodData().getFoodLevel() > 6
                            && !mc.player.horizontalCollision
                            && (allDirections.getValue() ? MoveUtils.isMoving() : mc.player.input.hasForwardImpulse())
                            && (!mc.player.isUsingItem() || !stopWhileUsing.getValue()));
        }
    }

    private void handleStrictMode() {
        if (shouldPauseWhileAura()) {
            syncSprintKeyState();
            return;
        }
        mc.options.keySprint.setDown(true);
    }

    private boolean shouldPauseWhileAura() {
        return pauseWhileAura.getValue() && KillAura.INSTANCE.isEnabled() && KillAura.INSTANCE.target != null;
    }

    private void syncSprintKeyState() {
        boolean isHoldingSprint = InputConstants.isKeyDown(mc.getWindow(), mc.options.keySprint.getKey().getValue());
        mc.options.keySprint.setDown(isHoldingSprint);
    }

}
