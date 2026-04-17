package com.github.epsilon;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import net.minecraft.client.KeyMapping;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/**
 * NeoForge implementation of {@link PlatformCompat}.
 * Uses NeoForge's directly-added API methods.
 */
public class NeoForgePlatformCompat implements PlatformCompat {

    @Override
    public InputConstants.Key getKeyMappingKey(KeyMapping keyMapping) {
        return keyMapping.getKey();
    }

    @Override
    public EquipmentSlot getEquipmentSlot(ItemStack stack) {
        return stack.getEquipmentSlot();
    }

    @Override
    public int findNextVertexFormatElementId() {
        return VertexFormatElement.findNextId();
    }

    @Override
    public VertexFormatElement registerVertexFormatElement(int id, int index, VertexFormatElement.Type type, boolean normalized, int count) {
        return VertexFormatElement.register(id, index, type, normalized, count);
    }

}

