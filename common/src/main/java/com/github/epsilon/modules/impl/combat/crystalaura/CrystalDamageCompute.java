package com.github.epsilon.modules.impl.combat.crystalaura;

import com.github.epsilon.assets.resources.ResourceLocationUtils;
import com.github.epsilon.graphics.LuminRenderSystem;
import com.github.epsilon.graphics.vulkan.buffer.VulkanOutputBuffer;
import com.github.epsilon.graphics.vulkan.buffer.VulkanStd430Buffer;
import com.github.epsilon.graphics.vulkan.compute.VulkanComputePipeline;
import com.github.epsilon.graphics.vulkan.descriptor.DescriptorLayoutSpec;
import com.github.epsilon.graphics.vulkan.descriptor.DescriptorSetWrite;
import com.github.epsilon.graphics.vulkan.descriptor.VulkanResourceManager;
import com.github.epsilon.graphics.vulkan.shader.Glsl2SpirVCompiler;
import com.mojang.blaze3d.vulkan.VulkanUtils;
import net.minecraft.world.Difficulty;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.vulkan.KHRSynchronization2.*;
import static org.lwjgl.vulkan.VK12.*;

/**
 * GPU 加速水晶爆炸伤害计算器。
 *
 * <h3>概述</h3>
 * 通过 Vulkan Compute Shader 并行计算多个 (放置点, 目标) 对的爆炸伤害，
 * 利用体素化地形做射线遮挡查询，替代 CPU 侧逐射线 clip 调用。
 *
 * <h3>SSBO 布局</h3>
 * <ul>
 *   <li>binding 0: 体素网格（由 {@link TerrainVoxelization} 填充）</li>
 *   <li>binding 1: 任务缓冲（放置点 + 目标信息）</li>
 *   <li>binding 2: 结果缓冲（每个任务对应一个 float 伤害值）</li>
 * </ul>
 *
 * @see TerrainVoxelization
 */
public class CrystalDamageCompute {

    /** 每个 Task 的 std430 大小：5 × vec4 = 80 bytes */
    private static final int TASK_STRIDE = 80;

    /** Task header: uint taskCount + 3×uint padding = 16 bytes */
    private static final int TASK_HEADER_BYTES = 16;

    /** 最大并行任务数 */
    public static final int MAX_TASKS = 512;

    /** Task buffer 总大小 */
    private static final long TASK_BUFFER_SIZE = TASK_HEADER_BYTES + (long) MAX_TASKS * TASK_STRIDE;

    /** Result buffer 总大小 */
    private static final long RESULT_BUFFER_SIZE = (long) MAX_TASKS * Float.BYTES;

    // ── Vulkan 资源 ──

    private boolean initialized;
    private @Nullable VulkanStd430Buffer voxelBuffer;
    private @Nullable VulkanStd430Buffer taskBuffer;
    private @Nullable VulkanOutputBuffer resultBuffer;
    private @Nullable VulkanComputePipeline pipeline;
    private @Nullable VulkanResourceManager resourceManager;
    private @Nullable VulkanResourceManager.ManagedDescriptorSet descriptorSet;
    private @Nullable VkCommandBuffer cmdBuf;
    private long fence = VK_NULL_HANDLE;

    private final DescriptorLayoutSpec layoutSpec = DescriptorLayoutSpec.builder()
            .addSsbo(0)
            .addSsbo(1)
            .addSsbo(2)
            .build();

    private final TerrainVoxelization voxelization = new TerrainVoxelization();

    // ── 任务写入状态 ──

    private int taskCount;
    private final List<GpuTask> pendingTasks = new ArrayList<>(MAX_TASKS);

    private record GpuTask(
            Vec3 crystalPos,
            float radius,
            Vec3 targetPos,
            float halfWidth,
            float height,
            float armor,
            float toughness,
            float enchantProt,
            float difficulty,
            float resistanceMultiplier,
            float applyDifficulty
    ) {
    }

    // ─── 初始化 ─────────────────────────────────────────────────────────────

