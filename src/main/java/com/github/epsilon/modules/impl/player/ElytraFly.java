package com.github.epsilon.modules.impl.player;

import com.github.epsilon.events.MotionEvent;
import com.github.epsilon.events.MoveEvent;
import com.github.epsilon.events.PacketEvent;
import com.github.epsilon.events.TravelEvent;
import com.github.epsilon.modules.Category;
import com.github.epsilon.modules.Module;
import com.github.epsilon.settings.impl.BoolSetting;
import com.github.epsilon.settings.impl.DoubleSetting;
import com.github.epsilon.settings.impl.EnumSetting;
import com.github.epsilon.utils.player.FindItemResult;
import com.github.epsilon.utils.player.InvUtils;
import com.github.epsilon.utils.timer.TimerUtils;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * By Nemophilist2009
 */
public class ElytraFly extends Module {

    public static final ElytraFly INSTANCE = new ElytraFly();

    private ElytraFly() {
        super("ElytraFly", Category.PLAYER);
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

    private final EnumSetting<Mode> mode = enumSetting("Mode", Mode.Control);
    private final BoolSetting visualSpoof = boolSetting("VisualSpoof", false, this::isArmoredMode);

    private final BoolSetting motionStop = boolSetting("MotionStop", true, this::isArmoredMode);
    private final BoolSetting setFlag = boolSetting("SetFlag", true, this::isArmoredMode);
    private final BoolSetting releaseSneak = boolSetting("ReleaseSneak", true, this::isArmoredMode);
    private final BoolSetting armoredSilent = boolSetting("ArmoredSilent", true, this::isArmoredMode);
    private final BoolSetting armoredAntiCollide = boolSetting("ArmoredAntiCollide", true, this::isArmoredMode);
    private final BoolSetting armoredSneakingFix = boolSetting("ArmoredSneakingFix", true, this::isArmoredMode);
    private final BoolSetting armoredGrimV3 = boolSetting("ArmoredGrimV3", false, this::isArmoredMode);
    private final BoolSetting armoredAutoJump = boolSetting("ArmoredAutoJump", false, this::isArmoredMode);
    private final BoolSetting armoredFireworks = boolSetting("ArmoredFireworks", true, this::isArmoredMode);
    private final DoubleSetting armoredDelay = doubleSetting("ArmoredDelay", 10.0, 0.0, 40.0, 1.0, () -> isArmoredMode() && armoredFireworks.getValue());
    private final BoolSetting armoredUsingPause = boolSetting("ArmoredUsingPause", false, () -> isArmoredMode() && armoredFireworks.getValue());
    private final DoubleSetting armoredForwardSpeed = doubleSetting("ArmoredForwardSpeed", 1.2, 0.1, 5.0, 0.1, this::isArmoredMode);
    private final DoubleSetting armoredVerticalSpeed = doubleSetting("ArmoredVerticalSpeed", 1.0, 0.1, 5.0, 0.1, this::isArmoredMode);
    private final BoolSetting armoredFreeze = boolSetting("ArmoredFreeze", false, this::isArmoredMode);
    private final EnumSetting<ArmoredFreezeMode> armoredFreezeMode = enumSetting("ArmoredFreezeMode", ArmoredFreezeMode.Static, () -> isArmoredMode() && armoredFreeze.getValue());

    private boolean hasElytra;
    private float yaw;
    private float rotationPitch;
    private Vec3 freezePos;
    private boolean sendingFreezePacket;

    private final TimerUtils armoredFireworkTimer = new TimerUtils();
    private Vec3 armoredFreezePos;

    private boolean isArmoredMode() {
        return mode.is(Mode.Armored);
    }

    public boolean shouldVisualSpoof() {
        return isEnabled() && isArmoredMode() && visualSpoof.getValue();
    }

    private void setShiftKey(boolean shift) {
        Input input = mc.player.input.keyPresses;
        mc.player.input.keyPresses = new Input(input.forward(), input.backward(), input.left(), input.right(), input.jump(), shift, input.sprint());
        mc.player.setShiftKeyDown(shift);
        mc.getConnection().send(new ServerboundPlayerInputPacket(mc.player.input.keyPresses));
    }

    private void releaseShiftKey() {
        this.setShiftKey(false);
    }

    private boolean shouldFreezeNow() {
        return this.mode.is(Mode.Freeze) && this.freezePos != null;
    }

    @Override
    public void onEnable() {
        if (!nullCheck()) {
            this.hasElytra = false;
            this.yaw = mc.player.getYRot();
            this.rotationPitch = mc.player.getXRot();
            this.freezePos = null;
            this.sendingFreezePacket = false;
            this.armoredFreezePos = null;
            //this.armoredFireworkTimer.reset();  操你妈这个reset纯纯就是搞我来的
            if (this.mode.is(ElytraFly.Mode.Armored)) {
                if (this.armoredAutoJump.getValue() && mc.player.onGround()) {
                    mc.player.jumpFromGround();
                }
                mc.player.stopFallFlying();
            }
        }
    }

    @Override
    public void onDisable() {
        if (!nullCheck()) {
            if (this.releaseSneak.getValue()) {
                this.releaseShiftKey();
            }

            if (this.mode.is(Mode.Armored) && this.armoredSneakingFix.getValue()) {
                this.releaseShiftKey();
            }

            this.armoredFreezePos = null;
            this.freezePos = null;
            this.sendingFreezePacket = false;
        }
    }

    @SubscribeEvent
    public void onUpdateRotate(MotionEvent event) {
        if (!nullCheck()) {
            if (this.mode.is(ElytraFly.Mode.Rotation)) {
                if (mc.player.isFallFlying()) {
                    if (isMoving()) {
                        if (mc.options.keyJump.isDown()) {
                            this.rotationPitch = -45.0F;
                        } else if (mc.options.keyShift.isDown()) {
                            this.rotationPitch = 45.0F;
                        } else {
                            this.rotationPitch = -1.9F;
                            if (this.motionStop.getValue()) {
                                this.setY(0.0);
                            }
                        }
                    } else if (mc.options.keyJump.isDown()) {
                        this.rotationPitch = -89.0F;
                    } else if (mc.options.keyShift.isDown()) {
                        this.rotationPitch = 89.0F;
                    } else if (this.motionStop.getValue()) {
                        this.setY(0.0);
                    }

                    if (mc.options.keyUp.isDown() || mc.options.keyDown.isDown() || mc.options.keyLeft.isDown() || mc.options.keyRight.isDown()) {
                        this.yaw = getSprintYaw(mc.player.getYRot());
                    } else if (this.motionStop.getValue()) {
                        this.setX(0.0);
                        this.setZ(0.0);
                    }

                    event.setYaw(this.yaw);
                    event.setPitch(this.rotationPitch);
                }
            } else if (this.mode.is(ElytraFly.Mode.Armored) && mc.player.isFallFlying()) {
                if (mc.options.keyJump.isDown()) {
                    event.setPitch(-90.0F);
                } else if (mc.options.keyShift.isDown()) {
                    event.setPitch(90.0F);
                }
            }
        }
    }

    @SubscribeEvent
    public void onUpdate(PlayerTickEvent.Pre event) {
        if (!nullCheck() && event.getEntity() == mc.player && this.mode.is(ElytraFly.Mode.Armored)) {
            this.updateArmoredMode();
        }
    }

    @SubscribeEvent
    public void onPlayerMove(MoveEvent event) {
        if (nullCheck() || !this.mode.is(Mode.Armored) || !this.armoredAntiCollide.getValue() || !mc.player.isFallFlying()) {
            return;
        }

        int chunkX = (int) Math.floor(mc.player.getX() / 16.0);
        int chunkZ = (int) Math.floor(mc.player.getZ() / 16.0);
        if (!mc.level.getChunkSource().hasChunk(chunkX, chunkZ)) {
            event.setX(0.0);
            event.setY(0.0);
            event.setZ(0.0);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    private void onPacketReceive(PacketEvent.Receive event) {
        if (!nullCheck()) {
            if (this.mode.is(Mode.Bounce) && this.hasElytra && event.getPacket() instanceof ClientboundPlayerPositionPacket) {
                mc.player.stopFallFlying();
            } else if (this.mode.is(Mode.Armored) && event.getPacket() instanceof ClientboundPlayerPositionPacket) {
                mc.player.stopFallFlying();
            } else if (this.shouldFreezeNow() && event.getPacket() instanceof ClientboundPlayerPositionPacket) {
                this.freezePos = null;
            }
        }
    }

    @SubscribeEvent
    private void onTravel(TravelEvent event) {
        if (!nullCheck() && this.mode.is(Mode.Armored) && mc.player.isFallFlying()) {
            this.applyArmoredMovement();
            event.setCanceled(true);
            //mc.player.move(MoverType.SELF, mc.player.getDeltaMovement());
        }
    }

    private float getSprintYaw(float yaw) {
        boolean forward = mc.options.keyUp.isDown();
        boolean backward = mc.options.keyDown.isDown();
        boolean left = mc.options.keyLeft.isDown();
        boolean right = mc.options.keyRight.isDown();
        if (forward && !backward) {
            if (left && !right) {
                yaw -= 45.0f;
            } else if (right && !left) {
                yaw += 45.0f;
            }
        } else if (backward && !forward) {
            yaw += 180.0f;
            if (left && !right) {
                yaw += 45.0f;
            } else if (right && !left) {
                yaw -= 45.0f;
            }
        } else if (left && !right) {
            yaw -= 90.0f;
        } else if (right && !left) {
            yaw += 90.0f;
        }
        return Mth.wrapDegrees(yaw);
    }

    private void updateArmoredMode() {
        if (this.armoredFreeze.getValue() && !this.isMoving() && mc.player.isFallFlying()) {
            if (this.armoredFreezePos == null) {
                this.armoredFreezePos = mc.player.position();
            }

            this.applyArmoredFreeze();
        } else {
            this.armoredFreezePos = null;
        }

        if (!mc.player.onGround()) {
            this.performArmoredFlight();
        }
    }

    private void applyArmoredMovement() {
        double verticalSpeed = this.armoredVerticalSpeed.getValue();
        if (mc.options.keyJump.isDown()) {
            this.setY(verticalSpeed);
        } else if (mc.options.keyShift.isDown()) {
            this.setY(-verticalSpeed);
        } else {
            this.setY(0.0);
        }

        if (mc.options.keyUp.isDown()) {
            float yaw = (float) Math.toRadians(mc.player.getYRot());
            double forwardSpeed = this.armoredForwardSpeed.getValue();
            this.setX(-Mth.sin(yaw) * forwardSpeed);
            this.setZ(Mth.cos(yaw) * forwardSpeed);
        } else {
            this.setX(0.0);
            this.setZ(0.0);
        }
    }

    private void performArmoredFlight() {
        if (this.armoredSilent.getValue()) {
            this.swapToElytraArmored();
        }

        if (this.armoredGrimV3.getValue()) {
            this.setShiftKey(true);
        }

        mc.getConnection().send(new ServerboundPlayerCommandPacket(mc.player, ServerboundPlayerCommandPacket.Action.START_FALL_FLYING));
        if (this.setFlag.getValue()) {
            mc.player.startFallFlying();
        }

        if (this.armoredGrimV3.getValue()) {
            this.releaseShiftKey();
        }

        if (this.armoredFireworks.getValue() && this.isMoving()) {
            int delayMs = this.armoredDelay.getValue().intValue() * 100;
            if (this.armoredFireworkTimer.passedMillise(delayMs) && (!this.armoredUsingPause.getValue() || !mc.player.isUsingItem())) {
                this.useArmoredFirework();
                this.armoredFireworkTimer.reset();
            }
        }

        if (this.armoredSilent.getValue()) {
            this.swapToChestplateArmored();
            mc.gameMode.ensureHasSentCarriedItem();
        }
    }

    private void applyArmoredFreeze() {
        if (this.armoredFreezePos == null) return;

        if (this.armoredFreezeMode.getValue() == ElytraFly.ArmoredFreezeMode.Static) {
            mc.player.setDeltaMovement(0.0, 0.0, 0.0);
            mc.player.setPos(this.armoredFreezePos.x, this.armoredFreezePos.y, this.armoredFreezePos.z);
        } else if (this.armoredFreezeMode.getValue() == ElytraFly.ArmoredFreezeMode.Timer) {
            mc.player.setDeltaMovement(0.0, mc.player.getDeltaMovement().y * 0.9, 0.0);
        } else if (this.armoredFreezeMode.getValue() == ElytraFly.ArmoredFreezeMode.Tick) {
            mc.player.setDeltaMovement(mc.player.getDeltaMovement().x * 0.5, mc.player.getDeltaMovement().y * 0.9, mc.player.getDeltaMovement().z * 0.5);
        }
    }

    private void useArmoredFirework() {
        if (mc.player.getMainHandItem().getItem() == Items.FIREWORK_ROCKET) {
            this.sendArmoredFireworkPacket();
            return;
        }

        FindItemResult inventorySlot = InvUtils.find(Items.FIREWORK_ROCKET);
        if (inventorySlot.found()) {
            InvUtils.invSwap(inventorySlot.slot());
            this.sendArmoredFireworkPacket();
            InvUtils.invSwapBack();
            return;
        }

        FindItemResult firework = InvUtils.findInHotbar(Items.FIREWORK_ROCKET);
        if (firework.found()) {
            InvUtils.swap(firework.slot(), true);
            this.sendArmoredFireworkPacket();
            InvUtils.swapBack();
        }
    }

    private void sendArmoredFireworkPacket() {
        mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
    }

    private void swapToElytraArmored() {
        int slot = this.findUsableElytraSlot();
        if (slot == -1) {
            return;
        }

        int containerId = mc.player.containerMenu.containerId;
        mc.gameMode.handleContainerInput(containerId, slot, 0, ContainerInput.PICKUP, mc.player);
        mc.gameMode.handleContainerInput(containerId, 6, 0, ContainerInput.PICKUP, mc.player);
        mc.gameMode.handleContainerInput(containerId, slot, 0, ContainerInput.PICKUP, mc.player);
    }

    private void swapToChestplateArmored() {
        int slot = this.findChestplateSlot();
        if (slot == -1) {
            return;
        }

        int containerId = mc.player.containerMenu.containerId;
        mc.gameMode.handleContainerInput(containerId, slot, 0, ContainerInput.PICKUP, mc.player);
        mc.gameMode.handleContainerInput(containerId, 6, 0, ContainerInput.PICKUP, mc.player);
        mc.gameMode.handleContainerInput(containerId, slot, 0, ContainerInput.PICKUP, mc.player);
    }

    private int findUsableElytraSlot() {
        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.is(Items.ELYTRA) && !stack.nextDamageWillBreak()) {
                return i < 9 ? i + 36 : i;
            }
        }

        return -1;
    }

    private int findChestplateSlot() {
        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (!stack.isEmpty() && this.isChestplate(stack)) {
                return i < 9 ? i + 36 : i;
            }
        }

        return -1;
    }

    private boolean isChestplate(ItemStack stack) {
        return stack.getItem().toString().toLowerCase().contains("chestplate");
    }

    private boolean isMoving() {
        return mc.options.keyUp.isDown() || mc.options.keyDown.isDown() || mc.options.keyLeft.isDown() || mc.options.keyRight.isDown();
    }

    private void setX(double x) {
        Vec3 movement = mc.player.getDeltaMovement();
        mc.player.setDeltaMovement(x, movement.y, movement.z);
    }

    private void setY(double y) {
        Vec3 movement = mc.player.getDeltaMovement();
        mc.player.setDeltaMovement(movement.x, y, movement.z);
    }

    private void setZ(double z) {
        Vec3 movement = mc.player.getDeltaMovement();
        mc.player.setDeltaMovement(movement.x, movement.y, z);
    }

}
