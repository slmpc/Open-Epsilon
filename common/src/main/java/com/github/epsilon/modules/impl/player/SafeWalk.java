package com.github.epsilon.modules.impl.player;
import com.github.epsilon.Epsilon;

import com.github.epsilon.events.movement.MotionEvent;
import com.github.epsilon.modules.Category;
import com.github.epsilon.modules.Module;
import com.mojang.blaze3d.platform.InputConstants;
import com.github.epsilon.events.bus.EventHandler;

public class SafeWalk extends Module {

    public static final SafeWalk INSTANCE = new SafeWalk();

    private SafeWalk() {
        super("Safe Walk", Category.PLAYER);
    }

    public boolean isOnBlockEdge(float sensitivity) {
        return !mc.level
                .getCollisions(mc.player, mc.player.getBoundingBox().move(0.0, -0.5, 0.0).inflate(-sensitivity, 0.0, -sensitivity))
                .iterator()
                .hasNext();
    }

    @EventHandler
    public void onMotion(MotionEvent e) {
        mc.options.keyShift.setDown(mc.player.onGround() && isOnBlockEdge(0.3F));
    }

    @Override
    public void onDisable() {
        boolean isHoldingShift = InputConstants.isKeyDown(mc.getWindow(), Epsilon.platform.getKeyMappingKey(mc.options.keyShift).getValue());
        mc.options.keyShift.setDown(isHoldingShift);
    }

}