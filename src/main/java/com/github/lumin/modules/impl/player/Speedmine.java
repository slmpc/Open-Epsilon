package com.github.lumin.modules.impl.player;

import com.github.lumin.events.PacketEvent;
import com.github.lumin.modules.Category;
import com.github.lumin.modules.Module;
import com.github.lumin.settings.impl.BoolSetting;
import com.github.lumin.settings.impl.DoubleSetting;
import com.github.lumin.settings.impl.EnumSetting;
import com.github.lumin.settings.impl.IntSetting;
import com.github.lumin.utils.player.InvUtils;
import com.github.lumin.utils.network.PacketUtils;
import com.github.lumin.utils.timer.TimerUtils;
import com.github.lumin.utils.tasks.TaskScheduler;
import com.github.lumin.utils.tps.TpsManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import java.util.ArrayDeque;

public class Speedmine extends Module {

    public static final Speedmine INSTANCE = new Speedmine();

    public enum RebreakMode {
        Off,
        Normal,
        Strict
    }

    public enum AutoSwapMode {
        Off,
        Normal,
        Silent
    }

    public final EnumSetting<RebreakMode> rebreak = enumSetting("Rebreak", RebreakMode.Off);
    public final EnumSetting<AutoSwapMode> autoSwap = enumSetting("AutoSwap", AutoSwapMode.Normal);
    public final BoolSetting swapBack = boolSetting("SwapBack", true);
    public final BoolSetting swapReset = boolSetting("SwapReset", true);
    public final BoolSetting alternative = boolSetting("Alternative", false);
    public final BoolSetting holdingBest = boolSetting("HoldingBest", false);
    public final DoubleSetting damage = doubleSetting("Damage", 1.0, 0.0, 10.0, 0.1);
    public final DoubleSetting range = doubleSetting("Range", 4.5, 1.0, 8.0, 0.1);
    public final DoubleSetting limit = doubleSetting("Limit", 1.0, 0.0, 5.0, 0.1);
    public final BoolSetting extraBreak = boolSetting("ExtraBreak", false);
    public final BoolSetting swing = boolSetting("Swing", true);
    public final BoolSetting assistant = boolSetting("Assistant", false);
    public final BoolSetting tpsSync = boolSetting("TPSSync", true);
    public final BoolSetting debug = boolSetting("Debug", false);
    public final IntSetting rebreakDelay = intSetting("RebreakDelay", 50, 0, 500, 10);
    public final IntSetting instantDelay = intSetting("InstantDelay", 0, 0, 200, 10);
    public final IntSetting swapResetDelay = intSetting("SwapResetDelay", 100, 0, 1000, 10);

    private final TimerUtils breakTimer = new TimerUtils();
    private final TimerUtils rebreakTimer = new TimerUtils();
    private final TimerUtils instantTimer = new TimerUtils();
    private final TimerUtils swapResetTimer = new TimerUtils();

    private BlockPos breakPos;
    private BlockPos lastBreakPos;
    private boolean breaking;
    private Direction lastDirection = Direction.UP;
    private int lastSwapSlot = -1;
    private float progress;
    private float lastProgress;
    private final ArrayDeque<Boolean> groundQueue = new ArrayDeque<>();
    private BlockPos assistPos;
    private boolean assistActive;

    public boolean swapResetFlag;

    private Speedmine() {
        super("Speedmine", Category.PLAYER);
    }

    @Override
    protected void onDisable() {
        breakPos = null;
        lastBreakPos = null;
        breaking = false;
        lastDirection = Direction.UP;
        lastSwapSlot = -1;
        progress = 0.0f;
        lastProgress = 0.0f;
        groundQueue.clear();
        assistPos = null;
        assistActive = false;
        swapResetFlag = false;
    }

    @SubscribeEvent
    public void onPacketSend(PacketEvent.Send event) {
        if (!(event.getPacket() instanceof ServerboundPlayerActionPacket packet)) return;
        ServerboundPlayerActionPacket.Action action = packet.getAction();
        if (action == ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK) {
            breakPos = packet.getPos();
            lastDirection = packet.getDirection();
            breaking = true;
            breakTimer.reset();
            instantTimer.reset();
            progress = 0.0f;
            lastProgress = 0.0f;
            if (assistant.getValue()) {
                assistPos = breakPos;
                assistActive = true;
            }
        } else if (action == ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK) {
            lastBreakPos = packet.getPos();
            breakPos = packet.getPos();
            lastDirection = packet.getDirection();
            breaking = false;
            breakTimer.reset();
            progress = 0.0f;
            lastProgress = 0.0f;
        } else if (action == ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK) {
            breaking = false;
            breakTimer.reset();
            progress = 0.0f;
            lastProgress = 0.0f;
        }
    }

