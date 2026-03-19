package com.github.lumin.modules.impl.player;

import com.github.lumin.managers.RotationManager;
import com.github.lumin.modules.Category;
import com.github.lumin.modules.Module;
import com.github.lumin.settings.impl.BoolSetting;
import com.github.lumin.settings.impl.DoubleSetting;
import com.github.lumin.settings.impl.EnumSetting;
import com.github.lumin.settings.impl.IntSetting;
import com.github.lumin.utils.player.FindItemResult;
import com.github.lumin.utils.player.InvUtils;
import com.github.lumin.utils.player.MoveUtils;
import com.github.lumin.utils.rotation.MovementFix;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.joml.Vector2f;

import java.util.HashSet;
import java.util.Set;

public class ElytraFly extends Module {

    public static final ElytraFly INSTANCE = new ElytraFly();

    private ElytraFly() {
        super("ElytraFly", Category.PLAYER);
    }

    private enum Mode {
        Firework
    }

    private final EnumSetting<Mode> mode = enumSetting("Mode", Mode.Firework);
    private final BoolSetting autoStart = boolSetting("AutoStart", true, () -> mode.is(Mode.Firework));
    private final BoolSetting onlyMoving = boolSetting("OnlyMoving", false, () -> mode.is(Mode.Firework));
    private final DoubleSetting horizontalSpeed = doubleSetting("HorizontalSpeed", 1.35, 0.1, 5.0, 0.05, () -> mode.is(Mode.Firework));
    private final DoubleSetting verticalSpeed = doubleSetting("VerticalSpeed", 0.8, 0.1, 2.0, 0.05, () -> mode.is(Mode.Firework));
    private final DoubleSetting glideDown = doubleSetting("GlideDown", -0.02, -0.2, 0.1, 0.01, () -> mode.is(Mode.Firework));
    private final DoubleSetting accel = doubleSetting("Acceleration", 0.35, 0.05, 1.0, 0.05, () -> mode.is(Mode.Firework));
    private final IntSetting boostDelay = intSetting("BoostDelay", 9, 2, 50, 1, () -> mode.is(Mode.Firework));
    private final IntSetting rotationSpeed = intSetting("RotationSpeed", 10, 1, 10, 1, () -> mode.is(Mode.Firework));

    private int rocketCooldown;
    private final Set<Integer> fireworkIds = new HashSet<>();

    @Override
    protected void onEnable() {
        rocketCooldown = 0;
        fireworkIds.clear();
    }

    @Override
    protected void onDisable() {
        rocketCooldown = 0;
        fireworkIds.clear();
    }

    public boolean isFirework(FireworkRocketEntity firework) {
        return fireworkIds.contains(firework.getId());
    }

    @SubscribeEvent
    private void onTick(ClientTickEvent.Pre event) {
        if (nullCheck()) return;

        if (mode.is(Mode.Firework)) {
            fireworkIds.removeIf(id -> mc.level.getEntity(id) == null);

            if (!canGlide()) {
                rocketCooldown = 0;
                return;
            }

            if (!mc.player.isFallFlying()) {
                if (autoStart.getValue() && !mc.player.onGround() && !mc.player.isInWater()) {
                    startFallFlying();
                }
                rocketCooldown = 0;
                return;
            }

            if (rocketCooldown > 0) {
                rocketCooldown--;
            }

            boolean controlling = hasControlInput();
            if (((!onlyMoving.getValue() || MoveUtils.isMoving()) && controlling) && rocketCooldown <= 0) {
                if (useFirework()) {
                    rocketCooldown = boostDelay.getValue();
                }
            }
        }
    }

    @SubscribeEvent
    private void onTickPost(ClientTickEvent.Post event) {
        if (nullCheck()) return;

        if (mode.is(Mode.Firework)) {
            if (!mc.player.isFallFlying()) return;
            applyMotion();
        }
    }

    private void startFallFlying() {
        if (mc.player.tryToStartFallFlying()) {
            mc.getConnection().send(new ServerboundPlayerCommandPacket(mc.player, ServerboundPlayerCommandPacket.Action.START_FALL_FLYING));
        }
    }

