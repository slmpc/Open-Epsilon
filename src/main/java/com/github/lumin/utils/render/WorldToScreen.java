package com.github.lumin.utils.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4d;
import org.joml.Vector4f;

public final class WorldToScreen {

    private static final Minecraft mc = Minecraft.getInstance();

    public static Vector4d getEntityPositionsOn2D(LivingEntity target, float tickDelta) {
        final int[] viewport = new int[]{0, 0, mc.getWindow().getWidth(), mc.getWindow().getHeight()};

        CameraRenderState cameraState = mc.gameRenderer.getGameRenderState().levelRenderState.cameraRenderState;
        Matrix4f viewProjectionMatrix = new Matrix4f(cameraState.projectionMatrix).mul(cameraState.viewRotationMatrix);

        final Vec3 position = interpolate(target, tickDelta);
        final float width = target.getBbWidth() / 2f;
        final float height = target.getBbHeight() + (target.isCrouching() ? 0.1f : 0.2f);

        final AABB boundingBox = new AABB(
                position.x - width, position.y, position.z - width,
                position.x + width, position.y + height, position.z + width
        );

        final Vector4d projection = projectEntity(viewport, viewProjectionMatrix, boundingBox);

        double guiScale = mc.getWindow().getGuiScale();
        projection.x /= guiScale;
        projection.y /= guiScale;
        projection.z /= guiScale;
        projection.w /= guiScale;

        return projection;
    }

    public static Vector4d projectEntity(final int[] viewport, final Matrix4f matrix, final AABB boundingBox) {
        final Vector4f out = new Vector4f();
        Vector4d result = null;

        for (int i = 0; i < 8; i++) {
            Vector3f point = new Vector3f(
                    (i & 1) == 0 ? (float)boundingBox.minX : (float)boundingBox.maxX,
                    (i & 2) == 0 ? (float)boundingBox.minY : (float)boundingBox.maxY,
                    (i & 4) == 0 ? (float)boundingBox.minZ : (float)boundingBox.maxZ
            );

            matrix.project(point, viewport, out);
            out.y = viewport[3] - out.y;

            if (result == null) {
                result = new Vector4d(out.x, out.y, out.x, out.y);
            } else {
                result.x = Math.min(result.x, out.x);
                result.y = Math.min(result.y, out.y);
                result.z = Math.max(result.z, out.x);
                result.w = Math.max(result.w, out.y);
            }
        }
        return result;
    }

    private static Vec3 interpolate(LivingEntity entity, float tickDelta) {
        double x = Mth.lerp(tickDelta, entity.xOld, entity.getX());
        double y = Mth.lerp(tickDelta, entity.yOld, entity.getY());
        double z = Mth.lerp(tickDelta, entity.zOld, entity.getZ());
        return new Vec3(x, y, z);
    }
}