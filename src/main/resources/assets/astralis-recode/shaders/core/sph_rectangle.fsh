#version 330 core

in vec4 v_Color;
in vec2 v_Size;
in vec4 v_Radii;
in vec4 v_OutlineColor;
in float v_OutlineWidth;
in vec2 v_LocalPos;

layout(location = 0) out vec4 fragColor;

float roundedBoxSDF(vec2 p, vec2 size, vec4 r) {
    float radius = (p.x > 0.0)
        ? ((p.y > 0.0) ? r.z : r.y)
        : ((p.y > 0.0) ? r.w : r.x);

    vec2 q = abs(p) - size + radius;
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - radius;
}

void main() {
    vec2 halfSize = v_Size * 0.5;
    bool outerAntialiasOnly = min(min(v_Radii.x, v_Radii.y), min(v_Radii.z, v_Radii.w)) < 0.0;
    vec4 radii = abs(v_Radii);

    if (max(max(radii.x, radii.y), max(radii.z, radii.w)) <= 0.0 && v_OutlineWidth <= 0.0) {
        fragColor = v_Color;
        return;
    }

    float currentRadius = (v_LocalPos.x > 0.0)
        ? ((v_LocalPos.y > 0.0) ? radii.z : radii.y)
        : ((v_LocalPos.y > 0.0) ? radii.w : radii.x);

    vec2 cornerOffset = abs(v_LocalPos) - (halfSize - currentRadius);
    float dist = roundedBoxSDF(v_LocalPos, halfSize, radii);
    float distFromCorner = length(max(cornerOffset, 0.0));

    // kys cioc
    float paddingFactor = currentRadius > 0.0
        ? smoothstep(currentRadius * 0.5, currentRadius * 1.5, distFromCorner)
        : 0.0;
    float padding = mix(0.5 * fwidth(dist), 0.0, paddingFactor);

    dist -= padding;

    float smoothing = fwidth(dist);
    float alpha = outerAntialiasOnly
        ? 1.0 - smoothstep(0.0, smoothing, dist)
        : 1.0 - smoothstep(-smoothing, smoothing, dist);

    if (alpha <= 0.0) discard;

    vec4 finalColor = v_Color;

    if (v_OutlineWidth > 0.0) {
        float outlineFactor = smoothstep(-v_OutlineWidth - smoothing, -v_OutlineWidth + smoothing, dist);
        finalColor = mix(v_OutlineColor, v_Color, outlineFactor);
    }

    fragColor = vec4(finalColor.rgb, finalColor.a * alpha);
}
