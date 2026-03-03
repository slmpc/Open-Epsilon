#version 410 core

in vec4 v_Color;
in vec2 v_TexCoord;

uniform sampler2D Sampler0;

layout(std140) uniform TtfInfo {
    float EdgeThreshold;
};

layout(location = 0) out vec4 f_Color;

void main() {
    float distance = 1.0f - texture(Sampler0, v_TexCoord).r;

    float afwidth = fwidth(distance) * 0.5;

    float alpha = smoothstep(EdgeThreshold - afwidth, EdgeThreshold + afwidth, distance);

    f_Color = vec4(v_Color.rgb, v_Color.a * alpha);

    if (f_Color.a < 0.01) {
        discard;
    }
}