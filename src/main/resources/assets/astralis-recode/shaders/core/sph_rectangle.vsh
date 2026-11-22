#version 330 core

layout (location = 0) in vec2 a_Pos;
layout (location = 1) in vec4 a_Color;
layout (location = 2) in vec2 a_Size;
layout (location = 3) in vec4 a_Radii;        // TL, TR, BR, BL
layout (location = 4) in vec4 a_OutlineColor;
layout (location = 5) in float a_OutlineWidth;
layout (location = 6) in vec2 a_LocalPos;

layout(std140) uniform SphProjection {
    mat4 u_Projection;
};

out vec4 v_Color;
out vec2 v_Size;
out vec4 v_Radii;
out vec4 v_OutlineColor;
out float v_OutlineWidth;
out vec2 v_LocalPos;

void main() {
    v_Color = a_Color;
    v_Size = a_Size;
    v_Radii = a_Radii;
    v_OutlineColor = a_OutlineColor;
    v_OutlineWidth = a_OutlineWidth;
    v_LocalPos = a_LocalPos;

    gl_Position = u_Projection * vec4(a_Pos, 0.0, 1.0);
}
