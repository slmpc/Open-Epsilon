package com.github.lumin.utils.rotation;

import com.github.lumin.managers.RotationManager;
import com.github.lumin.utils.math.MathUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector2f;
import org.joml.Vector3d;

public class RotationUtils {

    private static final Minecraft mc = Minecraft.getInstance();

    public static Vector2f calculate(final Vector3d from, final Vector3d to) {
        final Vector3d diff = to.sub(from);
        final double distance = Math.hypot(diff.x, diff.z);
        final float yaw = (float) (Mth.atan2(diff.z, diff.x) * MathUtils.TO_DEGREES) - 90.0F;
        final float pitch = (float) (-(Mth.atan2(diff.y, distance) * MathUtils.TO_DEGREES));
        return new Vector2f(yaw, pitch);
    }

    public static boolean isInFov(LivingEntity entity, float fov) {
        if (fov >= 360.0) return true;
        float yawDiff = Math.abs(Mth.wrapDegrees(RotationUtils.getRotationsToEntity(entity).x - mc.player.getYRot()));
        return yawDiff <= fov / 2.0;
    }

    public static Vector2f getRotationsToEntity(LivingEntity entity) {
        Vec3 eyePos = mc.player.getEyePosition();
        Vec3 targetPos = entity.position().add(0, entity.getBbHeight() / 2.0, 0);
        double dx = targetPos.x - eyePos.x;
        double dy = targetPos.y - eyePos.y;
        double dz = targetPos.z - eyePos.z;
        double dist = Math.sqrt(dx * dx + dz * dz);

        float yaw = (float) Math.toDegrees(-Math.atan2(dx, dz));
        float pitch = (float) Math.toDegrees(-Math.atan2(dy, dist));

        return new Vector2f(yaw, Mth.clamp(pitch, -90, 90));
    }

    public static double getEyeDistanceToEntity(LivingEntity entity) {
        Vec3 eyePos = mc.player.getEyePosition();
        AABB box = entity.getBoundingBox();
        double dx = Math.max(box.minX - eyePos.x, Math.max(0, eyePos.x - box.maxX));
        double dy = Math.max(box.minY - eyePos.y, Math.max(0, eyePos.y - box.maxY));
        double dz = Math.max(box.minZ - eyePos.z, Math.max(0, eyePos.z - box.maxZ));
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    public static Vector2f calculate(final Entity entity) {
        return calculate(new Vector3d(entity.getX(), entity.getY(), entity.getZ()).add(0, Math.max(0, Math.min(mc.player.getY() - entity.getY() + mc.player.getEyeHeight(mc.player.getPose()), (entity.getBoundingBox().maxY - entity.getBoundingBox().minY) * 0.9)), 0));
    }

    public static Vector2f calculate(final Entity entity, final boolean adaptive, final double range) {
        Vector2f normalRotations = calculate(entity);
        if (!adaptive || RaytraceUtils.facingEnemy(mc.player, entity, normalRotations, range, 0)) {
            return normalRotations;
        }

        for (double yPercent = 1; yPercent >= 0; yPercent -= 0.25 + Math.random() * 0.1) {
            for (double xPercent = 1; xPercent >= -0.5; xPercent -= 0.5) {
                for (double zPercent = 1; zPercent >= -0.5; zPercent -= 0.5) {
                    Vector2f adaptiveRotations = calculate(new Vector3d(entity.getX(), entity.getY(), entity.getZ()).add(
                            (entity.getBoundingBox().maxX - entity.getBoundingBox().minX) * xPercent,
                            (entity.getBoundingBox().maxY - entity.getBoundingBox().minY) * yPercent,
                            (entity.getBoundingBox().maxZ - entity.getBoundingBox().minZ) * zPercent));

                    if (RaytraceUtils.facingEnemy(mc.player, entity, adaptiveRotations, range, 0)) {
                        return adaptiveRotations;
                    }
                }
            }
        }

        return normalRotations;
    }

    public static Vector2f calculate(final Vec3 to, final Direction direction) {
        return calculate(new Vector3d(to.x, to.y, to.z), direction);
    }

    public static Vector2f calculate(final Vec3 to) {
        return calculate(new Vector3d(mc.player.getX(), mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()), mc.player.getZ()), new Vector3d(to.x, to.y, to.z));
    }

