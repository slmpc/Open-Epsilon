package com.github.epsilon.modules.impl.combat;

import com.github.epsilon.managers.TargetManager;
import com.github.epsilon.modules.Category;
import com.github.epsilon.modules.Module;
import com.github.epsilon.modules.impl.combat.AntiBot;
import com.github.epsilon.settings.impl.BoolSetting;
import com.github.epsilon.settings.impl.ColorSetting;
import com.github.epsilon.settings.impl.DoubleSetting;
import com.github.epsilon.settings.impl.EnumSetting;
import com.github.epsilon.settings.impl.IntSetting;
import com.github.epsilon.utils.math.MathUtils;
import com.github.epsilon.utils.player.ChatUtils;
import com.github.epsilon.utils.render.Render3DUtils;
import com.github.epsilon.utils.rotation.RotationUtils;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/* 
* Author Moli
* 平滑测试 平滑, 距离, DVD, 6.2, 100, 180, 6, 500, 0.3, 0.25, 0.31, 1.5, 10, 2, 3, 0.03, 0.02, 12, 5
* todo: 考虑引入机器学习预测目标轨迹、优化反作弊绕过。
*/

public class AimAssist extends Module {

    public static final AimAssist INSTANCE = new AimAssist();

    public EnumSetting<Mode> mode = enumSetting("Mode", Mode.Smooth);
    public DoubleSetting range = doubleSetting("Range", 4.2, 1.0, 8.0, 0.1);
    public IntSetting aimStrength = intSetting("Aim Strength", 30, 1, 100, 1);
    public IntSetting aimSmooth = intSetting("Aim Smooth", 45, 1, 180, 1);
    public IntSetting aimTime = intSetting("Aim Time", 2, 1, 10, 1);
    public BoolSetting ignoreWalls = boolSetting("Ignore Walls", true);
    public IntSetting reactionTime = intSetting("Reaction Time", 80, 1, 500, 1);
    public BoolSetting ignoreInvisible = boolSetting("Ignore Invisible", false);
    public BoolSetting ignoreScreen = boolSetting("Ignore Screen", true);
    public BoolSetting ignoreInventory = boolSetting("Ignore Inventory", true);
    public BoolSetting player = boolSetting("Player", true);
    public BoolSetting mob = boolSetting("Mob", true);
    public BoolSetting animal = boolSetting("Animal", true);
    public BoolSetting villager = boolSetting("Villager", true); // 为了在l站测试加的。不然还要再往后拖拖。
    public BoolSetting slime = boolSetting("Slime", true); // 史莱姆嫖娼。
    public DoubleSetting humanJitter = doubleSetting("Human Jitter", 0.3, 0.0, 2.0, 0.1);
    public DoubleSetting humanOvershoot = doubleSetting("Human Overshoot", 0.15, 0.0, 0.5, 0.05);
    public DoubleSetting inertia = doubleSetting("Inertia", 0.85, 0.0, 0.99, 0.01);
    public BoolSetting lockTarget = boolSetting("Lock Target", false);
    public BoolSetting lockedEsp = boolSetting("Locked ESP", false);
    public ColorSetting lockedEspColor = colorSetting("Locked ESP Color", new Color(255, 0, 0, 150));
    public BoolSetting closeTargetBoost = boolSetting("Close Target Boost", false);
    public DoubleSetting closeTargetBoostStrength = doubleSetting("Boost Strength", 1.5, 1.0, 3.0, 0.1);
    public DoubleSetting closeTargetThreshold = doubleSetting("Close Threshold", 10.0, 1.0, 30.0, 1.0);
    public DoubleSetting responsiveness = doubleSetting("Responsiveness", 1.2, 1.0, 2.0, 0.1);
    public EnumSetting<PriorityMode> targetPriority = enumSetting("Target Priority", PriorityMode.Distance);
    public BoolSetting prediction = boolSetting("Prediction", true);
    public IntSetting predictionTicks = intSetting("Prediction Ticks", 2, 0, 20, 1);
    public BoolSetting extrapolateVelocity = boolSetting("Extrapolate Velocity", true);
    public EnumSetting<AimPart> aimPart = enumSetting("Aim Part", AimPart.Torso);
    public DoubleSetting dvdSpeed = doubleSetting("DVD Speed", 0.03, 0.005, 0.1, 0.005);
    public DoubleSetting dvdJitter = doubleSetting("DVD Jitter", 0.02, 0.0, 0.1, 0.005);
    public BoolSetting dvdDebug = boolSetting("DVD Debug", false);
    public BoolSetting pauseWhileEating = boolSetting("Pause While Eating", true);
    public BoolSetting snapCompensation = boolSetting("Snap Compensation", true);
    public DoubleSetting snapThreshold = doubleSetting("Snap Threshold", 20.0, 5.0, 90.0, 1.0);
    public DoubleSetting snapStrength = doubleSetting("Snap Strength", 2.5, 1.0, 5.0, 0.1);

    private static class RotationState {
        float yaw, pitch;

        RotationState() {
            this.yaw = Float.NaN;
            this.pitch = Float.NaN;
        }

        RotationState(float yaw, float pitch) {
            this.yaw = yaw;
            this.pitch = pitch;
        }

        void set(float yaw, float pitch) {
            this.yaw = yaw;
            this.pitch = pitch;
        }

        void setNaN() {
            this.yaw = Float.NaN;
            this.pitch = Float.NaN;
        }

        boolean isNaN() {
            return Float.isNaN(yaw) || Float.isNaN(pitch);
        }

        void multiply(float factor) {
            this.yaw *= factor;
            this.pitch *= factor;
        }
    }

    private static class MotionState {
        double prevX, prevY, prevZ;
        double velX, velY, velZ;
        double accelX, accelY, accelZ;