    @SubscribeEvent
    public void onTick(ClientTickEvent.Pre event) {
        if (nullCheck() || !isEnabled()) return;
        if (breakPos == null && !tryAdoptBreakPos()) return;

        if (swapResetFlag && swapResetTimer.passedMillise(swapResetDelay.getValue())) {
            swapResetFlag = false;
        }

        if (!isRebreakActive()) return;
        long rebreakWindow = rebreakDelay.getValue();
        if (tpsSync.getValue()) {
            rebreakWindow = (long) (rebreakWindow * TpsManager.getTpsFactor());
        }
        if (!rebreakTimer.passedMillise(rebreakWindow)) return;
        if (!instantTimer.passedMillise(instantDelay.getValue())) return;

        if (rebreak.getValue() == RebreakMode.Strict && !breaking && !assistant.getValue()) return;

        BlockState state = mc.level.getBlockState(breakPos);
        if (state.isAir() || state.is(Blocks.AIR)) {
            if (rebreak.getValue() == RebreakMode.Strict && assistPos != null) {
                breakPos = assistPos;
            } else {
                breakPos = null;
            }
            return;
        }

        updateProgress(state);

        double damageValue = damage.getValue();
        if (progress < damageValue) return;
        if (limit.getValue() > 0.0) {
            float ratio = damageValue <= 0.0 ? progress : (float) (progress / damageValue);
            if (ratio < limit.getValue()) return;
        }

        double dist = mc.player.getEyePosition().distanceTo(net.minecraft.world.phys.Vec3.atCenterOf(breakPos));
        if (dist > range.getValue()) {
            if (rebreak.getValue() == RebreakMode.Strict && assistPos != null) {
                breakPos = assistPos;
            } else {
                breakPos = null;
                assistActive = false;
            }
            return;
        }

        int toolSlot = findPickaxeSlot();
        int currentSlot = mc.player.getInventory().getSelectedSlot();
        boolean swapped = false;

        if (autoSwap.getValue() != AutoSwapMode.Off && toolSlot != -1 && toolSlot != currentSlot) {
            if (holdingBest.getValue()) return;
            if (autoSwap.getValue() == AutoSwapMode.Silent) {
                if (!InvUtils.invSwap(toolSlot)) return;
                swapped = true;
            } else {
                lastSwapSlot = currentSlot;
                InvUtils.swap(toolSlot, false);
                swapped = true;
            }
        } else if (autoSwap.getValue() == AutoSwapMode.Off && holdingBest.getValue()) {
            if (!isHoldingPickaxe()) return;
        }

        rebreakTimer.reset();

        if (extraBreak.getValue()) {
            sendBreakPacket(ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK);
        }

        if (rebreak.getValue() == RebreakMode.Strict) {
            sendBreakPacket(ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK);
        }
        sendBreakPacket(ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK);
        if (rebreak.getValue() == RebreakMode.Strict) {
            sendBreakPacket(ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK);
        }

        if (alternative.getValue() && swapReset.getValue()) {
            swapResetFlag = true;
            swapResetTimer.reset();
            TaskScheduler.schedule(() -> swapResetFlag = false, Math.max(1, swapResetDelay.getValue() / 50));
        }

        if (swapped && swapBack.getValue()) {
            if (autoSwap.getValue() == AutoSwapMode.Silent) {
                InvUtils.invSwapBack();
            } else if (lastSwapSlot != -1) {
                InvUtils.swap(lastSwapSlot, false);
            }
            lastSwapSlot = -1;
        }

        if (assistant.getValue()) {
            assistActive = false;
        }
    }

    private void sendBreakPacket(ServerboundPlayerActionPacket.Action action) {
        if (breakPos == null) return;
        PacketUtils.sendPacketNoEvent(new ServerboundPlayerActionPacket(action, breakPos, lastDirection));
        if (swing.getValue()) {
            mc.player.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        }
        if (debug.getValue()) {
            mc.player.displayClientMessage(net.minecraft.network.chat.Component.literal("[Speedmine] " + action + " " + breakPos), false);
        }
    }

    public BlockPos getBreakPos() {
        return breakPos;
    }

    public BlockPos getLastBreakPos() {
        return lastBreakPos;
    }

    public boolean isBreaking() {
        return breaking;
    }

    public boolean isRebreakActive() {
        return rebreak.getValue() != RebreakMode.Off;
    }

    public boolean recentMine(long ms) {
        return !breakTimer.passedMillise(ms);
    }

    public float getProgress() {
        return progress;
    }

    public float getLastProgress() {
        return lastProgress;
    }

    public float getProgressRatio() {
        if (damage.getValue() <= 0.0) return progress;
        double damageValue = damage.getValue();
        return damageValue <= 0.0 ? progress : (float) (progress / damageValue);
    }

    public BlockPos getAssistPos() {
        return assistPos;
    }

    public boolean isAssistActive() {
        return assistActive;
    }

    private void updateProgress(BlockState state) {
        lastProgress = progress;
        float delta = state.getDestroyProgress(mc.player, mc.level, breakPos);
        float tps = tpsSync.getValue() ? TpsManager.getTpsFactor() : 1.0f;
        if (rebreak.getValue() == RebreakMode.Strict) {
            groundQueue.add(mc.player.onGround());
            if (groundQueue.size() > 200) {
                groundQueue.removeLast();
            }
            int consumed = 0;
            while (!groundQueue.isEmpty()) {
                boolean onGround = groundQueue.pollFirst();
                progress += (delta * (onGround ? 1.0f : 0.7f)) * tps;
                consumed++;
                if (consumed > 5) break;
            }
        } else {
            progress += delta * tps;
        }
        if (progress > 1.0f) progress = 1.0f;
    }

    private boolean isHoldingPickaxe() {
        ItemStack stack = mc.player.getMainHandItem();
        return !stack.isEmpty() && stack.is(ItemTags.PICKAXES);
    }

    private int findPickaxeSlot() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.is(ItemTags.PICKAXES)) {
                return i;
            }
        }
        return -1;
    }

    private boolean tryAdoptBreakPos() {
        if (!isRebreakActive()) return false;
        if (lastBreakPos != null && isInRange(lastBreakPos)) {
            breakPos = lastBreakPos;
            return true;
        }
        if (assistant.getValue() && assistPos != null && isInRange(assistPos)) {
            breakPos = assistPos;
            assistActive = true;
            return true;
        }
        return false;
    }

    private boolean isInRange(BlockPos pos) {
        double dist = mc.player.getEyePosition().distanceTo(net.minecraft.world.phys.Vec3.atCenterOf(pos));
        return dist <= range.getValue();
    }
}
