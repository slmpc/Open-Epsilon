package com.github.epsilon.modules.impl.combat;

import com.github.epsilon.managers.TargetManager;
import com.github.epsilon.modules.Category;
import com.github.epsilon.modules.Module;
import com.github.epsilon.settings.impl.BoolSetting;
import com.github.epsilon.settings.impl.DoubleSetting;
import com.github.epsilon.settings.impl.EnumSetting;
import com.github.epsilon.settings.impl.IntSetting;
import com.github.epsilon.utils.math.MathUtils;
import com.github.epsilon.utils.rotation.RotationUtils;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import org.joml.Vector2f;

/* 
* Author Moli
* 平滑测试 4.8,42,180,6,500,0.2,0,0.47
* todo 提升可配置性。
*/

public class AimAssist extends Module {

    public static final AimAssist INSTANCE = new AimAssist();

    public EnumSetting<Mode> mode = enumSetting("Mode", Mode.Smooth);
    public DoubleSetting range = doubleSetting("Range", 4.2, 1.0, 8.0, 0.1);
    public IntSetting aimStrength = intSetting("AimStrength", 30, 1, 100, 1);
    public IntSetting aimSmooth = intSetting("AimSmooth", 45, 1, 180, 1);
    public IntSetting aimTime = intSetting("AimTime", 2, 1, 10, 1);
    public BoolSetting ignoreWalls = boolSetting("IgnoreWalls", true);
    public BoolSetting ignoreTeam = boolSetting("IgnoreTeam", true);
    public IntSetting reactionTime = intSetting("ReactionTime", 80, 1, 500, 1);
    public BoolSetting ignoreInvisible = boolSetting("IgnoreInvis", false);
    public BoolSetting ignoreScreen = boolSetting("IgnoreScreen", true);
    public BoolSetting ignoreInventory = boolSetting("IgnoreInventory", true);
    public BoolSetting player = boolSetting("Player", true);
    public BoolSetting mob = boolSetting("Mob", true);
    public BoolSetting animal = boolSetting("Animal", true);
    public DoubleSetting humanJitter = doubleSetting("HumanJitter", 0.3, 0.0, 2.0, 0.1);
    public DoubleSetting humanOvershoot = doubleSetting("HumanOvershoot", 0.15, 0.0, 0.5, 0.05);
    public DoubleSetting inertia = doubleSetting("Inertia", 0.85, 0.0, 0.99, 0.01);

    private float rotationYaw, rotationPitch;
    private float velocityYaw, velocityPitch;
    private int aimTicks = 0;
    private long visibleTime = System.currentTimeMillis();
    private LivingEntity currentTarget;
    
    private float targetYaw, targetPitch;
    private float lastYaw, lastPitch;
    
    private float jitterYaw, jitterPitch;
    private float overshootYaw, overshootPitch;
    private int jitterTick = 0;
    
    private Mode previousMode = Mode.Smooth;
    private float transitionYaw, transitionPitch;
    private int transitionTicks = 0;
    private static final int TRANSITION_DURATION = 10;
    
    private float rawAcceleration = 0;
    private float smoothedAcceleration = 0;

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
    private static final float GCD_MOUSE_SENS_MULTIPLIER = 0.6f;
    private static final float GCD_MOUSE_SENS_OFFSET = 0.2f;
    private static final float GCD_FINAL_MULTIPLIER = 1.2f;
    private static final float GCD_TOLERANCE = 0.5f;
    private static final float PARTIAL_TICK_OFFSET = 0.5f;
    private static final float INSTANT_MODE_SMOOTH = 0.8f;
    private static final float EASE_OUT_POWER = 3;
    private static final float ACCELERATION_SMOOTH_LERP = 0.2f;

    private AimAssist() {
        super("AimAssist", Category.COMBAT);
    }

