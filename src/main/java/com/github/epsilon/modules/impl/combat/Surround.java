package com.github.epsilon.modules.impl.combat;

import com.github.epsilon.managers.RotationManager;
import com.github.epsilon.modules.Category;
import com.github.epsilon.modules.Module;
import com.github.epsilon.settings.impl.BoolSetting;
import com.github.epsilon.settings.impl.EnumSetting;
import com.github.epsilon.settings.impl.IntSetting;
import com.github.epsilon.utils.player.FindItemResult;
import com.github.epsilon.utils.player.InvUtils;
import com.github.epsilon.utils.player.MoveUtils;
import com.github.epsilon.utils.rotation.Priority;
import com.github.epsilon.utils.rotation.RaytraceUtils;
import com.github.epsilon.utils.rotation.RotationUtils;
import com.github.epsilon.utils.timer.TimerUtils;
import com.github.epsilon.utils.world.BlockUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class Surround extends Module {

    public static final Surround INSTANCE = new Surround();

    private Surround() {
        super("Surround", Category.COMBAT);
    }

    private static final int EXTEND_DEPTH = 2;
    private static final int NEIGHBOR_ATTEMPTS = 2;

    private enum SwapMode {
        Normal,
        Silent,
        InvSwitch
    }

    private final EnumSetting<SwapMode> swapMode = enumSetting("Swap Mode", SwapMode.Silent);
    private final IntSetting placeDelay = intSetting("Place Delay", 50, 0, 1000, 1);
    private final BoolSetting groundCheck = boolSetting("Ground Check", true);
    private final BoolSetting autoCenter = boolSetting("Auto Center", true);
    private final BoolSetting rotate = boolSetting("Rotate", false);
    private final BoolSetting attackCrystal = boolSetting("Attack Crystal", true);
    private final BoolSetting disableOnMove = boolSetting("Disable On Move", true);
    private final BoolSetting useEnderChest = boolSetting("Use Ender Chest", true);

    private BlockPos anchorPos;
    private final TimerUtils placeTimer = new TimerUtils();

    @Override
    protected void onEnable() {
        placeTimer.setMs(placeDelay.getValue().longValue());
        anchorPos = nullCheck() ? null : mc.player.blockPosition();
        if (!nullCheck() && autoCenter.getValue()) {
            centerPlayer();
        }
    }

    @Override
    protected void onDisable() {
        anchorPos = null;
    }

    @SubscribeEvent
    private void onTick(ClientTickEvent.Pre event) {
        if (nullCheck()) return;
        if (groundCheck.getValue() && !mc.player.onGround()) return;

        BlockPos currentPos = mc.player.blockPosition();
        if (anchorPos == null) {
            anchorPos = currentPos;
        }

        if (disableOnMove.getValue() && !currentPos.equals(anchorPos) && MoveUtils.isMoving()) {
            toggle();
            return;
        }

        if (!MoveUtils.isMoving()) {
            anchorPos = currentPos;
        }

        if (!placeTimer.passedMillise(placeDelay.getValue())) return;

        for (BlockPos targetPos : collectTargetPositions(anchorPos)) {
            if (!mc.level.getBlockState(targetPos).canBeReplaced()) continue;

            if (attackCrystal.getValue()) {
                fuckCrystal(targetPos);
            }

            if (!BlockUtils.canPlaceAt(targetPos)) continue;

            List<PlaceTarget> placeSequence = findPlaceSequence(targetPos, NEIGHBOR_ATTEMPTS, targetPos, 0);
            if (placeSequence == null || placeSequence.isEmpty()) continue;

            FindItemResult result = swapMode.is(SwapMode.InvSwitch) ? InvUtils.find(this::item) : InvUtils.findInHotbar(this::item);
            if (!result.found()) return;

            tryPlace(placeSequence.getFirst(), result);

            return;
        }
    }

    private void centerPlayer() {
        double centeredX = Math.floor(mc.player.getX()) + 0.5;
        double centeredZ = Math.floor(mc.player.getZ()) + 0.5;
        mc.player.setPos(centeredX, mc.player.getY(), centeredZ);
        anchorPos = mc.player.blockPosition();
    }

    private boolean item(ItemStack itemStack) {
        Item item = itemStack.getItem();
        if (item == Items.OBSIDIAN) {
            return true;
        } else if (useEnderChest.getValue() && item == Items.ENDER_CHEST) {
            return true;
        }
        return false;
    }

    private List<BlockPos> collectTargetPositions(BlockPos origin) {
        LinkedHashSet<BlockPos> targets = new LinkedHashSet<>();

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos targetPos = origin.relative(direction);
            if (!mc.level.getBlockState(targetPos).canBeReplaced()) continue;

            if (hasBlockingPlayer(targetPos)) {
                collectExtendedTargets(targetPos, origin, targets, EXTEND_DEPTH);
            } else {
                targets.add(targetPos);
            }
        }

        return new ArrayList<>(targets);
    }

    private void collectExtendedTargets(BlockPos targetPos, BlockPos origin, Set<BlockPos> targets, int depth) {
        if (depth <= 0) return;

        for (Direction direction : Direction.values()) {
            if (direction == Direction.UP) continue;
            BlockPos extendedPos = targetPos.relative(direction);
            if (extendedPos.equals(origin)) continue;
            if (!mc.level.getBlockState(extendedPos).canBeReplaced()) continue;

            if (hasBlockingPlayer(extendedPos)) {
                collectExtendedTargets(extendedPos, origin, targets, depth - 1);
            } else {
                targets.add(extendedPos);
            }
        }
    }

    private boolean hasBlockingPlayer(BlockPos pos) {
        for (Entity entity : mc.level.getEntities(null, new AABB(pos))) {
            if (!(entity instanceof Player player) || player == mc.player) continue;
            if (entity.isAlive()) {
                return true;
            }
        }
        return false;
    }

    private void fuckCrystal(BlockPos targetPos) {
        for (Entity entity : mc.level.getEntities(null, new AABB(targetPos))) {
            if (!(entity instanceof EndCrystal crystal) || !crystal.isAlive()) continue;
            mc.gameMode.attack(mc.player, crystal);
            mc.getConnection().send(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
            break;
        }
    }

    private List<PlaceTarget> findPlaceSequence(BlockPos targetPos, int attempts, BlockPos origin, int lastDist) {
        if (hasBlockingPlayer(targetPos)) {
            for (BlockPos extendedPos : getExtendedTargets(targetPos, origin)) {
                if (origin.distManhattan(extendedPos) <= lastDist) continue;

                List<PlaceTarget> extendedSequence = findPlaceSequence(extendedPos, attempts, origin, lastDist + 1);
                if (extendedSequence != null && !extendedSequence.isEmpty()) {
                    return extendedSequence;
                }
            }

            return null;
        }

        for (Direction side : Direction.values()) {
            PlaceTarget directTarget = buildPlaceTarget(targetPos, side, true, origin, lastDist);
            if (directTarget != null) {
                return new ArrayList<>(List.of(directTarget));
            }
        }

        if (attempts <= 1) {
            return null;
        }

        for (Direction side : Direction.values()) {
            PlaceTarget helperTarget = buildPlaceTarget(targetPos, side, false, origin, lastDist);
            if (helperTarget == null) continue;

            List<PlaceTarget> sequence = findPlaceSequence(targetPos.relative(side), attempts - 1, origin, lastDist + 1);
            if (sequence == null) continue;

            sequence.add(helperTarget);
            return sequence;
        }

        return null;
    }

    private List<BlockPos> getExtendedTargets(BlockPos targetPos, BlockPos origin) {
        ArrayList<BlockPos> targets = new ArrayList<>();

        for (Direction direction : Direction.values()) {
            if (direction == Direction.UP) continue;

            BlockPos extendedPos = targetPos.relative(direction);
            if (extendedPos.equals(origin)) continue;
            if (!mc.level.getBlockState(extendedPos).canBeReplaced()) continue;

            targets.add(extendedPos);
        }

        return targets;
    }

    private PlaceTarget buildPlaceTarget(BlockPos targetPos, Direction side, boolean requireSolidNeighbor, BlockPos origin, int lastDist) {
        BlockPos neighborPos = targetPos.relative(side);
        int distToOrigin = origin.distManhattan(neighborPos);
        if (distToOrigin <= lastDist) return null;
        if (!mc.level.getBlockState(targetPos).canBeReplaced()) return null;
        if (!BlockUtils.canPlaceAt(targetPos)) return null;
        if (requireSolidNeighbor && mc.level.getBlockState(neighborPos).canBeReplaced()) return null;

        Direction hitSide = side.getOpposite();
        Vec3 hitVec = new Vec3(neighborPos.getX() + 0.5 + hitSide.getStepX() * 0.5, neighborPos.getY() + 0.5 + hitSide.getStepY() * 0.5, neighborPos.getZ() + 0.5 + hitSide.getStepZ() * 0.5);

        if (mc.player.getEyePosition().distanceToSqr(hitVec) > 4.5 * 4.5) return null;

        Vector2f rotationVec = RotationUtils.calculate(neighborPos, hitSide);
        Vector2f reverseYaw = new Vector2f(Mth.wrapDegrees(mc.player.getYRot() - 180.0f), rotationVec.y);
        if (RaytraceUtils.overBlock(reverseYaw, neighborPos, hitSide, false)) {
            rotationVec = reverseYaw;
        }

        return new PlaceTarget(targetPos, neighborPos, hitSide, hitVec, rotationVec);
    }

    private void tryPlace(PlaceTarget placeTarget, FindItemResult item) {
        if (!rotate.getValue()) {
            placeBlock(placeTarget, item);
            return;
        }

        final int requestPriority = Priority.High.priority;
        RotationManager.INSTANCE.applyRotation(placeTarget.rotation(), 10, requestPriority, record -> {
            if (!isEnabled() || nullCheck()) return;
            if (record.selectedPriorityValue() != requestPriority) return;

            boolean b = RaytraceUtils.overBlock(record.currentRotation(), placeTarget.neighborPos(), placeTarget.side(), false);
            if (!b) {
                return;
            }

            placeBlock(placeTarget, item);
        });
    }

    private void placeBlock(PlaceTarget placeTarget, FindItemResult item) {
        if (!mc.level.getBlockState(placeTarget.targetPos()).canBeReplaced()) {
            return;
        }

        if (attackCrystal.getValue()) {
            fuckCrystal(placeTarget.targetPos());
            if (!BlockUtils.canPlaceAt(placeTarget.targetPos())) {
                return;
            }
        }

        InteractionHand hand = item.getHand();
        boolean shouldSwapBack = false;
        boolean shouldInvSwapBack = false;

        if (hand == InteractionHand.MAIN_HAND) {
            if (swapMode.is(SwapMode.InvSwitch)) {
                InvUtils.invSwap(item.slot());
                shouldInvSwapBack = true;
            } else {
                InvUtils.swap(item.slot(), swapMode.is(SwapMode.Silent));
                shouldSwapBack = swapMode.is(SwapMode.Silent);
            }
        }

        BlockHitResult hitResult = new BlockHitResult(placeTarget.hitVec(), placeTarget.side(), placeTarget.neighborPos(), false);
        InteractionResult result = mc.gameMode.useItemOn(mc.player, hand, hitResult);
        if (result.consumesAction()) {
            mc.getConnection().send(new ServerboundSwingPacket(hand));
            placeTimer.reset();
        }

        if (shouldSwapBack) {
            InvUtils.swapBack();
        } else if (shouldInvSwapBack) {
            InvUtils.invSwapBack();
        }
    }

    private record PlaceTarget(BlockPos targetPos, BlockPos neighborPos, Direction side, Vec3 hitVec,
                               Vector2f rotation) {
    }

}
