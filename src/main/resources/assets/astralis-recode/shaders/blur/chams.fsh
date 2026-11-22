#version 330 core

uniform sampler2D originalIn;

layout(std140) uniform SphChamsParams {
    vec4 chamsColor1;    // 0
    vec4 chamsColor2;    // 16
    float chamsMix;      // 32
    float gradientSpeed; // 36
    float gradientAngle; // 40
    float time;          // 44
    vec2 gradientOffset; // 48
    vec2 _pad;           // 56
};                       // total: 64 bytes

in vec2 uv;
out vec4 fragColor;

float hash(vec2 p) {
    vec3 p3 = fract(vec3(p.xyx) * vec3(443.897, 441.423, 437.195));
    p3 += dot(p3, p3.yzx + 19.19);
    return fract((p3.x + p3.y) * p3.z);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash(i), hash(i + vec2(1, 0)), u.x),
               mix(hash(i + vec2(0, 1)), hash(i + vec2(1, 1)), u.x), u.y);
}

float fbm(vec2 p) {
    float v = 0.0, a = 0.5;
    mat2 rot = mat2(0.8, 0.6, -0.6, 0.8);
    for (int i = 0; i < 3; i++) {
        v += a * noise(p);
        p = rot * p * 2.0;
        a *= 0.5;
    }
    return v;
}

void main() {
    float mask = texture(originalIn, uv).a;
    if (mask < 0.5) discard;

    vec2 p = uv * 3.0 + gradientOffset;
    float t = fbm(p);
    vec4 color = mix(chamsColor1, chamsColor2, t);

    fragColor = vec4(color.rgb, chamsMix * color.a);
}
