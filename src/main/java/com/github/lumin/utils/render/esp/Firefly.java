package com.github.lumin.utils.render.esp;

import com.github.lumin.assets.resources.ResourceLocationUtils;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.LayeringTransform;
import net.minecraft.client.renderer.rendertype.OutputTarget;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
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
            .withBlend(BlendFunction.LIGHTNING)
            .withCull(false)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .build();

    private static final RenderType fireflyLayer = RenderType.create("firefly_layer", RenderSetup.builder(fireflyPipeline)
            .withTexture("Sampler0", FIREFLY_TEX)
            .sortOnUpload()
            .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
            .setOutputTarget(OutputTarget.MAIN_TARGET).createRenderSetup()
    );

    public static void render(PoseStack matrices, LivingEntity target, int espLength, int factor, double shaking, double amplitude, Color color) {
        Camera camera = mc.gameRenderer.getMainCamera();
        float tickDelta = mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);

        double tPosX = Mth.lerp(tickDelta, target.xOld, target.getX()) - camera.position().x;
        double tPosY = Mth.lerp(tickDelta, target.yOld, target.getY()) - camera.position().y;
        double tPosZ = Mth.lerp(tickDelta, target.zOld, target.getZ()) - camera.position().z;
        float iAge = (float) (target.tickCount - 1) + tickDelta;

        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);

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

                buffer.addVertex(matrix, -scale, scale, 0).setUv(0f, 1f).setColor(renderColor);
                buffer.addVertex(matrix, scale, scale, 0).setUv(1f, 1f).setColor(renderColor);
                buffer.addVertex(matrix, scale, -scale, 0).setUv(1f, 0f).setColor(renderColor);
                buffer.addVertex(matrix, -scale, -scale, 0).setUv(0f, 0f).setColor(renderColor);

                matrices.popPose();
            }
        }

        fireflyLayer.draw(buffer.buildOrThrow());
    }

}
