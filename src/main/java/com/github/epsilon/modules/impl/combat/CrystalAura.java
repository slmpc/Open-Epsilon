package com.github.epsilon.modules.impl.combat;

import com.github.epsilon.managers.RotationManager;
import com.github.epsilon.managers.TargetManager;
import com.github.epsilon.modules.Category;
import com.github.epsilon.modules.Module;
import com.github.epsilon.settings.impl.*;
import com.github.epsilon.utils.combat.DamageUtils;
import com.github.epsilon.utils.player.FindItemResult;
import com.github.epsilon.utils.player.InvUtils;
import com.github.epsilon.utils.render.Render3DUtils;
import com.github.epsilon.utils.rotation.Priority;
import com.github.epsilon.utils.rotation.RotationUtils;
import com.github.epsilon.utils.timer.TimerUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class CrystalAura extends Module {

    public static final CrystalAura INSTANCE = new CrystalAura();

    private CrystalAura() {
        super("CrystalAura", Category.COMBAT);
    }

    private final DoubleSetting targetRange = doubleSetting("Target Range", 6.0, 0.0, 12.0, 0.5);
    private final DoubleSetting rotationSpeed = doubleSetting("Rotation Speed", 10.0, 1.0, 10.0, 0.5);
    private final BoolSetting eatingPause = boolSetting("Eating Pause", false);

    private final BoolSetting noSuicide = boolSetting("No Suicide", true);
    private final DoubleSetting lethalMaxSelfDamage = doubleSetting("Lethal Max Self Dmg", 8.0, 0.0, 36.0, 0.25);
    private final BoolSetting motionPrediction = boolSetting("Motion Prediction", false);
    private final IntSetting predictTick = intSetting("Predict Tick", 6, 0, 10, 1, motionPrediction::getValue);

    private final DoubleSetting forcePlaceHealth = doubleSetting("Force Place Health", 8.0, 0.0, 36.0, 0.5);
    private final IntSetting forcePlaceArmorRate = intSetting("Force Place Armor Rate", 3, 0, 25, 1);
    private final DoubleSetting forcePlaceMinDamage = doubleSetting("Force Place Min Dmg", 2.0, 0.0, 20.0, 0.25);
    private final DoubleSetting forcePlaceBalance = doubleSetting("Force Place Balance", -3.0, -10.0, 10.0, 0.25);

    private final EnumSetting<SwingMode> placeSwing = enumSetting("Place Swing", SwingMode.None);
    private final EnumSetting<SwapMode> placeSwapMode = enumSetting("Place Swap Mode", SwapMode.None);
    private final DoubleSetting placeMinDmg = doubleSetting("Place Min Dmg", 6.0, 0.0, 20.0, 0.25);
    private final DoubleSetting placeMaxSelfDmg = doubleSetting("Place Max Self Dmg", 10.0, 0.0, 36.0, 0.25);
    private final DoubleSetting placeBalance = doubleSetting("Place Balance", -3.0, -10.0, 10.0, 0.25);
    private final IntSetting placeDelay = intSetting("Place Delay", 50, 0, 1000, 10);
    private final DoubleSetting placeRange = doubleSetting("Place Range", 4.0, 1.0, 6.0, 0.1);

    private final EnumSetting<SwingMode> breakSwing = enumSetting("Break Swing", SwingMode.None);
    private final BoolSetting antiWeak = boolSetting("Anti Weak", false);
    private final EnumSetting<SwapMode> antiWeakSwapMode = enumSetting("Anti Weak Swap Mode", SwapMode.Silent, antiWeak::getValue);
    private final DoubleSetting breakMinDmg = doubleSetting("Break Min Dmg", 6.0, 0.0, 20.0, 0.25);
    private final DoubleSetting breakMaxSelfDmg = doubleSetting("Break Max Self Dmg", 10.0, 0.0, 36.0, 0.25);
    private final DoubleSetting breakBalance = doubleSetting("Break Balance", -3.0, -10.0, 10.0, 0.25);
    private final IntSetting breakDelay = intSetting("Break Delay", 50, 0, 1000, 10);
    private final DoubleSetting breakRange = doubleSetting("Break Range", 4.0, 1.0, 6.0, 0.1);

    private final ColorSetting filledColor = colorSetting("Filled Color", new Color(255, 150, 120, 100));
    private final ColorSetting outlineColor = colorSetting("Outline Color", new Color(255, 150, 120, 170));
    private final IntSetting movingLength = intSetting("Moving Length", 400, 0, 1000, 50);
    private final IntSetting fadeLength = intSetting("Fade Length", 200, 0, 1000, 50);

    private LivingEntity target;
    private Vec3 lastTargetPos;
    private final TimerUtils placeTimer = new TimerUtils();
    private final TimerUtils breakTimer = new TimerUtils();
    private final TimerUtils renderTimer = new TimerUtils();
    private final List<RenderRecord> renderRecords = new ArrayList<>();

    @Override
    protected void onEnable() {
        lastTargetPos = null;
        placeTimer.reset();
        breakTimer.reset();
        renderTimer.reset();
        renderRecords.clear();
    }

    @Override
    protected void onDisable() {
        target = null;
        lastTargetPos = null;
        renderRecords.clear();
    }

    @SubscribeEvent
    private void onTick(ClientTickEvent.Pre event) {
        if (nullCheck()) return;
        if (eatingPause.getValue() && mc.player.isUsingItem()) return;

        target = TargetManager.INSTANCE.acquirePrimary(
                TargetManager.TargetRequest.of(
                        targetRange.getValue(),
                        360.0f,
                        true,
                        false,
                        false,
                        false,
                        true,
                        1
                )
        );

        if (target == null || !target.isAlive()) {
            lastTargetPos = null;
            return;
        }

        Vec3 predictedPos = getPredictedTargetPos(target);

        tryBreakCrystal();
        tryPlaceCrystal(predictedPos);
        lastTargetPos = target.position();
    }

    private void tryBreakCrystal() {
        if (!breakTimer.passedMillise(breakDelay.getValue())) return;

        EndCrystal bestCrystal = null;
        float bestScore = Float.MIN_VALUE;

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof EndCrystal crystal)) continue;
            if (!crystal.isAlive()) continue;

            double distToPlayer = mc.player.distanceTo(crystal);
            if (distToPlayer > breakRange.getValue()) continue;

            Vec3 crystalPos = crystal.position();

            float targetDmg = DamageUtils.crystalDamage(target, crystalPos, DamageUtils.ArmorEnchantmentMode.None);
            float selfDmg = DamageUtils.selfCrystalDamage(crystalPos);
            if (exceedsSelfDamageLimit(selfDmg, breakMaxSelfDmg.getValue())) continue;
            if (targetDmg < breakMinDmg.getValue()) continue;
            float balance = targetDmg - selfDmg;
            if (balance < breakBalance.getValue()) continue;
            if (targetDmg > bestScore) {
                bestScore = targetDmg;
                bestCrystal = crystal;
            }
        }

        if (bestCrystal != null) {
            doBreakCrystal(bestCrystal);
        }
    }

    private void doBreakCrystal(EndCrystal crystal) {
        boolean swapped = false;
        if (antiWeak.getValue() && mc.player.hasEffect(net.minecraft.world.effect.MobEffects.WEAKNESS)) {
            FindItemResult sword = InvUtils.findInHotbar(Items.DIAMOND_SWORD, Items.NETHERITE_SWORD, Items.IRON_SWORD, Items.STONE_SWORD);
            if (sword.found()) {
                switch (antiWeakSwapMode.getValue()) {
                    case Swap -> InvUtils.swap(sword.slot(), false);
                    case Silent -> swapped = InvUtils.swap(sword.slot(), true);
                    default -> { }
                }
            }
        }

        Vector2f rotation = RotationUtils.calculate(crystal);
        RotationManager.INSTANCE.applyRotation(rotation, rotationSpeed.getValue(), Priority.High);
        mc.gameMode.attack(mc.player, crystal);
        doSwing(breakSwing.getValue());
        if (swapped) {
            InvUtils.swapBack();
        }
        breakTimer.reset();
        addRenderRecord(crystal.blockPosition().below());
    }

    private void tryPlaceCrystal(Vec3 predictedTargetPos) {
        if (!placeTimer.passedMillise(placeDelay.getValue())) return;

        FindItemResult crystalItem = findCrystalItem();
        if (!crystalItem.found()) return;
        List<PlaceCandidate> candidates = collectPlaceCandidates(predictedTargetPos);

        PlaceCandidate bestNormal = findBestCandidate(candidates,
                placeMinDmg.getValue().floatValue(),
                placeMaxSelfDmg.getValue().floatValue(),
                placeBalance.getValue().floatValue());

        if (bestNormal != null) {
            doPlaceCrystal(bestNormal, crystalItem);
            return;
        }

        if (shouldForcePlace()) {
            PlaceCandidate bestForce = findBestCandidate(candidates,
                    forcePlaceMinDamage.getValue().floatValue(),
                    placeMaxSelfDmg.getValue().floatValue(),
                    forcePlaceBalance.getValue().floatValue());

            if (bestForce != null) {
                doPlaceCrystal(bestForce, crystalItem);
            }
        }
    }

    /**
     * Checks whether Force Place conditions are met:
     * - target health ≤ forcePlaceHealth  OR
     * - target armor durability rate ≤ forcePlaceArmorRate (total armor value)
     */
    private boolean shouldForcePlace() {
        if (target.getHealth() <= forcePlaceHealth.getValue()) return true;

        float targetArmor = (float) target.getAttributeValue(Attributes.ARMOR);
        return targetArmor <= forcePlaceArmorRate.getValue();
    }

    private List<PlaceCandidate> collectPlaceCandidates(Vec3 predictedTargetPos) {
        List<PlaceCandidate> candidates = new ArrayList<>();

        BlockPos center = BlockPos.containing(predictedTargetPos);
        int range = 5;
        double placeRangeSq = placeRange.getValue() * placeRange.getValue();
        Vec3 playerEye = mc.player.getEyePosition();

        for (int x = -range; x <= range; x++) {
            for (int y = -range; y <= range; y++) {
                for (int z = -range; z <= range; z++) {
                    BlockPos supportPos = center.offset(x, y, z);

                    BlockState supportState = mc.level.getBlockState(supportPos);
                    if (!supportState.is(Blocks.OBSIDIAN) && !supportState.is(Blocks.BEDROCK)) continue;
                    BlockPos crystalBlockPos = supportPos.above();
                    if (!mc.level.getBlockState(crystalBlockPos).isAir()) continue;
                    if (!mc.level.getBlockState(crystalBlockPos.above()).isAir()) continue;
                    AABB crystalBB = new AABB(crystalBlockPos);
                    if (!mc.level.getEntities(null, crystalBB).isEmpty()) continue;
                    Vec3 supportCenter = Vec3.atCenterOf(supportPos);
                    if (playerEye.distanceToSqr(supportCenter) > placeRangeSq) continue;
                    Vec3 crystalPos = new Vec3(
                            supportPos.getX() + 0.5,
                            supportPos.getY() + 1.0,
                            supportPos.getZ() + 0.5
                    );

                    float targetDmg = DamageUtils.crystalDamage(target, crystalPos, DamageUtils.ArmorEnchantmentMode.None);
                    float selfDmg = DamageUtils.selfCrystalDamage(crystalPos);

                    candidates.add(new PlaceCandidate(supportPos, crystalPos, targetDmg, selfDmg));
                }
            }
        }

        return candidates;
    }

    private PlaceCandidate findBestCandidate(List<PlaceCandidate> candidates,
                                              float minDmg, float maxSelfDmg, float minBalance) {
        PlaceCandidate best = null;
        float bestScore = Float.MIN_VALUE;

        for (PlaceCandidate c : candidates) {
            if (exceedsSelfDamageLimit(c.selfDmg, maxSelfDmg)) continue;
            if (c.targetDmg < minDmg) continue;
            float balance = c.targetDmg - c.selfDmg;
            if (balance < minBalance) continue;

            if (c.targetDmg > bestScore) {
                bestScore = c.targetDmg;
                best = c;
            }
        }

        return best;
    }

    private void doPlaceCrystal(PlaceCandidate candidate, FindItemResult crystalItem) {
        boolean swapped = false;
        InteractionHand hand = crystalItem.getHand();
        if (crystalItem.slot() != mc.player.getInventory().getSelectedSlot() && crystalItem.slot() != 40) {
            switch (placeSwapMode.getValue()) {
                case Swap -> InvUtils.swap(crystalItem.slot(), false);
                case Silent -> swapped = InvUtils.swap(crystalItem.slot(), true);
                default -> { }
            }
            hand = InteractionHand.MAIN_HAND;
        }
        Vector2f rotation = RotationUtils.calculate(candidate.supportPos, Direction.UP);
        RotationManager.INSTANCE.applyRotation(rotation, rotationSpeed.getValue(), Priority.High);
        Vec3 hitVec = new Vec3(
                candidate.supportPos.getX() + 0.5,
                candidate.supportPos.getY() + 1.0,
                candidate.supportPos.getZ() + 0.5
        );
        BlockHitResult hitResult = new BlockHitResult(hitVec, Direction.UP, candidate.supportPos, false);
        InteractionResult result = mc.gameMode.useItemOn(mc.player, hand, hitResult);

        if (result.consumesAction()) {
            doSwing(placeSwing.getValue());
            addRenderRecord(candidate.supportPos);
        }
        if (swapped) {
            InvUtils.swapBack();
        }
        placeTimer.reset();
    }

    /**
     * Combined self-damage check — returns {@code true} when the crystal should be
     * <b>rejected</b> (self-damage is too high).
     * <ol>
     *   <li>selfDmg must not exceed the per-action max (placeMaxSelfDmg / breakMaxSelfDmg)</li>
     *   <li>noSuicide: selfDmg must not kill the player (hp + absorption - selfDmg &gt; 0.5)</li>
     *   <li>lethalMaxSelfDamage: when the remaining HP would be critically low, selfDmg
     *       must also be below lethalMaxSelfDamage.</li>
     * </ol>
     */
    private boolean exceedsSelfDamageLimit(float selfDmg, double maxSelfDmg) {
        if (selfDmg > maxSelfDmg) return true;

        float hp = mc.player.getHealth() + mc.player.getAbsorptionAmount();
        if (noSuicide.getValue() && hp - selfDmg <= 0.5f) return true;
        float remainingHp = hp - selfDmg;
        if (noSuicide.getValue() && remainingHp <= lethalMaxSelfDamage.getValue()
                && selfDmg > lethalMaxSelfDamage.getValue()) {
            return true;
        }

        return false;
    }

    private Vec3 getPredictedTargetPos(LivingEntity entity) {
        if (!motionPrediction.getValue() || lastTargetPos == null) {
            return entity.position();
        }

        Vec3 currentPos = entity.position();
        Vec3 velocity = currentPos.subtract(lastTargetPos);
        int ticks = predictTick.getValue();

        return currentPos.add(velocity.scale(ticks));
    }

    private FindItemResult findCrystalItem() {
        return InvUtils.findInHotbar(Items.END_CRYSTAL);
    }

    private void doSwing(SwingMode mode) {
        switch (mode) {
            case Client -> mc.player.swing(InteractionHand.MAIN_HAND);
            case Packet -> mc.getConnection().send(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
            default -> { }
        }
    }

    private void addRenderRecord(BlockPos pos) {
        renderRecords.removeIf(r -> r.pos.equals(pos));
        renderRecords.add(new RenderRecord(pos, renderTimer.getCurrentMS()));
    }

    @SubscribeEvent
    private void onRender3D(RenderLevelStageEvent.AfterLevel event) {
        if (nullCheck()) return;
        if (renderRecords.isEmpty()) return;

        long now = renderTimer.getCurrentMS();
        long totalLife = movingLength.getValue() + fadeLength.getValue();
        renderRecords.removeIf(r -> now - r.time > totalLife);

        for (RenderRecord record : renderRecords) {
            long age = now - record.time;
            float alpha;

            if (age <= movingLength.getValue()) {
                alpha = 1.0f;
            } else {
                float fadeProgress = (float) (age - movingLength.getValue()) / Math.max(1, fadeLength.getValue());
                alpha = 1.0f - Math.min(1.0f, fadeProgress);
            }

            if (alpha <= 0.01f) continue;

            Color fc = filledColor.getValue();
            Color oc = outlineColor.getValue();

            Color filled = new Color(fc.getRed(), fc.getGreen(), fc.getBlue(),
                    Math.max(0, Math.min(255, (int) (fc.getAlpha() * alpha))));
            Color outline = new Color(oc.getRed(), oc.getGreen(), oc.getBlue(),
                    Math.max(0, Math.min(255, (int) (oc.getAlpha() * alpha))));

            AABB box = new AABB(record.pos);
            Render3DUtils.drawFilledBox(box, filled);
            Render3DUtils.drawOutlineBox(event.getPoseStack(), box, outline.getRGB(), 1.5f);
        }
    }


    private record PlaceCandidate(BlockPos supportPos, Vec3 crystalPos, float targetDmg, float selfDmg) { }

    private record RenderRecord(BlockPos pos, long time) { }

    private enum SwapMode {
        None,
        Swap,
        Silent,
    }

    private enum SwingMode {
        None,
        Client,
        Packet,
    }

}
