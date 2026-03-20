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
import com.github.lumin.utils.timer.TimerUtils;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.joml.Vector2f;

import java.util.HashSet;
import java.util.Set;

public class ElytraFly extends Module {

    public static final ElytraFly INSTANCE = new ElytraFly();
    private static final int CHEST_ARMOR_MENU_SLOT = 8 - EquipmentSlot.CHEST.getIndex();

    private ElytraFly() {
        super("ElytraFly", Category.PLAYER);
    }

    private enum Mode {
        Control,
        Firework
    }

    private enum SwapMode {
        Silent,
        InvSwitch
    }

    private final EnumSetting<Mode> mode = enumSetting("Mode", Mode.Firework);
    private final EnumSetting<SwapMode> swapMode = enumSetting("SwapMode", SwapMode.InvSwitch);
    private final BoolSetting armored = boolSetting("Armored", false);
    private final BoolSetting autoStart = boolSetting("AutoStart", true, () -> mode.is(Mode.Firework));
    private final DoubleSetting horizontalSpeed = doubleSetting("HorizontalSpeed", 1.35, 0.1, 5.0, 0.05, () -> mode.is(Mode.Firework));
    private final DoubleSetting verticalSpeed = doubleSetting("VerticalSpeed", 0.8, 0.1, 2.0, 0.05, () -> mode.is(Mode.Firework));
    private final DoubleSetting accel = doubleSetting("Acceleration", 0.35, 0.05, 1.0, 0.05, () -> mode.is(Mode.Firework));
    private final IntSetting boostDelay = intSetting("BoostDelay", 9, 2, 50, 1, () -> mode.is(Mode.Firework));
    private final IntSetting rotationSpeed = intSetting("RotationSpeed", 10, 1, 10, 1, () -> mode.is(Mode.Firework));

    private int offGroundTicks;

    private final TimerUtils timer = new TimerUtils();
    private final Set<Integer> fireworkIds = new HashSet<>();

    @Override
    protected void onEnable() {
        offGroundTicks = 0;
        timer.reset();
        fireworkIds.clear();
    }

    @Override
    protected void onDisable() {
        timer.reset();
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
                timer.reset();
                return;
            }

            if (!mc.player.isFallFlying()) {
                if (!mc.player.onGround()) {
                    offGroundTicks++;
                    if (autoStart.getValue() && !mc.player.isInWater()) {
                        if (armored.getValue()) {
                            if (hasInput()) {
                                这个是甲飞();
                            }
                        } else {
                            if (startFallFlying() && hasInput()) {
                                useFirework(true);
                            }
                        }
                    }
                    return;
                } else {
                    offGroundTicks = 0;
                }
            }

            if (!armored.getValue()) {
                useFirework(false);
            }

            applyMotion();
        }
    }

    @SubscribeEvent
    private void onPlayerTick(PlayerTickEvent.Pre event) {
        // 我操了家人我想不到更好的办法在第一个tick放烟花的办法了。。。
        if (armored.getValue() && mc.player.isFallFlying() && offGroundTicks == 1) {
            useFirework(true);
        }
    }

    private boolean startFallFlying() {
        if (mc.player.tryToStartFallFlying()) {
            mc.getConnection().send(new ServerboundPlayerCommandPacket(mc.player, ServerboundPlayerCommandPacket.Action.START_FALL_FLYING));
            return true;
        }
        return false;
    }

    private boolean canGlide() {
        if (armored.getValue() && mode.is(Mode.Firework) && hasElytra()) {
            return true;
        }
        for (EquipmentSlot slot : EquipmentSlot.VALUES) {
            if (LivingEntity.canGlideUsing(mc.player.getItemBySlot(slot), slot)) {
                return true;
            }
        }
        return false;
    }

    private void applyMotion() {
        if (!hasInput()) {
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

    private boolean hasInput() {
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

    private void useFirework(boolean ignoreDelay) {
        if (hasInput() && (ignoreDelay || timer.delay(boostDelay.getValue()))) {
            FindItemResult rocket = swapMode.is(SwapMode.Silent) ? InvUtils.findInHotbar(Items.FIREWORK_ROCKET) : InvUtils.find(Items.FIREWORK_ROCKET);
            if (!rocket.found()) return;

            InteractionHand hand = rocket.getHand();
            boolean swapped = swapMode.is(SwapMode.Silent) ? InvUtils.swap(rocket.slot(), true) : InvUtils.invSwap(rocket.slot());

            InteractionResult result = mc.gameMode.useItem(mc.player, hand);

            if (result.consumesAction()) {
                timer.reset();
                mc.player.swing(hand);
                updateFireworks();
            }

            if (swapped) {
                if (swapMode.is(SwapMode.Silent)) {
                    InvUtils.swapBack();
                } else {
                    InvUtils.invSwapBack();
                }
            }
        }
    }

    private boolean hasElytra() {
        return findElytra() != -1;
    }

    private int findElytra() {
        return InvUtils.find(Items.ELYTRA).slot();
    }

    private int toContainerSlot(int slot) {
        if (slot < 9) {
            return slot + 36;
        }
        return slot;
    }

    private void 这个是甲飞() {
        if (!mc.player.containerMenu.getCarried().isEmpty()) return;

        int elytraSlot = findElytra();
        if (elytraSlot < 0) return;

        int elytraContainerSlot = toContainerSlot(elytraSlot);

        swapChestSlotWith(elytraContainerSlot);

        startFallFlying();
        useFirework(false);

        swapChestSlotWith(elytraContainerSlot);
    }

    private void swapChestSlotWith(int containerSlot) {
        int containerId = mc.player.containerMenu.containerId;
        mc.gameMode.handleInventoryMouseClick(containerId, containerSlot, 0, ClickType.PICKUP, mc.player);
        mc.gameMode.handleInventoryMouseClick(containerId, CHEST_ARMOR_MENU_SLOT, 0, ClickType.PICKUP, mc.player);
        mc.gameMode.handleInventoryMouseClick(containerId, containerSlot, 0, ClickType.PICKUP, mc.player);
    }

    private void updateFireworks() {
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity instanceof FireworkRocketEntity firework && firework.distanceToSqr(mc.player) <= 64.0) {
                fireworkIds.add(firework.getId());
            }
        }
    }

}
