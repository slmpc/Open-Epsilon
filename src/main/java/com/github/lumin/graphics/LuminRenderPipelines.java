package com.github.lumin.graphics;

import com.github.lumin.assets.resources.ResourceLocationUtils;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderPipelines;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;


public class LuminRenderPipelines {
    
    private final static RenderPipeline.Snippet NO_BLEND_DEPTH_SNIPPET = RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET)
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .buildSnippet();
    
    public final static RenderPipeline RECTANGLE = RenderPipeline.builder(NO_BLEND_DEPTH_SNIPPET)
            .withLocation(ResourceLocationUtils.getIdentifier("pipelines/rectangle"))
            .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS)
            .withVertexShader(ResourceLocationUtils.getIdentifier("rectangle"))
            .withFragmentShader(ResourceLocationUtils.getIdentifier("rectangle"))
            .withCull(false)
            .build();

    public final static RenderPipeline LINE = RenderPipeline.builder(NO_BLEND_DEPTH_SNIPPET)
            .withLocation(ResourceLocationUtils.getIdentifier("pipelines/line"))
            .withVertexFormat(LuminVertexFormats.LINE, VertexFormat.Mode.LINES)
            .withVertexShader(ResourceLocationUtils.getIdentifier("line"))
            .withFragmentShader(ResourceLocationUtils.getIdentifier("line"))
            .withCull(false)
            .build();

    private final static RenderPipeline.Snippet TTF_SNIPPET = RenderPipeline.builder(NO_BLEND_DEPTH_SNIPPET)
            .withUniform("TtfInfo", UniformType.UNIFORM_BUFFER)
            .buildSnippet();

    public final static RenderPipeline TTF_FONT = RenderPipeline.builder(TTF_SNIPPET)
            .withLocation(ResourceLocationUtils.getIdentifier("pipelines/ttf_font"))
            .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS)
            .withVertexShader(ResourceLocationUtils.getIdentifier("ttf_font"))
            .withFragmentShader(ResourceLocationUtils.getIdentifier("ttf_font"))
            .withSampler("Sampler0")
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
            .withSampler("Sampler0")
            .withCull(false)
            .build();

    private final static RenderPipeline.Snippet BLUR_SNIPPET = RenderPipeline.builder(NO_BLEND_DEPTH_SNIPPET)
            .withUniform("BlurInfo", UniformType.UNIFORM_BUFFER)
            .buildSnippet();

    public final static RenderPipeline BLUR_DOWN = RenderPipeline.builder(BLUR_SNIPPET)
            .withLocation(ResourceLocationUtils.getIdentifier("pipelines/blur_down"))
            .withVertexFormat(DefaultVertexFormat.POSITION_TEX, VertexFormat.Mode.QUADS)
            .withVertexShader(ResourceLocationUtils.getIdentifier("blur"))
            .withFragmentShader(ResourceLocationUtils.getIdentifier("blur_down"))
            .withSampler("Sampler0")
            .withCull(false)
            .build();

    public final static RenderPipeline BLUR_UP = RenderPipeline.builder(BLUR_SNIPPET)
            .withLocation(ResourceLocationUtils.getIdentifier("pipelines/blur_up"))
            .withVertexFormat(DefaultVertexFormat.POSITION_TEX, VertexFormat.Mode.QUADS)
            .withVertexShader(ResourceLocationUtils.getIdentifier("blur"))
            .withFragmentShader(ResourceLocationUtils.getIdentifier("blur_up"))
            .withSampler("Sampler0")
            .withCull(false)
            .build();

    public static void onRegisterRenderPipelines(RegisterRenderPipelinesEvent event) {
        event.registerPipeline(RECTANGLE);
        event.registerPipeline(TTF_FONT);
        event.registerPipeline(LINE);
        event.registerPipeline(ROUND_RECT);
        event.registerPipeline(SHADOW);
        event.registerPipeline(TEXTURE);
        event.registerPipeline(BLUR_DOWN);
        event.registerPipeline(BLUR_UP);
    }

}
