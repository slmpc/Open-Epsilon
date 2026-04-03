package com.github.epsilon.modules.impl.player;

import com.github.epsilon.events.KeyboardInputEvent;
import com.github.epsilon.modules.Category;
import com.github.epsilon.modules.Module;
import com.github.epsilon.utils.math.MathUtils;
import net.minecraft.util.Mth;

/**
 * @author jiuxian_baka
 */
public class MovementFix extends Module {

    public static final MovementFix INSTANCE = new MovementFix();

    private MovementFix() {
        super("MovementFix", Category.PLAYER);
    }

    private double getDirection(float rotationYaw, final double moveForward, final double moveStrafing) {
        if (moveForward < 0F) rotationYaw += 180F;

        float forward = 1F;

        if (moveForward < 0F) forward = -0.5F;
        else if (moveForward > 0F) forward = 0.5F;

        if (moveStrafing > 0F) rotationYaw -= 90F * forward;
        if (moveStrafing < 0F) rotationYaw += 90F * forward;

        return Math.toRadians(rotationYaw);
    }

    public void fixMovement(KeyboardInputEvent event, float yaw) {
        float left = event.getLeft();
        float forward = event.getForward();

        double angle = Mth.wrapDegrees(Math.toDegrees(getDirection(mc.player.getYRot(), forward, left)));

        if (forward == 0 && left == 0) {
            return;
        }

        float closestForward = 0, closestStrafe = 0, closestDifference = Float.MAX_VALUE;

        for (float predictedForward = -1F; predictedForward <= 1F; predictedForward += 1F) {
            for (float predictedStrafe = -1F; predictedStrafe <= 1F; predictedStrafe += 1F) {
                if (predictedStrafe == 0 && predictedForward == 0) continue;

                final double predictedAngle = Mth.wrapDegrees(Math.toDegrees(getDirection(yaw, predictedForward, predictedStrafe)));
                final double difference = MathUtils.wrappedDifference(angle, predictedAngle);

                if (difference < closestDifference) {
                    closestDifference = (float) difference;
                    closestForward = predictedForward;
                    closestStrafe = predictedStrafe;
                }
            }
        }

        event.setLeft(closestStrafe);
        event.setForward(closestForward);
    }

}

