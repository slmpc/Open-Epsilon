package com.github.epsilon;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import net.minecraft.client.KeyMapping;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Fabric implementation of {@link PlatformCompat}.
 * Uses reflection because NeoForge-added methods don't exist on Fabric.
 */
public class FabricPlatformCompat implements PlatformCompat {

    // --- KeyMapping.getKey() ---

    private Field keyMappingKeyField;

    @Override
    public InputConstants.Key getKeyMappingKey(KeyMapping keyMapping) {
        try {
            if (keyMappingKeyField == null) {
                keyMappingKeyField = KeyMapping.class.getDeclaredField("key");
                keyMappingKeyField.setAccessible(true);
            }
            return (InputConstants.Key) keyMappingKeyField.get(keyMapping);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get key from KeyMapping", e);
        }
    }

    // --- ItemStack.getEquipmentSlot() ---

    @Override
    public EquipmentSlot getEquipmentSlot(ItemStack stack) {
        try {
            Method m = ItemStack.class.getMethod("getEquipmentSlot");
            return (EquipmentSlot) m.invoke(stack);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get equipment slot from ItemStack", e);
        }
    }

    // --- VertexFormatElement.findNextId() & register() ---

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
    public VertexFormatElement registerVertexFormatElement(int id, int index, VertexFormatElement.Type type, boolean normalized, int count) {
        try {
            if (registerMethod == null) {
                registerMethod = VertexFormatElement.class.getDeclaredMethod(
                        "register", int.class, int.class, VertexFormatElement.Type.class, boolean.class, int.class);
                registerMethod.setAccessible(true);
            }
            return (VertexFormatElement) registerMethod.invoke(null, id, index, type, normalized, count);
        } catch (Exception e) {
            throw new RuntimeException("Failed to register VertexFormatElement", e);
        }
    }

}

