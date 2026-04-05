package com.github.epsilon.modules.impl.player;

import com.github.epsilon.managers.RotationManager;
import com.github.epsilon.modules.Category;
import com.github.epsilon.modules.Module;
import com.github.epsilon.settings.impl.BoolSetting;
import com.github.epsilon.settings.impl.DoubleSetting;
import com.github.epsilon.settings.impl.EnumSetting;
import com.github.epsilon.settings.impl.KeybindSetting;
import com.github.epsilon.settings.impl.IntSetting;
import com.github.epsilon.utils.player.FindItemResult;
import com.github.epsilon.utils.player.InvUtils;
import com.github.epsilon.utils.timer.TimerUtils;
import com.google.common.eventbus.Subscribe;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * @author ZhouJiaMei
 * Skidding from alien v4
 */
public class ElytraFly extends Module {

    public static final ElytraFly INSTANCE = new ElytraFly();

    private ElytraFly() {
        super("ElytraFly", Category.PLAYER);

        NeoForge.EVENT_BUS.register(new FireWorkTweak());
    }

    private enum Mode {
        Control,
        Boost,
        Bounce,
        Freeze,
        None,
        Rotation,
        Pitch
    }

    private final EnumSetting<Mode> mode = enumSetting("Mode", Mode.Control);
    private final BoolSetting infiniteDura = boolSetting("InfiniteDura", false);
    private final BoolSetting packet = boolSetting("Packet", false);
    private final IntSetting packetDelay = intSetting("PacketDelay", 0, 0, 20, 1, packet::getValue);
    private final BoolSetting setFlag = boolSetting("SetFlag", false, () -> !mode.is(Mode.Bounce));
    private final BoolSetting firework = boolSetting("Firework", false);
    private final KeybindSetting fireWork = keybindSetting("FireWorkBind", -1, firework::getValue);
    private final BoolSetting packetInteract = boolSetting("PacketInteract", true, firework::getValue);
    private final BoolSetting inventory = boolSetting("InventorySwap", true, firework::getValue);
    private final BoolSetting onlyOne = boolSetting("OnlyOne", true, firework::getValue);
    private final BoolSetting usingPause = boolSetting("UsingPause", true, firework::getValue);
    private final BoolSetting autoJump = boolSetting("AutoJump", true, () -> mode.is(Mode.Bounce));
    private final DoubleSetting upPitch = doubleSetting("UpPitch", 0.0, 0.0, 90.0, 1.0, () -> mode.is(Mode.Control));
    private final DoubleSetting upFactor = doubleSetting("UpFactor", 1.0, 0.0, 10.0, 0.1, () -> mode.is(Mode.Control));
    private final DoubleSetting downFactor = doubleSetting("FallSpeed", 1.0, 0.0, 10.0, 0.1, () -> mode.is(Mode.Control));
    private final DoubleSetting speed = doubleSetting("Speed", 1.0, 0.1, 10.0, 0.1, () -> mode.is(Mode.Control));
    private final BoolSetting speedLimit = boolSetting("SpeedLimit", true, () -> mode.is(Mode.Control));
    private final DoubleSetting maxSpeed = doubleSetting("MaxSpeed", 2.5, 0.1, 10.0, 0.1, () -> speedLimit.getValue() && mode.is(Mode.Control));
    private final BoolSetting noDrag = boolSetting("NoDrag", false, () -> mode.is(Mode.Control));

    private final BoolSetting autoStop = boolSetting("AutoStop", true);
    private final BoolSetting sprint = boolSetting("Sprint", true, () -> mode.is(Mode.Bounce));
    private final DoubleSetting pitch = doubleSetting("Pitch", 88.0, -90.0, 90.0, 0.1, () -> mode.is(Mode.Bounce));
    private final BoolSetting instantFly = boolSetting("AutoStart", true, () -> !mode.is(Mode.Bounce));
    private final BoolSetting checkSpeed = boolSetting("CheckSpeed", false, () -> !mode.is(Mode.Bounce));
    private final DoubleSetting minSpeed = doubleSetting("MinSpeed", 70.0, 0.1, 200.0, 0.1, () -> !mode.is(Mode.Bounce));
    private final IntSetting delay = intSetting("Delay", 1000, 0, 20000, 50, () -> !mode.is(Mode.Bounce));
    private final DoubleSetting timeout = doubleSetting("Timeout", 0.0, 0.1, 1.0, 0.1, () -> !mode.is(Mode.Bounce));
    private final DoubleSetting sneakDownSpeed = doubleSetting("DownSpeed", 1.0, 0.1, 10.0, 0.1, () -> mode.is(Mode.Control));
    private final DoubleSetting boost = doubleSetting("Boost", 1.0, 0.1, 4.0, 0.1, () -> mode.is(Mode.Boost));
    private final BoolSetting freeze = boolSetting("Freeze", false, () -> mode.is(Mode.Rotation));
    private final BoolSetting motionStop = boolSetting("MotionStop", false, () -> mode.is(Mode.Rotation));
    private final DoubleSetting infiniteMaxSpeed = doubleSetting("InfiniteMaxSpeed", 150.0, 50.0, 170.0, 1.0, () -> mode.is(Mode.Pitch));
    private final DoubleSetting infiniteMinSpeed = doubleSetting("InfiniteMinSpeed", 25.0, 10.0, 70.0, 1.0, () -> mode.is(Mode.Pitch));
    private final DoubleSetting infiniteMaxHeight = doubleSetting("InfiniteMaxHeight", 200.0, -50.0, 360.0, 1.0, () -> mode.is(Mode.Pitch));
    private final BoolSetting releaseSneak = boolSetting("ReleaseSneak", false);

