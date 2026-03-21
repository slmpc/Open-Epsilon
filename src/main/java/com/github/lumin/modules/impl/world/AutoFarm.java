package com.github.lumin.modules.impl.world;

import com.github.lumin.managers.RotationManager;
import com.github.lumin.modules.Category;
import com.github.lumin.modules.Module;
import com.github.lumin.settings.impl.BoolSetting;
import com.github.lumin.settings.impl.DoubleSetting;
import com.github.lumin.settings.impl.IntSetting;
import com.github.lumin.utils.player.FindItemResult;
import com.github.lumin.utils.player.InvUtils;
import com.github.lumin.utils.timer.TimerUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.SugarCaneBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class AutoFarm extends Module {

    public static final AutoFarm INSTANCE = new AutoFarm();

    private final DoubleSetting range = doubleSetting("Range", 4.0, 1.0, 6.0, 0.5);
    private final BoolSetting autoHarvest = boolSetting("AutoHarvest", true);
    private final BoolSetting autoPlant = boolSetting("AutoPlant", true);
    private final BoolSetting useBonemeal = boolSetting("UseBonemeal", false);
    private final BoolSetting rotate = boolSetting("Rotate", true);
    private final IntSetting rotationSpeed = intSetting("RotationSpeed", 10, 1, 10, 1, rotate::getValue);
    private final DoubleSetting harvestDelay = doubleSetting("HarvestDelay", 0.15, 0.0, 2.0, 0.05);
    private final BoolSetting wheat = boolSetting("Wheat", true);
    private final BoolSetting carrots = boolSetting("Carrots", true);
    private final BoolSetting potatoes = boolSetting("Potatoes", true);
    private final BoolSetting beetroot = boolSetting("Beetroot", true);
    private final BoolSetting sugarCane = boolSetting("SugarCane", true);

    private final TimerUtils actionTimer = new TimerUtils();
    private final TimerUtils harvestTimer = new TimerUtils();
    private int actionsThisTick;

    private AutoFarm() {
        super("AutoFarm", Category.WORLD);
    }

    @Override
    protected void onEnable() {
        actionsThisTick = 0;
        actionTimer.reset();
        harvestTimer.reset();
    }

    @Override
    protected void onDisable() {
        actionsThisTick = 0;
    }

    @SubscribeEvent
    private void onTick(ClientTickEvent.Pre event) {
        if (nullCheck() || mc.gameMode == null || mc.screen != null) {
            return;
        }

        if (!autoPlant.getValue() && !autoHarvest.getValue()) {
            return;
        }

        actionsThisTick = 0;
        for (BlockPos pos : collectTargetsInRadius()) {
            if (actionsThisTick >= 1) {
                break;
            }

            BlockState state = mc.level.getBlockState(pos);
            if (autoHarvest.getValue() && isSupportedCrop(state.getBlock())) {
                boolean grown = isHarvestReady(state, pos);
                if (grown && harvestTimer.passedMillise(harvestDelay.getValue() * 1000.0)) {
                    if (shouldWaitForRotation(pos)) {
                        break;
                    }

                    if (mc.gameMode.startDestroyBlock(pos, Direction.UP)) {
                        mc.player.swing(InteractionHand.MAIN_HAND);
                        actionTimer.reset();
                        harvestTimer.reset();
                        actionsThisTick++;
                    }
                    break;
                }
            }

            if (autoPlant.getValue()) {
                FindItemResult plantingItem = findPlantingItem(pos, state);
                if (plantingItem.found() && actionTimer.passedMillise(50.0)) {
                    if (shouldWaitForRotation(getPlantTarget(pos))) {
                        break;
                    }
                    plantAt(pos, plantingItem);
                    break;
                }
            }

            if (useBonemeal.getValue() && state.getBlock() instanceof CropBlock crop && crop.getAge(state) < crop.getMaxAge()) {
                FindItemResult bonemealItem = findPreferredHotbarItem(stack -> stack.is(Items.BONE_MEAL));
                if (bonemealItem.found() && actionTimer.passedMillise(50.0)) {
                    if (shouldWaitForRotation(getBonemealTarget(pos))) {
                        break;
                    }
                    applyBonemeal(pos, bonemealItem);
                    break;
                }
            }
        }
    }

    private List<BlockPos> collectTargetsInRadius() {
        List<BlockPos> targets = new ArrayList<>();
        BlockPos playerPos = BlockPos.containing(mc.player.position());
        double rangeValue = range.getValue();
        int radius = (int) Math.ceil(rangeValue);

        for (int x = -radius; x <= radius; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos pos = playerPos.offset(x, y, z);
                    if (Vec3.atCenterOf(pos).distanceToSqr(mc.player.position()) > rangeValue * rangeValue) {
                        continue;
                    }
                    targets.add(pos);
                }
            }
        }

        targets.sort(Comparator.comparingDouble(pos -> Vec3.atCenterOf(pos).distanceToSqr(mc.player.position())));
        return targets;
    }

    private boolean isSupportedCrop(Block block) {
        if (block == Blocks.WHEAT) {
            return wheat.getValue();
        }
        if (block == Blocks.CARROTS) {
            return carrots.getValue();
        }
        if (block == Blocks.POTATOES) {
            return potatoes.getValue();
        }
        if (block == Blocks.BEETROOTS) {
            return beetroot.getValue();
        }
        return block == Blocks.SUGAR_CANE && sugarCane.getValue();
    }

    private boolean isHarvestReady(BlockState state, BlockPos pos) {
        if (state.getBlock() instanceof CropBlock crop) {
            return crop.getAge(state) == crop.getMaxAge();
        }
        if (state.getBlock() instanceof SugarCaneBlock) {
            return mc.level.getBlockState(pos.below()).is(Blocks.SUGAR_CANE);
        }
        return false;
    }

    private Vector2f getRotation(Vec3 target) {
        float[] rotation = RotationManager.INSTANCE.getRotation(target);
        return new Vector2f(rotation[0], rotation[1]);
    }

    private boolean shouldWaitForRotation(BlockPos pos) {
        return shouldWaitForRotation(Vec3.atCenterOf(pos));
    }

    private boolean shouldWaitForRotation(Vec3 target) {
        if (!rotate.getValue()) {
            return false;
        }

        Vector2f targetRotation = getRotation(target);
        RotationManager.INSTANCE.setRotations(targetRotation, rotationSpeed.getValue(), com.github.lumin.utils.rotation.MovementFix.OFF);

        Vector2f currentRotation = RotationManager.INSTANCE.getRotation();
        float yawDiff = Math.abs(Mth.wrapDegrees(targetRotation.x - currentRotation.x));
        float pitchDiff = Math.abs(targetRotation.y - currentRotation.y);
        return yawDiff > 8.0f || pitchDiff > 8.0f;
    }

    private FindItemResult findPlantingItem(BlockPos pos, BlockState state) {
        // Current stage only supports main hand, offhand, and hotbar silent swap. Inventory planting can be added later.
        if (canPlantCropAt(pos, state)) {
            return findPreferredHotbarItem(this::isCropPlantingItem);
        }
        if (canPlantSugarCaneAt(pos, state)) {
            return findPreferredHotbarItem(stack -> stack.is(Items.SUGAR_CANE));
        }
        return new FindItemResult(-1, 0, 0);
    }

    private FindItemResult findPreferredHotbarItem(java.util.function.Predicate<net.minecraft.world.item.ItemStack> predicate) {
        if (predicate.test(mc.player.getMainHandItem())) {
            return new FindItemResult(mc.player.getInventory().getSelectedSlot(), mc.player.getMainHandItem().getCount(), mc.player.getMainHandItem().getMaxStackSize());
        }

        if (predicate.test(mc.player.getOffhandItem())) {
            return new FindItemResult(40, mc.player.getOffhandItem().getCount(), mc.player.getOffhandItem().getMaxStackSize());
        }

        for (int slot = 0; slot <= 8; slot++) {
            var stack = mc.player.getInventory().getItem(slot);
            if (predicate.test(stack)) {
                return new FindItemResult(slot, stack.getCount(), stack.getMaxStackSize());
            }
        }

        return new FindItemResult(-1, 0, 0);
    }

    private boolean isCropPlantingItem(net.minecraft.world.item.ItemStack stack) {
        return isCropPlantingItem(stack.getItem());
    }

    private boolean isCropPlantingItem(Item item) {
        return item == Items.WHEAT_SEEDS && wheat.getValue()
                || item == Items.CARROT && carrots.getValue()
                || item == Items.POTATO && potatoes.getValue()
                || item == Items.BEETROOT_SEEDS && beetroot.getValue();
    }

    private boolean canPlantCropAt(BlockPos pos, BlockState state) {
        return mc.level.getBlockState(pos.above()).isAir() && state.is(Blocks.FARMLAND);
    }

    private boolean canPlantSugarCaneAt(BlockPos pos, BlockState state) {
        if (!mc.level.getBlockState(pos.above()).isAir() || !(state.is(BlockTags.DIRT) || state.is(BlockTags.SAND))) {
            return false;
        }

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos sidePos = pos.relative(direction);
            FluidState fluidState = mc.level.getFluidState(sidePos);
            BlockState sideState = mc.level.getBlockState(sidePos);
            if (fluidState.is(FluidTags.WATER) || sideState.is(Blocks.FROSTED_ICE)) {
                return true;
            }
        }

        return false;
    }

    private Vec3 getPlantTarget(BlockPos pos) {
        // 放弃了，来个高人帮我优化。
        return new Vec3(pos.getX() + 0.5, pos.getY() + 0.95, pos.getZ() + 0.5);
    }

    private Vec3 getBonemealTarget(BlockPos pos) {
        return Vec3.atCenterOf(pos);
    }

    private void plantAt(BlockPos pos, FindItemResult result) {
        boolean swapped = result.slot() != 40 && result.slot() != mc.player.getInventory().getSelectedSlot();
        if (swapped && !InvUtils.swap(result.slot(), true)) {
            return;
        }

        InteractionHand hand = result.getHand();
        InteractionResult interaction = mc.gameMode.useItemOn(mc.player, hand, new BlockHitResult(getPlantTarget(pos), Direction.UP, pos, false));
        if (interaction.consumesAction()) {
            mc.player.swing(hand);
            actionTimer.reset();
            actionsThisTick++;
        }

        if (swapped) {
            InvUtils.swapBack();
        }
    }

    private void applyBonemeal(BlockPos pos, FindItemResult result) {
        boolean swapped = result.slot() != 40 && result.slot() != mc.player.getInventory().getSelectedSlot();
        if (swapped && !InvUtils.swap(result.slot(), true)) {
            return;
        }

        InteractionHand hand = result.getHand();
        InteractionResult interaction = mc.gameMode.useItemOn(mc.player, hand, new BlockHitResult(getBonemealTarget(pos), Direction.UP, pos, false));
        if (interaction.consumesAction()) {
            mc.player.swing(hand);
            actionTimer.reset();
            actionsThisTick++;
        }

        if (swapped) {
            InvUtils.swapBack();
        }
    }

}
