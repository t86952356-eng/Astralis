#version 330 core

uniform sampler2D textureIn;

layout(std140) uniform SphKawaseParams {
    vec2 texelSize;       // 0
    float offset;         // 8
    float _pad0;          // 12
    vec2 u_Center;        // 16
    vec2 u_HalfSize;      // 24
    float u_Radius;       // 32
    int u_ApplyRounding;  // 36
    int u_ForceOpaque;    // 40
    float _pad1;          // 44
};                        // total: 48 bytes

in vec2 uv;
out vec4 fragColor;

float roundedBoxSDF(vec2 p, vec2 size, float radius) {
    vec2 q = abs(p) - size + radius;
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - radius;
}

void main() {
    if (offset == 0.0) {
        vec4 color = texture(textureIn, uv);
        if (u_ForceOpaque == 1) color.a = 1.0;

        if (u_ApplyRounding == 1) {
            vec2 p = gl_FragCoord.xy - u_Center;
            float dist = roundedBoxSDF(p, u_HalfSize, u_Radius);
            float smoothing = fwidth(dist);
            float alpha = 1.0 - smoothstep(-smoothing, smoothing, dist);
            if (alpha <= 0.0) discard;
            color.a *= alpha;
        }

        fragColor = color;
        return;
    }

    vec2 off = texelSize * offset;

    vec4 sum = texture(textureIn, uv) * 4.0;
    sum += texture(textureIn, uv + vec2(-off.x, -off.y));
    sum += texture(textureIn, uv + vec2(off.x, -off.y));
    sum += texture(textureIn, uv + vec2(-off.x, off.y));
    sum += texture(textureIn, uv + vec2(off.x, off.y));

    sum += texture(textureIn, uv + vec2(-off.x, 0.0)) * 2.0;
    sum += texture(textureIn, uv + vec2(off.x, 0.0)) * 2.0;
    sum += texture(textureIn, uv + vec2(0.0, -off.y)) * 2.0;
    sum += texture(textureIn, uv + vec2(0.0, off.y)) * 2.0;

    fragColor = sum / 16.0;
}