        void update(double currentX, double currentY, double currentZ) {
            double newVelX = currentX - prevX;
            double newVelY = currentY - prevY;
            double newVelZ = currentZ - prevZ;

            accelX = newVelX - velX;
            accelY = newVelY - velY;
            accelZ = newVelZ - velZ;

            velX = newVelX;
            velY = newVelY;
            velZ = newVelZ;

            prevX = currentX;
            prevY = currentY;
            prevZ = currentZ;
        }

        void reset() {
            velX = 0; velY = 0; velZ = 0;
            accelX = 0; accelY = 0; accelZ = 0;
        }
    }

    private static class DVDBounds {
        double minX, maxX, minY, maxY, minZ, maxZ;
    }

    private static class Vector3 {
        double x, y, z;

        Vector3(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    private static class DVDAnimation {
        double pointX, pointY, pointZ;
        double velocityX, velocityY, velocityZ;
        double targetVelX, targetVelY, targetVelZ;
        double accelX, accelY, accelZ;
        double jitterX, jitterY, jitterZ;
        int jitterTick = 0;
        LivingEntity target;
        long lastDebugTime = 0;

        private static final float JITTER_UPDATE_INTERVAL = 3;
        private static final float ACCELERATION = 0.15f;
        private static final float VELOCITY_DAMPING = 0.92f;
        private static final float JITTER_SMOOTH = 0.3f;

        void reset() {
            pointX = 0;
            pointY = 0;
            pointZ = 0;
            velocityX = 0;
            velocityY = 0;
            velocityZ = 0;
            targetVelX = 0;
            targetVelY = 0;
            targetVelZ = 0;
            resetDynamicStates();
            target = null;
            lastDebugTime = 0;
        }

        void resetDynamicStates() {
            accelX = 0;
            accelY = 0;
            accelZ = 0;
            jitterX = 0;
            jitterY = 0;
            jitterZ = 0;
            jitterTick = 0;
        }

        void update(LivingEntity entity, double speed, double jitterAmount, boolean debug) {
            AABB bb = entity.getBoundingBox();

            if (target != entity) {
                initialize(bb);
                target = entity;
            }

            DVDBounds bounds = calculateBounds(bb);

            updateTargetVelocity(bounds);
            updateAcceleration();
            updateVelocity();
            updatePosition(speed);
            updateJitter(jitterAmount);
            handleBounce(bounds);

            if (debug) {
                long now = System.currentTimeMillis();
                if (now - lastDebugTime > 1000) {
                    lastDebugTime = now;
                    sendDebugInfo(bb, bounds);
                }
            }
        }

        private DVDBounds calculateBounds(AABB bb) {
            DVDBounds bounds = new DVDBounds();
            double width = bb.maxX - bb.minX;
            double height = bb.maxY - bb.minY;
            double boxSize = width * 0.8;
            double halfBoxSize = boxSize / 2.0;
            bounds.minX = -halfBoxSize;
            bounds.maxX = halfBoxSize;
            bounds.minY = height / 2.0 - halfBoxSize;
            bounds.maxY = height / 2.0 + halfBoxSize;
            bounds.minZ = -halfBoxSize;
            bounds.maxZ = halfBoxSize;
            return bounds;
        }

        private void initialize(AABB bb) {
            DVDBounds bounds = calculateBounds(bb);

            pointX = bounds.minX + Math.random() * (bounds.maxX - bounds.minX);
            pointY = bounds.minY + Math.random() * (bounds.maxY - bounds.minY);
            pointZ = bounds.minZ + Math.random() * (bounds.maxZ - bounds.minZ);

            do {
                velocityX = Math.random() * 2 - 1;
                velocityY = Math.random() * 2 - 1;
                velocityZ = Math.random() * 2 - 1;
            } while (Math.abs(velocityX) < 0.3 && Math.abs(velocityY) < 0.3 && Math.abs(velocityZ) < 0.3);

            Vector3 normalized = normalizeVector(velocityX, velocityY, velocityZ);
            velocityX = normalized.x;
            velocityY = normalized.y;
            velocityZ = normalized.z;

            targetVelX = velocityX;
            targetVelY = velocityY;
            targetVelZ = velocityZ;

            resetDynamicStates();
        }

        private void updateTargetVelocity(DVDBounds bounds) {
            double centerX = (bounds.minX + bounds.maxX) / 2.0;
            double centerY = (bounds.minY + bounds.maxY) / 2.0;
            double centerZ = (bounds.minZ + bounds.maxZ) / 2.0;

            double dxToCenter = centerX - pointX;
            double dyToCenter = centerY - pointY;
            double dzToCenter = centerZ - pointZ;

            double distToCenter = Math.sqrt(dxToCenter * dxToCenter + dyToCenter * dyToCenter + dzToCenter * dzToCenter);
            if (distToCenter > 0.001) {
                targetVelX = velocityX * 0.7 + (dxToCenter / distToCenter) * 0.3;
                targetVelY = velocityY * 0.7 + (dyToCenter / distToCenter) * 0.3;
                targetVelZ = velocityZ * 0.7 + (dzToCenter / distToCenter) * 0.3;
            }

            Vector3 normalized = normalizeVector(targetVelX, targetVelY, targetVelZ);
            targetVelX = normalized.x;
            targetVelY = normalized.y;
            targetVelZ = normalized.z;
        }

        private void updateAcceleration() {
            accelX = (targetVelX - velocityX) * ACCELERATION;
            accelY = (targetVelY - velocityY) * ACCELERATION;
            accelZ = (targetVelZ - velocityZ) * ACCELERATION;
        }

        private void updateVelocity() {
            velocityX += accelX;
            velocityY += accelY;
            velocityZ += accelZ;

            velocityX *= VELOCITY_DAMPING;
            velocityY *= VELOCITY_DAMPING;
            velocityZ *= VELOCITY_DAMPING;

            Vector3 normalized = normalizeVector(velocityX, velocityY, velocityZ);
            velocityX = normalized.x;
            velocityY = normalized.y;
            velocityZ = normalized.z;
        }

        private void updatePosition(double speed) {
            pointX += velocityX * speed;
            pointY += velocityY * speed;
            pointZ += velocityZ * speed;
        }

        private void updateJitter(double jitterAmount) {
            jitterTick++;
            if (jitterTick >= JITTER_UPDATE_INTERVAL) {
                jitterTick = 0;
                double targetJitterX = (Math.random() - 0.5) * jitterAmount;
                double targetJitterY = (Math.random() - 0.5) * jitterAmount;
                double targetJitterZ = (Math.random() - 0.5) * jitterAmount;

                jitterX = jitterX * (1 - JITTER_SMOOTH) + targetJitterX * JITTER_SMOOTH;
                jitterY = jitterY * (1 - JITTER_SMOOTH) + targetJitterY * JITTER_SMOOTH;
                jitterZ = jitterZ * (1 - JITTER_SMOOTH) + targetJitterZ * JITTER_SMOOTH;
            }

            pointX += jitterX;
            pointY += jitterY;
            pointZ += jitterZ;
        }

        private void handleBounce(DVDBounds bounds) {
            AxisBounceResult resultX = handleAxisBounce(pointX, bounds.minX, bounds.maxX, velocityX, targetVelX);
            AxisBounceResult resultY = handleAxisBounce(pointY, bounds.minY, bounds.maxY, velocityY, targetVelY);
            AxisBounceResult resultZ = handleAxisBounce(pointZ, bounds.minZ, bounds.maxZ, velocityZ, targetVelZ);

            pointX = resultX.position; velocityX = resultX.velocity; targetVelX = resultX.targetVelocity;
            pointY = resultY.position; velocityY = resultY.velocity; targetVelY = resultY.targetVelocity;
            pointZ = resultZ.position; velocityZ = resultZ.velocity; targetVelZ = resultZ.targetVelocity;
        }

        private AxisBounceResult handleAxisBounce(double value, double min, double max,
                                                   double velocity, double targetVelocity) {
            AxisBounceResult result = new AxisBounceResult();

            if (value > max) {
                result.position = max;
                result.velocity = -Math.abs(velocity);
                result.targetVelocity = -Math.abs(targetVelocity);
            } else if (value < min) {
                result.position = min;
                result.velocity = Math.abs(velocity);
                result.targetVelocity = Math.abs(targetVelocity);
            } else {
                result.position = value;
                result.velocity = velocity;
                result.targetVelocity = targetVelocity;
            }

            return result;
        }

        // dvd debug
        private void sendDebugInfo(AABB bb, DVDBounds bounds) {
            String debugInfo = String.format(
                "DVD Debug: Pos(%.3f, %.3f, %.3f) | Vel(%.2f, %.2f, %.2f) | TargetVel(%.2f, %.2f, %.2f) | Box[%.2f, %.2f] [%.2f, %.2f] [%.2f, %.2f]",
                pointX, pointY, pointZ,
                velocityX, velocityY, velocityZ,
                targetVelX, targetVelY, targetVelZ,
                bounds.minX, bounds.maxX, bounds.minY, bounds.maxY, bounds.minZ, bounds.maxZ
            );
            ChatUtils.addChatMessage(debugInfo);
        }

        private static Vector3 normalizeVector(double x, double y, double z) {
            double len = Math.sqrt(x * x + y * y + z * z);
            if (len > 0.001) {
                return new Vector3(x / len, y / len, z / len);
            }
            return new Vector3(x, y, z);
        }

        private static class AxisBounceResult {
            double position;
            double velocity;
            double targetVelocity;
        }
    }

    private static class PredictionState {
        double x, y, z;
        boolean initialized;

        void reset() {
            x = 0;
            y = 0;
            z = 0;
            initialized = false;
        }

        void initialize(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.initialized = true;
        }

        void lerp(double delta, double targetX, double targetY, double targetZ) {
            if (!initialized) {
                initialize(targetX, targetY, targetZ);
            } else {
                this.x = Mth.lerp(delta, this.x, targetX);
                this.y = Mth.lerp(delta, this.y, targetY);
                this.z = Mth.lerp(delta, this.z, targetZ);
            }
        }
    }

    private final RotationState rotation = new RotationState();
    private final RotationState angularVelocity = new RotationState(0, 0);
    private final RotationState target = new RotationState();
    private final RotationState lastRotation = new RotationState();
    private final RotationState jitter = new RotationState(0, 0);
    private final RotationState jitterTarget = new RotationState(0, 0);
    private final RotationState overshoot = new RotationState(0, 0);
    private final RotationState centripetalComp = new RotationState(0, 0);
    private final RotationState prevAngularVel = new RotationState(0, 0);
    private final RotationState transition = new RotationState();

    private int aimTicks = 0;
    private long visibleTime = 0;
    private LivingEntity currentTarget;
    private LivingEntity lockedTarget;
    private LivingEntity renderTarget;

    private int jitterTick = 0;

    private Mode previousMode = Mode.Smooth;
    private int transitionTicks = 0;
    private static final int TRANSITION_DURATION = 10;

    private float rawAcceleration = 0;
    private float smoothedAcceleration = 0;

    private final MotionState playerMotion = new MotionState();
    private final MotionState targetMotion = new MotionState();

    private int framesWithoutTarget = 0;
    private float espAlpha = 0f;

    private final PredictionState predictionState = new PredictionState();
    private final DVDAnimation dvdAnimation = new DVDAnimation();

    private static final float JITTER_UPDATE_INTERVAL = 2;
    private static final float JITTER_PITCH_RATIO = 0.6f;
    private static final float MIN_SMOOTH_FACTOR = 0.05f;
    private static final float MAX_SMOOTH_FACTOR = 0.95f;
    private static final float DYNAMIC_SMOOTH_THRESHOLD = 15.0f;
    private static final float OVERSHOOT_THRESHOLD = 20.0f;
    private static final float OVERSHOOT_DECAY = 0.85f;
    private static final float YAW_OVERSHOOT_MULTIPLIER = 0.3f;
    private static final float PITCH_OVERSHOOT_MULTIPLIER = 0.2f;
    private static final float ACCELERATION_DECAY = 0.9f;
    private static final float SMOOTHED_ACCEL_DECAY = 0.95f;
    private static final float EASE_OUT_POWER = 3;
    private static final float ACCELERATION_SMOOTH_LERP = 0.2f;
    private static final float GCD_MOUSE_SENS_MULTIPLIER = 0.6f;
    private static final float GCD_MOUSE_SENS_OFFSET = 0.2f;
    private static final float GCD_FINAL_MULTIPLIER = 1.2f;
    private static final float GCD_TOLERANCE = 0.5f;
    private static final float PARTIAL_TICK_OFFSET = 0.5f;
    private static final float INSTANT_MODE_SMOOTH = 0.8f;
    private static final float ANGULAR_VELOCITY_DAMPING = 0.92f;
    private static final float ANGULAR_ACCELERATION_DAMPING = 0.85f;
    private static final float MAX_ANGULAR_VELOCITY = 15.0f;
    private static final float CENTRIPETAL_COMPENSATION_STRENGTH = 0.35f;
    private static final float LAG_COMPENSATION_FACTOR = 0.15f;
    private static final float PREDICTION_VELOCITY_FACTOR = 0.95f;
    private static final float PREDICTION_SMOOTH_FACTOR = 0.35f;
    private static final float ESP_FADE_SPEED = 0.15f;
    private static final float JITTER_SMOOTH_FACTOR = 0.25f;
    private static final float CENTRIPETAL_SMOOTH_FACTOR = 0.4f;
    private static final float SNAP_CURVE_POWER = 1.5f;
    private static final float SNAP_DECELERATION_FACTOR = 0.3f;

    private AimAssist() {
        super("Aim Assist", Category.COMBAT);
    }

    @Override
    protected void onEnable() {
        previousMode = mode.getValue();
        transitionTicks = 0;
        lockedTarget = null;
        visibleTime = 0;
        dvdAnimation.reset();
    }

    @Override
    protected void onDisable() {
        resetAllStates();
    }

    @SubscribeEvent
    public void onTick(ClientTickEvent.Pre event) {
        if (shouldResetStates()) {
            resetAllStates();
            return;
        }

        if (isScreenPaused()) {
            pauseAndSlowDown();
            return;
        }

        updateAimTicks();
        if (shouldStopAiming()) {
            rawAcceleration = 0;
            return;
        }

        currentTarget = findTarget();
        updateAcceleration();
        handleModeTransition();

        updatePlayerMotion();
        updateTargetMotion();

        if (currentTarget != null) {
            if (aimPart.is("DVD")) {
                dvdAnimation.update(currentTarget, dvdSpeed.getValue(), dvdJitter.getValue(), dvdDebug.getValue());
            }
            processTargetAim();
            framesWithoutTarget = 0;
        } else {
            framesWithoutTarget++;
            if (framesWithoutTarget > 5) {
                resetAimStates();
            } else {
                applyAngularVelocityDamping();
            }
        }

        updateCentripetalCompensation();
        updateEspAlpha();
    }

    @SubscribeEvent
    public void onRenderFrame(RenderFrameEvent.Pre event) {
        if (shouldSkipRender()) return;

        if (shouldTakeControl()) {
            float partialTicks = event.getPartialTick().getGameTimeDeltaPartialTick(true);

            if (isInTransition()) {
                applyTransition(partialTicks);
                return;
            }

            if (mode.getValue() == Mode.Smooth) {
                applySmoothRotation(partialTicks);
            } else {
                applyInstantRotation();
            }
        }
    }

    private boolean shouldResetStates() {
        return nullCheck();
    }

    private boolean isScreenPaused() {
        return (ignoreScreen.getValue() && mc.screen != null) ||
               (ignoreInventory.getValue() && (mc.screen instanceof AbstractContainerScreen));
    }

    private boolean shouldSkipRender() {
        return nullCheck() || isScreenPaused();
    }

    private boolean shouldTakeControl() {
        return currentTarget != null && !rotation.isNaN();
    }

    private void updateEspAlpha() {
        boolean shouldShow = lockedEsp.getValue() && lockTarget.getValue() && lockedTarget != null && lockedTarget.isAlive();

        if (shouldShow) {
            espAlpha = Mth.clamp(espAlpha + ESP_FADE_SPEED, 0f, 1f);
            renderTarget = lockedTarget;
        } else {
            espAlpha = Mth.clamp(espAlpha - ESP_FADE_SPEED, 0f, 1f);
            if (espAlpha <= 0.01f) {
                renderTarget = null;
            }
        }
    }

    private double getAimPartHeight() {
        if (aimPart.is("DVD")) {
            return dvdAnimation.pointY;
        }
        return switch (aimPart.getValue()) {
            case Head -> 1.7f;
            case Neck -> 1.5f;
            case Torso -> 1.0f;
            case Legs -> 0.7f;
            case Feet -> 0.2f;
            case DVD -> 1.0f;
        };
    }

    private boolean isInTransition() {
        return transitionTicks > 0;
    }

    private void resetAllStates() {
        resetAimStates();
        playerMotion.reset();
        targetMotion.reset();
        rawAcceleration = 0;
        smoothedAcceleration = 0;
        dvdAnimation.reset();
        espAlpha = 0f;
        renderTarget = null;
        framesWithoutTarget = 0;
        if (!lockTarget.getValue()) {
            lockedTarget = null;
        }
    }

    private void pauseAndSlowDown() {
        rotation.setNaN();
        target.setNaN();
        angularVelocity.multiply(0.5f);
    }

    private void resetAimStates() {
        manageAimState(true);
        predictionState.reset();
    }

    private void manageAimState(boolean useNaN) {
        if (useNaN) {
            rotation.setNaN();
            target.setNaN();
        } else {
            rotation.set(mc.player.getYRot(), mc.player.getXRot());
            target.set(rotation.yaw, rotation.pitch);
        }

        lastRotation.set(rotation.yaw, rotation.pitch);

        jitter.set(0, 0);
        jitterTarget.set(0, 0);
        overshoot.set(0, 0);
        centripetalComp.set(0, 0);

        angularVelocity.set(0, 0);
        prevAngularVel.set(0, 0);
    }

    private void applyAngularVelocityDamping() {
        angularVelocity.multiply(ANGULAR_VELOCITY_DAMPING);

        if (!rotation.isNaN()) {
            rotation.yaw = lastRotation.yaw + angularVelocity.yaw;
            rotation.pitch = lastRotation.pitch + angularVelocity.pitch;
        }
    }

    private float calculateSnapFactor(float diff, float threshold, float strength) {
        float absDiff = Math.abs(diff);
        if (absDiff <= threshold) return 0f;

        float normalized = (absDiff - threshold) / (90f - threshold);
        normalized = Mth.clamp(normalized, 0f, 1f);
        return (float) Math.pow(normalized, SNAP_CURVE_POWER) * strength;
    }

    private float calculateSnapDecay(float absDiff, float threshold) {
        if (absDiff >= threshold * 1.5f) return 1f;
        float t = (absDiff - threshold) / (threshold * 0.5f);
        return 1f - Mth.clamp(t, 0f, 1f) * SNAP_DECELERATION_FACTOR;
    }

    private void updateAimTicks() {
        if (mc.hitResult != null && mc.hitResult.getType() == net.minecraft.world.phys.HitResult.Type.ENTITY)
            aimTicks++;
        else
            aimTicks = 0;
    }

    private boolean shouldStopAiming() {
        if (pauseWhileEating.getValue() && mc.player.isUsingItem()) {
            return true;
        }
        return aimTicks >= aimTime.getValue();
    }

    private void updateAcceleration() {
        if (currentTarget != null) {
            rawAcceleration += aimStrength.getValue() / 10000f;
            rawAcceleration = Mth.clamp(rawAcceleration, 0f, 1.0f);

            float eased = 1 - (float) Math.pow(1 - rawAcceleration, EASE_OUT_POWER);
            smoothedAcceleration = Mth.lerp(ACCELERATION_SMOOTH_LERP, smoothedAcceleration, eased);
        } else {
            rawAcceleration *= ACCELERATION_DECAY;
            smoothedAcceleration *= SMOOTHED_ACCEL_DECAY;
        }
    }

    private void handleModeTransition() {
        if (mode.getValue() != previousMode) {
            transitionTicks = TRANSITION_DURATION;
            transition.set(mc.player.getYRot(), mc.player.getXRot());
            previousMode = mode.getValue();
        }

        if (transitionTicks > 0) {
            transitionTicks--;
        }
    }

    private void processTargetAim() {
        if (!mc.player.hasLineOfSight(currentTarget)) {
            if (!ignoreWalls.getValue()) {
                if (visibleTime == 0) {
                    visibleTime = System.currentTimeMillis();
                } else if (System.currentTimeMillis() - visibleTime > reactionTime.getValue()) {
                    resetAimStates();
                    return;
                }
            }
        } else {
            visibleTime = 0;
        }

        initializeAimIfNeeded();
        calculateIdealRotation();
        updateHumanJitter();
        calculateDynamicSmoothing();

        applyGCDFix();

        lastRotation.set(rotation.yaw, rotation.pitch);
    }

    private void initializeAimIfNeeded() {
        if (rotation.isNaN()) {
            manageAimState(false);
        }
    }

    private void calculateIdealRotation() {
        double targetX, targetY, targetZ;

        if (prediction.getValue()) {
            double predictedX = currentTarget.getX();
            double predictedY = currentTarget.getY();
            double predictedZ = currentTarget.getZ();

            double predictedVelX = targetMotion.velX;
            double predictedVelY = targetMotion.velY;
            double predictedVelZ = targetMotion.velZ;

            if (extrapolateVelocity.getValue()) {
                predictedVelX += targetMotion.accelX * 0.5;
                predictedVelY += targetMotion.accelY * 0.5;
                predictedVelZ += targetMotion.accelZ * 0.5;
            }

            int ticks = predictionTicks.getValue();
            for (int i = 0; i < ticks; i++) {
                predictedX += predictedVelX * PREDICTION_VELOCITY_FACTOR;
                predictedY += predictedVelY * PREDICTION_VELOCITY_FACTOR;
                predictedZ += predictedVelZ * PREDICTION_VELOCITY_FACTOR;
            }

            predictionState.lerp(PREDICTION_SMOOTH_FACTOR, predictedX, predictedY, predictedZ);

            targetX = predictionState.x;
            targetY = predictionState.y;
            targetZ = predictionState.z;
        } else {
            targetX = currentTarget.getX();
            targetY = currentTarget.getY();
            targetZ = currentTarget.getZ();

            predictionState.reset();
        }

        calculateFinalRotation(targetX, targetY, targetZ);
    }

    private void calculateFinalRotation(double targetX, double targetY, double targetZ) {
        double dx = targetX - mc.player.getX();
        double dy = targetY + getAimPartHeight() - (mc.player.getY() + mc.player.getEyeHeight());
        double dz = targetZ - mc.player.getZ();

        if (aimPart.is("DVD")) {
            dx += dvdAnimation.pointX;
            dz += dvdAnimation.pointZ;
        }

        double distance = Math.sqrt(dx * dx + dz * dz);

        target.yaw = (float) Mth.wrapDegrees(Math.toDegrees(Math.atan2(dz, dx)) - 90f);
        target.pitch = (float) -Math.toDegrees(Math.atan2(dy, distance));
    }

    private void updateHumanJitter() {
        jitterTick++;
        if (jitterTick >= JITTER_UPDATE_INTERVAL) {
            jitterTick = 0;
            float jitterAmount = humanJitter.getValue().floatValue();
            jitterTarget.yaw = MathUtils.getRandom(-jitterAmount, jitterAmount);
            jitterTarget.pitch = MathUtils.getRandom(-jitterAmount * JITTER_PITCH_RATIO, jitterAmount * JITTER_PITCH_RATIO);
        }

        jitter.yaw = Mth.lerp(JITTER_SMOOTH_FACTOR, jitter.yaw, jitterTarget.yaw);
        jitter.pitch = Mth.lerp(JITTER_SMOOTH_FACTOR, jitter.pitch, jitterTarget.pitch);
    }

    private void calculateDynamicSmoothing() {
        float jitteredTargetYaw = target.yaw + jitter.yaw;
        float jitteredTargetPitch = target.pitch + jitter.pitch;

        float smoothFactor = aimSmooth.getValue() / 180.0f;
        smoothFactor = Mth.clamp(smoothFactor, MIN_SMOOTH_FACTOR, MAX_SMOOTH_FACTOR);

        float yawDiff = Mth.wrapDegrees(jitteredTargetYaw - rotation.yaw);
        float pitchDiff = jitteredTargetPitch - rotation.pitch;
        float distanceToTarget = (float) Math.sqrt(yawDiff * yawDiff + pitchDiff * pitchDiff);

        float dynamicSmoothFactor = smoothFactor;
        if (distanceToTarget < DYNAMIC_SMOOTH_THRESHOLD) {
            dynamicSmoothFactor = smoothFactor * (0.3f + (distanceToTarget / DYNAMIC_SMOOTH_THRESHOLD) * 0.7f);
        }

        float overshootAmount = humanOvershoot.getValue().floatValue();
        if (distanceToTarget > OVERSHOOT_THRESHOLD) {
            overshoot.yaw = yawDiff * overshootAmount * YAW_OVERSHOOT_MULTIPLIER;
            overshoot.pitch = pitchDiff * overshootAmount * PITCH_OVERSHOOT_MULTIPLIER;
        } else {
            overshoot.yaw *= OVERSHOOT_DECAY;
            overshoot.pitch *= OVERSHOOT_DECAY;
        }

        float adjustedYawDiff = yawDiff + overshoot.yaw;
        float adjustedPitchDiff = pitchDiff + overshoot.pitch;

        float desiredAngularVelYaw = adjustedYawDiff * dynamicSmoothFactor;
        float desiredAngularVelPitch = adjustedPitchDiff * dynamicSmoothFactor;

        if (snapCompensation.getValue()) {
            float snapThresholdVal = snapThreshold.getValue().floatValue();
            float snapStrengthVal = snapStrength.getValue().floatValue();

            float absYawDiff = Math.abs(yawDiff);
            float absPitchDiff = Math.abs(pitchDiff);

            if (absYawDiff > snapThresholdVal || absPitchDiff > snapThresholdVal) {
                float yawSnapFactor = calculateSnapFactor(yawDiff, snapThresholdVal, snapStrengthVal);
                float pitchSnapFactor = calculateSnapFactor(pitchDiff, snapThresholdVal, snapStrengthVal);

                desiredAngularVelYaw += yawDiff * yawSnapFactor * calculateSnapDecay(absYawDiff, snapThresholdVal);
                desiredAngularVelPitch += pitchDiff * pitchSnapFactor * calculateSnapDecay(absPitchDiff, snapThresholdVal);
            }
        }

        if (closeTargetBoost.getValue()) {
            float angleDistance = Mth.sqrt(yawDiff * yawDiff + pitchDiff * pitchDiff);
            float threshold = closeTargetThreshold.getValue().floatValue();
            if (angleDistance < threshold) {
                float boostFactor = closeTargetBoostStrength.getValue().floatValue();
                float t = 1f - angleDistance / threshold;
                float smoothT = t * t * (3f - 2f * t);
                float boost = 1f + (boostFactor - 1f) * smoothT;
                desiredAngularVelYaw *= boost;
                desiredAngularVelPitch *= boost;
            }
        }

        desiredAngularVelYaw += centripetalComp.yaw;
        desiredAngularVelPitch += centripetalComp.pitch;

        float lagCompYaw = prevAngularVel.yaw * LAG_COMPENSATION_FACTOR;
        float lagCompPitch = prevAngularVel.pitch * LAG_COMPENSATION_FACTOR;
        desiredAngularVelYaw += lagCompYaw;
        desiredAngularVelPitch += lagCompPitch;

        float responsiveFactor = responsiveness.getValue().floatValue();
        desiredAngularVelYaw *= responsiveFactor;
        desiredAngularVelPitch *= responsiveFactor;

        float inertiaFactor = inertia.getValue().floatValue();
        angularVelocity.yaw = angularVelocity.yaw * inertiaFactor + desiredAngularVelYaw * (1 - inertiaFactor);
        angularVelocity.pitch = angularVelocity.pitch * inertiaFactor + desiredAngularVelPitch * (1 - inertiaFactor);

        float angularAccelYaw = angularVelocity.yaw - prevAngularVel.yaw;
        float angularAccelPitch = angularVelocity.pitch - prevAngularVel.pitch;
        angularAccelYaw *= ANGULAR_ACCELERATION_DAMPING;
        angularAccelPitch *= ANGULAR_ACCELERATION_DAMPING;
        angularVelocity.yaw = prevAngularVel.yaw + angularAccelYaw;
        angularVelocity.pitch = prevAngularVel.pitch + angularAccelPitch;

        angularVelocity.yaw = Mth.clamp(angularVelocity.yaw, -MAX_ANGULAR_VELOCITY, MAX_ANGULAR_VELOCITY);
        angularVelocity.pitch = Mth.clamp(angularVelocity.pitch, -MAX_ANGULAR_VELOCITY, MAX_ANGULAR_VELOCITY);

        prevAngularVel.set(angularVelocity.yaw, angularVelocity.pitch);

        rotation.yaw = lastRotation.yaw + angularVelocity.yaw;
        rotation.pitch = lastRotation.pitch + angularVelocity.pitch;
    }

    private void updateCentripetalCompensation() {
        if (currentTarget == null) {
            centripetalComp.yaw = Mth.lerp(CENTRIPETAL_SMOOTH_FACTOR, centripetalComp.yaw, 0f);
            centripetalComp.pitch = Mth.lerp(CENTRIPETAL_SMOOTH_FACTOR, centripetalComp.pitch, 0f);
            return;
        }

        centripetalComp.yaw = calculateYawCentripetalCompensation();
        centripetalComp.pitch = calculatePitchCentripetalCompensation();
    }

    private float calculateYawCentripetalCompensation() {
        double playerX = mc.player.getX();
        double playerZ = mc.player.getZ();
        double targetX = currentTarget.getX();
        double targetZ = currentTarget.getZ();

        double dx = targetX - playerX;
        double dz = targetZ - playerZ;
        double distance = Math.sqrt(dx * dx + dz * dz);

        if (distance < 1) {
            return Mth.lerp(CENTRIPETAL_SMOOTH_FACTOR, centripetalComp.yaw, 0f);
        }

        double relVelX = targetMotion.velX - playerMotion.velX;
        double relVelZ = targetMotion.velZ - playerMotion.velZ;

        double normalizedDx = dx / distance;
        double normalizedDz = dz / distance;

        double tangentialSpeed = (relVelX * normalizedDz - relVelZ * normalizedDx);
        double radialSpeed = (relVelX * normalizedDx + relVelZ * normalizedDz);

        double centripetalAccel = tangentialSpeed * tangentialSpeed / distance;
        double coriolisAccel = 2 * radialSpeed * tangentialSpeed / distance;

        double totalAccel = centripetalAccel + coriolisAccel;
        double rawCompYaw = totalAccel * CENTRIPETAL_COMPENSATION_STRENGTH * 0.5;

        return Mth.lerp(CENTRIPETAL_SMOOTH_FACTOR, centripetalComp.yaw, (float) rawCompYaw);
    }

    private float calculatePitchCentripetalCompensation() {
        double playerX = mc.player.getX();
        double playerY = mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose());
        double playerZ = mc.player.getZ();
        double targetX = currentTarget.getX();
        double targetY = currentTarget.getY() + currentTarget.getEyeHeight(currentTarget.getPose());
        double targetZ = currentTarget.getZ();

        double dxTotal = targetX - playerX;
        double dyTotal = targetY - playerY;
        double dzTotal = targetZ - playerZ;
        double totalDist = Math.sqrt(dxTotal * dxTotal + dyTotal * dyTotal + dzTotal * dzTotal);

        if (totalDist < 1) {
            return Mth.lerp(CENTRIPETAL_SMOOTH_FACTOR, centripetalComp.pitch, 0f);
        }

        double relVelX = targetMotion.velX - playerMotion.velX;
        double relVelY = targetMotion.velY - playerMotion.velY;
        double relVelZ = targetMotion.velZ - playerMotion.velZ;

        double normalizedDx = dxTotal / totalDist;
        double normalizedDy = dyTotal / totalDist;
        double normalizedDz = dzTotal / totalDist;

        double radialSpeed = relVelX * normalizedDx + relVelY * normalizedDy + relVelZ * normalizedDz;
        double verticalTangentialSpeed = relVelY - radialSpeed * normalizedDy;
        double centripetalAccel = verticalTangentialSpeed * verticalTangentialSpeed / totalDist;
        double rawCompPitch = centripetalAccel * CENTRIPETAL_COMPENSATION_STRENGTH * 0.3;

        return Mth.lerp(CENTRIPETAL_SMOOTH_FACTOR, centripetalComp.pitch, (float) rawCompPitch);
    }

