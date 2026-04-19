package com.github.epsilon.fabric.compat;

import com.github.epsilon.compat.VertexFormatCompat;
import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class FabricVertexFormatCompat implements VertexFormatCompat {

    private Field byIdField;
    private Method registerMethod;

    @Override
    public int findNextVertexFormatElementId() {
        try {
            if (byIdField == null) {
                byIdField = VertexFormatElement.class.getDeclaredField("BY_ID");
                byIdField.setAccessible(true);
            }
            VertexFormatElement[] byId = (VertexFormatElement[]) byIdField.get(null);
            for (int i = 0; i < byId.length; i++) {
                if (byId[i] == null) return i;
            }
            throw new IllegalStateException("VertexFormatElement count limit exceeded");
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to find next VertexFormatElement id", e);
        }
    }

    @Override
    public VertexFormatElement registerVertexFormatElement(int id, int index, GpuFormat format) {
        try {
            if (registerMethod == null) {
                registerMethod = VertexFormatElement.class.getDeclaredMethod(
                        "register", int.class, int.class, GpuFormat.class);
                registerMethod.setAccessible(true);
            }
            return (VertexFormatElement) registerMethod.invoke(null, id, index, format);
        } catch (Exception e) {
            throw new RuntimeException("Failed to register VertexFormatElement", e);
        }
    }

}

