package com.github.epsilon.graphics.vulkan.descriptor;

import com.mojang.blaze3d.vulkan.VulkanUtils;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.lwjgl.vulkan.VK12.*;

public final class VulkanDescriptorSet implements AutoCloseable {

    private final VkDevice device;
    private final long descriptorPool;
    private final long descriptorSet;

    private VulkanDescriptorSet(VkDevice device, long descriptorPool, long descriptorSet) {
        this.device = device;
        this.descriptorPool = descriptorPool;
        this.descriptorSet = descriptorSet;
    }

    public static VulkanDescriptorSet create(
            VkDevice device,
            long descriptorSetLayout,
            DescriptorLayoutSpec layoutSpec,
            List<DescriptorSetWrite> writes
    ) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            Map<Integer, Integer> poolCounts = collectPoolCounts(layoutSpec.bindings());
            VkDescriptorPoolSize.Buffer poolSizes = VkDescriptorPoolSize.calloc(poolCounts.size(), stack);

            int idx = 0;
            for (var entry : poolCounts.entrySet()) {
                poolSizes.get(idx)
                        .type(entry.getKey())
                        .descriptorCount(entry.getValue());
                idx++;
            }

            VkDescriptorPoolCreateInfo poolInfo = VkDescriptorPoolCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO)
                    .pPoolSizes(poolSizes)
                    .maxSets(1);

            var pPool = stack.mallocLong(1);
            VulkanUtils.crashIfFailure(
                    vkCreateDescriptorPool(device, poolInfo, null, pPool),
                    "Can't create descriptor pool"
            );
            long descriptorPool = pPool.get(0);

            VkDescriptorSetAllocateInfo allocInfo = VkDescriptorSetAllocateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO)
                    .descriptorPool(descriptorPool)
                    .pSetLayouts(stack.longs(descriptorSetLayout));

            var pSet = stack.mallocLong(1);
            VulkanUtils.crashIfFailure(
                    vkAllocateDescriptorSets(device, allocInfo, pSet),
                    "Can't allocate descriptor set"
            );
            long descriptorSet = pSet.get(0);

            updateDescriptorSet(device, descriptorSet, layoutSpec.bindings(), writes, stack);
            return new VulkanDescriptorSet(device, descriptorPool, descriptorSet);
        }
    }

    @SuppressWarnings("resource")
    private static void updateDescriptorSet(
            VkDevice device,
            long descriptorSet,
            List<DescriptorBindingSpec> bindingSpecs,
            List<DescriptorSetWrite> writes,
            MemoryStack stack
    ) {
        Map<Integer, DescriptorBindingSpec> specByBinding = new LinkedHashMap<>();
        for (DescriptorBindingSpec spec : bindingSpecs) {
            specByBinding.put(spec.binding(), spec);
        }

        VkWriteDescriptorSet.Buffer vkWrites = VkWriteDescriptorSet.calloc(writes.size(), stack);

        for (int i = 0; i < writes.size(); i++) {
            DescriptorSetWrite write = writes.get(i);
            DescriptorBindingSpec expected = specByBinding.get(write.binding());
            if (expected == null) {
                throw new IllegalArgumentException("Binding " + write.binding() + " is not declared in layout spec");
            }
            if (expected.descriptorType() != write.descriptorType()) {
                throw new IllegalArgumentException("Descriptor type mismatch at binding " + write.binding());
            }
            if (write.descriptorCount() > expected.descriptorCount()) {
                throw new IllegalArgumentException("Descriptor count exceeds layout declaration at binding " + write.binding());
            }

            VkWriteDescriptorSet vkWrite = vkWrites.get(i)
                    .sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET)
                    .dstSet(descriptorSet)
                    .dstBinding(write.binding())
                    .dstArrayElement(0)
                    .descriptorType(write.descriptorType())
                    .descriptorCount(write.descriptorCount());

            if (write.isBufferDescriptor()) {
                if (write.buffer() == VK_NULL_HANDLE) {
                    throw new IllegalArgumentException("Buffer handle is required for binding " + write.binding());
                }
                VkDescriptorBufferInfo.Buffer bufferInfo = VkDescriptorBufferInfo.calloc(1, stack)
                        .buffer(write.buffer())
                        .offset(write.offset())
                        .range(write.range());
                vkWrite.pBufferInfo(bufferInfo);
            } else if (write.isImageDescriptor()) {
                if (write.imageView() == VK_NULL_HANDLE) {
                    throw new IllegalArgumentException("Image view is required for binding " + write.binding());
                }
                VkDescriptorImageInfo.Buffer imageInfo = VkDescriptorImageInfo.calloc(1, stack)
                        .sampler(write.sampler())
                        .imageView(write.imageView())
                        .imageLayout(write.imageLayout());
                vkWrite.pImageInfo(imageInfo);
            } else {
                throw new IllegalArgumentException("Unsupported descriptor type " + write.descriptorType());
            }
        }

        vkUpdateDescriptorSets(device, vkWrites, null);
    }

    private static Map<Integer, Integer> collectPoolCounts(List<DescriptorBindingSpec> bindings) {
        Map<Integer, Integer> result = new LinkedHashMap<>();
        for (DescriptorBindingSpec binding : bindings) {
            result.merge(binding.descriptorType(), binding.descriptorCount(), Integer::sum);
        }
        return result;
    }

    public long handle() {
        return descriptorSet;
    }

    @Override
    public void close() {
        vkDestroyDescriptorPool(device, descriptorPool, null);
    }
}

