package com.github.epsilon.utils.render.esp;

import com.github.epsilon.assets.resources.ResourceLocationUtils;
import com.github.epsilon.graphics.immediate.LuminImmediateRenderer;
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
import org.joml.Matrix4f;

import java.awt.*;

public class Firefly {

    private static final Minecraft mc = Minecraft.getInstance();

    private static final Identifier FIREFLY_TEX = ResourceLocationUtils.getIdentifier("textures/particles/firefly.png");

    private static final RenderPipeline fireflyPipeline = RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
            .withLocation("pipeline/firefly")
            .withColorTargetState(ColorTargetState.DEFAULT)
            .withCull(false)
            .build();

    public static void render(PoseStack matrices, LivingEntity target, int espLength, int factor, double shaking, double amplitude, Color color) {
        Camera camera = mc.gameRenderer.mainCamera();
        float tickDelta = mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);

        double tPosX = Mth.lerp(tickDelta, target.xOld, target.getX()) - camera.position().x;
        double tPosY = Mth.lerp(tickDelta, target.yOld, target.getY()) - camera.position().y;
        double tPosZ = Mth.lerp(tickDelta, target.zOld, target.getZ()) - camera.position().z;
        float iAge = (float) (target.tickCount - 1) + tickDelta;

        LuminImmediateRenderer.PosTexColorQuads builder = LuminImmediateRenderer.beginPosTexColorQuads(fireflyPipeline, FIREFLY_TEX);

        for (int j = 0; j < 3; j++) {
            for (int i = 0; i <= espLength; i++) {
                double radians = Math.toRadians((((float) i / 1.5f + iAge) * factor + (j * 120)) % (factor * 360));
                double sinQuad = Math.sin(Math.toRadians(iAge * 2.5f + i * (j + 1)) * amplitude) / shaking;

                float offset = (float) i / (float) espLength;

                matrices.pushPose();
                matrices.translate(tPosX + Math.cos(radians) * target.getBbWidth(), tPosY + target.getBbHeight() * 0.5 + sinQuad, tPosZ + Math.sin(radians) * target.getBbWidth());
                matrices.mulPose(Axis.YP.rotationDegrees(-camera.yRot()));
                matrices.mulPose(Axis.XP.rotationDegrees(camera.xRot()));

                Matrix4f matrix = matrices.last().pose();
                float scale = Math.max(0.24f * (offset), 0.2f);

                int renderColor = color.getRGB();

                builder.vertex(matrix, -scale, scale, 0, 0f, 1f, renderColor);
                builder.vertex(matrix, scale, scale, 0, 1f, 1f, renderColor);
                builder.vertex(matrix, scale, -scale, 0, 1f, 0f, renderColor);
                builder.vertex(matrix, -scale, -scale, 0, 0f, 0f, renderColor);

                matrices.popPose();
            }
        }

        builder.end();
    }

}