    public void ensureInitialized() {
        if (initialized) return;

        try {
            ByteBuffer shaderSource = ResourceLocationUtils.loadResource(
                    ResourceLocationUtils.getIdentifier("shaders/compute/crystal_damage.csh")
            );
            String glsl;
            try {
                glsl = MemoryUtil.memUTF8(shaderSource);
            } finally {
                MemoryUtil.memFree(shaderSource);
            }

            ByteBuffer spirv;
            try (Glsl2SpirVCompiler compiler = new Glsl2SpirVCompiler(glsl)) {
                compiler.compile();
                spirv = compiler.getSpirV();
            }

            voxelBuffer = new VulkanStd430Buffer(
                    LuminRenderSystem.vulkanContext.vma(),
                    TerrainVoxelization.BUFFER_SIZE,
                    VK_BUFFER_USAGE_STORAGE_BUFFER_BIT
            );

            taskBuffer = new VulkanStd430Buffer(
                    LuminRenderSystem.vulkanContext.vma(),
                    TASK_BUFFER_SIZE,
                    VK_BUFFER_USAGE_STORAGE_BUFFER_BIT
            );

            resultBuffer = new VulkanOutputBuffer(
                    LuminRenderSystem.vulkanContext.vma(),
                    RESULT_BUFFER_SIZE,
                    VK_BUFFER_USAGE_STORAGE_BUFFER_BIT
            );

            pipeline = new VulkanComputePipeline(
                    LuminRenderSystem.vulkanContext.device(),
                    spirv,
                    layoutSpec
            );

            resourceManager = new VulkanResourceManager(LuminRenderSystem.vulkanContext.device());

            descriptorSet = resourceManager.allocateDescriptorSet(
                    pipeline.descriptorSetLayout(),
                    layoutSpec,
                    List.of(
                            DescriptorSetWrite.storageBuffer(0, voxelBuffer.gpuBuffer(), TerrainVoxelization.BUFFER_SIZE),
                            DescriptorSetWrite.storageBuffer(1, taskBuffer.gpuBuffer(), TASK_BUFFER_SIZE),
                            DescriptorSetWrite.storageBuffer(2, resultBuffer.gpuBuffer(), RESULT_BUFFER_SIZE)
                    )
            );

            cmdBuf = allocateCommandBuffer();
            fence = createFence();
            initialized = true;
        } catch (Exception e) {
            e.printStackTrace();
            destroy();
        }
    }

    public boolean isInitialized() {
        return initialized;
    }

    // ─── 任务提交 API ───────────────────────────────────────────────────────

    /**
     * 开始新一轮任务提交：更新体素网格，清空任务列表。
     */
    public void beginFrame() {
        if (!initialized) return;
        voxelization.update();
        taskCount = 0;
        pendingTasks.clear();
    }

    /**
     * 添加一个伤害计算任务。
     *
     * @param crystalPos 水晶爆炸中心
     * @param radius     爆炸半径
     * @param targetPos  目标脚底位置
     * @param halfWidth  目标半宽
     * @param height     目标高度
     * @param armor      目标护甲值
     * @param toughness  目标护甲韧性
     * @param enchantProt 附魔保护等级总和 (clamped 0-20)
     * @param difficulty 难度 (0=Peaceful,1=Easy,2=Normal,3=Hard)
     * @return 任务索引，用于后续读取结果；-1 表示已满
     */
    public int addTask(Vec3 crystalPos, float radius,
                       Vec3 targetPos, float halfWidth, float height,
                       float armor, float toughness, float enchantProt,
                       float difficulty, float resistanceMultiplier, float applyDifficulty) {
        if (taskCount >= MAX_TASKS) return -1;

        int idx = taskCount;
        taskCount++;
        pendingTasks.add(new GpuTask(
                crystalPos, radius,
                targetPos, halfWidth, height,
                armor, toughness, enchantProt, difficulty,
                resistanceMultiplier, applyDifficulty
        ));
        return idx;
    }

    /**
     * 将难度枚举转为 shader 使用的 float。
     */
    public static float difficultyToFloat(Difficulty d) {
        return switch (d) {
            case PEACEFUL -> 0f;
            case EASY -> 1f;
            case NORMAL -> 2f;
            case HARD -> 3f;
        };
    }

    // ─── 执行 & 回读 ───────────────────────────────────────────────────────

    /**
     * 提交所有任务到 GPU 并等待完成。
     */
    public void dispatch() {
        if (!initialized || taskCount == 0) return;
        if (cmdBuf == null || pipeline == null || voxelBuffer == null
                || taskBuffer == null || resultBuffer == null || descriptorSet == null) return;

        writeTaskBuffer();

        // 上传体素数据
        voxelBuffer.writer().clear();
        voxelization.writeTo(voxelBuffer.writer());

        recordAndSubmit();
    }

    private void writeTaskBuffer() {
        if (taskBuffer == null) return;

        var writer = taskBuffer.writer();
        writer.clear();
        writer.putUInt(taskCount);
        writer.putUInt(0);
        writer.putUInt(0);
        writer.putUInt(0);

        for (GpuTask task : pendingTasks) {
            writer.putVec4((float) task.crystalPos.x, (float) task.crystalPos.y, (float) task.crystalPos.z, task.radius);
            writer.putVec4((float) task.targetPos.x, (float) task.targetPos.y, (float) task.targetPos.z, 0f);
            writer.putVec4(task.halfWidth, task.height, 0f, 0f);
            writer.putVec4(task.armor, task.toughness, task.enchantProt, task.difficulty);
            writer.putVec4(task.resistanceMultiplier, task.applyDifficulty, 0f, 0f);
        }
    }

