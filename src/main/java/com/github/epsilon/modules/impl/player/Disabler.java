package com.github.epsilon.modules.impl.player;

import com.github.epsilon.events.PacketEvent;
import com.github.epsilon.modules.Category;
import com.github.epsilon.modules.Module;
import com.github.epsilon.settings.impl.BoolSetting;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.neoforged.bus.api.SubscribeEvent;

import java.util.Random;

public class Disabler extends Module {

    public static final Disabler INSTANCE = new Disabler();

    private Disabler() {
        super("Disabler", Category.PLAYER);
    }

    private final BoolSetting duplicateRotPlace = boolSetting("DuplicateRotPlace", true);
    private final BoolSetting aim360 = boolSetting("AimModulo360", false);
    private final BoolSetting aimDuplicateLook = boolSetting("AimDuplicateLook", false);

    private float playerYaw, lastYaw, lastPitch;

    @SubscribeEvent
    public void onPacket(PacketEvent.Send event) {
        if (aim360.getValue()) {
            if (event.getPacket() instanceof ServerboundMovePlayerPacket packet && packet.hasRotation()) {
                float yaw = packet.yRot;
                if (yaw < 360.0f && yaw > -360.0f) {
                    packet.yRot = yaw + 720f;
                }
                return;
            }
        }

        if (aimDuplicateLook.getValue()) {
            if (event.getPacket() instanceof ServerboundMovePlayerPacket packet) {
                if (packet.hasRotation()) {
                    if (lastYaw == packet.yRot && lastPitch == packet.xRot) {
                        packet.yRot = packet.yRot + 0.001f;
                    }
                    lastYaw = packet.yRot;
                    lastPitch = packet.xRot;
                }
                return;
            }
        }

        if (duplicateRotPlace.getValue()) {
            if (event.getPacket() instanceof ServerboundMovePlayerPacket packet && packet.hasRotation()) {
                float originalYaw = packet.yRot;

                if (originalYaw < 360.0F && originalYaw > -360.0F) {
                    packet.yRot = originalYaw + 720f;
                }

                float lastPlayerYaw = this.playerYaw;
                this.playerYaw = packet.yRot;

                float deltaYaw = Math.abs(this.playerYaw - lastPlayerYaw);
                if (deltaYaw > 2.0F) {
                    Random random = new Random();
                    float perturbation = 0.005f + random.nextFloat() * 0.015f;
                    if (random.nextBoolean()) {
                        packet.yRot = packet.yRot + perturbation;
                    } else {
                        packet.yRot = packet.yRot - perturbation;
                    }
                }
            }
        }
    }

}
