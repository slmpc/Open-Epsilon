package com.github.epsilon.modules.impl.player;

import com.github.epsilon.events.KeyboardInputEvent;
import com.github.epsilon.modules.Category;
import com.github.epsilon.modules.Module;
import net.minecraft.util.Mth;

public class MovementFix extends Module {

    public static final MovementFix INSTANCE = new MovementFix();

    private MovementFix() {
        super("Movement Fix", Category.PLAYER);
    }

    public void fixMovement(KeyboardInputEvent event, float yaw) {
        float delta = (mc.player.getYRot() - yaw) * Mth.DEG_TO_RAD;

        float cos = Mth.cos(delta);
        float sin = Mth.sin(delta);

        float left = event.getLeft();
        float forward = event.getForward();

        float fixedLeft = Math.round(left * cos - forward * sin);
        float fixedForward = Math.round(forward * cos + left * sin);

        event.setLeft(fixedLeft);
        event.setForward(fixedForward);
    }

}

