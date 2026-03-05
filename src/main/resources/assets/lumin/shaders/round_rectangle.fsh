#version 410 core

smooth in vec2 f_Position;
smooth in vec4 f_Color;
flat in vec4 f_InnerRect;
flat in vec4 f_Radius;

layout(location = 0) out vec4 fragColor;

void main() {
    vec2 halfSize = (f_InnerRect.zw - f_InnerRect.xy) * 0.5;
    vec2 center = (f_InnerRect.xy + f_InnerRect.zw) * 0.5;
    vec2 p = f_Position - center;

    float r_current = (p.x > 0.0) ? 
        ((p.y > 0.0) ? f_Radius.z : f_Radius.y) : 
        ((p.y > 0.0) ? f_Radius.w : f_Radius.x);

    // SDF for rounded box
    vec2 q = abs(p) - halfSize + r_current;
    float dist = length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - r_current;

    // Smoothstep AA
    float alpha = 1.0 - smoothstep(-0.5, 0.5, dist);

    if (alpha < 0.001) discard;
    fragColor = vec4(f_Color.rgb, f_Color.a * alpha);
}
