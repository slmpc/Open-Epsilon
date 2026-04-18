package com.github.epsilon.graphics.vulkan.buffer;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkBufferCopy;
import org.lwjgl.vulkan.VkCommandBuffer;

import java.nio.ByteBuffer;
import java.util.Objects;

import static org.lwjgl.util.vma.Vma.*;
import static org.lwjgl.vulkan.VK12.*;

/**
 * GPU output/readback pair for compute pipelines.
 *
 * Callers are responsible for synchronization:
 * - record proper pipeline barriers outside this class
 * - wait for fence on CPU before reading mapped bytes
 */
public final class VulkanOutputBuffer implements AutoCloseable {

    private final VulkanBuffer gpu;
    private final VulkanBuffer readback;

    public VulkanOutputBuffer(long allocator, long sizeBytes, int gpuUsageFlags) {
        this.gpu = VulkanBuffer.create(
                allocator,
                sizeBytes,
                VK_BUFFER_USAGE_TRANSFER_SRC_BIT | gpuUsageFlags,
                VMA_MEMORY_USAGE_GPU_ONLY,
                0,
                false
        );

        this.readback = VulkanBuffer.create(
                allocator,
                sizeBytes,
                VK_BUFFER_USAGE_TRANSFER_DST_BIT,
                VMA_MEMORY_USAGE_GPU_TO_CPU,
                VMA_ALLOCATION_CREATE_HOST_ACCESS_RANDOM_BIT | VMA_ALLOCATION_CREATE_MAPPED_BIT,
                true
        );
    }

    public static VulkanOutputBuffer storageBuffer(long allocator, long sizeBytes) {
        return new VulkanOutputBuffer(allocator, sizeBytes, VK_BUFFER_USAGE_STORAGE_BUFFER_BIT);
    }

    public long gpuBuffer() {
        return gpu.handle();
    }

    public long readbackBuffer() {
        return readback.handle();
    }

    public long size() {
        return gpu.size();
    }

    public void map(VkCommandBuffer cmdBuf) {
        copyToReadback(cmdBuf, size());
    }

    public void copyToReadback(VkCommandBuffer cmdBuf) {
        copyToReadback(cmdBuf, size());
    }

    public void copyToReadback(VkCommandBuffer cmdBuf, long byteCount) {
        Objects.requireNonNull(cmdBuf, "cmdBuf");
        validateRange(0, byteCount);

        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkBufferCopy.Buffer region = VkBufferCopy.calloc(1, stack)
                    .srcOffset(0)
                    .dstOffset(0)
                    .size(byteCount);
            vkCmdCopyBuffer(cmdBuf, gpu.handle(), readback.handle(), region);
        }
    }

    public ByteBuffer readMapped(long byteCount) {
        return readMapped(0, byteCount);
    }

    public ByteBuffer readMapped(long offset, long byteCount) {
        validateRange(offset, byteCount);

        readback.invalidate(offset, byteCount);

        ByteBuffer source = readback.mappedData().duplicate();
        int start = Math.toIntExact(offset);
        int end = Math.toIntExact(offset + byteCount);
        source.position(start);
        source.limit(end);
        return source.slice();
    }

    private void validateRange(long offset, long byteCount) {
        if (offset < 0 || byteCount < 0 || offset + byteCount > size()) {
            throw new IllegalArgumentException("Invalid range: offset=" + offset + ", byteCount=" + byteCount);
        }
    }

    @Override
    public void close() {
        readback.close();
        gpu.close();
    }
}