    public static Vector2f calculate(final BlockPos to) {
        return calculate(new Vector3d(mc.player.getX(), mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()), mc.player.getZ()), new Vector3d(to.getX(), to.getY(), to.getZ()).add(0.5, 0.5, 0.5));
    }

    public static Vector2f calculate(final Vector3d to) {
        return calculate(new Vector3d(mc.player.getX(), mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()), mc.player.getZ()), to);
    }

    public static Vector2f calculate(Vector3d position, Direction direction) {
        double x = position.x + 0.5;
        double y = position.y + 0.5;
        double z = position.z + 0.5;

        x += (double) direction.getStepX() * 0.5;
        y += (double) direction.getStepY() * 0.5;
        z += (double) direction.getStepZ() * 0.5;
        return calculate(new Vector3d(x, y, z));
    }

    public static Vector2f calculate(BlockPos position, Direction direction) {
        double x = position.getX() + 0.5D;
        double y = position.getY() + 0.5D;
        double z = position.getZ() + 0.5D;

        x += (double) direction.getUnitVec3i().getX() * 0.5;
        y += (double) direction.getUnitVec3i().getY() * 0.5;
        z += (double) direction.getUnitVec3i().getZ() * 0.5;
        return calculate(new Vec3(x, y, z));
    }

    public static Vector2f applySensitivityPatch(final Vector2f rotation) {
        final Vector2f previousRotation = new Vector2f(mc.player.yRotO, mc.player.xRotO);
        final float mouseSensitivity = (float) (mc.options.sensitivity().get() * (1 + Math.random() / 10000000) * 0.6F + 0.2F);
        final double multiplier = mouseSensitivity * mouseSensitivity * mouseSensitivity * 8.0F * 0.15D;
        final float yaw = previousRotation.x + (float) (Math.round((rotation.x - previousRotation.x) / multiplier) * multiplier);
        final float pitch = previousRotation.y + (float) (Math.round((rotation.y - previousRotation.y) / multiplier) * multiplier);
        return new Vector2f(yaw, Mth.clamp(pitch, -90, 90));
    }

    public static Vector2f applySensitivityPatch(final Vector2f rotation, final Vector2f previousRotation) {
        final float mouseSensitivity = (float) (mc.options.sensitivity().get() * (1 + Math.random() / 10000000) * 0.6F + 0.2F);
        final double multiplier = mouseSensitivity * mouseSensitivity * mouseSensitivity * 8.0F * 0.15D;
        final float yaw = previousRotation.x + (float) (Math.round((rotation.x - previousRotation.x) / multiplier) * multiplier);
        final float pitch = previousRotation.y + (float) (Math.round((rotation.y - previousRotation.y) / multiplier) * multiplier);
        return new Vector2f(yaw, Mth.clamp(pitch, -90, 90));
    }

    public static Vector2f relateToPlayerRotation(final Vector2f rotation) {
        final Vector2f previousRotation = new Vector2f(mc.player.yRotO, mc.player.xRotO);
        final float yaw = previousRotation.x + Mth.wrapDegrees(rotation.x - previousRotation.x);
        final float pitch = Mth.clamp(rotation.y, -90, 90);
        return new Vector2f(yaw, pitch);
    }

    public static Vector2f resetRotation(final Vector2f rotation) {
        if (rotation == null) {
            return null;
        }

        final float yaw = rotation.x + Mth.wrapDegrees(mc.player.getYRot() - rotation.x);
        final float pitch = mc.player.getXRot();
        return new Vector2f(yaw, pitch);
    }

    public static Vector2f move(final Vector2f lastRotation, final Vector2f targetRotation, double speed) {
        if (speed != 0) {

            double deltaYaw = Mth.wrapDegrees(targetRotation.x - lastRotation.x);
            final double deltaPitch = (targetRotation.y - lastRotation.y);

            final double distance = Math.sqrt(deltaYaw * deltaYaw + deltaPitch * deltaPitch);
            if (distance < 1.0E-6) {
                return new Vector2f(0, 0);
            }
            final double distributionYaw = Math.abs(deltaYaw / distance);
            final double distributionPitch = Math.abs(deltaPitch / distance);

            final double maxYaw = speed * distributionYaw;
            final double maxPitch = speed * distributionPitch;

            final float moveYaw = (float) Math.max(Math.min(deltaYaw, maxYaw), -maxYaw);
            final float movePitch = (float) Math.max(Math.min(deltaPitch, maxPitch), -maxPitch);

            return new Vector2f(moveYaw, movePitch);
        }

        return new Vector2f(0, 0);
    }

    public static Vector2f smooth(final Vector2f targetRotation, final double speed) {
        return smooth(RotationManager.INSTANCE.lastRotations, targetRotation, speed);
    }

    public static Vector2f smooth(final Vector2f lastRotation, final Vector2f targetRotation, final double speed) {
        float yaw = targetRotation.x;
        float pitch = targetRotation.y;
        final float lastYaw = lastRotation.x;
        final float lastPitch = lastRotation.y;

        if (speed != 0) {
            Vector2f move = move(lastRotation, targetRotation, speed);

            yaw = lastYaw + move.x;
            pitch = lastPitch + move.y;

            float motion = Math.abs(move.x) + Math.abs(move.y);
            int iterations;
            if (motion < 0.02f) iterations = 1;
            else if (motion < 0.2f) iterations = 2;
            else if (motion < 2.0f) iterations = 3;
            else iterations = 4;

            for (int i = 0; i < iterations; i++) {
                if (motion > 0.0001f) {
                    yaw += (float) MathUtils.getRandom(-0.0006, 0.0006);
                    pitch += (float) MathUtils.getRandom(-0.0035, 0.0035);
                }

                final Vector2f rotations = new Vector2f(yaw, pitch);
                final Vector2f fixedRotations = applySensitivityPatch(rotations);

                yaw = shortestYaw(lastYaw, fixedRotations.x);
                pitch = Math.max(-90, Math.min(90, fixedRotations.y));
            }
        }

        return new Vector2f(yaw, pitch);
    }

    private static float shortestYaw(float from, float to) {
        return from + Mth.wrapDegrees(to - from);
    }

}