    private void applyGCDFix() {
        double sensitivity = Mth.clamp(mc.options.sensitivity().get(), 0.0, 1.0);
        double gcdFix = (Math.pow(sensitivity * GCD_MOUSE_SENS_MULTIPLIER + GCD_MOUSE_SENS_OFFSET, 3.0)) * GCD_FINAL_MULTIPLIER;
        gcdFix = Mth.clamp(gcdFix, 0.0, 100.0);
        rotation.yaw = applySmartGCDFix(rotation.yaw, lastRotation.yaw, gcdFix);
        rotation.pitch = applySmartGCDFix(rotation.pitch, lastRotation.pitch, gcdFix);
        rotation.pitch = Mth.clamp(rotation.pitch, -90.0f, 90.0f);
    }

    private void applyTransition(float partialTicks) {
        float t = 1 - (transitionTicks - partialTicks) / (float) TRANSITION_DURATION;
        t = Mth.clamp(t, 0f, 1f);
        t = t < 0.5f ? 2 * t * t : 1 - (float) Math.pow(-2 * t + 2, 2) / 2;

        float yaw = Mth.lerp(t, transition.yaw, rotation.yaw);
        float pitch = Mth.lerp(t, transition.pitch, rotation.pitch);

        mc.player.setYRot(yaw);
        mc.player.setXRot(pitch);
    }

