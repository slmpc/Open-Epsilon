package com.github.lumin.modules.impl.combat;

import com.github.lumin.managers.Managers;
import com.github.lumin.modules.Category;
import com.github.lumin.modules.Module;
import com.github.lumin.settings.impl.BoolSetting;
import com.github.lumin.settings.impl.DoubleSetting;
import com.github.lumin.settings.impl.EnumSetting;
import com.github.lumin.settings.impl.IntSetting;
import com.github.lumin.utils.math.MathUtils;
import com.github.lumin.utils.player.FindItemResult;
import com.github.lumin.utils.player.InvUtils;
import com.github.lumin.utils.render.Render3DUtils;
import com.github.lumin.utils.rotation.MovementFix;
import com.github.lumin.utils.rotation.Priority;
import com.github.lumin.utils.rotation.RaytraceUtils;
import com.github.lumin.utils.rotation.RotationUtils;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class KillAura extends Module {

    public static final KillAura INSTANCE = new KillAura();

    private KillAura() {
        super("KillAura", Category.COMBAT);
    }

    public EnumSetting<MoveFixMode> moveFix = enumSetting("MoveFixMode", MoveFixMode.Silent);
    public EnumSetting<TargetMode> targetMode = enumSetting("TargetMode", TargetMode.Single);
    public DoubleSetting range = doubleSetting("Range", 3.0, 1.0, 6.0, 0.01);
    public DoubleSetting aimRange = doubleSetting("AimRange", 4.0, 1.0, 6.0, 0.1);
    public IntSetting speed = intSetting("Speed", 10, 1, 10, 1);
    public DoubleSetting fov = doubleSetting("FOV", 360.0, 10.0, 360.0, 1.0);
    public BoolSetting raytrace = boolSetting("Raytrace", true);
    public DoubleSetting wallsRange = doubleSetting("WallsRange", 0.0, 0.0, 3.0, 0.1, () -> raytrace.getValue());
    public BoolSetting cooldownATK = boolSetting("CooldownATK", false);
    public BoolSetting esp = boolSetting("ESP", false);
    public DoubleSetting cps = doubleSetting("CPS", 10.0, 1.0, 20.0, 1.0);
    public DoubleSetting maxCps = doubleSetting("MaxCPS", 12, 1, 20, 1);
    public BoolSetting player = boolSetting("Player", true);
    public BoolSetting mob = boolSetting("Mob", true);
    public BoolSetting animal = boolSetting("Animal", true);
    public BoolSetting Invisible = boolSetting("Invisible", true);

    public static LivingEntity target;
    public static List<LivingEntity> targets = new ArrayList<>();

    private int switchIndex = 0;
    public float attacks = 0;
    private long lastAttackTime = 0;


    @Override
    protected void onDisable() {
        target = null;
        targets.clear();
        switchIndex = 0;
    }

    @SubscribeEvent
    public void onTick(ClientTickEvent.Pre e) {
        if (nullCheck()) return;

        targets.clear();
        updateTargets();

        if (targets.isEmpty()) {
            target = null;
            return;
        }

        if (targetMode.is("Single")) {
            target = targets.getFirst();
        } else if (targetMode.is("Switch")) {
            if (switchIndex >= targets.size()) switchIndex = 0;
            target = targets.get(switchIndex);
        } else if (targetMode.is("Multiple")) {
            target = targets.getFirst();
        }

        attacks += MathUtils.getRandom(cps.getValue().floatValue(), maxCps.getValue().floatValue()) / 20f;

        if (target != null) {
            float[] rotations = RotationUtils.getRotationsToEntity(target);
            boolean silent = moveFix.is("Silent");
            Managers.ROTATION.setRotations(new Vector2f(rotations[0], rotations[1]), speed.getValue().floatValue(), MovementFix.ON, Priority.Medium);
        }
    }

    @SubscribeEvent
    public void onRender3D(RenderLevelStageEvent.AfterLevel event) {
        if (!esp.getValue()) return;
        if (targets.isEmpty()) return;

        for (LivingEntity entity : targets) {
            if (entity.equals(target)) {
                Render3DUtils.drawFullBox(event.getPoseStack(), entity.getBoundingBox(), new Color(200, 0, 0, 60), new Color(200, 0, 0, 255), 2f);
            } else {
                Render3DUtils.drawFullBox(event.getPoseStack(), entity.getBoundingBox(), new Color(0, 200, 0, 60), new Color(0, 200, 0, 255), 2f);
            }
        }
    }

    @SubscribeEvent
    public void onClick(ClientTickEvent.Pre e) {
        if (nullCheck()) return;
        if (target == null) return;
        if (mc.player.isUsingItem() || mc.player.isBlocking()) return;
//        if (mc.player.getAttackStrengthScale(0.5f) < 1.0f && cooldownATK.getValue()) return;
        while (attacks >= 1) {
            FindItemResult weapon = findWeapon();
            if (weapon.found()) {
                InvUtils.swap(weapon.slot(), true);
            }
            if (targetMode.is("Multiple")) {
                for (LivingEntity t : targets) {
                    if (RotationUtils.getEyeDistanceToEntity(t) <= range.getValue() && canAttackTarget(target) && mc.hitResult.getType() == HitResult.Type.ENTITY) {
                        doAttack();
                    }
                }
                switchIndex++;
            } else {
                if (RotationUtils.getEyeDistanceToEntity(target) <= range.getValue() && canAttackTarget(target) && mc.hitResult.getType() == HitResult.Type.ENTITY && mc.crosshairPickEntity.is(target)) {
                    doAttack();
                    if (targetMode.is("Switch")) switchIndex++;
                } else if (targetMode.is("Switch")) {
                    switchIndex++;
                }
            }
            attacks -= 1;
        }
    }

    private void doAttack() {
        if (cooldownATK.getValue()) {
            if (mc.player.getAttackStrengthScale(0.5f) >= 1.0f) {
                mc.gameMode.attack(mc.player, target);
                mc.player.swing(InteractionHand.MAIN_HAND);
            }
        } else {
            long time = System.currentTimeMillis();
            double baseDelay = 1000.0 / MathUtils.getRandom(cps.getValue().floatValue(), maxCps.getValue().floatValue());
            baseDelay += MathUtils.getRandom(-20, 30);
            long delay = (long) (baseDelay + (Math.random() - 0.5) * baseDelay * 0.4);
            if (time - lastAttackTime >= delay) {
                mc.gameMode.attack(mc.player, target);
                mc.player.swing(InteractionHand.MAIN_HAND);
                lastAttackTime = time;
            }
        }
    }

    private FindItemResult findWeapon() {
        return InvUtils.findInHotbar(itemStack -> isWeapon(itemStack));
    }

    private boolean isWeapon(ItemStack itemStack) {
        if (itemStack.isEmpty()) return false;
        return itemStack.has(DataComponents.WEAPON);
    }

    private void updateTargets() {
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof LivingEntity living)) continue;
            if (living == mc.player) continue;
            if (!living.isAlive() || living.isDeadOrDying()) continue;
            if (AntiBot.INSTANCE.isBot(entity)) continue;

            double dist = RotationUtils.getEyeDistanceToEntity(living);
            if (dist > aimRange.getValue()) continue;

            if (!isValidTarget(living)) continue;
            if (!RotationUtils.isInFov(living, fov.getValue().floatValue())) continue;
            targets.sort(Comparator.comparingDouble(o -> (double) o.distanceTo(mc.player)));
            targets.add(living);
        }
        targets.sort(Comparator.comparingDouble(RotationUtils::getEyeDistanceToEntity));
    }

    private boolean isValidTarget(LivingEntity entity) {
        if (entity instanceof Player) {
            if (!player.getValue()) return false;
            return !entity.isInvisible() || Invisible.getValue();
        } else if (entity instanceof Animal || entity instanceof Villager) {
            return animal.getValue();
        } else if (entity instanceof Monster) {
            return mob.getValue();
        } else {
            return false;
        }
    }

    private boolean canAttackTarget(LivingEntity entity) {
        if (!raytrace.getValue()) {
            return mc.hitResult.getType() == HitResult.Type.ENTITY && mc.crosshairPickEntity == entity;
        }

        Vector2f rotation = Managers.ROTATION.lastRotations;
        if (rotation == null) {
            rotation = new Vector2f(mc.player.getYRot(), mc.player.getXRot());
        }

        return RaytraceUtils.facingEnemy(mc.player, entity, rotation, range.getValue(), wallsRange.getValue());
    }

    public enum MoveFixMode {
        Silent,
        Strict,
    }

    public enum TargetMode {
        Single,
        Switch,
        Multiple,
    }

}