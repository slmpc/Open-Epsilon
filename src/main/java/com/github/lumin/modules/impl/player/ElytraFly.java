package com.github.lumin.modules.impl.player;

import com.github.lumin.Lumin;
import com.github.lumin.events.MotionEvent;
import com.github.lumin.events.PacketEvent;
import com.github.lumin.modules.Category;
import com.github.lumin.modules.Module;
import com.github.lumin.settings.impl.BoolSetting;
import com.github.lumin.settings.impl.DoubleSetting;
import com.github.lumin.settings.impl.EnumSetting;
import com.github.lumin.settings.impl.IntSetting;
import com.github.lumin.utils.player.ChatUtils;
import com.github.lumin.utils.timer.TimerUtils;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.function.Predicate;

public class ElytraFly extends Module {

    public static final ElytraFly INSTANCE = new ElytraFly();
    private static final int CHEST_ARMOR_MENU_SLOT = 8 - EquipmentSlot.CHEST.getIndex();

    private final EnumSetting<Mode> mode = enumSetting("Mode", Mode.Armored);
    private final BoolSetting visualSpoof = boolSetting("VisualSpoof", true, () -> mode.is(Mode.Armored));
    private final BoolSetting setFlag = boolSetting("SetFlag", true, () -> mode.is(Mode.Armored));
    private final BoolSetting armoredSilent = boolSetting("ArmoredSilent", true, () -> mode.is(Mode.Armored));
    private final BoolSetting armoredAntiCollide = boolSetting("ArmoredAntiCollide", true, () -> mode.is(Mode.Armored));
    private final BoolSetting armoredSneakingFix = boolSetting("ArmoredSneakingFix", true, () -> mode.is(Mode.Armored));
    private final BoolSetting armoredGrimV3 = boolSetting("ArmoredGrimV3", false, () -> mode.is(Mode.Armored));
    private final BoolSetting armoredAutoJump = boolSetting("ArmoredAutoJump", false, () -> mode.is(Mode.Armored));
    private final BoolSetting armoredFireworks = boolSetting("ArmoredFireworks", true, () -> mode.is(Mode.Armored));
    private final IntSetting armoredDelay = intSetting("ArmoredDelay", 10, 0, 40, 1, () -> mode.is(Mode.Armored) && armoredFireworks.getValue());
    private final BoolSetting armoredUsingPause = boolSetting("ArmoredUsingPause", false, () -> mode.is(Mode.Armored) && armoredFireworks.getValue());
    private final DoubleSetting armoredForwardSpeed = doubleSetting("ArmoredForwardSpeed", 1.2, 0.1, 5.0, 0.1, () -> mode.is(Mode.Armored));
    private final DoubleSetting armoredVerticalSpeed = doubleSetting("ArmoredVerticalSpeed", 1.0, 0.1, 5.0, 0.1, () -> mode.is(Mode.Armored));
    private final BoolSetting armoredFreeze = boolSetting("ArmoredFreeze", false, () -> mode.is(Mode.Armored));
    private final EnumSetting<ArmoredFreezeMode> armoredFreezeMode = enumSetting("ArmoredFreezeMode", ArmoredFreezeMode.Static, () -> mode.is(Mode.Armored) && armoredFreeze.getValue());
    private final IntSetting armoredChestplateDelayTicks = intSetting("ArmoredChestplateDelayTicks", 0, 0, 10, 1, () -> mode.is(Mode.Armored) && armoredSilent.getValue());
    private final IntSetting armoredRetryCooldownTicksSetting = intSetting("ArmoredRetryCooldownTicks", 4, 0, 20, 1, () -> mode.is(Mode.Armored));
    private final BoolSetting armoredPacketDebug = boolSetting("ArmoredPacketDebug", false, () -> mode.is(Mode.Armored));

    private final TimerUtils armoredFireworkTimer = new TimerUtils();
    private final Set<Integer> fireworkIds = new HashSet<>();

    private Vec3 armoredFreezePos;
    private int armoredChestplateRestoreTicks;
    private int armoredRetryCooldownTicks;
    private int armoredPacketSequence;
    private String armoredLastStatusMessage;
    private boolean armoredChestplateRestorePending;
    private boolean armoredChestSwapped;
    private boolean armoredRestoredChestplateThisTick;

    private ElytraFly() {
        super("ElytraFly", Category.PLAYER);
    }

    @Override
    protected void onEnable() {
        if (nullCheck()) {
            return;
        }

        resetArmoredState(true);

        if (mode.is(Mode.Armored)) {
            if (armoredAutoJump.getValue() && mc.player.onGround()) {
                mc.player.jumpFromGround();
            }

            mc.player.stopFallFlying();
        }
    }

