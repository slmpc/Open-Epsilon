package com.github.epsilon.graphics;

import com.github.epsilon.assets.resources.ResourceLocationUtils;
import com.mojang.blaze3d.pipeline.BindGroupLayout;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.RenderPipelines;

import java.util.List;
import java.util.function.Consumer;


public class LuminRenderPipelines {

    private final static BindGroupLayout TTF_INFO_UBO = BindGroupLayout.builder()
            .withUniform("TtfInfo", UniformType.UNIFORM_BUFFER)
            .build();

    private final static RenderPipeline.Snippet NO_BLEND_DEPTH_SNIPPET = RenderPipeline.builder()
            .withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION)
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .buildSnippet();

    public final static RenderPipeline RECTANGLE = RenderPipeline.builder(NO_BLEND_DEPTH_SNIPPET)
            .withLocation(ResourceLocationUtils.getIdentifier("pipelines/rectangle"))
            .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS)
            .withVertexShader(ResourceLocationUtils.getIdentifier("rectangle"))
            .withFragmentShader(ResourceLocationUtils.getIdentifier("rectangle"))
            .withCull(false)
            .build();

    private final static RenderPipeline.Snippet TTF_SNIPPET = RenderPipeline.builder(NO_BLEND_DEPTH_SNIPPET)
            .withBindGroupLayout(TTF_INFO_UBO)
            .buildSnippet();

    public final static RenderPipeline TTF_FONT = RenderPipeline.builder(TTF_SNIPPET)
            .withLocation(ResourceLocationUtils.getIdentifier("pipelines/ttf_font"))
            .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS)
            .withVertexShader(ResourceLocationUtils.getIdentifier("ttf_font"))
            .withFragmentShader(ResourceLocationUtils.getIdentifier("ttf_font"))
            .withBindGroupLayout(BindGroupLayouts.SAMPLER0)
            .withCull(false)
            .build();

    public final static RenderPipeline ROUND_RECT = RenderPipeline.builder(NO_BLEND_DEPTH_SNIPPET)
            .withLocation(ResourceLocationUtils.getIdentifier("pipelines/round_rectangle"))
            .withVertexFormat(LuminVertexFormats.ROUND_RECT, VertexFormat.Mode.QUADS)
            .withVertexShader(ResourceLocationUtils.getIdentifier("round_rectangle"))
            .withFragmentShader(ResourceLocationUtils.getIdentifier("round_rectangle"))
            .withCull(false)
            .build();

    public final static RenderPipeline SHADOW = RenderPipeline.builder(NO_BLEND_DEPTH_SNIPPET)
            .withLocation(ResourceLocationUtils.getIdentifier("pipelines/shadow"))
            .withVertexFormat(LuminVertexFormats.ROUND_RECT, VertexFormat.Mode.QUADS)
            .withVertexShader(ResourceLocationUtils.getIdentifier("shadow"))
            .withFragmentShader(ResourceLocationUtils.getIdentifier("shadow"))
            .withCull(false)
            .build();

    public final static RenderPipeline TEXTURE = RenderPipeline.builder(NO_BLEND_DEPTH_SNIPPET)
            .withLocation(ResourceLocationUtils.getIdentifier("pipelines/texture"))
            .withVertexFormat(LuminVertexFormats.TEXTURE, VertexFormat.Mode.QUADS)
            .withVertexShader(ResourceLocationUtils.getIdentifier("texture"))
            .withFragmentShader(ResourceLocationUtils.getIdentifier("texture"))
            .withBindGroupLayout(BindGroupLayouts.SAMPLER0)
            .withCull(false)
            .build();

    public static final List<RenderPipeline> ALL_PIPELINES = List.of(
            RECTANGLE, TTF_FONT, ROUND_RECT, SHADOW, TEXTURE
    );

    public static void registerAll(Consumer<RenderPipeline> registrar) {
        ALL_PIPELINES.forEach(registrar);
    }

}


