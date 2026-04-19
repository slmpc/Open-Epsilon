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

layout(std430, set = 0, binding = 0) readonly buffer VoxelGrid {
    ivec4 header;       // xyz = gridOrigin, w = gridSize
    uint  voxelBits[];
} grid;

struct Task {
    vec4 crystalPos;    // xyz = 爆炸中心, w = 半径
    vec4 targetPos;     // xyz = 目标脚底, w = unused
    vec4 targetSize;    // x = halfWidth, y = height
    vec4 params;        // x = armor, y = toughness, z = enchantProt, w = difficulty
    vec4 extra;         // x = resistanceMultiplier, y = applyDifficulty(0/1)
};

layout(std430, set = 0, binding = 1) readonly buffer TaskBuffer {
    uint taskCount;
    uint _pad0;
    uint _pad1;
    uint _pad2;
    Task tasks[];
} taskBuf;

layout(std430, set = 0, binding = 2) writeonly buffer ResultBuffer {
    float damages[];
} resultBuf;

const float EPSILON = 1e-6;
const int   RAY_STEPS = 64;

// 注意力采样网格 (每轴)
const int SAMPLE_X = 3;
const int SAMPLE_Y = 5;
const int SAMPLE_Z = 3;
const int TOTAL_SAMPLES = SAMPLE_X * SAMPLE_Y * SAMPLE_Z; // = 45

shared float s_weightedHit[64];
shared float s_weight[64];

// 采样体素值（0 或 1），越界返回 0
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

// DDA 射线行进

float traceRay(vec3 origin, vec3 target) {
    vec3 dir = target - origin;
    float maxDist = length(dir);
    if (maxDist <= EPSILON) return 1.0;

    vec3 d = dir / maxDist;

    ivec3 pos = ivec3(floor(origin));
    ivec3 stepDir = ivec3(
        d.x > 0.0 ? 1 : (d.x < 0.0 ? -1 : 0),
        d.y > 0.0 ? 1 : (d.y < 0.0 ? -1 : 0),
        d.z > 0.0 ? 1 : (d.z < 0.0 ? -1 : 0)
    );

    vec3 tDelta = vec3(
        d.x != 0.0 ? abs(1.0 / d.x) : 1e20,
        d.y != 0.0 ? abs(1.0 / d.y) : 1e20,
        d.z != 0.0 ? abs(1.0 / d.z) : 1e20
    );

    vec3 fracOrigin = origin - floor(origin);
    vec3 tMax = vec3(
        stepDir.x > 0 ? (1.0 - fracOrigin.x) * tDelta.x : (stepDir.x < 0 ? fracOrigin.x * tDelta.x : 1e20),
        stepDir.y > 0 ? (1.0 - fracOrigin.y) * tDelta.y : (stepDir.y < 0 ? fracOrigin.y * tDelta.y : 1e20),
        stepDir.z > 0 ? (1.0 - fracOrigin.z) * tDelta.z : (stepDir.z < 0 ? fracOrigin.z * tDelta.z : 1e20)
    );

    float blocked = 0.0;
    float active_ = 1.0;

    for (int i = 0; i < RAY_STEPS; i++) {
        // 固定优先级 X -> Y -> Z，避免边界条件下的随机抖动。
        float chooseX = float(tMax.x <= tMax.y && tMax.x <= tMax.z);
        float chooseY = float(chooseX < 0.5 && tMax.y <= tMax.z);
        float chooseZ = 1.0 - chooseX - chooseY;

        float traveled = tMax.x * chooseX + tMax.y * chooseY + tMax.z * chooseZ;

        pos += ivec3(vec3(chooseX, chooseY, chooseZ) * vec3(stepDir));
        tMax += vec3(chooseX, chooseY, chooseZ) * tDelta;

        float within = step(traveled, maxDist) * active_;
        float solid = sampleVoxel(pos);
        blocked = max(blocked, solid * within);

        active_ *= (1.0 - blocked) * within;
    }

    return 1.0 - blocked;
}

// 等权采样
float attentionWeight(vec3 uvw) {
    // 为了与 CPU 逻辑对齐，曝光率使用等权采样。
    return 1.0;
}

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

void main() {
    uint taskId = gl_WorkGroupID.x;
    uint rayId  = gl_LocalInvocationID.x;

    s_weightedHit[rayId] = 0.0;
    s_weight[rayId]      = 0.0;

    if (taskId >= taskBuf.taskCount) {
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
    vec3 stepSize = vec3(1.0) / (bbSize * 2.0 + vec3(1.0));
    offset = (vec3(1.0) - floor(vec3(1.0) / stepSize) * stepSize) * 0.5;
    offset.y = 0.0;

    // ── 每个线程追踪一条射线（超出采样数的线程按 0 权重参与） ──
    float rayActive = float(rayId < uint(TOTAL_SAMPLES));
    uint sampleId = min(rayId, uint(TOTAL_SAMPLES - 1));

    uint iz = sampleId / uint(SAMPLE_X * SAMPLE_Y);
    uint rem = sampleId % uint(SAMPLE_X * SAMPLE_Y);
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

    s_weightedHit[rayId] = vis * att * rayActive;
    s_weight[rayId]      = att * rayActive;

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
        float dist = distance(targetPos, crystalPos) / doubleRadius;
        float inRange = step(dist, 1.0);

        float impact = (1.0 - dist) * exposure;
        float baseDamage = (impact * impact + impact) * 0.5 * 7.0 * doubleRadius + 1.0;

        float totalArmor  = task.params.x;
        float armorTough  = task.params.y;
        float enchantProt = task.params.z;
        float difficulty  = task.params.w;
        float resistanceMul = task.extra.x;
        float applyDifficultyFlag = clamp(task.extra.y, 0.0, 1.0);

        float difficultyScaled = applyDifficulty(baseDamage, difficulty);
        baseDamage = mix(baseDamage, difficultyScaled, applyDifficultyFlag);
        baseDamage = applyArmor(baseDamage, totalArmor, armorTough);
        baseDamage *= resistanceMul;

        float enchantClamped = clamp(enchantProt, 0.0, 20.0);
        baseDamage *= (1.0 - enchantClamped / 25.0);

        float finalDamage = max(baseDamage * inRange * step(EPSILON, exposure), 0.0);
        resultBuf.damages[taskId] = finalDamage;
    }
}