    private void recordAndSubmit() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            vkResetCommandBuffer(cmdBuf, 0);

            var beginInfo = VkCommandBufferBeginInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO)
                    .flags(VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT);
            vkBeginCommandBuffer(cmdBuf, beginInfo);

            // 上传 voxel + task buffer
            voxelBuffer.map(cmdBuf);
            taskBuffer.map(cmdBuf);

            // Barrier: transfer → compute
            var barriers = VkBufferMemoryBarrier2KHR.calloc(2, stack);
            barriers.get(0)
                    .sType(VK_STRUCTURE_TYPE_BUFFER_MEMORY_BARRIER_2_KHR)
                    .srcStageMask(VK_PIPELINE_STAGE_2_TRANSFER_BIT_KHR)
                    .srcAccessMask(VK_ACCESS_2_TRANSFER_WRITE_BIT_KHR)
                    .dstStageMask(VK_PIPELINE_STAGE_2_COMPUTE_SHADER_BIT_KHR)
                    .dstAccessMask(VK_ACCESS_2_SHADER_STORAGE_READ_BIT_KHR)
                    .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                    .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                    .buffer(voxelBuffer.gpuBuffer())
                    .offset(0)
                    .size(TerrainVoxelization.BUFFER_SIZE);
            barriers.get(1)
                    .sType(VK_STRUCTURE_TYPE_BUFFER_MEMORY_BARRIER_2_KHR)
                    .srcStageMask(VK_PIPELINE_STAGE_2_TRANSFER_BIT_KHR)
                    .srcAccessMask(VK_ACCESS_2_TRANSFER_WRITE_BIT_KHR)
                    .dstStageMask(VK_PIPELINE_STAGE_2_COMPUTE_SHADER_BIT_KHR)
                    .dstAccessMask(VK_ACCESS_2_SHADER_STORAGE_READ_BIT_KHR)
                    .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                    .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                    .buffer(taskBuffer.gpuBuffer())
                    .offset(0)
                    .size(TASK_BUFFER_SIZE);

            // 上一帧 readback 对 resultBuffer 的 TRANSFER_READ 与本帧 COMPUTE_WRITE 存在同资源依赖。
            var resultWriteBarrier = VkBufferMemoryBarrier2KHR.calloc(1, stack)
                    .sType(VK_STRUCTURE_TYPE_BUFFER_MEMORY_BARRIER_2_KHR)
                    .srcStageMask(VK_PIPELINE_STAGE_2_TRANSFER_BIT_KHR)
                    .srcAccessMask(VK_ACCESS_2_TRANSFER_READ_BIT_KHR)
                    .dstStageMask(VK_PIPELINE_STAGE_2_COMPUTE_SHADER_BIT_KHR)
                    .dstAccessMask(VK_ACCESS_2_SHADER_STORAGE_WRITE_BIT_KHR)
                    .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                    .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                    .buffer(resultBuffer.gpuBuffer())
                    .offset(0)
                    .size((long) taskCount * Float.BYTES);

            var dep = VkDependencyInfoKHR.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_DEPENDENCY_INFO_KHR)
                    .pBufferMemoryBarriers(VkBufferMemoryBarrier2KHR.calloc(3, stack)
                            .put(0, barriers.get(0))
                            .put(1, barriers.get(1))
                            .put(2, resultWriteBarrier.get(0)));
            vkCmdPipelineBarrier2KHR(cmdBuf, dep);

            // Bind & dispatch
            vkCmdBindPipeline(cmdBuf, VK_PIPELINE_BIND_POINT_COMPUTE, pipeline.pipeline());
            vkCmdBindDescriptorSets(cmdBuf, VK_PIPELINE_BIND_POINT_COMPUTE,
                    pipeline.pipelineLayout(), 0,
                    stack.longs(descriptorSet.handle()), null);

            // 每个 workgroup 处理一个 task（内部 64 线程并行追踪射线）
            vkCmdDispatch(cmdBuf, taskCount, 1, 1);

            // Barrier: compute → transfer
            var readbackBarrier = VkBufferMemoryBarrier2KHR.calloc(1, stack)
                    .sType(VK_STRUCTURE_TYPE_BUFFER_MEMORY_BARRIER_2_KHR)
                    .srcStageMask(VK_PIPELINE_STAGE_2_COMPUTE_SHADER_BIT_KHR)
                    .srcAccessMask(VK_ACCESS_2_SHADER_STORAGE_WRITE_BIT_KHR)
                    .dstStageMask(VK_PIPELINE_STAGE_2_TRANSFER_BIT_KHR)
                    .dstAccessMask(VK_ACCESS_2_TRANSFER_READ_BIT_KHR)
                    .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                    .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                    .buffer(resultBuffer.gpuBuffer())
                    .offset(0)
                    .size(RESULT_BUFFER_SIZE);

            var readbackDep = VkDependencyInfoKHR.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_DEPENDENCY_INFO_KHR)
                    .pBufferMemoryBarriers(readbackBarrier);
            vkCmdPipelineBarrier2KHR(cmdBuf, readbackDep);

            resultBuffer.copyToReadback(cmdBuf, (long) taskCount * Float.BYTES);
            vkEndCommandBuffer(cmdBuf);

            // Submit & wait
            vkResetFences(LuminRenderSystem.vulkanContext.device(), stack.longs(fence));
            var submitInfo = VkSubmitInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_SUBMIT_INFO)
                    .pCommandBuffers(stack.pointers(cmdBuf));
            vkQueueSubmit(LuminRenderSystem.vulkanContext.graphicsQueue().vkQueue(), submitInfo, fence);
            vkWaitForFences(LuminRenderSystem.vulkanContext.device(), stack.longs(fence), true, Long.MAX_VALUE);
        }
    }

    /**
     * 读取指定任务的计算结果。
     *
     * @param taskIndex addTask 返回的索引
     * @return 计算的伤害值
     */
    public float readResult(int taskIndex) {
        if (!initialized || resultBuffer == null || taskIndex < 0 || taskIndex >= taskCount) return 0f;
        ByteBuffer result = resultBuffer.readMapped((long) taskIndex * Float.BYTES, Float.BYTES);
        return result.getFloat(0);
    }

    /**
     * 批量读取所有任务结果。
     */
    public float[] readAllResults() {
        if (!initialized || resultBuffer == null || taskCount == 0) return new float[0];
        ByteBuffer result = resultBuffer.readMapped((long) taskCount * Float.BYTES);
        float[] damages = new float[taskCount];
        for (int i = 0; i < taskCount; i++) {
            damages[i] = result.getFloat(i * Float.BYTES);
        }
        return damages;
    }

    public int getTaskCount() {
        return taskCount;
    }

    // ─── 资源管理 ───────────────────────────────────────────────────────────

    private VkCommandBuffer allocateCommandBuffer() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            var pCmd = stack.mallocPointer(1);
            var allocInfo = VkCommandBufferAllocateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO)
                    .commandPool(LuminRenderSystem.vulkanContext.cmdPool())
                    .level(VK_COMMAND_BUFFER_LEVEL_PRIMARY)
                    .commandBufferCount(1);
            VulkanUtils.crashIfFailure(
                    vkAllocateCommandBuffers(LuminRenderSystem.vulkanContext.device(), allocInfo, pCmd),
                    "Failed to allocate command buffer for CrystalDamageCompute"
            );
            return new VkCommandBuffer(pCmd.get(0), LuminRenderSystem.vulkanContext.device());
        }
    }

    private long createFence() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            var pFence = stack.mallocLong(1);
            var createInfo = VkFenceCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_FENCE_CREATE_INFO);
            VulkanUtils.crashIfFailure(
                    vkCreateFence(LuminRenderSystem.vulkanContext.device(), createInfo, null, pFence),
                    "Failed to create fence for CrystalDamageCompute"
            );
            return pFence.get(0);
        }
    }

    public void destroy() {
        if (fence != VK_NULL_HANDLE) {
            vkDestroyFence(LuminRenderSystem.vulkanContext.device(), fence, null);
            fence = VK_NULL_HANDLE;
        }
        if (descriptorSet != null) { descriptorSet.close(); descriptorSet = null; }
        if (resourceManager != null) { resourceManager.close(); resourceManager = null; }
        if (pipeline != null) { pipeline.close(); pipeline = null; }
        if (resultBuffer != null) { resultBuffer.close(); resultBuffer = null; }
        if (taskBuffer != null) { taskBuffer.close(); taskBuffer = null; }
        if (voxelBuffer != null) { voxelBuffer.close(); voxelBuffer = null; }
        if (cmdBuf != null) {
            vkFreeCommandBuffers(LuminRenderSystem.vulkanContext.device(),
                    LuminRenderSystem.vulkanContext.cmdPool(), cmdBuf);
            cmdBuf = null;
        }
        initialized = false;
    }
}