    @Override
    protected void onDisable() {
        if (nullCheck()) {
            return;
        }

        if (mode.is(Mode.Armored) && armoredSneakingFix.getValue()) {
            sendShiftInput(false);
        }

        if (mode.is(Mode.Armored) && armoredSilent.getValue()) {
            restoreChestplateArmoredNow();
        }

        resetArmoredState(false);
    }

    public boolean isFirework(FireworkRocketEntity firework) {
        return fireworkIds.contains(firework.getId());
    }

    public boolean shouldVisualSpoof() {
        return isEnabled() && mode.is(Mode.Armored) && visualSpoof.getValue();
    }

    public boolean handleArmoredTravel() {
        if (nullCheck() || !isEnabled() || !mode.is(Mode.Armored) || !mc.player.isFallFlying()) {
            return false;
        }

        if (armoredAntiCollide.getValue() && !mc.level.hasChunk(Mth.floor(mc.player.getX() / 16.0), Mth.floor(mc.player.getZ() / 16.0))) {
            return true;
        }

        if (armoredFreeze.getValue() && !isArmoredMoving() && armoredFreezePos != null) {
            mc.player.move(MoverType.SELF, mc.player.getDeltaMovement());
            return true;
        }

        applyArmoredMovement();
        mc.player.move(MoverType.SELF, mc.player.getDeltaMovement());
        return true;
    }

    @SubscribeEvent
    private void onTick(ClientTickEvent.Pre event) {
        if (nullCheck() || !mode.is(Mode.Armored)) {
            return;
        }

        armoredRestoredChestplateThisTick = false;
        fireworkIds.removeIf(id -> mc.level.getEntity(id) == null);
        tickArmoredRetryCooldown();
        tickArmoredChestplateRestore();
        updateArmoredMode();
    }

    @SubscribeEvent
    private void onMotion(MotionEvent event) {
        if (nullCheck() || !mode.is(Mode.Armored) || !mc.player.isFallFlying()) {
            return;
        }

        if (mc.options.keyJump.isDown()) {
            event.setPitch(-90.0f);
        } else if (mc.options.keyShift.isDown()) {
            event.setPitch(90.0f);
        }
    }

    @SubscribeEvent
    private void onPacketReceive(PacketEvent.Receive event) {
        if (nullCheck() || !mode.is(Mode.Armored)) {
            return;
        }

        if (event.getPacket() instanceof ClientboundPlayerPositionPacket) {
            mc.player.stopFallFlying();
            if (armoredSilent.getValue()) {
                restoreChestplateArmoredNow();
            }
            setArmoredRetryCooldown();
        }
    }

    @SubscribeEvent
    private void onPacketTraceSend(PacketEvent.Send event) {
        if (shouldTraceArmoredPacket(event.getPacket())) {
            traceArmoredPacket("OUT", event.getPacket());
        }
    }

    @SubscribeEvent
    private void onPacketTraceReceive(PacketEvent.Receive event) {
        if (shouldTraceArmoredPacket(event.getPacket())) {
            traceArmoredPacket("IN", event.getPacket());
        }
    }

    private void updateArmoredMode() {
        updateArmoredFreezeState();
        if (shouldPauseArmoredFlight()) {
            return;
        }
        performArmoredFlight();
    }

    private void applyArmoredMovement() {
        double verticalSpeed = armoredVerticalSpeed.getValue();
        if (mc.options.keyJump.isDown()) {
            mc.player.setDeltaMovement(mc.player.getDeltaMovement().x, verticalSpeed, mc.player.getDeltaMovement().z);
        } else if (mc.options.keyShift.isDown()) {
            mc.player.setDeltaMovement(mc.player.getDeltaMovement().x, -verticalSpeed, mc.player.getDeltaMovement().z);
        } else {
            mc.player.setDeltaMovement(mc.player.getDeltaMovement().x, 0.0, mc.player.getDeltaMovement().z);
        }

        if (mc.options.keyUp.isDown()) {
            float yaw = (float) Math.toRadians(mc.player.getYRot());
            double forwardSpeed = armoredForwardSpeed.getValue();
            mc.player.setDeltaMovement(-Mth.sin(yaw) * forwardSpeed, mc.player.getDeltaMovement().y, Mth.cos(yaw) * forwardSpeed);
        } else {
            mc.player.setDeltaMovement(0.0, mc.player.getDeltaMovement().y, 0.0);
        }
    }

