#version 330 core

in vec2 v_TexCoords;
in vec4 v_Color;
in vec2 v_Gradient;

layout(location = 0) out vec4 fragColor;

uniform sampler2D u_Texture;

layout(std140) uniform SphWorldTextParams {
    mat4 u_ViewProjection;
    vec4 u_CameraPositionRange;
    vec4 u_AtlasSize;
    vec4 u_GradientStart;
    vec4 u_GradientEnd;
    vec4 u_GradientParams;
};

float median(float r, float g, float b) {
    return max(min(r, g), min(max(r, g), b));
}

float screenPxRange() {
    vec2 unitRange = vec2(u_CameraPositionRange.w) / u_AtlasSize.xy;
    vec2 screenTexSize = vec2(1.0) / fwidth(v_TexCoords);
    return max(0.5 * dot(unitRange, screenTexSize), 1.0);
}

void main() {
    vec3 msd = texture(u_Texture, v_TexCoords).rgb;
    float opacity = clamp(screenPxRange() * (median(msd.r, msd.g, msd.b) - 0.5) + 0.5, 0.0, 1.0);
    if (opacity < 0.01) discard;

    vec4 color = v_Color;
    if (u_AtlasSize.z > 0.5) {
        float gradientPosition = (sin(u_GradientParams.x * 3.0 + gl_FragCoord.x / 100.0) + 1.0) * 0.5;
        vec4 gradient = mix(u_GradientEnd, u_GradientStart, gradientPosition);
        color.rgb = mix(v_Color.rgb, gradient.rgb, clamp(v_Gradient.y, 0.0, 1.0));
    }

    fragColor = vec4(color.rgb, color.a * opacity);
}
