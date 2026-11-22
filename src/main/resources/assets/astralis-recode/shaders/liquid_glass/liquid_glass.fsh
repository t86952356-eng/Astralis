#version 330 core

uniform sampler2D u_BlurredScene;

layout(std140) uniform SphLiquidGlassParams {
    vec2 u_Center;          // 0
    vec2 u_HalfSize;        // 8
    vec2 u_Resolution;      // 16
    float u_CornerRadius;   // 24
    float u_PowerFactor;    // 28
    float u_RefractionA;    // 32
    float u_RefractionB;    // 36
    float u_RefractionC;    // 40
    float u_RefractionD;    // 44
    float u_RefractionPower;// 48
    float u_Noise;          // 52
    float u_GlowWeight;     // 56
    float u_GlowEdge0;      // 60
    float u_GlowEdge1;      // 64
    float _pad0;            // 68
    float _pad1;            // 72
    float _pad2;            // 76
    vec4 u_GlassColor;      // 80 (aligned to 16)
};                          // total: 96 bytes

in vec2 uv;
out vec4 fragColor;

const float M_E = 2.718281828459045;

float rand(vec2 co) {
    return fract(sin(dot(co, vec2(12.9898, 78.233))) * 43758.5453);
}

vec3 roundedBoxSdg(vec2 p, vec2 halfSize, float radius) {
    vec2 q = abs(p) - halfSize + radius;
    float g = max(q.x, q.y);
    vec2 qm = max(q, 0.0);
    float l = length(qm);
    float d = (g > 0.0) ? l - radius : g - radius;
    vec2 s = sign(p);
    vec2 n = (l > 1e-6) ? s * (qm / l) : s * ((q.x > q.y) ? vec2(1.0, 0.0) : vec2(0.0, 1.0));
    return vec3(d, n);
}

float refractionFalloff(float x) {
    return 1.0 - u_RefractionB * pow(u_RefractionC * M_E, -u_RefractionD * x - u_RefractionA);
}

void main() {
    vec2 p = gl_FragCoord.xy - u_Center;

    vec3 sdg = roundedBoxSdg(p, u_HalfSize, u_CornerRadius);
    float sdf = sdg.x;
    vec2 outwardNormal = sdg.yz;

    float aa = fwidth(sdf);
    float alpha = 1.0 - smoothstep(-aa, aa, sdf);
    if (alpha <= 0.001) discard;

    float minHalf = min(u_HalfSize.x, u_HalfSize.y);
    float dist = clamp(-sdf / max(minHalf, 1.0), 0.0, 1.5);

    float falloff = clamp(refractionFalloff(dist), 0.0, 1.0);
    float edgeFactor = 1.0 - pow(falloff, u_RefractionPower);

    vec2 uvBase = gl_FragCoord.xy / u_Resolution;
    vec2 aspect = vec2(u_Resolution.y / u_Resolution.x, 1.0);
    vec2 refrBase = -outwardNormal * edgeFactor * u_PowerFactor * 0.02 * aspect;

    vec2 disp = outwardNormal * edgeFactor * 0.004 * aspect;
    vec4 color;
    color.r = texture(u_BlurredScene, clamp(uvBase + refrBase + disp, 0.0, 1.0)).r;
    color.g = texture(u_BlurredScene, clamp(uvBase + refrBase,        0.0, 1.0)).g;
    color.b = texture(u_BlurredScene, clamp(uvBase + refrBase - disp, 0.0, 1.0)).b;
    color.a = 1.0;

    float grain = (rand(gl_FragCoord.xy * 1e-3) - 0.5) * u_Noise;
    color.rgb += grain;

    float glowAngle = sin(atan(p.y, p.x) - 0.5);
    float glowMask = smoothstep(u_GlowEdge0, u_GlowEdge1, dist);
    color.rgb *= (1.0 + glowAngle * u_GlowWeight * glowMask);

    color.rgb = mix(color.rgb, u_GlassColor.rgb, u_GlassColor.a);

    float rimAlpha = smoothstep(2.5, 0.0, abs(sdf)) * 0.12;
    color.rgb += rimAlpha;

    color.a = alpha;
    fragColor = color;
}