    @Override
    protected void onEnable() {
        previousMode = mode.getValue();
        transitionTicks = 0;
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

        if (shouldPauseForScreen()) {
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

        if (currentTarget != null) {
            processTargetAim();
        } else {
            resetAimStates();
        }
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

    private boolean shouldPauseForScreen() {
        return (ignoreScreen.getValue() && mc.screen != null) ||
               (ignoreInventory.getValue() && (mc.screen instanceof AbstractContainerScreen));
    }

    private boolean shouldSkipRender() {
        if (nullCheck()) return true;
        if (ignoreScreen.getValue() && mc.screen != null) return true;
        return ignoreInventory.getValue() && (mc.screen instanceof AbstractContainerScreen);
    }

    private boolean shouldTakeControl() {
        return currentTarget != null && !Float.isNaN(rotationYaw) && !Float.isNaN(rotationPitch);
    }

    private boolean isInTransition() {
        return transitionTicks > 0;
    }

    private void resetAllStates() {
        rotationYaw = Float.NaN;
        rotationPitch = Float.NaN;
        targetYaw = Float.NaN;
        targetPitch = Float.NaN;
        currentTarget = null;
        jitterYaw = 0;
        jitterPitch = 0;
        overshootYaw = 0;
        overshootPitch = 0;
        velocityYaw = 0;
        velocityPitch = 0;
        rawAcceleration = 0;
        smoothedAcceleration = 0;
    }

    private void pauseAndSlowDown() {
        rotationYaw = Float.NaN;
        rotationPitch = Float.NaN;
        targetYaw = Float.NaN;
        targetPitch = Float.NaN;
        velocityYaw *= 0.5f;
        velocityPitch *= 0.5f;
    }

    private void resetAimStates() {
        rotationYaw = Float.NaN;
        rotationPitch = Float.NaN;
        targetYaw = Float.NaN;
        targetPitch = Float.NaN;
        jitterYaw = 0;
        jitterPitch = 0;
        overshootYaw = 0;
        overshootPitch = 0;
        velocityYaw = 0;
        velocityPitch = 0;
    }

    private void updateAimTicks() {
        if (mc.hitResult != null && mc.hitResult.getType() == net.minecraft.world.phys.HitResult.Type.ENTITY)
            aimTicks++;
        else
            aimTicks = 0;
    }

    private boolean shouldStopAiming() {
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
            transitionYaw = mc.player.getYRot();
            transitionPitch = mc.player.getXRot();
            previousMode = mode.getValue();
        }
        
        if (transitionTicks > 0) {
            transitionTicks--;
        }
    }

    private void processTargetAim() {
        if (!mc.player.hasLineOfSight(currentTarget)) {
            if (!ignoreWalls.getValue())
                visibleTime = System.currentTimeMillis();
        }

        if (System.currentTimeMillis() - visibleTime < reactionTime.getValue()) {
            resetAimStates();
            return;
        }

        initializeAimIfNeeded();
        calculateIdealRotation();
        updateHumanJitter();
        calculateDynamicSmoothing();
        
        applyGCDFix();
        
        lastYaw = rotationYaw;
        lastPitch = rotationPitch;
    }

    private void initializeAimIfNeeded() {
        if (Float.isNaN(rotationYaw)) {
            rotationYaw = mc.player.getYRot();
            rotationPitch = mc.player.getXRot();
            targetYaw = rotationYaw;
            targetPitch = rotationPitch;
            lastYaw = rotationYaw;
            lastPitch = rotationPitch;
            jitterYaw = 0;
            jitterPitch = 0;
            overshootYaw = 0;
            overshootPitch = 0;
            velocityYaw = 0;
            velocityPitch = 0;
        }
    }

    private void calculateIdealRotation() {
        Vector2f idealRotation = RotationUtils.getRotationsToEntity(currentTarget);
        targetYaw = idealRotation.x;
        targetPitch = idealRotation.y;
    }

    private void updateHumanJitter() {
        jitterTick++;
        if (jitterTick >= JITTER_UPDATE_INTERVAL) {
            jitterTick = 0;
            float jitterAmount = humanJitter.getValue().floatValue();
            jitterYaw = MathUtils.getRandom(-jitterAmount, jitterAmount);
            jitterPitch = MathUtils.getRandom(-jitterAmount * JITTER_PITCH_RATIO, jitterAmount * JITTER_PITCH_RATIO);
        }
    }

    private void calculateDynamicSmoothing() {
        float jitteredTargetYaw = targetYaw + jitterYaw;
        float jitteredTargetPitch = targetPitch + jitterPitch;
        
        float smoothFactor = aimSmooth.getValue() / 180.0f;
        smoothFactor = Mth.clamp(smoothFactor, MIN_SMOOTH_FACTOR, MAX_SMOOTH_FACTOR);
        
        float yawDiff = Mth.wrapDegrees(jitteredTargetYaw - rotationYaw);
        float pitchDiff = jitteredTargetPitch - rotationPitch;
        float distanceToTarget = (float) Math.sqrt(yawDiff * yawDiff + pitchDiff * pitchDiff);
        
        float dynamicSmoothFactor = smoothFactor;
        if (distanceToTarget < DYNAMIC_SMOOTH_THRESHOLD) {
            dynamicSmoothFactor = smoothFactor * (0.3f + (distanceToTarget / DYNAMIC_SMOOTH_THRESHOLD) * 0.7f);
        }
        
        float overshootAmount = humanOvershoot.getValue().floatValue();
        if (distanceToTarget > OVERSHOOT_THRESHOLD) {
            overshootYaw = yawDiff * overshootAmount * YAW_OVERSHOOT_MULTIPLIER;
            overshootPitch = pitchDiff * overshootAmount * PITCH_OVERSHOOT_MULTIPLIER;
        } else {
            overshootYaw *= OVERSHOOT_DECAY;
            overshootPitch *= OVERSHOOT_DECAY;
        }
        
        float adjustedYawDiff = yawDiff + overshootYaw;
        float adjustedPitchDiff = pitchDiff + overshootPitch;
        
        float desiredYawChange = adjustedYawDiff * dynamicSmoothFactor;
        float desiredPitchChange = adjustedPitchDiff * dynamicSmoothFactor;
        
        float inertiaFactor = inertia.getValue().floatValue();
        velocityYaw = velocityYaw * inertiaFactor + desiredYawChange * (1 - inertiaFactor);
        velocityPitch = velocityPitch * inertiaFactor + desiredPitchChange * (1 - inertiaFactor);
        
        rotationYaw = lastYaw + velocityYaw;
        rotationPitch = lastPitch + velocityPitch;
    }

    private void applyGCDFix() {
        double gcdFix = (Math.pow(mc.options.sensitivity().get() * GCD_MOUSE_SENS_MULTIPLIER + GCD_MOUSE_SENS_OFFSET, 3.0)) * GCD_FINAL_MULTIPLIER;
        rotationYaw = applySmartGCDFix(rotationYaw, lastYaw, gcdFix);
        rotationPitch = applySmartGCDFix(rotationPitch, lastPitch, gcdFix);
        rotationPitch = Mth.clamp(rotationPitch, -90.0f, 90.0f);
    }

    private void applyTransition(float partialTicks) {
        float t = 1 - (transitionTicks - partialTicks) / (float) TRANSITION_DURATION;
        t = Mth.clamp(t, 0f, 1f);
        t = t < 0.5f ? 2 * t * t : 1 - (float) Math.pow(-2 * t + 2, 2) / 2;
        
        float yaw = Mth.lerp(t, transitionYaw, rotationYaw);
        float pitch = Mth.lerp(t, transitionPitch, rotationPitch);
        
        mc.player.setYRot(yaw);
        mc.player.setXRot(pitch);
    }

    private void applySmoothRotation(float partialTicks) {
        float yawDiff = Mth.wrapDegrees(rotationYaw - mc.player.getYRot());
        float pitchDiff = rotationPitch - mc.player.getXRot();

        float smoothAmount = smoothedAcceleration;
        
        float lerpFactor = Mth.clamp(smoothAmount * (PARTIAL_TICK_OFFSET + partialTicks * PARTIAL_TICK_OFFSET), 0.0f, 1.0f);
        
        float interpolatedYaw = mc.player.getYRot() + yawDiff * lerpFactor;
        float interpolatedPitch = mc.player.getXRot() + pitchDiff * lerpFactor;

        mc.player.setYRot(interpolatedYaw);
        mc.player.setXRot(interpolatedPitch);
    }

    private void applyInstantRotation() {
        float yawDiff = Mth.wrapDegrees(rotationYaw - mc.player.getYRot());
        float pitchDiff = rotationPitch - mc.player.getXRot();
        
        float interpolatedYaw = mc.player.getYRot() + yawDiff * INSTANT_MODE_SMOOTH;
        float interpolatedPitch = mc.player.getXRot() + pitchDiff * INSTANT_MODE_SMOOTH;
        
        mc.player.setYRot(interpolatedYaw);
        mc.player.setXRot(interpolatedPitch);
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
        return TargetManager.INSTANCE.acquirePrimary(
                TargetManager.TargetRequest.of(
                        range.getValue(),
                        360.0f,
                        player.getValue(),
                        mob.getValue(),
                        animal.getValue(),
                        false,
                        ignoreInvisible.getValue(),
                        1
                )
        );
    }

    public enum Mode {
        Smooth,
        Instant
    }
}
