package com.github.epsilon.modules.impl.combat;

import com.github.epsilon.graphics.renderers.TextRenderer;
import com.github.epsilon.managers.RenderManager;
import com.github.epsilon.managers.RotationManager;
import com.github.epsilon.managers.TargetManager;
import com.github.epsilon.modules.Category;
import com.github.epsilon.modules.Module;
import com.github.epsilon.settings.impl.*;
import com.github.epsilon.utils.combat.DamageUtils;
import com.github.epsilon.utils.player.FindItemResult;
import com.github.epsilon.utils.player.InvUtils;
import com.github.epsilon.utils.render.Render3DUtils;
import com.github.epsilon.utils.render.WorldToScreen;
import com.github.epsilon.utils.rotation.Priority;
import com.github.epsilon.utils.rotation.RaytraceUtils;
import com.github.epsilon.utils.rotation.RotationUtils;
import com.github.epsilon.utils.timer.TimerUtils;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffects;
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
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector4d;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CrystalAura extends Module {

    public static final CrystalAura INSTANCE = new CrystalAura();

    private CrystalAura() {
        super("CrystalAura", Category.COMBAT);
    }

    // General
    private final DoubleSetting targetRange = doubleSetting("Target Range", 6.0, 0.0, 12.0, 0.5);
    private final EnumSetting<AimOptimizeMode> aimOptimizeMode = enumSetting("Aim Optimize Mode", AimOptimizeMode.MaxDamage);
    private final DoubleSetting aimDamageSacrifice = doubleSetting("Aim Damage Sacrifice", 1.0, 0.0, 10.0, 0.25,
            () -> aimOptimizeMode.is(AimOptimizeMode.LowRotation));
    private final BoolSetting eatingPause = boolSetting("Eating Pause", false);

    // Calculation
    private final BoolSetting noSuicide = boolSetting("No Suicide", true);
    private final DoubleSetting lethalMaxSelfDamage = doubleSetting("Lethal Max Self Dmg", 8.0, 0.0, 36.0, 0.25);
    private final BoolSetting motionPrediction = boolSetting("Motion Prediction", false);
    private final IntSetting predictTick = intSetting("Predict Tick", 6, 0, 10, 1, motionPrediction::getValue);
    private final EnumSetting<DamageUtils.ArmorEnchantmentMode> armorMode = enumSetting("Armor Mode", DamageUtils.ArmorEnchantmentMode.PPBP);
    private final BoolSetting armorForSelf = boolSetting("Armor Mode For Self", false, () -> !armorMode.is(DamageUtils.ArmorEnchantmentMode.None));

    // Force Place
    private final DoubleSetting forcePlaceHealth = doubleSetting("Force Place Health", 8.0, 0.0, 36.0, 0.5);
    private final IntSetting forcePlaceArmorRate = intSetting("Force Place Armor Rate", 3, 0, 25, 1);
    private final DoubleSetting forcePlaceMinDamage = doubleSetting("Force Place Min Dmg", 2.0, 0.0, 20.0, 0.25);
    private final DoubleSetting forcePlaceBalance = doubleSetting("Force Place Balance", -3.0, -10.0, 10.0, 0.25);

    // Place
    private final EnumSetting<SwingMode> placeSwing = enumSetting("Place Swing", SwingMode.None);
    private final DoubleSetting placeRotationSpeed = doubleSetting("Place Rotation Speed", 10.0, 1.0, 10.0, 0.5);
    private final EnumSetting<SwapMode> placeSwapMode = enumSetting("Place Swap Mode", SwapMode.None);
    private final DoubleSetting placeMinDmg = doubleSetting("Place Min Dmg", 6.0, 0.0, 20.0, 0.25);
    private final DoubleSetting placeMaxSelfDmg = doubleSetting("Place Max Self Dmg", 10.0, 0.0, 36.0, 0.25);
    private final DoubleSetting placeBalance = doubleSetting("Place Balance", -3.0, -10.0, 10.0, 0.25);
    private final IntSetting placeDelay = intSetting("Place Delay", 50, 0, 1000, 10);
    private final DoubleSetting placeRange = doubleSetting("Place Range", 4.0, 1.0, 6.0, 0.1);
    private final DoubleSetting placeWallRange = doubleSetting("Place Wall Range", 3.0, 0.0, 6.0, 0.1);

    // Break
    private final EnumSetting<SwingMode> breakSwing = enumSetting("Break Swing", SwingMode.None);
    private final DoubleSetting breakRotationSpeed = doubleSetting("Break Rotation Speed", 10.0, 1.0, 10.0, 0.5);
    private final BoolSetting antiWeak = boolSetting("Anti Weak", false);
    private final EnumSetting<SwapMode> antiWeakSwapMode = enumSetting("Anti Weak Swap Mode", SwapMode.Silent, antiWeak::getValue);
    private final DoubleSetting breakMinDmg = doubleSetting("Break Min Dmg", 6.0, 0.0, 20.0, 0.25);
    private final DoubleSetting breakMaxSelfDmg = doubleSetting("Break Max Self Dmg", 10.0, 0.0, 36.0, 0.25);
    private final DoubleSetting breakBalance = doubleSetting("Break Balance", -3.0, -10.0, 10.0, 0.25);
    private final IntSetting breakDelay = intSetting("Break Delay", 50, 0, 1000, 10);
    private final DoubleSetting breakRange = doubleSetting("Break Range", 4.0, 1.0, 6.0, 0.1);

    // Render
    private final BoolSetting targetDamage = boolSetting("Target Damage", true);
    private final BoolSetting selfDamage = boolSetting("Self Damage", false);
    private final ColorSetting filledColor = colorSetting("Filled Color", new Color(255, 150, 120, 100));
    private final ColorSetting outlineColor = colorSetting("Outline Color", new Color(255, 150, 120, 170));
    private final DoubleSetting outlineWidth = doubleSetting("Outline Width", 3.0, 1.0, 10.0, 0.5);
    private final IntSetting movingLength = intSetting("Moving Length", 400, 0, 1000, 50);
    private final IntSetting fadeLength = intSetting("Fade Length", 200, 0, 1000, 50);

    private LivingEntity target;
    private final TimerUtils placeTimer = new TimerUtils();
    private final TimerUtils breakTimer = new TimerUtils();

    private BlockPos renderBlockPos;
    private Vec3 renderPrevPos;
    private Vec3 renderCurrentPos;
    private Vec3 renderLastRenderedPos;
    private long renderMoveStartTime;
    private long renderFadeStartTime;
    private float renderScale;
    private float renderDamage;
    private float renderSelfDamage;
    private boolean renderHasTarget;

    private final Supplier<TextRenderer> rectRenderer = Suppliers.memoize(() -> new TextRenderer(128 * 1024));

    @Override
    protected void onEnable() {
        placeTimer.reset();
        breakTimer.reset();
        resetRenderState();
    }

    @Override
    protected void onDisable() {
        target = null;
        resetRenderState();
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
            deactivateRenderTarget();
            return;
        }

        Vec3 predictedPos = getPredictedTargetPos(target);

        tryBreakCrystal();
        tryPlaceCrystal(predictedPos);
    }

    private void tryBreakCrystal() {
        if (!breakTimer.passedMillise(breakDelay.getValue())) return;

        List<BreakCandidate> candidates = new ArrayList<>();

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof EndCrystal crystal)) continue;
            if (!crystal.isAlive()) continue;

            double distToPlayer = mc.player.distanceTo(crystal);
            if (distToPlayer > breakRange.getValue()) continue;

            Vec3 crystalPos = crystal.position();

            float targetDmg = DamageUtils.crystalDamage(target, crystalPos, armorMode.getValue());
            float selfDmg = DamageUtils.selfCrystalDamage(crystalPos, armorForSelf.getValue() ? armorMode.getValue() : DamageUtils.ArmorEnchantmentMode.None);
            if (exceedsSelfDamageLimit(selfDmg, breakMaxSelfDmg.getValue())) continue;
            if (targetDmg < breakMinDmg.getValue()) continue;
            float balance = targetDmg - selfDmg;
            if (balance < breakBalance.getValue()) continue;

            Vector2f targetRotation = RotationUtils.calculate(crystal);
            float rotationDelta = getRotationDelta(getPrevTickRotation(), targetRotation);
            candidates.add(new BreakCandidate(crystal, targetDmg, targetRotation, rotationDelta));
        }

        EndCrystal bestCrystal = selectBestBreakCandidate(candidates);
        if (bestCrystal != null) {
            doBreakCrystal(bestCrystal);
        }
    }

    private void doBreakCrystal(EndCrystal crystal) {
        if (antiWeak.getValue() && mc.player.hasEffect(MobEffects.WEAKNESS)) {
            FindItemResult sword = InvUtils.findInHotbar(Items.DIAMOND_SWORD, Items.NETHERITE_SWORD, Items.IRON_SWORD, Items.STONE_SWORD);
            if (sword.found()) {
                switch (antiWeakSwapMode.getValue()) {
                    case Swap -> InvUtils.swap(sword.slot(), false);
                    case Silent -> InvUtils.swap(sword.slot(), true);
                    default -> {
                    }
                }
            }
        }

        final int requestPriority = Priority.High.priority;
        final int crystalId = crystal.getId();
        final Vector2f rotation = RotationUtils.calculate(crystal);

        RotationManager.INSTANCE.applyRotation(rotation, breakRotationSpeed.getValue(), requestPriority, record -> {
            if (!isEnabled() || nullCheck()) {
                InvUtils.swapBack();
                return;
            }
            if (record.selectedPriorityValue() != requestPriority) return;

            Entity current = mc.level.getEntity(crystalId);
            if (!(current instanceof EndCrystal currentCrystal) || !currentCrystal.isAlive()) {
                InvUtils.swapBack();
                return;
            }

            if (!RaytraceUtils.facingEnemy(currentCrystal, breakRange.getValue(), record.currentRotation())) {
                InvUtils.swapBack();
                return;
            }

            mc.gameMode.attack(mc.player, currentCrystal);
            doSwing(breakSwing.getValue());
            breakTimer.reset();
            Vec3 crystalPosition = currentCrystal.position();
            float tgtDmg = DamageUtils.crystalDamage(target, crystalPosition, armorMode.getValue());
            float selfDmg = DamageUtils.selfCrystalDamage(crystalPosition, armorForSelf.getValue() ? armorMode.getValue() : DamageUtils.ArmorEnchantmentMode.None);
            updateRenderTarget(currentCrystal.blockPosition().below(), tgtDmg, selfDmg);

            InvUtils.swapBack();
        });
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
        double wallRangeSq = placeWallRange.getValue() * placeWallRange.getValue();
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
                    double distanceSq = playerEye.distanceToSqr(supportCenter);
                    if (distanceSq > placeRangeSq) continue;
                    Vec3 crystalPos = new Vec3(
                            supportPos.getX() + 0.5,
                            supportPos.getY() + 1.0,
                            supportPos.getZ() + 0.5
                    );

                    float targetDmg = DamageUtils.crystalDamage(target, crystalPos, armorMode.getValue());
                    float selfDmg = DamageUtils.selfCrystalDamage(crystalPos, armorForSelf.getValue() ? armorMode.getValue() : DamageUtils.ArmorEnchantmentMode.None);
                    Vector2f targetRotation = RotationUtils.calculate(supportPos, Direction.UP);
                    boolean visible = RaytraceUtils.overBlock(targetRotation, Direction.UP, supportPos, false);
                    boolean wallBypassAllowed = !visible && distanceSq <= wallRangeSq;
                    if (!visible && !wallBypassAllowed) continue;

                    candidates.add(new PlaceCandidate(
                            supportPos,
                            crystalPos,
                            targetDmg,
                            selfDmg,
                            targetRotation,
                            !visible,
                            wallBypassAllowed
                    ));
                }
            }
        }

        return candidates;
    }

    private PlaceCandidate findBestCandidate(List<PlaceCandidate> candidates,
                                             float minDmg, float maxSelfDmg, float minBalance) {
        List<PlaceCandidate> visibleValid = new ArrayList<>();
        List<PlaceCandidate> wallValid = new ArrayList<>();

        for (PlaceCandidate c : candidates) {
            if (exceedsSelfDamageLimit(c.selfDmg, maxSelfDmg)) continue;
            if (c.targetDmg < minDmg) continue;
            float balance = c.targetDmg - c.selfDmg;
            if (balance < minBalance) continue;
            if (c.throughWall) {
                if (c.wallBypassAllowed) {
                    wallValid.add(c);
                }
            } else {
                visibleValid.add(c);
            }
        }

        List<PlaceCandidate> valid = !visibleValid.isEmpty() ? visibleValid : wallValid;

        if (valid.isEmpty()) return null;

        float bestDamage = Float.MIN_VALUE;
        for (PlaceCandidate c : valid) {
            if (c.targetDmg > bestDamage) {
                bestDamage = c.targetDmg;
            }
        }

        if (aimOptimizeMode.is(AimOptimizeMode.MaxDamage)) {
            PlaceCandidate best = null;
            float bestScore = Float.MIN_VALUE;
            for (PlaceCandidate c : valid) {
                if (c.targetDmg > bestScore) {
                    bestScore = c.targetDmg;
                    best = c;
                }
            }
            return best;
        }

        float threshold = bestDamage - aimDamageSacrifice.getValue().floatValue();
        PlaceCandidate selected = null;
        float bestDelta = Float.MAX_VALUE;
        Vector2f prev = getPrevTickRotation();
        for (PlaceCandidate c : valid) {
            if (c.targetDmg < threshold) continue;
            float delta = getRotationDelta(prev, c.targetRotation);
            if (delta < bestDelta) {
                bestDelta = delta;
                selected = c;
            }
        }

        return selected != null ? selected : valid.get(0);
    }

    private void doPlaceCrystal(PlaceCandidate candidate, FindItemResult crystalItem) {
        InteractionHand hand = crystalItem.getHand();
        if (crystalItem.slot() != mc.player.getInventory().getSelectedSlot() && crystalItem.slot() != 40) {
            switch (placeSwapMode.getValue()) {
                case Swap -> InvUtils.swap(crystalItem.slot(), false);
                case Silent -> InvUtils.swap(crystalItem.slot(), true);
                default -> {
                }
            }
            hand = InteractionHand.MAIN_HAND;
        }

        final InteractionHand placeHand = hand;
        final int requestPriority = Priority.High.priority;
        final Vector2f rotation = candidate.targetRotation;
        final Vec3 hitVec = new Vec3(
                candidate.supportPos.getX() + 0.5,
                candidate.supportPos.getY() + 1.0,
                candidate.supportPos.getZ() + 0.5
        );
        RotationManager.INSTANCE.applyRotation(rotation, placeRotationSpeed.getValue(), requestPriority, record -> {
            if (!isEnabled() || nullCheck()) {
                InvUtils.swapBack();
                return;
            }
            if (record.selectedPriorityValue() != requestPriority) return;
            boolean canPlace = candidate.throughWall
                    ? candidate.wallBypassAllowed && isAimingAtBlock(record.currentRotation(), candidate.targetRotation)
                    : RaytraceUtils.overBlock(record.currentRotation(), Direction.UP, candidate.supportPos, false);
            if (!canPlace) {
                InvUtils.swapBack();
                return;
            }

            BlockHitResult hitResult = new BlockHitResult(hitVec, Direction.UP, candidate.supportPos, false);
            InteractionResult result = mc.gameMode.useItemOn(mc.player, placeHand, hitResult);
            if (result.consumesAction()) {
                doSwing(placeSwing.getValue());
                updateRenderTarget(candidate.supportPos, candidate.targetDmg, candidate.selfDmg);
                placeTimer.reset();
            }

            InvUtils.swapBack();
        });
    }

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
        if (!motionPrediction.getValue()) {
            return entity.position();
        }

        double motionX = Mth.clamp(entity.getX() - entity.xo, -0.6, 0.6);
        double motionY = Mth.clamp(entity.getY() - entity.yo, -0.5, 0.5);
        double motionZ = Mth.clamp(entity.getZ() - entity.zo, -0.6, 0.6);

        AABB entityBox = entity.getBoundingBox();
        AABB targetBox = entityBox;
        int ticks = predictTick.getValue();

        for (int tick = 0; tick <= ticks; tick++) {
            AABB moved = canMove(entity, targetBox, motionX, motionY, motionZ);
            if (moved == null) moved = canMove(entity, targetBox, motionX, 0.0, motionZ);
            if (moved == null) moved = canMove(entity, targetBox, 0.0, motionY, 0.0);
            if (moved == null) break;
            targetBox = moved;
        }

        double offsetX = targetBox.minX - entityBox.minX;
        double offsetY = targetBox.minY - entityBox.minY;
        double offsetZ = targetBox.minZ - entityBox.minZ;
        return entity.position().add(offsetX, offsetY, offsetZ);
    }

    private AABB canMove(Entity entity, AABB box, double motionX, double motionY, double motionZ) {
        AABB moved = box.move(motionX, motionY, motionZ);
        return mc.level.noCollision(entity, moved) ? moved : null;
    }

    private boolean isAimingAtBlock(Vector2f currentRotation, Vector2f targetRotation) {
        return getRotationDelta(currentRotation, targetRotation) <= 2.5f;
    }

    private FindItemResult findCrystalItem() {
        return InvUtils.findInHotbar(Items.END_CRYSTAL);
    }

    private void doSwing(SwingMode mode) {
        switch (mode) {
            case Client -> mc.player.swing(InteractionHand.MAIN_HAND);
            case Packet -> mc.getConnection().send(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
            default -> {
            }
        }
    }

    private void updateRenderTarget(BlockPos pos, float damage, float selfDmg) {
        long now = System.currentTimeMillis();
        if (!pos.equals(renderBlockPos)) {
            renderCurrentPos = Vec3.atCenterOf(pos);
            renderPrevPos = renderLastRenderedPos != null ? renderLastRenderedPos : renderCurrentPos;
            renderMoveStartTime = now;
            if (renderBlockPos == null) {
                renderFadeStartTime = now;
            }
            renderBlockPos = pos;
        }
        renderHasTarget = true;
        renderDamage = damage;
        renderSelfDamage = selfDmg;
    }

    private void deactivateRenderTarget() {
        if (renderHasTarget) {
            renderHasTarget = false;
            renderFadeStartTime = System.currentTimeMillis();
        }
    }

    private void resetRenderState() {
        renderBlockPos = null;
        renderPrevPos = null;
        renderCurrentPos = null;
        renderLastRenderedPos = null;
        renderMoveStartTime = 0L;
        renderFadeStartTime = 0L;
        renderScale = 0.0f;
        renderDamage = 0.0f;
        renderSelfDamage = 0.0f;
        renderHasTarget = false;
    }

    private static float easeOutQuart(float t) {
        float u = 1.0f - t;
        return 1.0f - u * u * u * u;
    }

    private static float easeOutCubic(float t) {
        float u = 1.0f - t;
        return 1.0f - u * u * u;
    }

    private static float easeInCubic(float t) {
        return t * t * t;
    }

    private static float toDelta(long startTime, int lengthMs) {
        long elapsed = System.currentTimeMillis() - startTime;
        return Math.min(1.0f, Math.max(0.0f, (float) elapsed / Math.max(1, lengthMs)));
    }

    @SubscribeEvent
    private void onRender3D(RenderLevelStageEvent.AfterLevel event) {
        if (nullCheck()) return;
        if (renderPrevPos == null || renderCurrentPos == null) return;

        float moveDelta = toDelta(renderMoveStartTime, movingLength.getValue());
        float moveMultiplier = easeOutQuart(moveDelta);
        Vec3 renderPos = renderPrevPos.add(
                renderCurrentPos.subtract(renderPrevPos).scale(moveMultiplier)
        );

        float fadeDelta = toDelta(renderFadeStartTime, fadeLength.getValue());
        if (renderHasTarget) {
            renderScale = easeOutCubic(fadeDelta);
        } else {
            renderScale = 1.0f - easeInCubic(fadeDelta);
        }

        if (renderScale <= 0.01f) return;

        double halfSize = 0.5 * renderScale;
        AABB box = new AABB(
                renderPos.x - halfSize, renderPos.y - halfSize, renderPos.z - halfSize,
                renderPos.x + halfSize, renderPos.y + halfSize, renderPos.z + halfSize
        );

        Color fc = filledColor.getValue();
        Color oc = outlineColor.getValue();

        Color filled = new Color(fc.getRed(), fc.getGreen(), fc.getBlue(),
                Math.max(0, Math.min(255, (int) (fc.getAlpha() * renderScale))));
        Color outline = new Color(oc.getRed(), oc.getGreen(), oc.getBlue(),
                Math.max(0, Math.min(255, (int) (oc.getAlpha() * renderScale))));

        Render3DUtils.drawFilledBox(box, filled);
        Render3DUtils.drawOutlineBox(event.getPoseStack(), box, outline.getRGB(), outlineWidth.getValue().floatValue());

        renderLastRenderedPos = renderPos;

        if (!targetDamage.getValue() && !selfDamage.getValue()) return;

        Vector2f screenPos = projectToScreen(renderPos);
        if (screenPos == null) return;

        StringBuilder sb = new StringBuilder();
        if (targetDamage.getValue()) sb.append(String.format(Locale.ROOT, "%.1f", renderDamage));
        if (selfDamage.getValue()) {
            if (!sb.isEmpty()) sb.append('/');
            sb.append(String.format(Locale.ROOT, "%.1f", renderSelfDamage));
        }
        String text = sb.toString();

        TextRenderer textRenderer = rectRenderer.get();
        float textScale = 1.0f;
        float textWidth = textRenderer.getWidth(text, textScale);
        float textHeight = textRenderer.getHeight(textScale);
        float textX = screenPos.x - textWidth / 2.0f;
        float textY = screenPos.y - textHeight / 2.0f;
        float screenWidth = mc.getWindow().getGuiScaledWidth();
        float screenHeight = mc.getWindow().getGuiScaledHeight();
        if (textX + textWidth < 0.0f || textY + textHeight < 0.0f || textX > screenWidth || textY > screenHeight) {
            return;
        }
        Color textColor = new Color(255, 255, 255, Math.max(0, Math.min(255, (int) (220 * renderScale))));

        RenderManager.INSTANCE.applyRenderWorldHud(() -> {
            TextRenderer tr = rectRenderer.get();
            tr.addText(text, textX, textY, textScale, textColor);
            tr.drawAndClear();
        });
    }

    private Vector2f projectToScreen(Vec3 pos) {
        int[] viewport = new int[]{0, 0, mc.getWindow().getWidth(), mc.getWindow().getHeight()};
        CameraRenderState cameraState = mc.gameRenderer.getGameRenderState().levelRenderState.cameraRenderState;
        Matrix4f viewProjectionMatrix = new Matrix4f(cameraState.projectionMatrix).mul(cameraState.viewRotationMatrix);

        AABB box = new AABB(
                pos.x - 0.5, pos.y - 0.5, pos.z - 0.5,
                pos.x + 0.5, pos.y + 0.5, pos.z + 0.5
        );
        Vector4d projected = WorldToScreen.projectEntity(viewport, viewProjectionMatrix, box);
        if (projected == null) return null;

        double guiScale = mc.getWindow().getGuiScale();
        double minX = projected.x / guiScale;
        double minY = projected.y / guiScale;
        double maxX = projected.z / guiScale;
        double maxY = projected.w / guiScale;

        float screenWidth = mc.getWindow().getGuiScaledWidth();
        float screenHeight = mc.getWindow().getGuiScaledHeight();

        if (maxX < 0 || maxY < 0 || minX > screenWidth || minY > screenHeight) return null;
        float centerX = (float) ((minX + maxX) * 0.5);
        float centerY = (float) ((minY + maxY) * 0.5);
        if (centerX < 0.0f || centerY < 0.0f || centerX > screenWidth || centerY > screenHeight) return null;
        return new Vector2f(centerX, centerY);
    }

    private record PlaceCandidate(
            BlockPos supportPos,
            Vec3 crystalPos,
            float targetDmg,
            float selfDmg,
            Vector2f targetRotation,
            boolean throughWall,
            boolean wallBypassAllowed
    ) {
    }

    private EndCrystal selectBestBreakCandidate(List<BreakCandidate> candidates) {
        if (candidates.isEmpty()) return null;

        float bestDamage = Float.MIN_VALUE;
        BreakCandidate maxDamage = null;
        for (BreakCandidate c : candidates) {
            if (c.targetDmg > bestDamage) {
                bestDamage = c.targetDmg;
                maxDamage = c;
            }
        }

        if (maxDamage == null || aimOptimizeMode.is(AimOptimizeMode.MaxDamage)) {
            return maxDamage != null ? maxDamage.crystal : null;
        }

        float threshold = bestDamage - aimDamageSacrifice.getValue().floatValue();
        BreakCandidate selected = null;
        float bestDelta = Float.MAX_VALUE;
        for (BreakCandidate c : candidates) {
            if (c.targetDmg < threshold) continue;
            if (c.rotationDelta < bestDelta) {
                bestDelta = c.rotationDelta;
                selected = c;
            }
        }

        return selected != null ? selected.crystal : maxDamage.crystal;
    }

    private Vector2f getPrevTickRotation() {
        Vector2f prev = RotationManager.INSTANCE.lastRotations;
        if (prev == null) {
            return new Vector2f(mc.player.getYRot(), mc.player.getXRot());
        }
        return new Vector2f(prev.x, prev.y);
    }

    private float getRotationDelta(Vector2f from, Vector2f to) {
        float yawDiff = Math.abs(Mth.wrapDegrees(to.x - from.x));
        float pitchDiff = Math.abs(to.y - from.y);
        return (float) Math.hypot(yawDiff, pitchDiff);
    }

    private record BreakCandidate(
            EndCrystal crystal,
            float targetDmg,
            Vector2f targetRotation,
            float rotationDelta
    ) {
    }


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

    private enum AimOptimizeMode {
        MaxDamage,
        LowRotation
    }

}
