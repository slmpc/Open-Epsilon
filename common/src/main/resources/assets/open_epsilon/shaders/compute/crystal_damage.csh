#version 450

/*
 * crystal_damage.csh — GPU 加速水晶爆炸伤害计算
 *
 * 调度模型：
 *   - gl_WorkGroupID.x   = taskId（每个 workgroup 处理一个任务）
 *   - gl_LocalInvocationID.x = rayId（每个线程追踪一条射线）
 *   - local_size_x = 64，其中前 TOTAL_SAMPLES 条线程活跃
 *   - 通过 shared memory 做加权归约，thread 0 计算最终伤害并写出
 *
 * 设计原则：
 *   1. 最小化分支 —— step/mix/clamp 替代 if-else
 *   2. 注意力加权采样 —— 高斯衰减，中心射线权重高
 *   3. 无 bounce —— 仅直线遮挡判断
 */

layout(local_size_x = 64) in;

// ─── Voxel Grid SSBO (binding 0) ────────────────────────────────────────────

layout(std430, set = 0, binding = 0) readonly buffer VoxelGrid {
    ivec4 header;       // xyz = gridOrigin, w = gridSize
    uint  voxelBits[];
} grid;

// ─── Task Buffer SSBO (binding 1) ───────────────────────────────────────────

struct Task {
    vec4 crystalPos;    // xyz = 爆炸中心, w = 半径
    vec4 targetPos;     // xyz = 目标脚底, w = unused
    vec4 targetSize;    // x = halfWidth, y = height
    vec4 params;        // x = armor, y = toughness, z = enchantProt, w = difficulty
};

layout(std430, set = 0, binding = 1) readonly buffer TaskBuffer {
    uint taskCount;
    uint _pad0;
    uint _pad1;
    uint _pad2;
    Task tasks[];
} taskBuf;

// ─── Result Buffer SSBO (binding 2) ─────────────────────────────────────────

layout(std430, set = 0, binding = 2) writeonly buffer ResultBuffer {
    float damages[];
} resultBuf;

// ─── 常量 ───────────────────────────────────────────────────────────────────

const float EPSILON = 1e-6;
const int   RAY_STEPS = 64;

// 注意力采样网格 (每轴)
const int SAMPLE_X = 3;
const int SAMPLE_Y = 4;
const int SAMPLE_Z = 3;
const int TOTAL_SAMPLES = SAMPLE_X * SAMPLE_Y * SAMPLE_Z; // = 36

// ─── Shared memory：每线程写入 (weightedHit, weight) ─────────────────────────

shared float s_weightedHit[64];
shared float s_weight[64];

// ─── 体素查询（无分支） ─────────────────────────────────────────────────────

float sampleVoxel(ivec3 worldPos) {
    ivec3 origin = grid.header.xyz;
    int   size   = grid.header.w;
    ivec3 local  = worldPos - origin;

    bvec3 inBounds = greaterThanEqual(local, ivec3(0));
    bvec3 ltSize   = lessThan(local, ivec3(size));
    float valid = float(all(inBounds) && all(ltSize));

    ivec3 safe = clamp(local, ivec3(0), ivec3(size - 1));
    int flatIdx = (safe.z * size + safe.y) * size + safe.x;
    uint word = grid.voxelBits[flatIdx >> 5];
    float bit = float((word >> (flatIdx & 31)) & 1u);

    return valid * bit;
}

// ─── DDA 射线行进（无分支循环体） ───────────────────────────────────────────

float traceRay(vec3 origin, vec3 target) {
    vec3 dir = target - origin;
    float maxDist = length(dir);
    vec3 d = dir / max(maxDist, EPSILON);

    ivec3 pos = ivec3(floor(origin));
    ivec3 stepDir = ivec3(sign(d));
    vec3 tDelta = abs(vec3(1.0) / max(abs(d), vec3(EPSILON)));

    vec3 fracOrigin = origin - vec3(pos);
    vec3 tMaxPos = (vec3(1.0) - fracOrigin) * tDelta;
    vec3 tMaxNeg = fracOrigin * tDelta;
    vec3 tMax = mix(tMaxNeg, tMaxPos, step(vec3(0.0), d));

    float visibility = 1.0;

    for (int i = 0; i < RAY_STEPS; i++) {
        float tMinVal = min(tMax.x, min(tMax.y, tMax.z));
        float withinRange = step(tMinVal, maxDist);

        float mx = step(tMax.x, min(tMax.y, tMax.z));
        float my = (1.0 - mx) * step(tMax.y, tMax.z);
        float mz = (1.0 - mx) * (1.0 - my);

        pos += ivec3(vec3(mx, my, mz) * vec3(stepDir));
        tMax += vec3(mx, my, mz) * tDelta;

        float solid = sampleVoxel(pos);
        visibility *= (1.0 - solid * withinRange);
    }

    return visibility;
}

// ─── 注意力权重（高斯衰减） ─────────────────────────────────────────────────

float attentionWeight(vec3 uvw) {
    vec3 centered = uvw - vec3(0.5);
    float distSq = dot(centered, centered);
    return exp(-distSq / 0.125);
}

