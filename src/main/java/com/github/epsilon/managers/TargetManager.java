package com.github.epsilon.managers;

import com.github.epsilon.modules.impl.combat.AntiBot;
import com.github.epsilon.utils.rotation.RotationUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

public class TargetManager {

    public static final TargetManager INSTANCE = new TargetManager();

    private final Minecraft mc = Minecraft.getInstance();

    private LivingEntity sharedTarget;

    private TargetManager() {
        NeoForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    private void onTick(ClientTickEvent.Pre event) {
        if (mc.player == null || mc.level == null) {
            sharedTarget = null;
            return;
        }

        if (!isSharedTargetAlive()) {
            sharedTarget = null;
        }
    }

    public LivingEntity acquirePrimary(TargetRequest request) {
        List<LivingEntity> targets = acquireTargets(request);
        if (targets.isEmpty()) return null;
        return targets.getFirst();
    }

    public List<LivingEntity> acquireTargets(TargetRequest request) {
        if (mc.player == null || mc.level == null) return List.of();

        List<LivingEntity> candidates = collectTargets(request);
        if (candidates.isEmpty()) return candidates;

        if (!isSharedTargetAlive()) {
            sharedTarget = null;
        }

        if (sharedTarget != null && isValidTarget(sharedTarget, request)) {
            // Keep the same target as the highest priority when it is still valid for this module.
            if (candidates.remove(sharedTarget)) {
                candidates.add(0, sharedTarget);
            }
        } else if (sharedTarget == null) {
            sharedTarget = candidates.getFirst();
        }

        int maxTargets = Math.max(1, request.maxTargets());
        if (candidates.size() > maxTargets) {
            return List.copyOf(candidates.subList(0, maxTargets));
        }
        return List.copyOf(candidates);
    }

    public LivingEntity getSharedTarget() {
        if (!isSharedTargetAlive()) {
            sharedTarget = null;
        }
        return sharedTarget;
    }

    public void clearSharedTarget() {
        sharedTarget = null;
    }

    private List<LivingEntity> collectTargets(TargetRequest request) {
        List<LivingEntity> targets = new ArrayList<>();

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof LivingEntity living)) continue;
            if (living == mc.player) continue;
            if (!isValidTarget(living, request)) continue;
            targets.add(living);
        }

        targets.sort(Comparator.comparingDouble(RotationUtils::getEyeDistanceToEntity));
        return targets;
    }

    private boolean isSharedTargetAlive() {
        if (sharedTarget == null) return false;
        if (sharedTarget == mc.player) return false;
        if (!sharedTarget.isAlive() || sharedTarget.isDeadOrDying()) return false;
        if (sharedTarget.level() != mc.level) return false;
        return !AntiBot.INSTANCE.isBot(sharedTarget);
    }

    private boolean isValidTarget(LivingEntity entity, TargetRequest request) {
        if (!entity.isAlive() || entity.isDeadOrDying()) return false;
        if (AntiBot.INSTANCE.isBot(entity)) return false;

        double dist = RotationUtils.getEyeDistanceToEntity(entity);
        if (dist > request.range()) return false;

        if (request.fov() < 360.0f && !RotationUtils.isInFov(entity, request.fov())) return false;

        switch (entity) {
            case Player player -> {
                if (FriendManager.INSTANCE.isFriend(player)) return false;
                if (!request.player()) return false;
                if (entity.isInvisible() && !request.invisible()) return false;
            }
            case Villager _ -> {
                if (!request.villager()) return false;
            }
            case Animal _ -> {
                if (!request.animal()) return false;
            }
            case Monster _ -> {
                if (!request.mob()) return false;
            }
            default -> {}
        }

        return request.extraFilter().test(entity);
    }

    public record TargetRequest(
            double range,
            float fov,
            boolean player,
            boolean mob,
            boolean animal,
            boolean villager,
            boolean invisible,
            Predicate<LivingEntity> extraFilter,
            int maxTargets
    ) {
        public TargetRequest {
            if (range < 0.0) range = 0.0;
            if (fov < 0.0f) fov = 0.0f;
            if (fov > 360.0f) fov = 360.0f;
            if (extraFilter == null) extraFilter = living -> true;
            if (maxTargets < 1) maxTargets = 1;
        }

        public static TargetRequest of(
                double range,
                float fov,
                boolean player,
                boolean mob,
                boolean animal,
                boolean villager,
                boolean invisible,
                int maxTargets
        ) {
            return new TargetRequest(range, fov, player, mob, animal, villager, invisible, living -> true, maxTargets);
        }

        public static TargetRequest of(
                double range,
                float fov,
                boolean player,
                boolean mob,
                boolean animal,
                boolean villager,
                boolean invisible,
                Predicate<LivingEntity> extraFilter,
                int maxTargets
        ) {
            return new TargetRequest(range, fov, player, mob, animal, villager, invisible, extraFilter, maxTargets);
        }
    }
}



