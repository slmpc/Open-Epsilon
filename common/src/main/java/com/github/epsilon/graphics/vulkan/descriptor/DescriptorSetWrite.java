package com.github.epsilon.graphics.vulkan.descriptor;

import static org.lwjgl.vulkan.VK12.*;

public record DescriptorSetWrite(
        int binding,
        int descriptorType,
        int descriptorCount,
        long buffer,
        long offset,
        long range,
        long sampler,
        long imageView,
        int imageLayout
) {

    public DescriptorSetWrite {
        if (binding < 0) {
            throw new IllegalArgumentException("binding must be >= 0");
        }
        if (descriptorCount <= 0) {
            throw new IllegalArgumentException("descriptorCount must be > 0");
        }
    }

    public static DescriptorSetWrite storageBuffer(int binding, long buffer, long range) {
        return new DescriptorSetWrite(binding, VK_DESCRIPTOR_TYPE_STORAGE_BUFFER, 1, buffer, 0L, range, VK_NULL_HANDLE, VK_NULL_HANDLE, VK_IMAGE_LAYOUT_UNDEFINED);
    }

    public static DescriptorSetWrite uniformBuffer(int binding, long buffer, long range) {
        return new DescriptorSetWrite(binding, VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER, 1, buffer, 0L, range, VK_NULL_HANDLE, VK_NULL_HANDLE, VK_IMAGE_LAYOUT_UNDEFINED);
    }

    public static DescriptorSetWrite combinedImageSampler(int binding, long sampler, long imageView, int imageLayout) {
        return new DescriptorSetWrite(binding, VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER, 1, VK_NULL_HANDLE, 0L, 0L, sampler, imageView, imageLayout);
    }

    public static DescriptorSetWrite sampledImage(int binding, long imageView, int imageLayout) {
        return new DescriptorSetWrite(binding, VK_DESCRIPTOR_TYPE_SAMPLED_IMAGE, 1, VK_NULL_HANDLE, 0L, 0L, VK_NULL_HANDLE, imageView, imageLayout);
    }

    public static DescriptorSetWrite storageImage(int binding, long imageView, int imageLayout) {
        return new DescriptorSetWrite(binding, VK_DESCRIPTOR_TYPE_STORAGE_IMAGE, 1, VK_NULL_HANDLE, 0L, 0L, VK_NULL_HANDLE, imageView, imageLayout);
    }

    public boolean isBufferDescriptor() {
        return descriptorType == VK_DESCRIPTOR_TYPE_STORAGE_BUFFER || descriptorType == VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER;
    }

    public boolean isImageDescriptor() {
        return descriptorType == VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER
                || descriptorType == VK_DESCRIPTOR_TYPE_SAMPLED_IMAGE
                || descriptorType == VK_DESCRIPTOR_TYPE_STORAGE_IMAGE;
    }
}

