#version 330 core

uniform sampler2D u_Texture;
uniform sampler2D u_OriginalTexture;

layout(std140) uniform SphShadowCompositeParams {
    vec4 u_Color; // 0: 16 bytes
};

in vec2 uv;
out vec4 fragColor;

void main() {
    float shadowAlpha = texture(u_Texture, uv).a;
    float maskAlpha = texture(u_OriginalTexture, uv).a;

    float finalAlpha = shadowAlpha * (1.0 - maskAlpha);
    if (finalAlpha <= 0.0001) discard;

    fragColor = vec4(u_Color.rgb, u_Color.a * finalAlpha);
}
