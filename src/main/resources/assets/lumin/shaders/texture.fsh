#version 410 core

smooth in vec2 f_Position;
smooth in vec4 f_Color;
smooth in vec2 f_TexCoord;
flat in vec4 f_InnerRect;
flat in float f_Radius;

layout(location = 0) out vec4 fragColor;

uniform sampler2D Sampler0;

float aastep(float x) {
    float afwidth = fwidth(x);
    return smoothstep(-afwidth, afwidth, x);
}

void main() {
    vec4 tex = texture(Sampler0, f_TexCoord);
    vec4 color = tex * f_Color;

    vec2 tl = f_InnerRect.xy - f_Position;
    vec2 br = f_Position - f_InnerRect.zw;

    vec2 dis = max(tl, br);

    float v = length(max(vec2(0.0), dis)) - f_Radius;

    float alpha = 1.0 - aastep(v);

    if (alpha < 0.001 || color.a < 0.01) discard;
    fragColor = vec4(color.rgb, color.a * alpha);
}
