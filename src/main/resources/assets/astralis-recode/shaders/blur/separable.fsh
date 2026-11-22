#version 330 core

uniform sampler2D textureIn;

layout(std140) uniform SphSeparableBlurParams {
    vec2 texelSize;  // 0
    vec2 direction;  // 8
    float radius;    // 16
    float forceOpaque; // 20
    vec2 _pad;       // 24
};                   // total: 32 bytes

in vec2 uv;
out vec4 fragColor;

void main() {
    vec2 offset = direction * texelSize * max(0.5, radius * 0.67);
    vec4 color = texture(textureIn, uv) * 0.204164;
    color += texture(textureIn, uv + offset) * 0.180174;
    color += texture(textureIn, uv - offset) * 0.180174;
    color += texture(textureIn, uv + offset * 2.0) * 0.123831;
    color += texture(textureIn, uv - offset * 2.0) * 0.123831;
    color += texture(textureIn, uv + offset * 3.0) * 0.066283;
    color += texture(textureIn, uv - offset * 3.0) * 0.066283;
    color += texture(textureIn, uv + offset * 4.0) * 0.027630;
    color += texture(textureIn, uv - offset * 4.0) * 0.027630;
    if (forceOpaque > 0.5) color.a = 1.0;
    fragColor = color;
}
