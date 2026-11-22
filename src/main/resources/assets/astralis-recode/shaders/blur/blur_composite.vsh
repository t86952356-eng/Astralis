#version 330 core

layout(location = 0) in vec2 a_Pos;

out vec2 uv;

void main() {
    uv = a_Pos * 0.5 + 0.5;
    gl_Position = vec4(a_Pos, 0.0, 1.0);
}
