package com.github.lumin.modules.impl.player;

import com.github.lumin.events.PacketEvent;
import com.github.lumin.mixins.IClientInput;
import com.github.lumin.mixins.IMinecraft;
import com.github.lumin.mixins.IServerboundContainerClickPacket;
import com.github.lumin.modules.Category;
import com.github.lumin.modules.Module;
import com.github.lumin.settings.impl.BoolSetting;
import com.github.lumin.settings.impl.ModeSetting;
import com.github.lumin.utils.player.MoveUtils;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.phys.Vec2;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.IntFunction;

public class NoSlow extends Module {
    public static final NoSlow INSTANCE = new NoSlow();
    final Queue<ServerboundContainerClickPacket> storedClicks = new LinkedList<>();
    final AtomicBoolean pause = new AtomicBoolean();

    private final ModeSetting mode = modeSetting("模式", "Vanilla", new String[]{"Vanilla", "NCP", "Grim", "GrimPacket", "Drop", "None"});
    private final BoolSetting soulSand = boolSetting("灵魂沙", true);
    private final BoolSetting sneak = boolSetting("潜行", false);
    private final BoolSetting climb = boolSetting("攀爬", false);
    private final BoolSetting gui = boolSetting("界面移动", true);
    private final BoolSetting allowSneak = boolSetting("允许潜行", false, gui::getValue);
    private final ModeSetting clickBypass = modeSetting("界面移动绕过", "None", new String[]{"None", "NCP", "NCP2", "Grim", "Delay"});

    boolean using = false;
    int delay = 0;

    public NoSlow() {
        super("无减速", "不厌食", Category.PLAYER);
    }

    private static float getMovementMultiplier(boolean positive, boolean negative) {
        if (positive == negative) {
            return 0.0f;
        }
        return positive ? 1.0f : -1.0f;
    }

    @Override
    public String getDescription() {
        return mode.getValue();
    }

