#version 330 core

#define NUM_OCTAVES 6

layout(std140) uniform SphSandboxParams {
    vec2 resolution; // 0
    vec2 offset;     // 8
    vec4 color1;     // 16 (rgb used, a unused)
    vec4 color2;     // 32 (rgb used, a unused)
    float time;      // 48
    float alpha;     // 52
    vec2 _pad;       // 56
};                   // total: 64 bytes

out vec4 fragColor;

float random(vec2 pos) {
    return fract(sin(dot(pos.xy, vec2(13.9898, 78.233))) * 43758.5453123);
}
float noise(vec2 pos) {
    vec2 i = floor(pos);
    vec2 f = fract(pos);
    float a = random(i + vec2(0.0, 0.0));
    float b = random(i + vec2(1.0, 0.0));
    float c = random(i + vec2(0.0, 1.0));
    float d = random(i + vec2(1.0, 1.0));
    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(a, b, u.x) + (c - a) * u.y * (1.0 - u.x) + (d - b) * u.x * u.y;
}
float fbm(vec2 pos) {
    float v = 0.0;
    float a = 0.5;
    vec2 shift = vec2(100.0);
    mat2 rot = mat2(cos(0.5), sin(0.5), -sin(0.5), cos(0.5));
    for (int i = 0; i < NUM_OCTAVES; i++) {
        float dir = mod(float(i), 2.0) > 0.5 ? 1.0 : -1.0;
        v += a * noise(pos - 0.05 * dir * time);
        pos = rot * pos * 2.0 + shift;
        a *= 0.5;
    }
    return v;
}

void main(void) {
    vec2 localCoord = gl_FragCoord.xy - offset;

    vec2 p = (localCoord * 3.0 - resolution.xy) / min(resolution.x, resolution.y);
    p -= vec2(12.0, 0.0);

    float time2 = 2.0;

    vec2 q = vec2(0.0);
    q.x = fbm(p + 0.00 * time2);
    q.y = fbm(p + vec2(1.0));

    vec2 r = vec2(0.0);
    r.x = fbm(p + 1.0 * q + vec2(1.7, 1.2) + 0.15 * time2);
    r.y = fbm(p + 1.0 * q + vec2(8.3, 2.8) + 0.126 * time2);

    float f = fbm(p + r);

    vec3 col = mix(color1.rgb, color1.rgb, clamp((f * f) * 5.5, 1.2, 15.5));
    col = mix(col, color1.rgb, clamp(length(q), 2.0, 2.0));
    col = mix(col, color2.rgb, clamp(length(r.x), 0.0, 5.0));

    col = (f * f * f * 1.0 + 0.5 * 1.7 * 0.0 + 0.9 * f) * col;

    vec2 uv = localCoord / resolution.xy;

    float shape = 50.0 - max(pow(100.0 * distance(uv.x, -1.0), 0.0), pow(2.0 * distance(uv.y, 0.5), 5.0));
    float shapeMask = clamp(shape, 0.0, 1.0);

    fragColor = vec4(col, shapeMask * alpha);
}
