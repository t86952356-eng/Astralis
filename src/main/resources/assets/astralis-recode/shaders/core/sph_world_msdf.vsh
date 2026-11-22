#version 330 core

layout(location = 0) in vec3 a_Position;
layout(location = 1) in vec2 a_TexCoords;
layout(location = 2) in vec4 a_Color;
layout(location = 3) in vec2 a_Gradient;

layout(std140) uniform SphWorldTextParams {
    mat4 u_ViewProjection;
    vec4 u_CameraPositionRange;
    vec4 u_AtlasSize;
    vec4 u_GradientStart;
    vec4 u_GradientEnd;
    vec4 u_GradientParams;
};

out vec2 v_TexCoords;
out vec4 v_Color;
out vec2 v_Gradient;

void main() {
    v_TexCoords = a_TexCoords;
    v_Color = a_Color;
    v_Gradient = a_Gradient;
    gl_Position = u_ViewProjection * vec4(a_Position - u_CameraPositionRange.xyz, 1.0);
}