    private void performArmoredFlight() {
        if (armoredSilent.getValue()) {
            if (!prepareArmoredFlightChestSlot()) {
                return;
            }
        } else if (!prepareArmoredFlightChestSlot()) {
            emitArmoredStatus("Armored cannot start: no usable elytra equipped");
            return;
        }

        sendArmoredLiftOffPackets();
        tryUseArmoredFirework();
        scheduleChestplateRestore();
    }

    private void applyArmoredFreeze() {
        if (armoredFreezePos == null) {
            return;
        }

        switch (armoredFreezeMode.getValue()) {
            case Static -> {
                mc.player.setDeltaMovement(Vec3.ZERO);
                mc.player.setPos(armoredFreezePos.x, armoredFreezePos.y, armoredFreezePos.z);
            }
            case Timer -> mc.player.setDeltaMovement(0.0, mc.player.getDeltaMovement().y * 0.9, 0.0);
            case Tick -> mc.player.setDeltaMovement(mc.player.getDeltaMovement().x * 0.5, mc.player.getDeltaMovement().y * 0.9, mc.player.getDeltaMovement().z * 0.5);
        }
    }

    private void useArmoredFirework() {
        if (mc.player.getMainHandItem().is(Items.FIREWORK_ROCKET)) {
            clearArmoredStatus();
            useCurrentArmoredFirework();
            return;
        }

        int inventorySlot = findInventoryItemSlot(Items.FIREWORK_ROCKET);
        if (inventorySlot != -1) {
            int syncId = mc.player.containerMenu.containerId;
            int selectedSlot = mc.player.getInventory().getSelectedSlot();
            mc.gameMode.handleContainerInput(syncId, inventorySlot, selectedSlot, ContainerInput.SWAP, mc.player);
            clearArmoredStatus();
            useCurrentArmoredFirework();
            mc.gameMode.handleContainerInput(syncId, inventorySlot, selectedSlot, ContainerInput.SWAP, mc.player);
            syncArmoredInventory();
            return;
        }

        int hotbarSlot = findHotbarItemSlot(Items.FIREWORK_ROCKET);
        if (hotbarSlot != -1) {
            int prevSlot = mc.player.getInventory().getSelectedSlot();
            mc.player.getInventory().setSelectedSlot(hotbarSlot);
            mc.gameMode.ensureHasSentCarriedItem();
            clearArmoredStatus();
            useCurrentArmoredFirework();
            mc.player.getInventory().setSelectedSlot(prevSlot);
            mc.gameMode.ensureHasSentCarriedItem();
            return;
        }

        emitArmoredStatus("Armored firework failed: no firework rocket found");
    }

    private void useCurrentArmoredFirework() {
        syncArmoredInventory();
        InteractionResult result = mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
        if (result.consumesAction()) {
            mc.player.swing(InteractionHand.MAIN_HAND);
            updateFireworks();
        }
    }

    private boolean swapToElytraArmored() {
        if (!canSwapArmoredChestSlot("Armored swap blocked: carried stack is not empty")) {
            return false;
        }

        int slot = findUsableElytraSlot();
        if (slot == -1) {
            emitArmoredStatus("Armored swap failed: no usable elytra found");
            return false;
        }

        clearArmoredStatus();
        swapContainerSlotWithChest(slot);
        return true;
    }

    private boolean swapToChestplateArmored() {
        if (!canSwapArmoredChestSlot("Armored restore blocked: carried stack is not empty")) {
            return false;
        }

        int slot = findChestplateSlot();
        if (slot == -1) {
            emitArmoredStatus("Armored restore failed: no chestplate found");
            return false;
        }

        clearArmoredStatus();
        swapContainerSlotWithChest(slot);
        return true;
    }

    private int findUsableElytraSlot() {
        return findContainerSlot(0, 36, stack ->
                !stack.isEmpty() && stack.is(Items.ELYTRA) && LivingEntity.canGlideUsing(stack, EquipmentSlot.CHEST)
        );
    }

    private int findChestplateSlot() {
        return findContainerSlot(0, 36, stack -> !stack.isEmpty() && isChestplate(stack));
    }

    private int findInventoryItemSlot(net.minecraft.world.item.Item item) {
        return findRawInventorySlot(9, 36, stack -> stack.is(item));
    }

    private int findHotbarItemSlot(net.minecraft.world.item.Item item) {
        return findRawInventorySlot(0, 9, stack -> stack.is(item));
    }

    private int toContainerSlot(int slot) {
        return slot < 9 ? slot + 36 : slot;
    }

    private boolean isChestplate(ItemStack stack) {
        return stack.getItem().toString().toLowerCase(Locale.ROOT).contains("chestplate");
    }