    private boolean flying = false;
    private int packetDelayInt = 0;

    private boolean prev;
    private float prePitch;
    private boolean hasElytra = false;

    private final TimerUtils fireworkTimer = new TimerUtils();
    private final TimerUtils instantFlyTimer = new TimerUtils();

    private boolean down;
    private float lastInfinitePitch;
    private float infinitePitch;

    public void off() {
        if (inventory.getValue() && !inInventory()) return;
        if (onlyOne.getValue()) {
            for (Entity entity : mc.level.entitiesForRendering()) {
                if (entity instanceof FireworkRocketEntity fireworkRocketEntity) {
                    if (fireworkRocketEntity.attachedToEntity == mc.player) {
                        return;
                    }
                }
            }
        }
        ElytraFly.INSTANCE.fireworkTimer.reset();
        FindItemResult firework;
        if (mc.player.getMainHandItem().getItem() == Items.FIREWORK_ROCKET) {
            if (packetInteract.getValue()) {
                mc.getConnection().send(new ServerboundUseItemPacket(InteractionHand.MAIN_HAND, mc.level.getBlockStatePredictionHandler().startPredicting().currentSequence(), RotationManager.INSTANCE.getYaw(), RotationManager.INSTANCE.getPitch()));
            } else {
                mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
            }
        } else if (inventory.getValue() && (firework = InvUtils.find(Items.FIREWORK_ROCKET)).found()) {
            InvUtils.invSwap(firework.slot());
            if (packetInteract.getValue()) {
                mc.getConnection().send(new ServerboundUseItemPacket(InteractionHand.MAIN_HAND, mc.level.getBlockStatePredictionHandler().startPredicting().currentSequence(), RotationManager.INSTANCE.getYaw(), RotationManager.INSTANCE.getPitch()));
            } else {
                mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
            }
            InvUtils.invSwapBack();
            mc.getConnection().send(new ServerboundContainerClosePacket(mc.player.containerMenu.containerId));
        } else if ((firework = InvUtils.findInHotbar(Items.FIREWORK_ROCKET)).found()) {
            InvUtils.swap(firework.slot(), true);
            if (packetInteract.getValue()) {
                mc.getConnection().send(new ServerboundUseItemPacket(InteractionHand.MAIN_HAND, mc.level.getBlockStatePredictionHandler().startPredicting().currentSequence(), RotationManager.INSTANCE.getYaw(), RotationManager.INSTANCE.getPitch()));
            } else {
                mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
            }
            InvUtils.swapBack();
        }
    }

    private boolean isFallFlying() {
        return mc.player.isFallFlying() || packet.getValue() && hasElytra && !mc.player.onGround() || flying;
    }

    private boolean inInventory() {
        if (mc.screen instanceof InventoryScreen) {
            return true;
        }

        return mc.screen instanceof AbstractContainerScreen<?> container && container.getMenu().containerId == mc.player.inventoryMenu.containerId;
    }

    private class FireWorkTweak {

        boolean press;

        @SubscribeEvent
        public void onTick(ClientTickEvent.Pre event) {
            if (nullCheck()) return;
            if (inventory.getValue() && !inInventory()) return;
            if (mc.screen == null) {
                if (InputConstants.isKeyDown(mc.getWindow(), fireWork.getValue())) {
                    if (!press) {
                        if (fireworkTimer.passedMillise(delay.getValue()) && (!mc.player.isUsingItem() || !usingPause.getValue()) && isFallFlying()) {
                            off();
                            fireworkTimer.reset();
                        }
                    }
                    press = true;
                } else {
                    press = false;
                }
            } else {
                press = false;
            }
        }
    }

}