// ─── 伤害公式（无分支） ─────────────────────────────────────────────────────

float applyArmor(float damage, float armor, float toughness) {
    float t = 2.0 + toughness / 4.0;
    float effective = clamp(armor - damage / t, armor * 0.2, 20.0);
    return damage * (1.0 - effective / 25.0);
}

float applyDifficulty(float damage, float difficulty) {
    float peaceful = 0.0;
    float easy     = min(damage * 0.5 + 1.0, damage);
    float normal   = damage;
    float hard     = damage * 1.5;

    float isEasy   = step(0.5, difficulty) * step(difficulty, 1.5);
    float isNormal = step(1.5, difficulty) * step(difficulty, 2.5);
    float isHard   = step(2.5, difficulty);

    return peaceful * (1.0 - step(0.5, difficulty))
         + easy     * isEasy
         + normal   * isNormal
         + hard     * isHard;
}

// ─── 主入口 ──────────────────────────────────────────────────────────────────

void main() {
    uint taskId = gl_WorkGroupID.x;
    uint rayId  = gl_LocalInvocationID.x;

    // 初始化 shared memory
    s_weightedHit[rayId] = 0.0;
    s_weight[rayId]      = 0.0;

    // 越界 workgroup 直接写 0 并返回
    if (taskId >= taskBuf.taskCount) {
        if (rayId == 0u) {
            resultBuf.damages[taskId] = 0.0;
        }
        return;
    }

    Task task = taskBuf.tasks[taskId];

    vec3  crystalPos  = task.crystalPos.xyz;
    float radius      = task.crystalPos.w;
    vec3  targetPos   = task.targetPos.xyz;
    float halfWidth   = task.targetSize.x;
    float height      = task.targetSize.y;

    vec3 bbMin = targetPos - vec3(halfWidth, 0.0, halfWidth);
    vec3 bbMax = targetPos + vec3(halfWidth, height, halfWidth);
    vec3 bbSize = bbMax - bbMin;
    vec3 offset = (vec3(1.0) - floor((bbSize * 2.0 + vec3(1.0)) / vec3(1.0))
                   / (bbSize * 2.0 + vec3(1.0))) * 0.5;
    // 简化 offset 计算：与原版 getSeenPercent 一致
    vec3 stepSize = vec3(1.0) / (bbSize * 2.0 + vec3(1.0));
    offset = (vec3(1.0) - floor(vec3(1.0) / stepSize) * stepSize) * 0.5;
    offset.y = 0.0;

    // ── 每个线程追踪一条射线 ──
    if (rayId < TOTAL_SAMPLES) {
        // 将 rayId 解码为 (ix, iy, iz) 采样索引
        uint iz = rayId / uint(SAMPLE_X * SAMPLE_Y);
        uint rem = rayId % uint(SAMPLE_X * SAMPLE_Y);
        uint iy = rem / uint(SAMPLE_X);
        uint ix = rem % uint(SAMPLE_X);

        float u = float(ix) / float(SAMPLE_X - 1);
        float v = float(iy) / float(SAMPLE_Y - 1);
        float w = float(iz) / float(SAMPLE_Z - 1);

        vec3 samplePos = vec3(
            mix(bbMin.x, bbMax.x, u) + offset.x,
            mix(bbMin.y, bbMax.y, v),
            mix(bbMin.z, bbMax.z, w) + offset.z
        );

        float att = attentionWeight(vec3(u, v, w));
        float vis = traceRay(samplePos, crystalPos);

        s_weightedHit[rayId] = vis * att;
        s_weight[rayId]      = att;
    }

    // ── shared memory 归约 ──
    barrier();

    // 仅 thread 0 做最终归约和伤害计算
    if (rayId == 0u) {
        float totalWeightedHit = 0.0;
        float totalWeight      = 0.0;
        for (int i = 0; i < TOTAL_SAMPLES; i++) {
            totalWeightedHit += s_weightedHit[i];
            totalWeight      += s_weight[i];
        }

        float exposure = totalWeightedHit / max(totalWeight, EPSILON);

        float doubleRadius = radius * 2.0;
        float dist = distance(targetPos + vec3(0.0, height * 0.5, 0.0), crystalPos) / doubleRadius;
        float inRange = step(dist, 1.0);

        float impact = (1.0 - dist) * exposure;
        float baseDamage = (impact * impact + impact) * 0.5 * 7.0 * doubleRadius + 1.0;

        float totalArmor  = task.params.x;
        float armorTough  = task.params.y;
        float enchantProt = task.params.z;
        float difficulty  = task.params.w;

        baseDamage = applyDifficulty(baseDamage, difficulty);
        baseDamage = applyArmor(baseDamage, totalArmor, armorTough);

        float enchantClamped = clamp(enchantProt, 0.0, 20.0);
        baseDamage *= (1.0 - enchantClamped / 25.0);

        float finalDamage = max(baseDamage * inRange * step(EPSILON, exposure), 0.0);
        resultBuf.damages[taskId] = finalDamage;
    }
}
