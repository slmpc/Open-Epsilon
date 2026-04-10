package com.github.epsilon.graphics;

import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;

import static com.mojang.blaze3d.vertex.VertexFormatElement.findNextId;
import static com.mojang.blaze3d.vertex.VertexFormatElement.register;

public class LuminVertexFormats {

    public static final VertexFormatElement ROUND_INNER_RECT =
            register(findNextId(), 2, VertexFormatElement.Type.FLOAT, false, 4);

    public static final VertexFormatElement ROUND_RADIUS =
            register(findNextId(), 4, VertexFormatElement.Type.FLOAT, false, 4);

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
