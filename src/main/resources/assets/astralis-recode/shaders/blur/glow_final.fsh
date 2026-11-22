#version 330 core

uniform sampler2D textureIn;

layout(std140) uniform SphGlowParams {
    vec4 glowColor; // 0: 16 bytes
};

in vec2 uv;
out vec4 fragColor;

void main() {
    float alpha = texture(textureIn, uv).a;
    if (alpha <= 0.0001) discard;
    fragColor = vec4(glowColor.rgb, alpha * glowColor.a);
}
