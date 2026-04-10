package com.github.epsilon.modules.impl.player;

import com.github.epsilon.events.KeyboardInputEvent;
import com.github.epsilon.events.MotionEvent;
import com.github.epsilon.modules.Category;
import com.github.epsilon.modules.Module;
import com.github.epsilon.settings.impl.DoubleSetting;
import com.github.epsilon.settings.impl.EnumSetting;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.neoforged.bus.api.SubscribeEvent;

public class NoFall extends Module {

    public static final NoFall INSTANCE = new NoFall();

    private NoFall() {
        super("NoFall", Category.PLAYER);
    }

    private enum Mode {
        GroundSpoof,
        Packet,
        GrimMotion
    }

    private final EnumSetting<Mode> mode = enumSetting("Mode", Mode.GroundSpoof);
    private final DoubleSetting fallDistance = doubleSetting("Fall Distance", 3, 3, 16, 1);

    private boolean flag;
    private boolean jump;

    @SubscribeEvent
    private void onMotion(MotionEvent event) {
        if (nullCheck()) return;

        if (mc.player.fallDistance > fallDistance.getValue()) {
            flag = true;
        }

        if (flag && mc.player.onGround()) {
            switch (mode.getValue()) {
                case GroundSpoof -> event.setOnGround(false);
                case Packet -> {
                    mc.getConnection().send(new ServerboundMovePlayerPacket.StatusOnly(false, false));
                }
                case GrimMotion -> {
                    event.setY(event.getY() + 0.1f);
                    jump = true;
                }
            }
            flag = false;
        }
    }

    @SubscribeEvent
    private void onMovementInputEvent(KeyboardInputEvent event) {
        if (jump) {
            event.setJump(true);
            jump = false;
        }
    }

}
