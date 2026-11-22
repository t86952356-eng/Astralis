package astralis.util.render;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.vertex.VertexFormat;

public final class RenderVertexFormats {
    public static VertexFormat rectangleFormat =
            VertexFormat.builder(0)
                    .addAttribute("a_Pos", GpuFormat.RG32_FLOAT)
                    .addAttribute("a_Color", GpuFormat.RGBA32_FLOAT)
                    .addAttribute("a_Size", GpuFormat.RG32_FLOAT)
                    .addAttribute("a_Radii", GpuFormat.RGBA32_FLOAT)
                    .addAttribute("a_OutlineColor", GpuFormat.RGBA32_FLOAT)
                    .addAttribute("a_OutlineWidth", GpuFormat.R32_FLOAT)
                    .addAttribute("a_LocalPos", GpuFormat.RG32_FLOAT)
                    .build();
    public static VertexFormat textureFormat =
            VertexFormat.builder(0)
                    .addAttribute("a_Pos", GpuFormat.RG32_FLOAT)
                    .addAttribute("a_Color", GpuFormat.RGBA32_FLOAT)
                    .addAttribute("a_TexCoords", GpuFormat.RG32_FLOAT)
                    .addAttribute("a_Size", GpuFormat.RG32_FLOAT)
                    .addAttribute("a_Radii", GpuFormat.RGBA32_FLOAT)
                    .addAttribute("a_LocalPos", GpuFormat.RG32_FLOAT)
                    .addAttribute("a_TexIndex", GpuFormat.R32_FLOAT)
                    .build();
    public static VertexFormat msdfTextFormat =
            VertexFormat.builder(0)
                    .addAttribute("a_Pos", GpuFormat.RG32_FLOAT)
                    .addAttribute("a_TexCoords", GpuFormat.RG32_FLOAT)
                    .addAttribute("a_Color", GpuFormat.RGBA32_FLOAT)
                    .build();
    public static VertexFormat worldMsdfTextFormat =
            VertexFormat.builder(0)
                    .addAttribute("a_Position", GpuFormat.RGB32_FLOAT)
                    .addAttribute("a_TexCoords", GpuFormat.RG32_FLOAT)
                    .addAttribute("a_Color", GpuFormat.RGBA32_FLOAT)
                    .addAttribute("a_Gradient", GpuFormat.RG32_FLOAT)
                    .build();
    public static VertexFormat fullscreenFormat =
            VertexFormat.builder(0).addAttribute("a_Pos", GpuFormat.RG32_FLOAT).build();

    private RenderVertexFormats() {}
}
