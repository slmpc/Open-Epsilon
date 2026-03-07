package com.github.lumin.graphics;

import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;

import javax.annotation.Nonnull;

public record LuminTexture(
        @Nonnull GpuTexture texture,
        @Nonnull GpuTextureView textureView,
        @Nonnull GpuSampler sampler,
        boolean closeTexture,
        boolean closeSampler
) implements AutoCloseable {

    public LuminTexture(@Nonnull GpuTexture texture, @Nonnull GpuTextureView textureView, @Nonnull GpuSampler sampler) {
        this(texture, textureView, sampler, true, true);
    }

    @Override
    public void close() {
        if (closeSampler) {
            sampler.close();
        }
        if (closeTexture) {
            textureView.close();
            texture.close();
        }
    }

}