    @SubscribeEvent
    public void onUpdate(ClientTickEvent.Pre event) {
        if (nullCheck()) return;

        this.using = mc.player.isUsingItem();
        --this.delay;
        if (this.using) {
            this.delay = 2;
        }

        if (this.using && !mc.player.isPassenger() && !mc.player.isFallFlying()) {
            String currentMode = mode.getValue();
            switch (currentMode) {
                case "NCP" -> {
                    mc.getConnection().send(new ServerboundSetCarriedItemPacket(mc.player.getInventory().getSelectedSlot()));
                }
                case "Grim" -> {
                    float yaw = mc.player.getYRot();
                    float pitch = mc.player.getXRot();

                    if (mc.player.getUsedItemHand() == InteractionHand.OFF_HAND) {
                        sendSequencedPacket(id -> new ServerboundUseItemPacket(InteractionHand.MAIN_HAND, id, yaw, pitch));
                    } else {
                        sendSequencedPacket(id -> new ServerboundUseItemPacket(InteractionHand.OFF_HAND, id, yaw, pitch));
                    }
                }
                case "GrimPacket" -> {
                    mc.gameMode.handleInventoryMouseClick(mc.player.inventoryMenu.containerId, 1, 0, ClickType.PICKUP, mc.player);

                    float yaw = mc.player.getYRot();
                    float pitch = mc.player.getXRot();

                    if (mc.player.getUsedItemHand() == InteractionHand.OFF_HAND) {
                        sendSequencedPacket(id -> new ServerboundUseItemPacket(InteractionHand.MAIN_HAND, id, yaw, pitch));
                    } else {
                        sendSequencedPacket(id -> new ServerboundUseItemPacket(InteractionHand.OFF_HAND, id, yaw, pitch));
                    }
                }
            }
        }

        if (this.gui.getValue() && !(mc.screen instanceof ChatScreen)) {
            for (KeyMapping k : new KeyMapping[]{mc.options.keyUp, mc.options.keyLeft, mc.options.keyDown, mc.options.keyRight}) {
                k.setDown(isPhysicalKeyDown(k));
            }

            mc.options.keyJump.setDown(isPhysicalKeyDown(mc.options.keyJump));
            mc.options.keyUp.setDown(isPhysicalKeyDown(mc.options.keyUp));

            boolean sprintOn = false;
            if (Sprint.INSTANCE.isEnabled()) sprintOn = true;

            mc.options.keySprint.setDown((sprintOn && !mc.player.isInWater()) || isPhysicalKeyDown(mc.options.keySprint));

            if (this.allowSneak.getValue()) {
                mc.options.keyShift.setDown(isPhysicalKeyDown(mc.options.keyShift));
            }
        }

        if (this.clickBypass.is("Delay") && !this.storedClicks.isEmpty() && (mc.screen == null || !MoveUtils.isMoving())) {
            flushDelayedClicks();
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onInput(MovementInputUpdateEvent event) {
        if (nullCheck()) return;

        IClientInput clientInput = (IClientInput) event.getInput();
        Input keyPresses = clientInput.getKeyPresses();

        if (this.sneak.getValue()) {
            keyPresses = new Input(keyPresses.forward(), keyPresses.backward(), keyPresses.left(), keyPresses.right(), keyPresses.jump(), false, keyPresses.sprint());
        }

        boolean inGuiMovement = this.gui.getValue() && mc.screen != null && !(mc.screen instanceof ChatScreen);
        boolean applyNoSlowInput = this.noSlow() && mc.player.isUsingItem();

        if (inGuiMovement || applyNoSlowInput) {
            boolean up = isPhysicalKeyDown(mc.options.keyUp);
            boolean down = isPhysicalKeyDown(mc.options.keyDown);
            boolean left = isPhysicalKeyDown(mc.options.keyLeft);
            boolean right = isPhysicalKeyDown(mc.options.keyRight);
            boolean jump = isPhysicalKeyDown(mc.options.keyJump);
            boolean shift = inGuiMovement ? this.allowSneak.getValue() && isPhysicalKeyDown(mc.options.keyShift) : isPhysicalKeyDown(mc.options.keyShift);
            boolean sprint = (Sprint.INSTANCE.isEnabled() && !mc.player.isInWater()) || isPhysicalKeyDown(mc.options.keySprint);

            keyPresses = new Input(up, down, left, right, jump, shift, sprint);
            clientInput.setMoveVector(new Vec2(getMovementMultiplier(left, right), getMovementMultiplier(up, down)));
        }

        clientInput.setKeyPresses(keyPresses);
    }

    @SubscribeEvent
    public void onInteract(PlayerInteractEvent.RightClickItem event) {
        if (!event.getLevel().isClientSide()) return;

        if (this.delay > 0) {
            ((IMinecraft) mc).setRightClickDelay(0);
            event.setCanceled(true);
            event.setCancellationResult(net.minecraft.world.InteractionResult.FAIL);
        } else if (this.mode.is("GrimPacket") && event.getItemStack().has(DataComponents.FOOD)) {
            mc.gameMode.handleInventoryMouseClick(mc.player.inventoryMenu.containerId, 1, 0, ClickType.PICKUP, mc.player);
        }
    }

    @SubscribeEvent
    public void onPacketSend(PacketEvent.Send e) {
        if (nullCheck()) return;

        Packet<?> packet = e.getPacket();

        if (this.mode.is("Drop") && packet instanceof ServerboundUseItemPacket useItemPacket && useItemPacket.getHand() == InteractionHand.MAIN_HAND && mc.player.getMainHandItem().has(DataComponents.FOOD)) {
            mc.getConnection().send(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.DROP_ITEM, BlockPos.ZERO, Direction.DOWN));
            return;
        }

        if (this.pause.get()) {
            return;
        }

        if (packet instanceof ServerboundContainerClickPacket click) {
            String bypassMode = this.clickBypass.getValue();
            switch (bypassMode) {
                case "Grim" -> {
                    mc.getConnection().send(new ServerboundContainerClosePacket(0));
                }
                case "NCP" -> {
                    if (!mc.player.onGround())
                        break;
                    if (mc.player.isSprinting()) {
                        mc.getConnection().send(new ServerboundPlayerCommandPacket(mc.player, ServerboundPlayerCommandPacket.Action.STOP_SPRINTING));
                    }
                    mc.getConnection().send(new ServerboundMovePlayerPacket.Pos(mc.player.getX(), mc.player.getY() + 0.0656, mc.player.getZ(), false, mc.player.horizontalCollision));
                }
                case "NCP2" -> {
                    if (!mc.player.onGround())
                        break;
                    if (mc.player.isSprinting()) {
                        mc.getConnection().send(new ServerboundPlayerCommandPacket(mc.player, ServerboundPlayerCommandPacket.Action.STOP_SPRINTING));
                    }
                    mc.getConnection().send(new ServerboundMovePlayerPacket.Pos(mc.player.getX(), mc.player.getY() + 2.71875E-7, mc.player.getZ(), false, mc.player.horizontalCollision));
                }
                case "Delay" -> {
                    this.storedClicks.add(click);
                    e.setCanceled(true);
                }
            }
        }

        if (packet instanceof ServerboundContainerClosePacket && this.clickBypass.is("Delay")) {
            flushDelayedClicks();
        }
    }

    private void flushDelayedClicks() {
        this.pause.set(true);
        while (!this.storedClicks.isEmpty()) {
            mc.getConnection().send(this.storedClicks.poll());
        }
        this.pause.set(false);
    }

    private boolean isPhysicalKeyDown(KeyMapping keyMapping) {
        return InputConstants.isKeyDown(mc.getWindow(), keyMapping.getKey().getValue());
    }

    public boolean noSlow() {
        return this.isEnabled() && !this.mode.is("None") && (!this.mode.is("Drop") && !this.mode.is("GrimPacket") || this.using);
    }

    public boolean soulSand() {
        return this.isEnabled() && this.soulSand.getValue();
    }

    public boolean climb() {
        return this.isEnabled() && this.climb.getValue();
    }

    private void sendSequencedPacket(IntFunction<Packet<?>> packetFactory) {
        int sequence = 0;
        Packet<?> packet = packetFactory.apply(sequence);
        mc.getConnection().send(packet);
    }

    private ClickType getClickType(ServerboundContainerClickPacket packet) {
        try {
            return ((IServerboundContainerClickPacket) (Object) packet).getClickType();
        } catch (Exception ignored) {
        }
        try {
            try {
                return packet.clickType();
            } catch (NoSuchMethodError | Exception e) {
            }

            Field field = ServerboundContainerClickPacket.class.getDeclaredField("clickType");
            field.setAccessible(true);
            return (ClickType) field.get(packet);
        } catch (Exception e) {
            return ClickType.PICKUP;
        }
    }
}
