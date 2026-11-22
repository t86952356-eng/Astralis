#version 330 core

uniform sampler2D textureIn;
uniform sampler2D originalIn;

layout(std140) uniform SphHandGlowParams {
    vec4 glowColor1;        // 0
    vec4 glowColor2;        // 16
    vec4 outlineColor1;     // 32
    vec4 outlineColor2;     // 48
    float outlineThickness; // 64
    float glowIntensity;    // 68
    float glowBehind;       // 72
    float gradientSpeed;    // 76
    float gradientAngle;    // 80
    float time;             // 84
    vec2 gradientOffset;    // 88
};                          // total: 96 bytes

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
    for (int i = 0; i < 4; i++) {
        v += a * noise(p);
        p = rot * p * 2.0;
        a *= 0.5;
    }
    return v;
}

float gradient(vec2 coord) {
    vec2 p = coord * 3.0 + gradientOffset;
    return fbm(p + fbm(p + fbm(p)));
}

void main() {
    vec2 texelSize = 1.0 / textureSize(originalIn, 0);
    float blurredAlpha = texture(textureIn, uv).a;
    float originalAlpha = texture(originalIn, uv).a;

    if (originalAlpha > 0.5 && glowBehind < 0.5) discard;
    if (blurredAlpha <= 0.0001 && originalAlpha <= 0.0001) discard;
    if (glowIntensity <= 0.0 && outlineThickness <= 0.0) discard;

    float outlineVal = 0.0;
    if (outlineThickness > 0.0 && originalAlpha < 0.5) {
        int steps = int(ceil(outlineThickness));
        float radiusSquared = outlineThickness * outlineThickness;
        for (int x = -steps; x <= steps; x++) {
            for (int y = -steps; y <= steps; y++) {
                if (x == 0 && y == 0) continue;
                if (float(x * x + y * y) > radiusSquared) continue;
                float s = texture(originalIn, uv + vec2(x, y) * texelSize).a;
                if (s > 0.5) {
                    outlineVal = 1.0;
                    break;
                }
            }
            if (outlineVal > 0.0) break;
        }
    }

    vec4 gColor = glowColor1;
    vec4 oColor = outlineColor1;
    if (any(notEqual(glowColor1, glowColor2))
            || (outlineVal > 0.0 && any(notEqual(outlineColor1, outlineColor2)))) {
        float t = gradient(uv);
        gColor = mix(glowColor1, glowColor2, t);
        oColor = mix(outlineColor1, outlineColor2, t);
    }

    float glow = blurredAlpha * glowIntensity;
    glow = clamp(glow, 0.0, 1.0);

    vec3 color = gColor.rgb;
    float alpha = glow * gColor.a;

    if (outlineVal > 0.0) {
        color = mix(oColor.rgb, gColor.rgb, glow);
        alpha = max(oColor.a, alpha);
    }

    fragColor = vec4(color, alpha);
}
