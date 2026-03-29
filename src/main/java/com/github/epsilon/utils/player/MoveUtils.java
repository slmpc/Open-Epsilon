package com.github.epsilon.utils.player;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec2;

public class MoveUtils {

    private static final Minecraft mc = Minecraft.getInstance();

    public static boolean isMoving() {
        Vec2 moveVector = mc.player.input.getMoveVector();
        return moveVector.x != 0 || moveVector.y != 0;
    }

    public static double[] forwardWithoutStrafe(final double d) {
        float f3 = mc.player.getYRot();
        final double d4 = d * Math.cos(Math.toRadians(f3 + 90.0f));
        final double d5 = d * Math.sin(Math.toRadians(f3 + 90.0f));
        return new double[]{d4, d5};
    }

    public static double[] forward(final double d) {
        Vec2 moveVector = mc.player.input.getMoveVector();
        float f = moveVector.y;
        float f2 = moveVector.x;
        float f3 = mc.player.getYRot();
        if (f != 0.0f) {
            if (f2 > 0.0f) {
                f3 += ((f > 0.0f) ? -45 : 45);
            } else if (f2 < 0.0f) {
                f3 += ((f > 0.0f) ? 45 : -45);
            }
            f2 = 0.0f;
            if (f > 0.0f) {
                f = 1.0f;
            } else if (f < 0.0f) {
                f = -1.0f;
            }
        }
        final double d2 = Math.sin(Math.toRadians(f3 + 90.0f));
        final double d3 = Math.cos(Math.toRadians(f3 + 90.0f));
        final double d4 = f * d * d3 + f2 * d * d2;
        final double d5 = f * d * d2 - f2 * d * d3;
        return new double[]{d4, d5};
    }

}
