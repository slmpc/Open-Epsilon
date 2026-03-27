package com.github.lumin.graphics;

import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Projection;
import net.minecraft.client.renderer.ProjectionMatrixBuffer;
import net.minecraft.client.renderer.rendertype.TextureTransform;
import net.minecraft.client.renderer.state.WindowRenderState;
import org.joml.Vector3f;
import org.joml.Vector4f;

public class LuminRenderSystem {

    public static final Projection guiOrthoProjection =
            new Projection();

    private static final ProjectionMatrixBuffer guiProjectionMatrixBuffer =
            new ProjectionMatrixBuffer("lumin-gui");

    public static void applyOrthoProjection() {
        WindowRenderState windowState = Minecraft.getInstance().gameRenderer.getGameRenderState().windowRenderState;

        guiOrthoProjection
                .setupOrtho(-1000.0F, 1000.0F,
                        (float)windowState.width / windowState.guiScale,
                        (float)windowState.height / windowState.guiScale,
                        true
                );
        RenderSystem.setProjectionMatrix(
                guiProjectionMatrixBuffer.getBuffer(guiOrthoProjection), ProjectionType.ORTHOGRAPHIC);
    }

    public static QuadRenderingInfo prepareQuadRendering(int vertexCount) {
        LuminRenderSystem.applyOrthoProjection();

        RenderTarget target = Minecraft.getInstance().getMainRenderTarget();
        if (target.getColorTextureView() == null) return null;

        final var indexCount = vertexCount / 4 * 6;

        RenderSystem.AutoStorageIndexBuffer autoIndices =
                RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
        GpuBuffer ibo = autoIndices.getBuffer(indexCount);

        GpuBufferSlice dynamicUniforms = RenderSystem.getDynamicUniforms().writeTransform(
                RenderSystem.getModelViewMatrix(),
                new Vector4f(1, 1, 1, 1),
                new Vector3f(0, 0, 0),
                TextureTransform.DEFAULT_TEXTURING.getMatrix()
        );

        return new QuadRenderingInfo(target, autoIndices, ibo, indexCount, dynamicUniforms);
    }

    public record QuadRenderingInfo(
            RenderTarget target,
            RenderSystem.AutoStorageIndexBuffer autoIndices,
            GpuBuffer ibo,
            int indexCount,
            GpuBufferSlice dynamicUniforms
    ) {
    }

}
