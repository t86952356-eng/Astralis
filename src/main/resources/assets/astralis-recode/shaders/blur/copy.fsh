#version 330 core

uniform sampler2D textureIn;

in vec2 uv;
out vec4 fragColor;

void main() {
    fragColor = texture(textureIn, uv);
}
