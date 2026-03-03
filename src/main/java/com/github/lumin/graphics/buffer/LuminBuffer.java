package com.github.lumin.graphics.buffer;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.RenderSystem;

import java.nio.ByteBuffer;

/**
 * LuminBuffer 是对 Blaze3D 的 GpuBuffer 的包装
 * 会自动创建 GpuBuffer 并对 Buffer 进行 Map
 * <p>
 * 1) 在支持 GL_MAP_PERSISTENT_BIT GL_MAP_FLUSH_EXPLICIT_BIT 的情况下不会执行 glUnmapBuffer 只会调用 Flush
 * <p>
 * 2) 在均不支持的情况下 会退化至 glBufferData + glMapBufferRange + glUnmapBuffer
 */
public class LuminBuffer {

    private final GpuBuffer gpuBuffer;

    private GpuBuffer.MappedView mappedBuffer;

    private boolean mapped;

    public LuminBuffer(long size, @GpuBuffer.Usage int usage) {

        gpuBuffer = RenderSystem.getDevice().createBuffer(
                () -> "lumin-buffer",
                GpuBuffer.USAGE_MAP_WRITE | GpuBuffer.USAGE_COPY_DST | usage,
                size
        );

        mappedBuffer = RenderSystem.getDevice().createCommandEncoder().mapBuffer(
                gpuBuffer, false, true
        );
        mapped = true;
    }

    public ByteBuffer getMappedBuffer() {
        return mappedBuffer.data();
    }

    /**
     * 尝试 Map 此 Buffer
     * 如已经 Map 则不会执行
     */
    public void tryMap() {
        if (mapped) return;
        mappedBuffer = RenderSystem.getDevice().createCommandEncoder().mapBuffer(
                gpuBuffer, false, true
        );
        mapped = true;
    }

    /**
     * 调用 Blaze3D 的 unmap
     * 在支持 GL_MAP_PERSISTENT_BIT GL_MAP_FLUSH_EXPLICIT_BIT 的情况下不会执行 Unmap 只会调用 Flush
     * 在均不支持的情况下 会退化至 glBufferData + glMapBufferRange + glUnmapBuffer
     */
    public void unmap() {
        mappedBuffer.close();
        mapped = false;
    }

    public GpuBuffer getGpuBuffer() {
        return gpuBuffer;
    }

    public void close() {
        gpuBuffer.close();
    }

}
