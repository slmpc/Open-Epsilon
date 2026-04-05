package com.github.epsilon.modules.impl.player;

import com.github.epsilon.modules.Category;
import com.github.epsilon.modules.Module;
import com.github.epsilon.settings.impl.EnumSetting;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.util.Mth;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

public class VClip extends Module {

    public static final VClip INSTANCE = new VClip();

    private VClip() {
        super("V Clip", Category.PLAYER);
    }

    private enum Mode {
        Glitch,
        Teleport,
        Jump
    }

    private final EnumSetting<Mode> mode = enumSetting("Mode", Mode.Jump);

    @SubscribeEvent
    private void onTick(ClientTickEvent.Pre event) {
        if (nullCheck()) return;

        toggle();

        switch (mode.getValue()) {
            case Glitch -> {
                double posX = mc.player.getX();
                double posY = Mth.floor(mc.player.getY());
                double posZ = mc.player.getZ();
                boolean onGround = mc.player.onGround();
                mc.getConnection().send(new ServerboundMovePlayerPacket.Pos(posX, posY, posZ, onGround, false));
                double halfY = 0.005;
                posY -= halfY;
                mc.player.setPos(posX, posY, posZ);
                mc.getConnection().send(new ServerboundMovePlayerPacket.Pos(posX, posY, posZ, onGround, false));
                posY -= halfY * 300.0;
                mc.player.setPos(posX, posY, posZ);
                mc.getConnection().send(new ServerboundMovePlayerPacket.Pos(posX, posY, posZ, onGround, false));
            }
            case Teleport -> {
                mc.player.setPos(mc.player.getX(), mc.player.getY() + 3.0, mc.player.getZ());
                mc.getConnection().send(new ServerboundMovePlayerPacket.Pos(mc.player.getX(), mc.player.getY(), mc.player.getZ(), true, false));
            }
            case Jump -> {
                mc.getConnection().send(new ServerboundMovePlayerPacket.Pos(mc.player.getX(), mc.player.getY() + 0.4199999868869781, mc.player.getZ(), false, false));
                mc.getConnection().send(new ServerboundMovePlayerPacket.Pos(mc.player.getX(), mc.player.getY() + 0.7531999805212017, mc.player.getZ(), false, false));
                mc.player.setPos(mc.player.getX(), mc.player.getY() + 1.0, mc.player.getZ());
                mc.getConnection().send(new ServerboundMovePlayerPacket.Pos(mc.player.getX(), mc.player.getY(), mc.player.getZ(), true, false));
            }
        }
    }

}
