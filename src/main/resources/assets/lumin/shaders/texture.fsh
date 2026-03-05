#version 410 core

smooth in vec2 f_Position;
smooth in vec4 f_Color;
smooth in vec2 f_TexCoord;
flat in vec4 f_InnerRect;
flat in vec4 f_Radius;

layout(location = 0) out vec4 fragColor;

uniform sampler2D Sampler0;

float aastep(float x) {
    float afwidth = fwidth(x);
    return smoothstep(-afwidth, afwidth, x);
}

void main() {
    vec4 tex = texture(Sampler0, f_TexCoord);
    vec4 color = tex * f_Color;

    vec2 center = (f_InnerRect.xy + f_InnerRect.zw) / 2.0;
    vec2 halfSize = (f_InnerRect.zw - f_InnerRect.xy) / 2.0;
    vec2 pos = f_Position - center;

    float r = 0.0;
    if (pos.x < 0.0) {
        if (pos.y < 0.0) r = f_Radius.x; // TL
        else r = f_Radius.w; // BL
    } else {
        if (pos.y < 0.0) r = f_Radius.y; // TR
        else r = f_Radius.z; // BR
    }

    vec2 halfSize2 = (f_InnerRect.zw - f_InnerRect.xy) * 0.5;
    vec2 center2 = (f_InnerRect.xy + f_InnerRect.zw) * 0.5;
    vec2 p = f_Position - center2;

    float r_current = (p.x > 0.0) ? 
        ((p.y > 0.0) ? f_Radius.z : f_Radius.y) : 
        ((p.y > 0.0) ? f_Radius.w : f_Radius.x);

    vec2 q = abs(p) - halfSize2 + r_current;
    float dist = length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - r_current;

    float alpha = 1.0 - smoothstep(-0.5, 0.5, dist); // Simple AA
    
    if (alpha < 0.001 || color.a < 0.01) discard;
    fragColor = vec4(color.rgb, color.a * alpha);
}
