package com.github.lumin.modules.impl.player;

import com.github.lumin.events.PacketEvent;
import com.github.lumin.modules.Category;
import com.github.lumin.modules.Module;
import com.github.lumin.settings.impl.BoolSetting;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.neoforged.bus.api.SubscribeEvent;

import java.util.Random;

public class Disabler extends Module {
    public static final Disabler INSTANCE = new Disabler();
    private final BoolSetting DuplicateRotPlace = boolSetting("DuplicateRotPlace", true);
    private final BoolSetting aim360 = boolSetting("AimModulo360", false);
    private final BoolSetting AimDuplicateLook = new BoolSetting("AimDuplicateLook", false);

    private Disabler() {
        super("残疾人", "Disabler", Category.PLAYER);
    }

    private float playerYaw;
    private float deltaYaw;
    float lastYaw, lastPitch = 0;
    private float lastPlacedDeltaYaw;

    @SubscribeEvent
    public void onPacket(PacketEvent.Send event) {
        if (aim360.getValue()) {
            if (event.getPacket() instanceof ServerboundMovePlayerPacket packet && packet.hasRotation()) {
                float yaw = packet.getYRot(0);
                if (yaw < 360.0f && yaw > -360.0f) {
                    packet.yRot = yaw + 720f;
                }
            }
        }

        if (AimDuplicateLook.getValue()) {
            if (event.getPacket() instanceof ServerboundMovePlayerPacket c03) {
                if (c03.hasRotation()) {
                    if (lastYaw == c03.getYRot(0) && lastPitch == c03.getXRot(0)) {
                        c03.yRot = c03.getYRot(0) + 0.001f;
                    }
                    lastYaw = c03.getYRot(0);
                    lastPitch = c03.getXRot(0);
                }
            }
        }

        if (DuplicateRotPlace.getValue()) {
            if (event.getPacket() instanceof ServerboundMovePlayerPacket packet) {
                if (packet.hasRotation()) {
                    float originalYaw = packet.getYRot(0.0F);

                    if (originalYaw < 360.0F && originalYaw > -360.0F) {
                        packet.yRot = originalYaw + 720f;
                    }

                    float lastPlayerYaw = this.playerYaw;
                    this.playerYaw = packet.getYRot(0.0F);
                    this.deltaYaw = Math.abs(this.playerYaw - lastPlayerYaw);

                    if (this.deltaYaw > 2.0F) {
                        float xDiff = Math.abs(this.deltaYaw - this.lastPlacedDeltaYaw);
                        Random random = new Random();
                        float perturbation = 0.005f + random.nextFloat() * 0.015f;
                        if (random.nextBoolean()) {
                            packet.yRot = packet.getYRot(0) + perturbation;
                        } else {
                            packet.yRot = packet.getYRot(0) - perturbation;
                        }

                        this.lastPlacedDeltaYaw = this.deltaYaw;
                    }
                }
            }
        }
    }
}
