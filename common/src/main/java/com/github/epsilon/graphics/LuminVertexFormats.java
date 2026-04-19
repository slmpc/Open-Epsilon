package com.github.epsilon.graphics;
import com.github.epsilon.Epsilon;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;

public class LuminVertexFormats {

    public static final VertexFormatElement ROUND_INNER_RECT =
            Epsilon.platform.registerVertexFormatElement(Epsilon.platform.findNextVertexFormatElementId(), 2, GpuFormat.RGBA32_FLOAT);

    public static final VertexFormatElement ROUND_RADIUS =
            Epsilon.platform.registerVertexFormatElement(Epsilon.platform.findNextVertexFormatElementId(), 4, GpuFormat.RGBA32_FLOAT);

    public static final VertexFormat ROUND_RECT = VertexFormat.builder()
            .add("Position", VertexFormatElement.POSITION)
            .add("Color", VertexFormatElement.COLOR)
            .add("InnerRect", ROUND_INNER_RECT)
            .add("Radius", ROUND_RADIUS)
            .build();

    public static final VertexFormat LINE = VertexFormat.builder()
            .add("Position", VertexFormatElement.POSITION)
            .add("Color", VertexFormatElement.COLOR)
            .build();

    public static final VertexFormat TEXTURE = VertexFormat.builder()
            .add("Position", VertexFormatElement.POSITION)
            .add("Color", VertexFormatElement.COLOR)
            .add("UV0", VertexFormatElement.UV0)
            .add("InnerRect", ROUND_INNER_RECT)
            .add("Radius", ROUND_RADIUS)
            .build();

}
