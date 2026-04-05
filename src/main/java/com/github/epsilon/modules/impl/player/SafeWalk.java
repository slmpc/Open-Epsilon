package com.github.epsilon.modules.impl.player;

import com.github.epsilon.events.MotionEvent;
import com.github.epsilon.modules.Category;
import com.github.epsilon.modules.Module;
import com.mojang.blaze3d.platform.InputConstants;
import net.neoforged.bus.api.SubscribeEvent;

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

    @SubscribeEvent
    public void onMotion(MotionEvent e) {
        mc.options.keyShift.setDown(mc.player.onGround() && isOnBlockEdge(0.3F));
    }

    @Override
    public void onDisable() {
        boolean isHoldingShift = InputConstants.isKeyDown(mc.getWindow(), mc.options.keyShift.getKey().getValue());
        mc.options.keyShift.setDown(isHoldingShift);
    }

}