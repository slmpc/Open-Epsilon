package com.github.epsilon.modules.impl.combat;

import com.github.epsilon.managers.RotationManager;
import com.github.epsilon.managers.TargetManager;
import com.github.epsilon.modules.Category;
import com.github.epsilon.modules.Module;
import com.github.epsilon.settings.impl.*;
import com.github.epsilon.utils.math.MathUtils;
import com.github.epsilon.utils.render.esp.CaptureMark;
import com.github.epsilon.utils.render.esp.Firefly;
import com.github.epsilon.utils.rotation.Priority;
import com.github.epsilon.utils.rotation.RotationUtils;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class KillAura extends Module {

    public static final KillAura INSTANCE = new KillAura();

    private KillAura() {
        super("Kill Aura", Category.COMBAT);
    }

    private enum Mode {
        OnePointEight,
        OnePointNinePlus
    }

    public enum TargetMode {
        Single,
        Switch,
        Multiple,
    }

    private enum ESPMode {
        CaptureMark,
        Firefly
    }

    private final EnumSetting<Mode> mode = enumSetting("Mode", Mode.OnePointEight);
    private final EnumSetting<TargetMode> targetMode = enumSetting("Target Mode", TargetMode.Single);
    private final DoubleSetting range = doubleSetting("Range", 3.0, 1.0, 6.0, 0.01);
    private final DoubleSetting aimRange = doubleSetting("Aim Range", 4.0, 1.0, 6.0, 0.1);
    private final IntSetting rotationSpeed = intSetting("roationspeed", 10, 1, 10, 1);
    private final DoubleSetting fov = doubleSetting("FOV", 360.0, 10.0, 360.0, 1.0);
    private final DoubleSetting cps = doubleSetting("CPS", 10.0, 1.0, 20.0, 1.0);
    private final DoubleSetting maxCps = doubleSetting("Max CPS", 12, 1, 20, 1);
    private final BoolSetting player = boolSetting("Player", true);
    private final BoolSetting mob = boolSetting("Mob", true);
    private final BoolSetting animal = boolSetting("Animal", true);
    private final BoolSetting Invisible = boolSetting("Invisible", true);
    private final BoolSetting esp = boolSetting("ESP", false);
    private final EnumSetting<ESPMode> espMode = enumSetting("ESP Mode", ESPMode.Firefly, esp::getValue);
    private final ColorSetting espColor1 = colorSetting("ESP Main", new Color(255, 183, 197), () -> esp.getValue() && espMode.is(ESPMode.CaptureMark));
    private final ColorSetting espColor2 = colorSetting("ESP Second", new Color(255, 133, 161), () -> esp.getValue() && espMode.is(ESPMode.CaptureMark));
    private final DoubleSetting espSize = doubleSetting("ESP Size", 1.2, 0.5, 3.0, 0.1, () -> esp.getValue() && espMode.is(ESPMode.CaptureMark));
    private final DoubleSetting espRotSpeed = doubleSetting("Rot Speed", 2.0, 0.5, 10.0, 0.1, () -> esp.getValue() && espMode.is(ESPMode.CaptureMark));
    private final DoubleSetting waveSpeed = doubleSetting("Wave Speed", 3.0, 0.5, 10.0, 0.1, () -> esp.getValue() && espMode.is(ESPMode.CaptureMark));
    private final ColorSetting fireflyColor = colorSetting("Firefly Color", new Color(149, 149, 149, 80), () -> esp.getValue() && espMode.is(ESPMode.Firefly));
    private final IntSetting fireflyLength = intSetting("Firefly Length", 14, 8, 128, 1, () -> esp.getValue() && espMode.is(ESPMode.Firefly));
    private final IntSetting fireflyFactor = intSetting("Firefly Factor", 8, 1, 10, 1, () -> esp.getValue() && espMode.is(ESPMode.Firefly));
    private final DoubleSetting fireflyShaking = doubleSetting("Firefly Shaking", 1.8, 0.25, 10.0, 0.25, () -> esp.getValue() && espMode.is(ESPMode.Firefly));
    private final DoubleSetting fireflyAmplitude = doubleSetting("Firefly Amplitude", 3.0, 0.0, 10.0, 0.25, () -> esp.getValue() && espMode.is(ESPMode.Firefly));

    public LivingEntity target;
    private final List<LivingEntity> targets = new ArrayList<>();
    private boolean rotationReady;

    private int switchIndex = 0;
    private float attacks = 0;

    @Override
    protected void onDisable() {
        target = null;
        targets.clear();
        switchIndex = 0;
        rotationReady = false;
    }

    @SubscribeEvent
    public void onTick(ClientTickEvent.Pre e) {
        if (nullCheck()) return;

        targets.clear();
        targets.addAll(TargetManager.INSTANCE.acquireTargets(
                TargetManager.TargetRequest.of(
                        aimRange.getValue(),
                        fov.getValue().floatValue(),
                        player.getValue(),
                        mob.getValue(),
                        animal.getValue(),
                        animal.getValue(),
                        Invisible.getValue(),
                        64
                )
        ));

        if (targets.isEmpty()) {
            target = null;
            rotationReady = false;
            return;
        }

        if (targetMode.is(TargetMode.Single)) {
            target = targets.getFirst();
        } else if (targetMode.is(TargetMode.Switch)) {
            if (switchIndex >= targets.size()) switchIndex = 0;
            target = targets.get(switchIndex);
        } else if (targetMode.is(TargetMode.Multiple)) {
            target = targets.getFirst();
        }

        attacks += MathUtils.getRandom(cps.getValue().floatValue(), maxCps.getValue().floatValue()) / 20f;

        if (target != null) {
            rotationReady = false;
            final LivingEntity requestedTarget = target;
            final int requestPriority = Priority.Medium.priority;
            RotationManager.INSTANCE.applyRotation(
                    RotationUtils.getRotationsToEntity(requestedTarget),
                    rotationSpeed.getValue().floatValue(),
                    requestPriority,
                    record -> {
                        if (!isEnabled() || nullCheck()) return;
                        if (target != requestedTarget) return;
                        if (record.selectedPriorityValue() != requestPriority) return;
                        rotationReady = true;
                    }
            );
        }
    }

    @SubscribeEvent
    public void onClick(ClientTickEvent.Pre event) {
        if (nullCheck()) return;
        if (target == null) return;
        if (!rotationReady) return;
        if (mc.player.isUsingItem() || mc.player.isBlocking()) return;
        if (mc.player.getAttackStrengthScale(0.5f) < 1.0f && mode.is(Mode.OnePointNinePlus)) return;

        while (attacks >= 1) {
            if (targetMode.is(TargetMode.Multiple)) {
                for (LivingEntity t : targets) {
                    if (RotationUtils.getEyeDistanceToEntity(t) <= range.getValue() && mc.hitResult.getType() == HitResult.Type.ENTITY) {
                        doAttack();
                    }
                }
                switchIndex++;
            } else {
                if (RotationUtils.getEyeDistanceToEntity(target) <= range.getValue() && mc.hitResult.getType() == HitResult.Type.ENTITY && mc.crosshairPickEntity.is(target)) {
                    doAttack();
                    if (targetMode.is(TargetMode.Switch)) switchIndex++;
                } else if (targetMode.is(TargetMode.Switch)) {
                    switchIndex++;
                }
            }
            attacks -= 1;
        }
    }

    @SubscribeEvent
    private void onRender3D(RenderLevelStageEvent.AfterLevel event) {
        if (nullCheck() || !esp.getValue() || target == null) return;

        switch (espMode.getValue()) {
            case CaptureMark ->
                    CaptureMark.render(event.getPoseStack(), target, espSize.getValue(), espRotSpeed.getValue(), waveSpeed.getValue(), espColor1.getValue(), espColor2.getValue());
            case Firefly ->
                    Firefly.render(event.getPoseStack(), target, fireflyLength.getValue(), fireflyFactor.getValue(), fireflyShaking.getValue(), fireflyAmplitude.getValue(), fireflyColor.getValue());
        }

    }

    private void doAttack() {
        mc.gameMode.attack(mc.player, target);
        mc.player.swing(InteractionHand.MAIN_HAND);
    }


}
