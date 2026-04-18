#version 450

layout(local_size_x = 64) in;

layout(std430, set = 0, binding = 0) readonly buffer InputBuffer {
    float data[];
} inputData;

layout(std430, set = 0, binding = 1) writeonly buffer OutputBuffer {
    float data[];
} outputData;

void main() {
    uint index = gl_GlobalInvocationID.x;

    if (index >= 128) {
        return;
    }

    outputData.data[index] = inputData.data[index];
}