    private boolean canGlide() {
        for (EquipmentSlot slot : EquipmentSlot.VALUES) {
            if (LivingEntity.canGlideUsing(mc.player.getItemBySlot(slot), slot)) {
                return true;
            }
        }
        return false;
    }

    private void applyMotion() {
        if (!hasControlInput()) {
            mc.player.setDeltaMovement(Vec3.ZERO);
            mc.player.fallDistance = 0.0f;
            return;
        }

        Vec3 moveDir = getMoveDirection();
        Vec3 motion = mc.player.getDeltaMovement();
        boolean up = mc.options.keyJump.isDown();
        boolean down = mc.options.keyShift.isDown();
        boolean sprinting = mc.player.isSprinting() || mc.options.keySprint.isDown();

        double speedMul = sprinting ? 1.35 : 1.0;
        double targetX = moveDir.x * horizontalSpeed.getValue() * speedMul;
        double targetZ = moveDir.z * horizontalSpeed.getValue() * speedMul;
        double targetY = up == down ? 0.0 : (up ? verticalSpeed.getValue() : -verticalSpeed.getValue());
        double factor = Math.max(accel.getValue(), 0.85);
        double newX = Mth.lerp(factor, motion.x, targetX);
        double newY = Mth.lerp(factor, motion.y, targetY);
        double newZ = Mth.lerp(factor, motion.z, targetZ);

        mc.player.setDeltaMovement(newX, newY, newZ);
        mc.player.fallDistance = 0.0f;
        rotateToMovement(newX, newY, newZ);
    }

    private void rotateToMovement(double x, double y, double z) {
        double horizontal = Math.sqrt(x * x + z * z);
        if (horizontal < 1.0E-5 && Math.abs(y) < 1.0E-5) return;
        float yaw = (float) Math.toDegrees(Math.atan2(z, x)) - 90.0f;
        float pitch = (float) (-Math.toDegrees(Math.atan2(y, Math.max(horizontal, 1.0E-5))));
        RotationManager.INSTANCE.setRotations(new Vector2f(Mth.wrapDegrees(yaw), Mth.clamp(pitch, -90.0f, 90.0f)), rotationSpeed.getValue(), MovementFix.OFF);
    }

    private boolean hasControlInput() {
        return MoveUtils.isMoving() || mc.options.keyJump.isDown() || mc.options.keyShift.isDown();
    }

    private Vec3 getMoveDirection() {
        float forward = mc.player.zza;
        float strafe = mc.player.xxa;
        if (forward == 0 && strafe == 0) {
            return Vec3.ZERO;
        }

        double yawRad = Math.toRadians(mc.player.getYRot());
        double sin = Math.sin(yawRad);
        double cos = Math.cos(yawRad);

        double x = forward * -sin + strafe * cos;
        double z = forward * cos + strafe * sin;

        double length = Math.sqrt(x * x + z * z);
        if (length <= 0.0) return Vec3.ZERO;
        return new Vec3(x / length, 0.0, z / length);
    }

    private boolean useFirework() {
        FindItemResult rocket = InvUtils.findInHotbar(Items.FIREWORK_ROCKET);
        if (!rocket.found()) {
            return false;
        }

        InteractionHand hand = rocket.getHand();
        boolean swapped = false;
        if (hand == InteractionHand.MAIN_HAND && rocket.slot() != mc.player.getInventory().getSelectedSlot()) {
            swapped = InvUtils.swap(rocket.slot(), true);
        }

        InteractionResult result = mc.gameMode.useItem(mc.player, hand);
        if (result.consumesAction()) {
            mc.player.swing(hand);
            updateFireworks();
        }

        if (swapped) {
            InvUtils.swapBack();
        }

        return result.consumesAction();
    }

    private void updateFireworks() {
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity instanceof FireworkRocketEntity firework && firework.distanceToSqr(mc.player) <= 64.0) {
                fireworkIds.add(firework.getId());
            }
        }
    }

}
