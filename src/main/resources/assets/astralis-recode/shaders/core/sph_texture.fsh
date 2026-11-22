#version 330 core

in vec4 v_Color;
in vec2 v_TexCoords;
in vec2 v_Size;
in vec4 v_Radii;
in vec2 v_LocalPos;
in float v_TexIndex;

layout(location = 0) out vec4 fragColor;

uniform sampler2D u_Tex0;
uniform sampler2D u_Tex1;
uniform sampler2D u_Tex2;
uniform sampler2D u_Tex3;
uniform sampler2D u_Tex4;
uniform sampler2D u_Tex5;
uniform sampler2D u_Tex6;
uniform sampler2D u_Tex7;
uniform sampler2D u_Tex8;
uniform sampler2D u_Tex9;
uniform sampler2D u_Tex10;
uniform sampler2D u_Tex11;

float roundedBoxSDF(vec2 p, vec2 size, vec4 r) {
    float radius = (p.x > 0.0)
        ? ((p.y > 0.0) ? r.z : r.y)
        : ((p.y > 0.0) ? r.w : r.x);

    vec2 q = abs(p) - size + radius;
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - radius;
}

void main() {
    vec4 sampledTexture;

    int index = int(v_TexIndex);
    switch (index) {
        case 0:  sampledTexture = texture(u_Tex0,  v_TexCoords); break;
        case 1:  sampledTexture = texture(u_Tex1,  v_TexCoords); break;
        case 2:  sampledTexture = texture(u_Tex2,  v_TexCoords); break;
        case 3:  sampledTexture = texture(u_Tex3,  v_TexCoords); break;
        case 4:  sampledTexture = texture(u_Tex4,  v_TexCoords); break;
        case 5:  sampledTexture = texture(u_Tex5,  v_TexCoords); break;
        case 6:  sampledTexture = texture(u_Tex6,  v_TexCoords); break;
        case 7:  sampledTexture = texture(u_Tex7,  v_TexCoords); break;
        case 8:  sampledTexture = texture(u_Tex8,  v_TexCoords); break;
        case 9:  sampledTexture = texture(u_Tex9,  v_TexCoords); break;
        case 10: sampledTexture = texture(u_Tex10, v_TexCoords); break;
        case 11: sampledTexture = texture(u_Tex11, v_TexCoords); break;
        default: sampledTexture = texture(u_Tex0,  v_TexCoords); break;
    }

    vec4 finalColor = sampledTexture * v_Color;
    bool outerAntialiasOnly = min(min(v_Radii.x, v_Radii.y), min(v_Radii.z, v_Radii.w)) < 0.0;
    vec4 radii = abs(v_Radii);

    if (max(max(radii.x, radii.y), max(radii.z, radii.w)) <= 0.0) {
        fragColor = finalColor;
        return;
    }

    vec2 halfSize = v_Size * 0.5;
    float dist = roundedBoxSDF(v_LocalPos, halfSize, radii);
    float alpha = outerAntialiasOnly
        ? 1.0 - smoothstep(0.0, 0.5, dist)
        : 1.0 - smoothstep(-0.5, 0.5, dist);

    if (alpha <= 0.0) discard;

    fragColor = vec4(finalColor.rgb, finalColor.a * alpha);
}
