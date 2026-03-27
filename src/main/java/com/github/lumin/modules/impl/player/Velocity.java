package com.github.lumin.modules.impl.player;

import com.github.lumin.events.PacketEvent;
import com.github.lumin.modules.Category;
import com.github.lumin.modules.Module;
import com.github.lumin.modules.impl.combat.AntiBot;
import com.github.lumin.settings.impl.BoolSetting;
import com.github.lumin.settings.impl.DoubleSetting;
import com.github.lumin.settings.impl.EnumSetting;
import com.github.lumin.settings.impl.IntSetting;
import com.github.lumin.utils.player.MoveUtils;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundDisconnectPacket;
import net.minecraft.network.protocol.common.ClientboundPingPacket;
import net.minecraft.network.protocol.game.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Velocity extends Module {

    public static final Velocity INSTANCE = new Velocity();

    private Velocity() {
        super("Velocity", Category.PLAYER);
    }

    private enum Mode {
        Vanilla,
        Legit,
        NoXZ
    }

    private enum VelocityStage {
        NONE,
        DELAY,
        ATTACK,
        CLEAR,
        LAG
    }

    private final EnumSetting<Mode> mode = enumSetting("Mode", Mode.NoXZ);
    public final BoolSetting waterPush = boolSetting("NoWaterPush", true, () -> mode.is(Mode.Vanilla));
    public final BoolSetting entityPush = boolSetting("NoEntityPush", true, () -> mode.is(Mode.Vanilla));
    public final BoolSetting blockPush = boolSetting("NoBlockPush", true, () -> mode.is(Mode.Vanilla));
    private final IntSetting attacks = intSetting("Attacks", 4, 1, 5, 1, () -> mode.is(Mode.NoXZ));
    private final DoubleSetting delayTime = doubleSetting("MaxDelayTime", 2500.0, 50.0, 10000.0, 50.0, () -> mode.is(Mode.NoXZ));

    private Player target;

    private boolean lag;
    private boolean jump;

    private Vec3 velocity;
    private long velocityTime;

    private VelocityStage stage;

    private final Map<Entity, Vec3> targets = new HashMap<>();
    private final Queue<Packet<? super ClientPacketListener>> packets = new ConcurrentLinkedQueue<>();

    @Override
    public void onEnable() {
        jump = false;
        lag = false;
        targets.clear();
        target = null;
        stage = VelocityStage.NONE;
    }

    @Override
    public void onDisable() {
        jump = false;
        lag = false;
        targets.clear();
        target = null;
        stage = VelocityStage.NONE;
        clear(true);
    }

    @SubscribeEvent
    public void onPacket(PacketEvent.Receive event) {
        switch (mode.getValue()) {
            case NoXZ -> {
                if (event.getPacket() instanceof ClientboundPlayerPositionPacket && stage == VelocityStage.NONE) {
                    lag = true;
                    return;
                }
                if (event.getPacket() instanceof ClientboundSetEntityMotionPacket packet && packet.getId() == mc.player.getId()) {
                    if (stage == VelocityStage.NONE) {
                        if (!lag) {
                            stage = VelocityStage.DELAY;
                            velocityTime = System.currentTimeMillis();
                            event.setCanceled(true);
                            velocity = packet.getMovement();
                        } else {
                            lag = false;
                        }
                    } else {
                        velocity = packet.getMovement();
                        stage = VelocityStage.LAG;
                        event.setCanceled(true);
                    }
                    return;
                }

                if (stage != VelocityStage.NONE) {
                    Packet<? super ClientPacketListener> packet = (Packet<? super ClientPacketListener>) event.getPacket();

                    if (packet instanceof ClientboundPlayerPositionPacket) {
                        stage = VelocityStage.LAG;
                        return;
                    }

                    if (packet instanceof ClientboundPlayerLookAtPacket) {
                        stage = VelocityStage.LAG;
                        return;
                    }

                    if (packet instanceof ClientboundDisconnectPacket || packet instanceof ClientboundRespawnPacket) {
                        clear(false);
                        return;
                    }

                    if (!(packet instanceof ClientboundPingPacket) && !(packet instanceof ClientboundMoveEntityPacket) && !(packet instanceof ClientboundTeleportEntityPacket)) {
                        return;
                    }

                    if (packet instanceof ClientboundMoveEntityPacket movePacket) {
                        Entity entity = movePacket.getEntity(mc.level);
                        if (entity != null) {
                            Vec3 currentPos = targets.getOrDefault(entity, entity.position());

                            if (movePacket.hasPosition()) {
                                double dx = movePacket.getXa() / 4096.0D;
                                double dy = movePacket.getYa() / 4096.0D;
                                double dz = movePacket.getZa() / 4096.0D;
                                targets.put(entity, currentPos.add(dx, dy, dz));
                            }
                        }
                    }

                    if (packet instanceof ClientboundTeleportEntityPacket teleportPacket) {
                        Entity entity = mc.level.getEntity(teleportPacket.id());
                        targets.put(entity, teleportPacket.change().position());
                    }

                    packets.add(packet);
                    event.setCanceled(true);
                }
            }

            case Legit -> {
                if (event.getPacket() instanceof ClientboundSetEntityMotionPacket packet && packet.getId() == mc.player.getId()) {
                    jump = true;
                }
            }
        }
    }

    @SubscribeEvent
    public void onPreTick(ClientTickEvent.Pre event) {
        if (nullCheck()) return;

        switch (mode.getValue()) {
            case NoXZ -> {
                if (stage == VelocityStage.ATTACK) {
                    if (mc.hitResult instanceof EntityHitResult entityHitResult && entityHitResult.getEntity() instanceof Player player && !AntiBot.INSTANCE.isBot(player)) {
                        var motionXZ = 1.0D;
                        for (int i = 0; i < attacks.getValue(); i++) {
                            if (mc.player.isSprinting()) mc.player.setSprinting(false);
                            mc.gameMode.attack(mc.player, target);
                            mc.player.swing(InteractionHand.MAIN_HAND);
                            motionXZ *= 0.6D;
                        }
                        mc.player.setDeltaMovement(velocity.x * motionXZ, velocity.y, velocity.z * motionXZ);
                        stage = VelocityStage.CLEAR;
                    }
                } else if (System.currentTimeMillis() - velocityTime >= delayTime.getValue() && stage == VelocityStage.DELAY) {
                    mc.player.setDeltaMovement(velocity.x, velocity.y, velocity.z);
                    stage = VelocityStage.CLEAR;
                }

                if (lag && mc.player.hurtTime == 0) {
                    lag = false;
                }
            }
        }
    }

    public void clear(boolean handle) {
        lag = false;
        stage = VelocityStage.NONE;
        targets.clear();
        target = null;
        if (!handle) {
            packets.clear();
            return;
        }
        while (!packets.isEmpty()) {
            Packet<? super ClientPacketListener> packet = packets.poll();
            packet.handle(mc.getConnection());
        }
    }

    @SubscribeEvent
    public void onMoveInput(MovementInputUpdateEvent event) {
        if (mode.is(Mode.NoXZ)) {
            if (stage == VelocityStage.DELAY && velocity != null && mc.hitResult instanceof EntityHitResult entityHitResult && entityHitResult.getEntity() instanceof Player player && !AntiBot.INSTANCE.isBot(player)) {
                event.getInput().moveVector = new Vec2(1, 0);
                stage = VelocityStage.ATTACK;
                this.target = player;
            }
        }
        if (jump) {
            if (mc.player.onGround() && MoveUtils.isMoving()) {
                event.getInput().makeJump();
            }
            jump = false;
        }
    }

}
