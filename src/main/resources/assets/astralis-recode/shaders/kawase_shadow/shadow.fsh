#version 330 core

layout(std140) uniform SphShadowRectParams {
    vec2 u_pos;     // 0
    vec2 u_size;    // 8
    float u_radius; // 16
    float u_spread; // 20
    vec2 _pad;      // 24
    vec4 u_color;   // 32 (aligned to 16)
};                  // total: 48 bytes

in vec2 uv;
out vec4 fragColor;

float roundedBoxSDF(vec2 centerPos, vec2 size, float radius) {
    return length(max(abs(centerPos) - size + radius, 0.0)) - radius;
}

void main() {
    vec2 rectCenter = u_pos + u_size * 0.5;
    vec2 p = gl_FragCoord.xy - rectCenter;
    vec2 halfSize = u_size * 0.5;

    float dist = roundedBoxSDF(p, halfSize, u_radius);

    float softness = 1.0;
    float alpha = smoothstep(softness, -softness, dist - u_spread);

    fragColor = vec4(u_color.rgb, u_color.a * alpha);
}
