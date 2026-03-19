package com.github.lumin.managers;

import com.github.lumin.events.JumpEvent;
import com.github.lumin.events.MotionEvent;
import com.github.lumin.events.RayTraceEvent;
import com.github.lumin.events.StrafeEvent;
import com.github.lumin.utils.player.MoveUtils;
import com.github.lumin.utils.rotation.MovementFix;
import com.github.lumin.utils.rotation.Priority;
import com.github.lumin.utils.rotation.RotationUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.joml.Vector2f;

public class MioRotationManager {

    public static final MioRotationManager INSTANCE = new MioRotationManager();

    private final Minecraft mc = Minecraft.getInstance();

    public Vector2f rotations;
    public Vector2f lastRotations = new Vector2f(0, 0);
    public Vector2f targetRotations;
    private boolean active;
    private double rotationSpeed;
    private MovementFix correctMovement = MovementFix.OFF;
    private int priority;

    private MioRotationManager() {
        NeoForge.EVENT_BUS.register(this);
    }

    public void setRotations(final Vector2f rotations, final double rotationSpeed, final MovementFix correctMovement, Priority priority) {
        if (rotations == null || Double.isNaN(rotations.x) || Double.isNaN(rotations.y) || Double.isInfinite(rotations.x) || Double.isInfinite(rotations.y)) {
            return;
        }
        if (active && priority != null && priority.priority < this.priority) {
            return;
        }
        this.targetRotations = rotations;
        this.rotationSpeed = rotationSpeed * 18;
        this.correctMovement = correctMovement;
        this.priority = priority != null ? priority.priority : Priority.Lowest.priority;
        active = true;
        smooth();
    }

    private void smooth() {
        if (targetRotations == null) return;
        rotations = RotationUtils.smooth(new Vector2f(targetRotations.x, targetRotations.y), rotationSpeed + Math.random());
        if (Float.isNaN(rotations.x) || Float.isInfinite(rotations.x)) {
            rotations.x = mc.player.getYRot();
        }
        if (Float.isNaN(rotations.y) || Float.isInfinite(rotations.y)) {
            rotations.y = mc.player.getXRot();
        }
    }

    public boolean isActive() {
        return active;
    }

    public boolean isMovementFixEnabled() {
        return active && correctMovement == MovementFix.ON && rotations != null;
    }

    public float getYaw() {
        if (rotations == null) return mc.player.getYRot();
        return rotations.x;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    private void onClientTick(ClientTickEvent.Pre event) {
        if (mc.player == null || mc.level == null) return;
        if (active) smooth();
    }

    @SubscribeEvent
    private void onMoveInput(MovementInputUpdateEvent event) {
        if (active && correctMovement == MovementFix.ON && rotations != null) {
            MoveUtils.fixMovement(event, rotations.x);
        }
    }

    @SubscribeEvent
    private void onRaytrace(RayTraceEvent event) {
        if (active && rotations != null) {
            event.setYaw(rotations.x);
            event.setPitch(rotations.y);
        }
    }

    @SubscribeEvent
    private void onStrafe(StrafeEvent event) {
        if (active && correctMovement == MovementFix.ON && rotations != null) {
            event.setYaw(rotations.x);
        }
    }

    @SubscribeEvent
    private void onJump(JumpEvent event) {
        if (active && correctMovement == MovementFix.ON && rotations != null) {
            event.setYaw(rotations.x);
        }
    }

    @SubscribeEvent
    private void onMotion(MotionEvent event) {
        if (active && rotations != null) {
            float yaw = rotations.x;
            float pitch = rotations.y;
            if (Float.isNaN(yaw) || Float.isInfinite(yaw)) yaw = mc.player.getYRot();
            if (Float.isNaN(pitch) || Float.isInfinite(pitch)) pitch = mc.player.getXRot();
            pitch = Mth.clamp(pitch, -90.0f, 90.0f);
            event.setYaw(yaw);
            event.setPitch(pitch);

            if (Math.abs((rotations.x - mc.player.getYRot()) % 360) < 1 && Math.abs((rotations.y - mc.player.getXRot())) < 1) {
                active = false;
                priority = 0;
            }

            lastRotations = rotations;
        } else {
            lastRotations = new Vector2f(mc.player.getYRot(), mc.player.getXRot());
        }
    }
}
