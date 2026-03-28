package com.github.epsilon.utils.player;

import net.minecraft.client.Minecraft;

public class MoveUtils {

    private static final Minecraft mc = Minecraft.getInstance();

    public static boolean isMoving() {
        return mc.player.zza != 0 || mc.player.xxa != 0;
    }

}