    private void applyRotation(float lerpFactor) {
        float yawDiff = Mth.wrapDegrees(rotation.yaw - mc.player.getYRot());
        float pitchDiff = rotation.pitch - mc.player.getXRot();

        float interpolatedYaw = mc.player.getYRot() + yawDiff * lerpFactor;
        float interpolatedPitch = mc.player.getXRot() + pitchDiff * lerpFactor;

        mc.player.setYRot(interpolatedYaw);
        mc.player.setXRot(interpolatedPitch);
    }

    private void applySmoothRotation(float partialTicks) {
        float smoothAmount = smoothedAcceleration;
        float lerpFactor = Mth.clamp(smoothAmount * (PARTIAL_TICK_OFFSET + partialTicks * PARTIAL_TICK_OFFSET), 0.0f, 1.0f);
        applyRotation(lerpFactor);
    }

    private void applyInstantRotation() {
        applyRotation(INSTANT_MODE_SMOOTH);
    }

    private float applySmartGCDFix(float target, float current, double gcd) {
        if (gcd <= 0) return target;

        double diff = target - current;
        double absDiff = Math.abs(diff);

        if (absDiff < gcd * GCD_TOLERANCE) {
            return target;
        }

        double roundedDiff = Math.round(diff / gcd) * gcd;
        return (float) (current + roundedDiff);
    }

