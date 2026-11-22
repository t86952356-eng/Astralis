#version 330 core

uniform sampler2D textureIn;

in vec2 uv;
out vec4 fragColor;

void main() {
    fragColor = vec4(texture(textureIn, uv).rgb, 1.0);
}
