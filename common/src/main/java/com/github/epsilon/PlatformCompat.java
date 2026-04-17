package com.github.epsilon;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import net.minecraft.client.KeyMapping;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/**
 * Platform abstraction interface for methods that differ between loaders
 * (NeoForge adds extra methods to vanilla classes that don't exist on Fabric).
 *
 * <p>The active implementation is stored in {@link Epsilon#platform} and must be
 * set by the loader-specific entry point before {@link Epsilon#init()} is called.</p>
 */
public interface PlatformCompat {

    /** Returns the {@link InputConstants.Key} bound to a {@link KeyMapping}. */
    InputConstants.Key getKeyMappingKey(KeyMapping keyMapping);

    /** Returns the {@link EquipmentSlot} an {@link ItemStack}'s item occupies. */
    EquipmentSlot getEquipmentSlot(ItemStack stack);

    /**
     * Returns the next available id for a custom {@link VertexFormatElement}.
     * Equivalent to NeoForge's {@code VertexFormatElement.findNextId()}.
     */
    int findNextVertexFormatElementId();

    /**
     * Registers and returns a custom {@link VertexFormatElement}.
     * Equivalent to NeoForge's {@code VertexFormatElement.register(...)}.
     */
    VertexFormatElement registerVertexFormatElement(int id, int index, VertexFormatElement.Type type, boolean normalized, int count);

}
