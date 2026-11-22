#version 330 core

#define NUM_OCTAVES 3

layout(std140) uniform SphSkyParams {
    vec4  color1;
    vec4  color2;
    vec2  resolution;
    float time;
    float alpha;
    mat4  invViewProj;
};

in  vec2 clipPos;
out vec4 fragColor;

float hash(vec3 p) {
    p = fract(p * vec3(127.1, 311.7, 74.7));
    p += dot(p, p.yzx + 19.19);
    return fract((p.x + p.y) * p.z);
}

float valueNoise3(vec3 p) {
    vec3 i = floor(p);
    vec3 f = fract(p);
    vec3 u = f * f * (3.0 - 2.0 * f);
    return mix(mix(mix(hash(i),              hash(i + vec3(1,0,0)), u.x),
                   mix(hash(i + vec3(0,1,0)), hash(i + vec3(1,1,0)), u.x), u.y),
               mix(mix(hash(i + vec3(0,0,1)), hash(i + vec3(1,0,1)), u.x),
                   mix(hash(i + vec3(0,1,1)), hash(i + vec3(1,1,1)), u.x), u.y), u.z);
}

float fbm(vec3 p) {
    float v = 0.0, a = 0.5;
    mat3 rot = mat3( 0.00,  0.80,  0.60,
                    -0.80,  0.36, -0.48,
                    -0.60, -0.48,  0.64);
    for (int i = 0; i < NUM_OCTAVES; i++) {
        v += a * valueNoise3(p);
        p = rot * p * 2.0;
        a *= 0.5;
    }
    return v;
}

void main() {
    vec4 worldDir = invViewProj * vec4(clipPos, 1.0, 1.0);
    vec3 dir = normalize(worldDir.xyz / worldDir.w);

    vec3 p = dir * 3.0 + vec3(time * 0.025, time * 0.008, -time * 0.018);
    float macro = fbm(p);
    float detail = fbm(p * 1.9 + vec3(macro * 0.9, macro * 0.55, -macro * 0.7));
    float cloud = smoothstep(0.46, 0.78, macro * 0.68 + detail * 0.32);

    float horizon = smoothstep(-0.35, 0.65, dir.y);
    vec3 sky = mix(color1.rgb, color2.rgb, horizon);
    vec3 cloudColor = mix(color1.rgb, color2.rgb, clamp(detail * 1.35, 0.0, 1.0));
    vec3 col = mix(sky, cloudColor, cloud * 0.6);
    col *= 0.62 + 0.38 * clamp(macro + detail * 0.35, 0.0, 1.0);

    fragColor = vec4(col, alpha);
}