    private boolean isArmoredMoving() {
        Vec3 velocity = mc.player.getDeltaMovement();
        return Math.abs(velocity.x) > 0.001
                || Math.abs(velocity.z) > 0.001
                || mc.options.keyUp.isDown()
                || mc.options.keyDown.isDown()
                || mc.options.keyLeft.isDown()
                || mc.options.keyRight.isDown();
    }

    private void updateFireworks() {
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity instanceof FireworkRocketEntity firework && firework.distanceToSqr(mc.player) <= 64.0) {
                fireworkIds.add(firework.getId());
            }
        }
    }

    private void sendPlayerCommand(ServerboundPlayerCommandPacket.Action action) {
        mc.getConnection().send(new ServerboundPlayerCommandPacket(mc.player, action));
    }

    private void syncArmoredInventory() {
        mc.gameMode.ensureHasSentCarriedItem();
    }

    private void resetArmoredState(boolean resetFireworkTimer) {
        armoredFreezePos = null;
        if (resetFireworkTimer) {
            armoredFireworkTimer.reset();
        }
        fireworkIds.clear();
        armoredChestplateRestoreTicks = -1;
        armoredRetryCooldownTicks = 0;
        armoredPacketSequence = 0;
        armoredLastStatusMessage = null;
        armoredChestplateRestorePending = false;
        armoredChestSwapped = false;
        armoredRestoredChestplateThisTick = false;
    }

    private void updateArmoredFreezeState() {
        if (armoredFreeze.getValue() && !isArmoredMoving() && mc.player.isFallFlying()) {
            if (armoredFreezePos == null) {
                armoredFreezePos = mc.player.position();
            }

            applyArmoredFreeze();
            return;
        }

        armoredFreezePos = null;
    }

    private boolean shouldPauseArmoredFlight() {
        if (mc.player.onGround()) {
            restoreChestplateIfNeeded();
            emitArmoredStatus("Armored waiting for airborne state");
            return true;
        }

        if (armoredRestoredChestplateThisTick) {
            return true;
        }

        if (armoredRetryCooldownTicks > 0 && !mc.player.isFallFlying()) {
            emitArmoredStatus("Armored retry cooldown active");
            return true;
        }

        return false;
    }

    private void restoreChestplateIfNeeded() {
        if (armoredSilent.getValue() && armoredChestSwapped) {
            restoreChestplateArmoredNow();
        }
    }

    private boolean prepareArmoredFlightChestSlot() {
        if (!armoredSilent.getValue()) {
            return canStartArmoredFlightWithChestSlot();
        }

        if (!armoredChestSwapped && !swapToElytraArmored()) {
            return false;
        }

        armoredChestSwapped = true;
        return true;
    }

    private void sendArmoredLiftOffPackets() {
        if (armoredGrimV3.getValue()) {
            sendShiftInput(true);
        }

        sendPlayerCommand(ServerboundPlayerCommandPacket.Action.START_FALL_FLYING);
        if (setFlag.getValue()) {
            mc.player.startFallFlying();
        }

        if (armoredGrimV3.getValue()) {
            sendShiftInput(false);
        }
    }

    private void tryUseArmoredFirework() {
        if (!armoredFireworks.getValue() || !isArmoredMoving()) {
            return;
        }

        double delayMs = armoredDelay.getValue() * 100.0;
        if (!armoredFireworkTimer.passedMillise(delayMs)) {
            return;
        }

        if (armoredUsingPause.getValue() && mc.player.isUsingItem()) {
            return;
        }

        useArmoredFirework();
        armoredFireworkTimer.reset();
    }

    private void scheduleChestplateRestore() {
        if (!armoredSilent.getValue()) {
            return;
        }

        if (armoredChestplateDelayTicks.getValue() <= 0) {
            restoreChestplateArmoredNow();
            return;
        }

        if (!armoredChestplateRestorePending) {
            armoredChestplateRestoreTicks = armoredChestplateDelayTicks.getValue();
            armoredChestplateRestorePending = true;
        }
    }

    private boolean canSwapArmoredChestSlot(String blockedMessage) {
        if (mc.player.containerMenu.getCarried().isEmpty()) {
            return true;
        }

        emitArmoredStatus(blockedMessage);
        setArmoredRetryCooldown();
        return false;
    }

    private void swapContainerSlotWithChest(int slot) {
        int syncId = mc.player.containerMenu.containerId;
        mc.gameMode.handleContainerInput(syncId, slot, 0, ContainerInput.PICKUP, mc.player);
        mc.gameMode.handleContainerInput(syncId, CHEST_ARMOR_MENU_SLOT, 0, ContainerInput.PICKUP, mc.player);
        mc.gameMode.handleContainerInput(syncId, slot, 0, ContainerInput.PICKUP, mc.player);
    }

    private int findContainerSlot(int start, int end, Predicate<ItemStack> predicate) {
        int slot = findRawInventorySlot(start, end, predicate);
        return slot == -1 ? -1 : toContainerSlot(slot);
    }

    private int findRawInventorySlot(int start, int end, Predicate<ItemStack> predicate) {
        for (int i = start; i < end; i++) {
            if (predicate.test(mc.player.getInventory().getItem(i))) {
                return i;
            }
        }

        return -1;
    }

    private void tickArmoredChestplateRestore() {
        if (!armoredChestplateRestorePending || !armoredChestSwapped) {
            return;
        }

        armoredChestplateRestoreTicks--;
        if (armoredChestplateRestoreTicks > 0) {
            return;
        }

        if (restoreChestplateArmoredNow()) {
            armoredRestoredChestplateThisTick = true;
        }
    }

    private boolean restoreChestplateArmoredNow() {
        if (!armoredChestSwapped) {
            armoredChestplateRestorePending = false;
            armoredChestplateRestoreTicks = -1;
            return true;
        }

        if (!swapToChestplateArmored()) {
            armoredChestplateRestorePending = true;
            armoredChestplateRestoreTicks = 1;
            return false;
        }

        syncArmoredInventory();
        armoredChestplateRestorePending = false;
        armoredChestplateRestoreTicks = -1;
        armoredChestSwapped = false;
        return true;
    }

    private void tickArmoredRetryCooldown() {
        if (armoredRetryCooldownTicks > 0) {
            armoredRetryCooldownTicks--;
        }
    }

    private void setArmoredRetryCooldown() {
        armoredRetryCooldownTicks = Math.max(armoredRetryCooldownTicks, armoredRetryCooldownTicksSetting.getValue());
    }

    private boolean canStartArmoredFlightWithChestSlot() {
        ItemStack chestStack = mc.player.getItemBySlot(EquipmentSlot.CHEST);
        return !chestStack.isEmpty()
                && chestStack.is(Items.ELYTRA)
                && LivingEntity.canGlideUsing(chestStack, EquipmentSlot.CHEST);
    }

    private boolean shouldTraceArmoredPacket(Packet<?> packet) {
        if (nullCheck() || !isEnabled() || !mode.is(Mode.Armored) || !armoredPacketDebug.getValue()) {
            return false;
        }

        if (packet instanceof ServerboundPlayerCommandPacket
                || packet instanceof ServerboundPlayerInputPacket
                || packet instanceof ClientboundPlayerPositionPacket) {
            return true;
        }

        String name = packet.getClass().getSimpleName();
        return name.contains("ContainerClick")
                || name.contains("UseItem")
                || name.contains("SetCarriedItem")
                || name.contains("SetSlot")
                || name.contains("SetContent");
    }

    private void traceArmoredPacket(String direction, Packet<?> packet) {
        armoredPacketSequence++;
        Lumin.LOGGER.info(String.format(
                Locale.ROOT,
                "[ArmoredTrace %03d] %s %s",
                armoredPacketSequence,
                direction,
                describeArmoredPacket(packet)
        ));
    }

    private void emitArmoredStatus(String message) {
        if (message.equals(armoredLastStatusMessage)) {
            return;
        }

        armoredLastStatusMessage = message;
        ChatUtils.addChatMessage("[Armored] " + message);
        Lumin.LOGGER.info("[Armored] {}", message);
    }

    private void clearArmoredStatus() {
        armoredLastStatusMessage = null;
    }

    private String describeArmoredPacket(Packet<?> packet) {
        if (packet instanceof ServerboundPlayerCommandPacket commandPacket) {
            return packet.getClass().getSimpleName() + " action=" + commandPacket.getAction().name();
        }

        return packet.getClass().getSimpleName();
    }

    private void sendShiftInput(boolean shift) {
        Input input = new Input(
                mc.options.keyUp.isDown(),
                mc.options.keyDown.isDown(),
                mc.options.keyLeft.isDown(),
                mc.options.keyRight.isDown(),
                mc.options.keyJump.isDown(),
                shift,
                mc.options.keySprint.isDown()
        );
        mc.getConnection().send(new ServerboundPlayerInputPacket(input));
    }

    public enum Mode {
        Control,
        Boost,
        Bounce,
        Freeze,
        None,
        Rotation,
        Pitch,
        Armored
    }

    public enum ArmoredFreezeMode {
        Static,
        Timer,
        Tick
    }
}
