package com.github.epsilon.graphics.vulkan;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vulkan.VulkanDevice;
import com.mojang.blaze3d.vulkan.VulkanQueue;
import com.mojang.blaze3d.vulkan.VulkanUtils;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkCommandPoolCreateInfo;
import org.lwjgl.vulkan.VkDevice;

import javax.annotation.Nullable;

import java.nio.LongBuffer;

import static org.lwjgl.vulkan.VK10.*;

public class LuminVulkanContext {

    private @Nullable VkDevice device;
    private long vma;

    // Queues
    private VulkanQueue graphicsQueue;
    private VulkanQueue computeQueue;
    private VulkanQueue transferQueue;

    private long cmdPool;

    public LuminVulkanContext() {
        if (!(RenderSystem.getDevice().backend instanceof VulkanDevice)) {
            return;
        }

        VulkanDevice blz3dDevice = (VulkanDevice) RenderSystem.getDevice().backend;

        this.device = blz3dDevice.vkDevice();
        this.vma = blz3dDevice.vma();

        this.graphicsQueue = blz3dDevice.graphicsQueue();
        this.computeQueue = blz3dDevice.computeQueue();
        this.transferQueue = blz3dDevice.transferQueue();

        try (MemoryStack stack = MemoryStack.stackPush()) {

            VkCommandPoolCreateInfo poolInfo = VkCommandPoolCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO)
                    .flags(VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT)
                    .queueFamilyIndex(graphicsQueue.queueFamilyIndex());

            final var cmdPool = stack.callocLong(1);

            VulkanUtils.crashIfFailure(
                    vkCreateCommandPool(device, poolInfo, null, cmdPool),
                    "Failed to create command pool"
            );

             this.cmdPool = cmdPool.get();

        }
    }

    public VkDevice device() {
        if (this.device == null) {
            throw new IllegalStateException("Vulkan device is not initialized. Make sure to initialize the Vulkan context properly.");
        }
        return this.device;
    }

    public long vma() {
        return this.vma;
    }

    public long cmdPool() {
        return this.cmdPool;
    }

    public VulkanQueue graphicsQueue() {
        return this.graphicsQueue;
    }

    public VulkanQueue computeQueue() {
        return this.computeQueue;
    }

    public VulkanQueue transferQueue() {
        return this.transferQueue;
    }

}