    private LivingEntity findTarget() {
        if (lockTarget.getValue() && lockedTarget != null) {
            if (lockedTarget.isAlive() && mc.player.distanceTo(lockedTarget) <= range.getValue()) {
                if (ignoreWalls.getValue() || mc.player.hasLineOfSight(lockedTarget)) {
                    return lockedTarget;
                } else {
                    lockedTarget = null;
                }
            } else {
                lockedTarget = null;
            }
        }

        List<LivingEntity> candidates = new ArrayList<>();
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof LivingEntity living)) continue;
            if (living == mc.player) continue;
            if (!isValidAimTarget(living)) continue;
            candidates.add(living);
        }

        if (candidates.isEmpty()) {
            return null;
        }

        candidates.sort((a, b) -> {
            return switch (targetPriority.getValue()) {
                case Distance -> Double.compare(
                    mc.player.distanceTo(a),
                    mc.player.distanceTo(b)
                );
                case Health -> Float.compare(
                    a.getHealth(),
                    b.getHealth()
                );
                case Angle -> {
                    float yawA = Mth.wrapDegrees(RotationUtils.getRotationsToEntity(a).x - mc.player.getYRot());
                    float yawB = Mth.wrapDegrees(RotationUtils.getRotationsToEntity(b).x - mc.player.getYRot());
                    yield Float.compare(Math.abs(yawA), Math.abs(yawB));
                }
            };
        });

        LivingEntity target = candidates.getFirst();

        if (lockTarget.getValue() && target != null) {
            lockedTarget = target;
        }

        return target;
    }

    private boolean isValidAimTarget(LivingEntity entity) {
        if (!entity.isAlive() || entity.isDeadOrDying()) return false;
        if (AntiBot.INSTANCE.isBot(entity)) return false;

        double dist = mc.player.distanceTo(entity);
        if (dist > range.getValue()) return false;

        if (entity instanceof Player) {
            if (!player.getValue()) return false;
            if (entity.isInvisible() && !ignoreInvisible.getValue()) return false;
        } else if (entity instanceof net.minecraft.world.entity.npc.villager.Villager) {
            if (!villager.getValue()) return false;
        } else if (entity instanceof net.minecraft.world.entity.animal.Animal) {
            if (!animal.getValue()) return false;
        } else if (entity instanceof net.minecraft.world.entity.monster.Slime) {
            if (!slime.getValue()) return false;
        } else if (entity instanceof net.minecraft.world.entity.monster.Monster) {
            if (!mob.getValue()) return false;
        } else {
            return false;
        }

        return true;
    }

    @SubscribeEvent
    public void onRender3D(RenderLevelStageEvent.AfterLevel event) {
        if (nullCheck()) return;
        if (!lockedEsp.getValue()) return;
        if (!lockTarget.getValue()) return;
        if (renderTarget == null) return;
        if (!renderTarget.isAlive()) return;
        if (espAlpha <= 0.01f) return;

        float partialTicks = mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);

        double x = renderTarget.xo + (renderTarget.getX() - renderTarget.xo) * partialTicks;
        double y = renderTarget.yo + (renderTarget.getY() - renderTarget.yo) * partialTicks;
        double z = renderTarget.zo + (renderTarget.getZ() - renderTarget.zo) * partialTicks;

        AABB interpolatedBox = renderTarget.getBoundingBox()
            .move(x - renderTarget.getX(), y - renderTarget.getY(), z - renderTarget.getZ());

        Color originalColor = lockedEspColor.getValue();
        int alpha = (int) (originalColor.getAlpha() * espAlpha);
        Color fadedColor = new Color(originalColor.getRed(), originalColor.getGreen(), originalColor.getBlue(), alpha);

        Render3DUtils.drawFilledBox(interpolatedBox, fadedColor);
    }

    public boolean shouldBlockMouse() {
        if (!isEnabled()) return false;
        if (mode.getValue() != Mode.Smooth) return false;
        return !rotation.isNaN();
    }

    private void updatePlayerMotion() {
        playerMotion.update(mc.player.getX(), mc.player.getY(), mc.player.getZ());
    }

    private void updateTargetMotion() {
        if (currentTarget == null) {
            targetMotion.reset();
            return;
        }

        targetMotion.update(currentTarget.getX(), currentTarget.getY(), currentTarget.getZ());
    }

    public enum Mode {
        Smooth,
        Instant
    }

    public enum PriorityMode {
        Distance,
        Health,
        Angle
    }

    public enum AimPart {
        Head,
        Neck,
        Torso,
        Legs,
        Feet,
        DVD // 实验性
    }
}
