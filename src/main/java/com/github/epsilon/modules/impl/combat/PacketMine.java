package com.github.epsilon.modules.impl.combat;

import com.github.epsilon.events.AttackBlockEvent;
import com.github.epsilon.events.MotionEvent;
import com.github.epsilon.events.PacketEvent;
import com.github.epsilon.modules.Category;
import com.github.epsilon.modules.Module;
import com.github.epsilon.settings.impl.*;
import com.github.epsilon.utils.player.EnchantmentUtils;
import com.github.epsilon.utils.player.InvUtils;
import com.github.epsilon.utils.player.PlayerUtils;
import com.github.epsilon.utils.render.Render3DUtils;
import com.github.epsilon.utils.render.animation.Easing;
import com.github.epsilon.utils.rotation.RotationUtils;
import com.github.epsilon.utils.timer.TimerUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.AirBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class PacketMine extends Module {

    public static final PacketMine INSTANCE = new PacketMine();

    private PacketMine() {
        super("PacketMine", Category.COMBAT);
    }

    public enum Mode {
        Packet,
        Instant,
        Damage
    }

    public enum RenderMode {
        Block,
        Shrink,
        Grow
    }

    public enum SwitchMode {
        Silent,
        Normal,
        Alternative
    }

    public enum StartMode {
        StartAbort,
        StartStop
    }

    // General
    public final EnumSetting<Mode> mode = enumSetting("Mode", Mode.Packet);
    public final BoolSetting doubleMine = boolSetting("DoubleMine", false);
    private final EnumSetting<StartMode> startMode = enumSetting("StartMode", StartMode.StartAbort, () -> mode.is(Mode.Packet) && !doubleMine.getValue());
    private final EnumSetting<SwitchMode> switchMode = enumSetting("SwitchMode", SwitchMode.Alternative, () -> mode.getValue() != Mode.Damage);
    private final IntSetting swapDelay = intSetting("SwapDelay", 50, 0, 1000, 1, () -> mode.getValue() != Mode.Damage);
    private final DoubleSetting factor = doubleSetting("Factor", 1.0, 0.5, 2.0, 0.1, () -> mode.getValue() != Mode.Damage);
    private final DoubleSetting speed = doubleSetting("Speed", 0.5, 0.0, 1.0, 0.1, () -> mode.is(Mode.Damage));
    public final DoubleSetting range = doubleSetting("Range", 4.2, 3.0, 10.0, 0.1, () -> mode.getValue() != Mode.Damage);
    private final BoolSetting rotate = boolSetting("Rotate", false, () -> mode.getValue() != Mode.Damage);
    private final BoolSetting placeCrystal = boolSetting("PlaceCrystal", true);
    private final BoolSetting resetOnSwitch = boolSetting("ResetOnSwitch", true, () -> mode.getValue() != Mode.Damage);
    private final IntSetting breakAttempts = intSetting("BreakAttempts", 10, 1, 50, 1, () -> mode.is(Mode.Packet));
    private final BoolSetting pauseEat = boolSetting("PauseOnEat", false);
    private final BoolSetting clientRemove = boolSetting("ClientRemove", true);

    // Packet
    private final BoolSetting stop = boolSetting("Stop", true, () -> mode.is(Mode.Packet) && !doubleMine.getValue());
    private final BoolSetting abort = boolSetting("Abort", true, () -> mode.is(Mode.Packet) && !doubleMine.getValue());
    private final BoolSetting start = boolSetting("Start", true, () -> mode.is(Mode.Packet) && !doubleMine.getValue());
    private final BoolSetting stop2 = boolSetting("Stop2", true, () -> mode.is(Mode.Packet) && !doubleMine.getValue());

    // Render
    private final BoolSetting render = boolSetting("Render", false, () -> mode.getValue() != Mode.Damage);
    private final BoolSetting smooth = boolSetting("Smooth", true, () -> mode.getValue() != Mode.Damage && render.getValue());
    private final EnumSetting<RenderMode> renderMode = enumSetting("RenderMode", RenderMode.Shrink, () -> mode.getValue() != Mode.Damage && render.getValue());
    private final ColorSetting startLineColor = colorSetting("StartLineColor", new Color(255, 0, 0, 200), () -> mode.getValue() != Mode.Damage && render.getValue());
    private final ColorSetting endLineColor = colorSetting("EndLineColor", new Color(47, 255, 0, 200), () -> mode.getValue() != Mode.Damage && render.getValue());
    private final IntSetting lineWidth = intSetting("LineWidth", 2, 1, 10, 1, () -> mode.getValue() != Mode.Damage && render.getValue());
    private final ColorSetting startFillColor = colorSetting("StartFillColor", new Color(255, 0, 0, 120), () -> mode.getValue() != Mode.Damage && render.getValue());
    private final ColorSetting endFillColor = colorSetting("EndFillColor", new Color(47, 255, 0, 120), () -> mode.getValue() != Mode.Damage && render.getValue());

    public ArrayList<MineAction> actions = new ArrayList<>();
    private final List<DelayedAction> delayedActions = new ArrayList<>();

    @Override
    protected void onDisable() {
        actions.forEach(MineAction::reset);
        actions.clear();
        delayedActions.clear();
    }

    @Override
    protected void onEnable() {
        actions.forEach(MineAction::reset);
        actions.clear();
        delayedActions.clear();
    }

    @SubscribeEvent
    private void onClientTick(ClientTickEvent.Pre event) {
        if (nullCheck() || mc.player.getAbilities().instabuild) return;

        runDelayedActions();

        if (PlayerUtils.isEating() && pauseEat.getValue()) return;

        if (mode.getValue() == Mode.Damage) {
            float cSpeed = speed.getValue().floatValue();
            if (mc.gameMode.destroyProgress < cSpeed) {
                mc.gameMode.destroyProgress = cSpeed;
            }
        }

        actions.removeIf(MineAction::update);
    }

    @SubscribeEvent
    private void onAttackBlock(AttackBlockEvent event) {
        if (!canBreak(event.getBlockPos()) || mc.player.getAbilities().instabuild || mode.is(Mode.Damage))
            return;

        if (!alreadyActing(event.getBlockPos())) {
            if (!doubleMine.getValue() || actions.size() >= 2) {
                if (!actions.isEmpty())
                    actions.removeFirst().cancel();
            }

            actions.add(new MineAction(event.getBlockPos(), event.getDirection()));
        }

        event.setCanceled(true);
    }

    @SubscribeEvent
    private void onPacketSend(PacketEvent.Send event) {
        if (event.getPacket() instanceof ServerboundSetCarriedItemPacket && resetOnSwitch.getValue() && !switchMode.is(SwitchMode.Silent) && !mode.is(Mode.Instant)) {
            actions.forEach(MineAction::reset);
        }
    }

    @SubscribeEvent
    private void onMotion(MotionEvent event) {
        actions.forEach(MineAction::onSync);
    }

    @SubscribeEvent
    private void onRenderLevel(RenderLevelStageEvent.AfterLevel event) {
        if (!render.getValue() || actions.isEmpty() || mode.is(Mode.Damage)) return;

        float partialTick = mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);

        for (MineAction action : actions) {
            float progress = smooth.getValue() ? Mth.lerp(partialTick, action.getPrevProgress(), action.getProgress()) : action.getProgress();
            progress = Mth.clamp(progress, 0.0f, 1.0f);

            Color lineColor = lerpColor(startLineColor.getValue(), endLineColor.getValue(), progress);
            Color fillColor = lerpColor(startFillColor.getValue(), endFillColor.getValue(), progress);
            AABB box = getRenderBox(action.pos, progress);

            Render3DUtils.drawFilledBox(box, fillColor);
            Render3DUtils.drawOutlineBox(event.getPoseStack(), box, lineColor.getRGB(), lineWidth.getValue().floatValue());
        }
    }

    private AABB getRenderBox(BlockPos pos, float progress) {
        AABB box = new AABB(pos);
        return switch (renderMode.getValue()) {
            case Block -> box;
            case Shrink -> scaleBox(box, 1.0 - Easing.EASE_OUT_QUAD.getFunction().apply(progress));
            case Grow -> scaleBox(box, Easing.EASE_OUT_QUAD.getFunction().apply(progress));
        };
    }

    private AABB scaleBox(AABB box, double scale) {
        double clampedScale = Mth.clamp(scale, 0.0, 1.0);
        Vec3 center = box.getCenter();
        double halfX = box.getXsize() * 0.5 * clampedScale;
        double halfY = box.getYsize() * 0.5 * clampedScale;
        double halfZ = box.getZsize() * 0.5 * clampedScale;
        return new AABB(center.x - halfX, center.y - halfY, center.z - halfZ, center.x + halfX, center.y + halfY, center.z + halfZ);
    }

    private Color lerpColor(Color start, Color end, float progress) {
        float clamped = Mth.clamp(progress, 0.0f, 1.0f);
        int red = Mth.lerpInt(clamped, start.getRed(), end.getRed());
        int green = Mth.lerpInt(clamped, start.getGreen(), end.getGreen());
        int blue = Mth.lerpInt(clamped, start.getBlue(), end.getBlue());
        int alpha = Mth.lerpInt(clamped, start.getAlpha(), end.getAlpha());
        return new Color(red, green, blue, alpha);
    }

    public boolean alreadyActing(BlockPos blockPos) {
        return actions.stream().anyMatch(a -> a.pos.equals(blockPos));
    }

    private void schedule(int delayMs, Runnable runnable) {
        if (delayMs <= 0) {
            runnable.run();
            return;
        }
        delayedActions.add(new DelayedAction(System.currentTimeMillis() + delayMs, runnable));
    }

    private void runDelayedActions() {
        long now = System.currentTimeMillis();
        delayedActions.removeIf(action -> {
            if (action.runAt > now) {
                return false;
            }

            action.runnable.run();
            return true;
        });
    }

    public int getTool(final BlockPos pos) {
        int index = -1;
        float currentFastest = 1.f;

        if (mc.level == null
                || mc.player == null
                || mc.level.getBlockState(pos).getBlock() instanceof AirBlock)
            return -1;

        for (int i = 9; i < 45; ++i) {
            final ItemStack stack = mc.player.getInventory().getItem(i >= 36 ? i - 36 : i);

            if (!stack.isEmpty()) {
                if (!(stack.getMaxDamage() - stack.getDamageValue() > 10))
                    continue;

                final float digSpeed = EnchantmentUtils.getEnchantmentLevel(stack, Enchantments.EFFICIENCY);
                final float destroySpeed = stack.getDestroySpeed(mc.level.getBlockState(pos));

                if (digSpeed + destroySpeed > currentFastest) {
                    currentFastest = digSpeed + destroySpeed;
                    index = i;
                }
            }
        }

        return index >= 36 ? index - 36 : index;
    }

    private float getDestroySpeed(BlockPos position, BlockState state) {
        float destroySpeed = 1;
        int slot = getTool(position);

        if (slot != -1) {
            if (!mc.player.getInventory().getItem(slot).isEmpty()) {
                destroySpeed *= mc.player.getInventory().getItem(slot).getDestroySpeed(state);
            }
        }

        return destroySpeed;
    }

    public float getDigSpeed(BlockState state, BlockPos position) {
        if (mc.player == null) return 0;
        float digSpeed = getDestroySpeed(position, state);

        if (digSpeed > 1) {
            int slot = getTool(position);
            if (slot != -1) {
                ItemStack itemstack = mc.player.getInventory().getItem(slot);
                int efficiencyModifier = EnchantmentUtils.getEnchantmentLevel(itemstack, Enchantments.EFFICIENCY);
                if (efficiencyModifier > 0 && !itemstack.isEmpty()) {
                    digSpeed += (float) (StrictMath.pow(efficiencyModifier, 2) + 1);
                }
            }
        }

        if (mc.player.hasEffect(MobEffects.HASTE)) {
            digSpeed *= 1 + (Objects.requireNonNull(mc.player.getEffect(MobEffects.HASTE)).getAmplifier() + 1) * 0.2F;
        }

        if (mc.player.hasEffect(MobEffects.MINING_FATIGUE)) {
            digSpeed *= (float) Math.pow(0.3f, Objects.requireNonNull(mc.player.getEffect(MobEffects.MINING_FATIGUE)).getAmplifier() + 1);
        }

        if (mc.player.isEyeInFluid(FluidTags.WATER)) {
            digSpeed *= (float) mc.player.getAttributeValue(Attributes.SUBMERGED_MINING_SPEED);
        }

        if (!mc.player.onGround()/* && ModuleManager.freeCam.isDisabled()*/) {
            digSpeed /= 5;
        }

        return digSpeed < 0.0f ? 0.0f : digSpeed * factor.getValue().floatValue();
    }

    private boolean canBreak(BlockPos pos) {
        if (mc.player.distanceToSqr(pos.getCenter()) > range.getValue() * range.getValue()) return false;

        final BlockState blockState = mc.level.getBlockState(pos);
        return blockState.getDestroySpeed(mc.level, pos) != -1.0F;
    }

    public float getBlockStrength(BlockState state, BlockPos position) {
        if (state.isAir())
            return 0.02f;

        float hardness = state.getDestroySpeed(mc.level, position);

        if (hardness < 0)
            return 0;

        return getDigSpeed(state, position) / hardness / (canBreak(position) ? 30f : 100f);
    }

    public void placeCrystal() {
//        if (AutoCrystal.target == null) return;
//
//        AutoCrystal.PlaceData data = getCevData();
//
//        if (data == null)
//            data = getBestData();
//
//        if (data != null) {
//            ModuleManager.autoCrystal.placeCrystal(data.bhr(), true, false);
//            debug("placing..");
//            ModuleManager.autoTrap.pause();
//            ModuleManager.breaker.pause();
//        }
    }

    public class MineAction {
        private final BlockPos pos;
        private float progress, prevProgress;

        private int mineBreaks;

        private final TimerUtils attackTimer = new TimerUtils();

        public MineAction(BlockPos pos, Direction direction) {
            this.pos = pos;
            progress = 0;
            mineBreaks = 0;
            start(direction);
        }

        public void start(Direction direction) {
            Direction startDirection = RotationUtils.getDirection(pos, direction);
            if (doubleMine.getValue()) {
                mc.getConnection().send(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, pos, startDirection));
                mc.getConnection().send(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, pos, startDirection));
                mc.getConnection().send(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, pos, startDirection));
            } else {
                mc.getConnection().send(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, pos, startDirection));
                mc.getConnection().send(new ServerboundPlayerActionPacket(startMode.getValue() == StartMode.StartAbort ? ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK : ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, pos, startDirection));
            }
        }

        public boolean update() {
            Direction dir = RotationUtils.getDirection(pos, null);

            if (mineBreaks >= breakAttempts.getValue() && !mode.is(Mode.Instant))
                return true;

            if (mc.player.distanceToSqr(pos.getCenter()) > range.getValue() * range.getValue()) {
                cancel();
                return true;
            }

            if (mc.level.getBlockState(pos).isAir()) {
                progress = 0;
                prevProgress = -1;
                return false;
            }

            if (progress == 0 && prevProgress == -1 && mode.is(Mode.Packet) && attackTimer.every(800)) {
                start(dir);
                mc.player.swing(InteractionHand.MAIN_HAND);
            }

            int pickSlot = getTool(pos);
            int prevSlot = mc.player.getInventory().getSelectedSlot();

            if (pickSlot == -1)
                return false;

            boolean instant = mineBreaks > 0 && mode.is(Mode.Instant);

            if (progress >= 1 || instant) {
                if (placeCrystal.getValue())
                    placeCrystal();

                switchTo(pickSlot, -1);

                if (mode.getValue() == Mode.Instant || doubleMine.getValue()) {
                    mc.getConnection().send(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, pos, dir));
                } else {
                    if (stop.getValue()) {
                        mc.getConnection().send(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, pos, dir));
                    }
                    if (abort.getValue()) {
                        mc.getConnection().send(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK, pos, dir));
                    }
                    if (start.getValue()) {
                        mc.getConnection().send(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, pos, dir));
                    }
                    if (stop2.getValue()) {
                        mc.getConnection().send(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, pos, dir));
                    }
                }

                if (clientRemove.getValue()) {
                    mc.gameMode.destroyBlock(pos);
                }

                int delay = doubleMine.getValue() ? 100 : swapDelay.getValue();
                schedule(delay, () -> switchTo(prevSlot, pickSlot));

                mineBreaks++;

                progress = prevProgress = 0;

                if (doubleMine.getValue() && mode.is(Mode.Instant) && actions.size() >= 2)
                    return true;
            } else {
                prevProgress = progress;
                progress += getBlockStrength(mc.level.getBlockState(pos), pos);
            }

            return false;
        }

        private void switchTo(int slot, int from) {
            // 我真急哭了这个会卡背包
            if (switchMode.getValue() == SwitchMode.Alternative || slot >= 9) {
                if (from == -1) {
                    mc.gameMode.handleContainerInput(mc.player.containerMenu.containerId, slot < 9 ? slot + 36 : slot, mc.player.getInventory().getSelectedSlot(), ContainerInput.SWAP, mc.player);
                } else {
                    mc.gameMode.handleContainerInput(mc.player.containerMenu.containerId, from < 9 ? from + 36 : from, mc.player.getInventory().getSelectedSlot(), ContainerInput.SWAP, mc.player);
                }
                mc.getConnection().send(new ServerboundContainerClosePacket(mc.player.containerMenu.containerId));
            } else if (switchMode.is(SwitchMode.Silent)) {
                // 2b2t.site 亲测稳定可用
                mc.getConnection().send(new ServerboundSetCarriedItemPacket(slot));
            } else {
                InvUtils.swap(slot, false);
            }
        }

        public BlockPos getPos() {
            return pos;
        }

        public float getPrevProgress() {
            return prevProgress;
        }

        public float getProgress() {
            return progress;
        }

        public void onSync() {
            if (rotate.getValue() && progress > 0.95) {
                // TODO:做转头逻辑
            }
        }

        public void reset() {
            if (progress == 0)
                return;

            prevProgress = progress = 0;
            Direction dir = RotationUtils.getDirection(pos, null);
            mc.getConnection().send(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK, pos, dir));
            start(dir);
        }

        public void cancel() {
            if (progress != 0) {
                mc.getConnection().send(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK, pos, RotationUtils.getDirection(pos, Direction.DOWN)));
            }
        }

        public boolean instantBreaking() {
            return mineBreaks > 0 && mode.is(Mode.Instant);
        }
    }

    private record DelayedAction(long runAt, Runnable runnable) {
    }

}
