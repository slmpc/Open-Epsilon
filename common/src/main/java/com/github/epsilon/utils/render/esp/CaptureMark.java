package com.github.epsilon.utils.render.esp;

import com.github.epsilon.assets.resources.ResourceLocationUtils;
import com.github.epsilon.graphics.immediate.LuminImmediateRenderer;
import com.github.epsilon.utils.render.ColorUtils;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.awt.*;

public class CaptureMark {

    private static final Minecraft mc = Minecraft.getInstance();

    private static final Identifier CAPTUREMARK = ResourceLocationUtils.getIdentifier("textures/particles/target.png");

    private static final RenderPipeline TARGET_ICON_PIPELINE = RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
            .withLocation("pipeline/target_icon")
            .withColorTargetState(ColorTargetState.DEFAULT)
            .withCull(false)
            .build();

    public static void render(PoseStack poseStack, LivingEntity target, double espSize, double rotSpeed, double waveSpeed, Color color1, Color color2) {
        float rotation = (float) (System.currentTimeMillis() / 1000.0 * rotSpeed) % 360f;

        Vec3 cam = mc.getEntityRenderDispatcher().camera.position();
        float partialTick = mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);

        double ex = Mth.lerp(partialTick, target.xOld, target.getX()) - cam.x;
        double ey = Mth.lerp(partialTick, target.yOld, target.getY()) - cam.y;
        double ez = Mth.lerp(partialTick, target.zOld, target.getZ()) - cam.z;

        float entityHeight = target.getBbHeight();
        float size = (float) espSize * 0.5f;

        poseStack.pushPose();
        poseStack.translate(ex, ey + entityHeight * 0.5, ez);

        Camera camera = mc.gameRenderer.mainCamera();
        poseStack.mulPose(Axis.YP.rotationDegrees(-camera.yRot()));
        poseStack.mulPose(Axis.XP.rotationDegrees(camera.xRot()));
        poseStack.mulPose(Axis.ZP.rotationDegrees(rotation));

        Matrix4f matrix = poseStack.last().pose();
        LuminImmediateRenderer.PosTexColorQuads builder = LuminImmediateRenderer.beginPosTexColorQuads(TARGET_ICON_PIPELINE, CAPTUREMARK);

        Color c1 = getColorForProgress(0, waveSpeed, color1, color2);
        Color c2 = getColorForProgress(0.25f, waveSpeed, color1, color2);
        Color c3 = getColorForProgress(0.5f, waveSpeed, color1, color2);
        Color c4 = getColorForProgress(0.75f, waveSpeed, color1, color2);

        builder.vertex(matrix, -size, -size, 0, 0, 0, c1.getRGB());
        builder.vertex(matrix, -size, size, 0, 0, 1, c2.getRGB());
        builder.vertex(matrix, size, size, 0, 1, 1, c3.getRGB());
        builder.vertex(matrix, size, -size, 0, 1, 0, c4.getRGB());

        builder.end();

        poseStack.popPose();
    }

    private static Color getColorForProgress(float progress, double waveSpeed, Color color1, Color color2) {
        float wave = (float) Math.sin((progress * Math.PI * 2) + (System.currentTimeMillis() / 1000f * (float) waveSpeed));
        wave = (wave + 1f) / 2f;
        return ColorUtils.interpolateColor(color1, color2, wave);
    }

}
