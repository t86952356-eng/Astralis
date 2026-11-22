package astralis.util.render;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.BindGroupLayout;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

import java.util.Optional;

public final class AstralisPipelines {
    private static String namespace;
    public static RenderPipeline rectangle;
    public static RenderPipeline rectangleDepthWrite;
    public static RenderPipeline rectangleDepthRead;
    public static RenderPipeline textureQuad;
    public static RenderPipeline msdfText;
    public static RenderPipeline worldMsdfText;
    public static RenderPipeline kawaseBlur;
    public static RenderPipeline separableBlur;
    public static RenderPipeline kawaseCopy;
    public static RenderPipeline kawaseOpaqueCopy;
    public static RenderPipeline sandbox;
    public static RenderPipeline skyShader;
    public static RenderPipeline skyCustom;
    public static RenderPipeline liquidGlass;
    public static RenderPipeline glow;
    public static RenderPipeline handGlow;
    public static RenderPipeline handGlowOffscreen;
    public static RenderPipeline glowComposite;
    public static RenderPipeline chams;
    public static RenderPipeline shadowRect;
    public static RenderPipeline shadowComposite;
    private static BindGroupLayout projectionLayout;

    private AstralisPipelines() {}

    public static boolean isPipelineValid(RenderPipeline renderPipeline) {
        return RenderSystem.getDevice().precompilePipeline(renderPipeline).isValid();
    }

    static {
        namespace = "astralis-recode";
        projectionLayout =
                BindGroupLayout.builder()
                        .withUniform("SphProjection", UniformType.UNIFORM_BUFFER)
                        .build();
        rectangle =
                RenderPipelines.register(
                        RenderPipeline.builder()
                                .withLocation(
                                        Identifier.fromNamespaceAndPath(
                                                "astralis-recode", "pipeline/sph_rectangle"))
                                .withVertexShader(
                                        Identifier.fromNamespaceAndPath(
                                                "astralis-recode", "core/sph_rectangle"))
                                .withFragmentShader(
                                        Identifier.fromNamespaceAndPath(
                                                "astralis-recode", "core/sph_rectangle"))
                                .withVertexBinding(0, RenderVertexFormats.rectangleFormat)
                                .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
                                .withColorTargetState(
                                        new ColorTargetState(BlendFunction.TRANSLUCENT))
                                .withCull(false)
                                .withBindGroupLayout(projectionLayout)
                                .build());
        rectangleDepthWrite =
                RenderPipelines.register(
                        RenderPipeline.builder()
                                .withLocation(
                                        Identifier.fromNamespaceAndPath(
                                                "astralis-recode",
                                                "pipeline/sph_rectangle_depth_write"))
                                .withVertexShader(
                                        Identifier.fromNamespaceAndPath(
                                                "astralis-recode", "core/sph_rectangle"))
                                .withFragmentShader(
                                        Identifier.fromNamespaceAndPath(
                                                "astralis-recode", "core/sph_rectangle"))
                                .withVertexBinding(0, RenderVertexFormats.rectangleFormat)
                                .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
                                .withColorTargetState(
                                        new ColorTargetState(
                                                Optional.empty(), GpuFormat.RGBA8_UNORM, 0))
                                .withDepthStencilState(
                                        new DepthStencilState(CompareOp.ALWAYS_PASS, true))
                                .withCull(false)
                                .withBindGroupLayout(projectionLayout)
                                .build());
        rectangleDepthRead =
                RenderPipelines.register(
                        RenderPipeline.builder()
                                .withLocation(
                                        Identifier.fromNamespaceAndPath(
                                                "astralis-recode",
                                                "pipeline/sph_rectangle_depth_read"))
                                .withVertexShader(
                                        Identifier.fromNamespaceAndPath(
                                                "astralis-recode", "core/sph_rectangle"))
                                .withFragmentShader(
                                        Identifier.fromNamespaceAndPath(
                                                "astralis-recode", "core/sph_rectangle"))
                                .withVertexBinding(0, RenderVertexFormats.rectangleFormat)
                                .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
                                .withColorTargetState(
                                        new ColorTargetState(BlendFunction.TRANSLUCENT))
                                .withDepthStencilState(
                                        new DepthStencilState(CompareOp.EQUAL, false))
                                .withCull(false)
                                .withBindGroupLayout(projectionLayout)
                                .build());
        textureQuad =
                RenderPipelines.register(
                        RenderPipeline.builder()
                                .withLocation(
                                        Identifier.fromNamespaceAndPath(
                                                "astralis-recode", "pipeline/sph_texture"))
                                .withVertexShader(
                                        Identifier.fromNamespaceAndPath(
                                                "astralis-recode", "core/sph_texture"))
                                .withFragmentShader(
                                        Identifier.fromNamespaceAndPath(
                                                "astralis-recode", "core/sph_texture"))
                                .withVertexBinding(0, RenderVertexFormats.textureFormat)
                                .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
                                .withColorTargetState(
                                        new ColorTargetState(BlendFunction.TRANSLUCENT))
                                .withCull(false)
                                .withBindGroupLayout(
                                        BindGroupLayout.builder()
                                                .withUniform(
                                                        "SphProjection", UniformType.UNIFORM_BUFFER)
                                                .withSampler("u_Tex0")
                                                .withSampler("u_Tex1")
                                                .withSampler("u_Tex2")
                                                .withSampler("u_Tex3")
                                                .withSampler("u_Tex4")
                                                .withSampler("u_Tex5")
                                                .withSampler("u_Tex6")
                                                .withSampler("u_Tex7")
                                                .withSampler("u_Tex8")
                                                .withSampler("u_Tex9")
                                                .withSampler("u_Tex10")
                                                .withSampler("u_Tex11")
                                                .build())
                                .build());
        msdfText =
                RenderPipelines.register(
                        RenderPipeline.builder()
                                .withLocation(
                                        Identifier.fromNamespaceAndPath(
                                                "astralis-recode", "pipeline/sph_msdf"))
                                .withVertexShader(
                                        Identifier.fromNamespaceAndPath(
                                                "astralis-recode", "core/sph_msdf"))
                                .withFragmentShader(
                                        Identifier.fromNamespaceAndPath(
                                                "astralis-recode", "core/sph_msdf"))
                                .withVertexBinding(0, RenderVertexFormats.msdfTextFormat)
                                .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
                                .withColorTargetState(
                                        new ColorTargetState(BlendFunction.TRANSLUCENT))
                                .withCull(false)
                                .withBindGroupLayout(
                                        BindGroupLayout.builder()
                                                .withUniform(
                                                        "SphProjection", UniformType.UNIFORM_BUFFER)
                                                .withUniform(
                                                        "SphMsdfParams", UniformType.UNIFORM_BUFFER)
                                                .withSampler("u_Texture")
                                                .build())
                                .build());
        worldMsdfText =
                RenderPipelines.register(
                        RenderPipeline.builder()
                                .withLocation(
                                        Identifier.fromNamespaceAndPath(
                                                "astralis-recode", "pipeline/sph_world_msdf"))
                                .withVertexShader(
                                        Identifier.fromNamespaceAndPath(
                                                "astralis-recode", "core/sph_world_msdf"))
                                .withFragmentShader(
                                        Identifier.fromNamespaceAndPath(
                                                "astralis-recode", "core/sph_world_msdf"))
                                .withVertexBinding(0, RenderVertexFormats.worldMsdfTextFormat)
                                .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
                                .withColorTargetState(
                                        new ColorTargetState(BlendFunction.TRANSLUCENT))
                                .withDepthStencilState(
                                        new DepthStencilState(
                                                CompareOp.GREATER_THAN_OR_EQUAL, false))
                                .withCull(false)
                                .withBindGroupLayout(
                                        BindGroupLayout.builder()
                                                .withUniform(
                                                        "SphWorldTextParams",
                                                        UniformType.UNIFORM_BUFFER)
                                                .withSampler("u_Texture")
                                                .build())
                                .build());
        kawaseBlur =
                RenderPipelines.register(
                        RenderPipeline.builder()
                                .withLocation(
                                        Identifier.fromNamespaceAndPath(
                                                "astralis-recode", "pipeline/sph_kawase"))
                                .withVertexShader(
                                        Identifier.fromNamespaceAndPath(
                                                "astralis-recode", "blur/blur_composite"))
                                .withFragmentShader(
                                        Identifier.fromNamespaceAndPath(
                                                "astralis-recode", "blur/kawase"))
                                .withVertexBinding(0, RenderVertexFormats.fullscreenFormat)
                                .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
                                .withColorTargetState(
                                        new ColorTargetState(
                                                Optional.empty(), GpuFormat.RGBA8_UNORM, 15))
                                .withCull(false)
                                .withBindGroupLayout(
                                        BindGroupLayout.builder()
                                                .withUniform(
                                                        "SphKawaseParams",
                                                        UniformType.UNIFORM_BUFFER)
                                                .withSampler("textureIn")
                                                .build())
                                .build());
        separableBlur =
                RenderPipelines.register(
                        RenderPipeline.builder()
                                .withLocation(
                                        Identifier.fromNamespaceAndPath(
                                                "astralis-recode", "pipeline/sph_separable_blur"))
                                .withVertexShader(
                                        Identifier.fromNamespaceAndPath(
                                                "astralis-recode", "blur/blur_composite"))
                                .withFragmentShader(
                                        Identifier.fromNamespaceAndPath(
                                                "astralis-recode", "blur/separable"))
                                .withVertexBinding(0, RenderVertexFormats.fullscreenFormat)
                                .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
                                .withColorTargetState(
                                        new ColorTargetState(
                                                Optional.empty(), GpuFormat.RGBA8_UNORM, 15))
                                .withCull(false)
                                .withBindGroupLayout(
                                        BindGroupLayout.builder()
                                                .withUniform(
                                                        "SphSeparableBlurParams",
                                                        UniformType.UNIFORM_BUFFER)
                                                .withSampler("textureIn")
                                                .build())
                                .build());
        kawaseCopy =
                RenderPipelines.register(
                        RenderPipeline.builder()
                                .withLocation(
                                        Identifier.fromNamespaceAndPath(
                                                "astralis-recode", "pipeline/sph_kawase_copy"))
                                .withVertexShader(
                                        Identifier.fromNamespaceAndPath(
                                                "astralis-recode", "blur/blur_composite"))
                                .withFragmentShader(
                                        Identifier.fromNamespaceAndPath(
                                                "astralis-recode", "blur/copy"))
                                .withVertexBinding(0, RenderVertexFormats.fullscreenFormat)
                                .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
                                .withColorTargetState(
                                        new ColorTargetState(
                                                Optional.empty(), GpuFormat.RGBA8_UNORM, 15))
                                .withCull(false)
                                .withBindGroupLayout(
                                        BindGroupLayout.builder().withSampler("textureIn").build())
                                .build());
        kawaseOpaqueCopy =
                RenderPipelines.register(
                        RenderPipeline.builder()
                                .withLocation(
                                        Identifier.fromNamespaceAndPath(
                                                "astralis-recode",
                                                "pipeline/sph_kawase_opaque_copy"))
                                .withVertexShader(
                                        Identifier.fromNamespaceAndPath(
                                                "astralis-recode", "blur/blur_composite"))
                                .withFragmentShader(
                                        Identifier.fromNamespaceAndPath(
                                                "astralis-recode", "blur/copy_opaque"))
                                .withVertexBinding(0, RenderVertexFormats.fullscreenFormat)
                                .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
                                .withColorTargetState(
                                        new ColorTargetState(
                                                Optional.empty(), GpuFormat.RGBA8_UNORM, 15))
                                .withCull(false)
                                .withBindGroupLayout(
                                        BindGroupLayout.builder().withSampler("textureIn").build())
                                .build());
        sandbox =
                RenderPipelines.register(
                        RenderPipeline.builder()
                                .withLocation(
                                        Identifier.fromNamespaceAndPath(
                                                "astralis-recode", "pipeline/sph_sandbox"))
                                .withVertexShader(
                                        Identifier.fromNamespaceAndPath(
                                                "astralis-recode", "misc/sandbox"))
                                .withFragmentShader(
                                        Identifier.fromNamespaceAndPath(
                                                "astralis-recode", "misc/sandbox"))
                                .withVertexBinding(0, RenderVertexFormats.fullscreenFormat)
                                .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
                                .withColorTargetState(
                                        new ColorTargetState(BlendFunction.TRANSLUCENT))
                                .withCull(false)
                                .withBindGroupLayout(
                                        BindGroupLayout.builder()
                                                .withUniform(
                                                        "SphSandboxParams",
                                                        UniformType.UNIFORM_BUFFER)
                                                .build())
                                .build());
        skyShader =
                RenderPipelines.register(
                        RenderPipeline.builder()
                                .withLocation(
                                        Identifier.fromNamespaceAndPath(
                                                "astralis-recode", "pipeline/sph_sky_shader"))
                                .withVertexShader(
                                        Identifier.fromNamespaceAndPath(
                                                "astralis-recode", "misc/sky_shader"))
                                .withFragmentShader(
                                        Identifier.fromNamespaceAndPath(
                                                "astralis-recode", "misc/sky_shader"))
                                .withVertexBinding(0, RenderVertexFormats.fullscreenFormat)
                                .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
                                .withColorTargetState(
                                        new ColorTargetState(
                                                Optional.empty(), GpuFormat.RGBA8_UNORM, 15))
                                .withCull(false)
                                .withBindGroupLayout(
                                        BindGroupLayout.builder()
                                                .withUniform(
                                                        "SphSkyParams", UniformType.UNIFORM_BUFFER)
                                                .build())
                                .build());
        skyCustom =
                RenderPipelines.register(
                        RenderPipeline.builder()
                                .withLocation(
                                        Identifier.fromNamespaceAndPath(
                                                "astralis-recode", "pipeline/sph_sky_custom"))
                                .withVertexShader(
                                        Identifier.fromNamespaceAndPath(
                                                "astralis-recode", "misc/sky_shader"))
                                .withFragmentShader(
                                        Identifier.fromNamespaceAndPath(
                                                "astralis-recode", "misc/sky_custom"))
                                .withVertexBinding(0, RenderVertexFormats.fullscreenFormat)
                                .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
                                .withColorTargetState(
                                        new ColorTargetState(
                                                Optional.empty(), GpuFormat.RGBA8_UNORM, 15))
                                .withCull(false)
                                .withBindGroupLayout(
                                        BindGroupLayout.builder()
                                                .withUniform(
                                                        "SphSkyParams", UniformType.UNIFORM_BUFFER)
                                                .withUniform(
                                                        "SphSkyCustomParams",
                                                        UniformType.UNIFORM_BUFFER)
                                                .build())
                                .build());
        liquidGlass =
                RenderPipelines.register(
                        RenderPipeline.builder()
                                .withLocation(
                                        Identifier.fromNamespaceAndPath(
                                                "astralis-recode", "pipeline/sph_liquid_glass"))
                                .withVertexShader(
                                        Identifier.fromNamespaceAndPath(
                                                "astralis-recode", "liquid_glass/liquid_glass"))
                                .withFragmentShader(
                                        Identifier.fromNamespaceAndPath(
                                                "astralis-recode", "liquid_glass/liquid_glass"))
                                .withVertexBinding(0, RenderVertexFormats.fullscreenFormat)
                                .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
                                .withColorTargetState(
                                        new ColorTargetState(BlendFunction.TRANSLUCENT))
                                .withCull(false)
                                .withBindGroupLayout(
                                        BindGroupLayout.builder()
                                                .withUniform(
                                                        "SphLiquidGlassParams",
                                                        UniformType.UNIFORM_BUFFER)
                                                .withSampler("u_BlurredScene")
                                                .build())
                                .build());
        glow =
                RenderPipelines.register(
                        RenderPipeline.builder()
                                .withLocation(
                                        Identifier.fromNamespaceAndPath(
                                                "astralis-recode", "pipeline/sph_glow"))
                                .withVertexShader(
                                        Identifier.fromNamespaceAndPath(
                                                "astralis-recode", "blur/blur_composite"))
                                .withFragmentShader(
                                        Identifier.fromNamespaceAndPath(
                                                "astralis-recode", "blur/glow_final"))
                                .withVertexBinding(0, RenderVertexFormats.fullscreenFormat)
                                .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
                                .withColorTargetState(new ColorTargetState(BlendFunction.LIGHTNING))
                                .withCull(false)
                                .withBindGroupLayout(
                                        BindGroupLayout.builder()
                                                .withUniform(
                                                        "SphGlowParams", UniformType.UNIFORM_BUFFER)
                                                .withSampler("textureIn")
                                                .build())
                                .build());
        handGlow =
                RenderPipelines.register(
                        RenderPipeline.builder()
                                .withLocation(
                                        Identifier.fromNamespaceAndPath(
                                                "astralis-recode", "pipeline/sph_hand_glow"))
                                .withVertexShader(
                                        Identifier.fromNamespaceAndPath(
                                                "astralis-recode", "blur/blur_composite"))
                                .withFragmentShader(
                                        Identifier.fromNamespaceAndPath(
                                                "astralis-recode", "blur/glow_enhanced"))
                                .withVertexBinding(0, RenderVertexFormats.fullscreenFormat)
                                .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
                                .withColorTargetState(
                                        new ColorTargetState(BlendFunction.TRANSLUCENT))
                                .withCull(false)
                                .withBindGroupLayout(
                                        BindGroupLayout.builder()
                                                .withUniform(
                                                        "SphHandGlowParams",
                                                        UniformType.UNIFORM_BUFFER)
                                                .withSampler("textureIn")
                                                .withSampler("originalIn")
                                                .build())
                                .build());
        handGlowOffscreen =
                RenderPipelines.register(
                        RenderPipeline.builder()
                                .withLocation(
                                        Identifier.fromNamespaceAndPath(
                                                "astralis-recode",
                                                "pipeline/sph_hand_glow_offscreen"))
                                .withVertexShader(
                                        Identifier.fromNamespaceAndPath(
                                                "astralis-recode", "blur/blur_composite"))
                                .withFragmentShader(
                                        Identifier.fromNamespaceAndPath(
                                                "astralis-recode", "blur/glow_enhanced"))
                                .withVertexBinding(0, RenderVertexFormats.fullscreenFormat)
                                .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
                                .withColorTargetState(
                                        new ColorTargetState(
                                                Optional.empty(), GpuFormat.RGBA8_UNORM, 15))
                                .withCull(false)
                                .withBindGroupLayout(
                                        BindGroupLayout.builder()
                                                .withUniform(
                                                        "SphHandGlowParams",
                                                        UniformType.UNIFORM_BUFFER)
                                                .withSampler("textureIn")
                                                .withSampler("originalIn")
                                                .build())
                                .build());
        glowComposite =
                RenderPipelines.register(
                        RenderPipeline.builder()
                                .withLocation(
                                        Identifier.fromNamespaceAndPath(
                                                "astralis-recode", "pipeline/sph_glow_composite"))
                                .withVertexShader(
                                        Identifier.fromNamespaceAndPath(
                                                "astralis-recode", "blur/blur_composite"))
                                .withFragmentShader(
                                        Identifier.fromNamespaceAndPath(
                                                "astralis-recode", "blur/copy"))
                                .withVertexBinding(0, RenderVertexFormats.fullscreenFormat)
                                .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
                                .withColorTargetState(
                                        new ColorTargetState(BlendFunction.TRANSLUCENT))
                                .withCull(false)
                                .withBindGroupLayout(
                                        BindGroupLayout.builder().withSampler("textureIn").build())
                                .build());
        chams =
                RenderPipelines.register(
                        RenderPipeline.builder()
                                .withLocation(
                                        Identifier.fromNamespaceAndPath(
                                                "astralis-recode", "pipeline/sph_chams"))
                                .withVertexShader(
                                        Identifier.fromNamespaceAndPath(
                                                "astralis-recode", "blur/blur_composite"))
                                .withFragmentShader(
                                        Identifier.fromNamespaceAndPath(
                                                "astralis-recode", "blur/chams"))
                                .withVertexBinding(0, RenderVertexFormats.fullscreenFormat)
                                .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
                                .withColorTargetState(
                                        new ColorTargetState(BlendFunction.TRANSLUCENT))
                                .withCull(false)
                                .withBindGroupLayout(
                                        BindGroupLayout.builder()
                                                .withUniform(
                                                        "SphChamsParams",
                                                        UniformType.UNIFORM_BUFFER)
                                                .withSampler("originalIn")
                                                .build())
                                .build());
        shadowRect =
                RenderPipelines.register(
                        RenderPipeline.builder()
                                .withLocation(
                                        Identifier.fromNamespaceAndPath(
                                                "astralis-recode", "pipeline/sph_shadow_rect"))
                                .withVertexShader(
                                        Identifier.fromNamespaceAndPath(
                                                "astralis-recode", "kawase_shadow/shadow"))
                                .withFragmentShader(
                                        Identifier.fromNamespaceAndPath(
                                                "astralis-recode", "kawase_shadow/shadow"))
                                .withVertexBinding(0, RenderVertexFormats.fullscreenFormat)
                                .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
                                .withColorTargetState(
                                        new ColorTargetState(
                                                Optional.empty(), GpuFormat.RGBA8_UNORM, 15))
                                .withCull(false)
                                .withBindGroupLayout(
                                        BindGroupLayout.builder()
                                                .withUniform(
                                                        "SphShadowRectParams",
                                                        UniformType.UNIFORM_BUFFER)
                                                .build())
                                .build());
        shadowComposite =
                RenderPipelines.register(
                        RenderPipeline.builder()
                                .withLocation(
                                        Identifier.fromNamespaceAndPath(
                                                "astralis-recode", "pipeline/sph_shadow_composite"))
                                .withVertexShader(
                                        Identifier.fromNamespaceAndPath(
                                                "astralis-recode", "kawase_shadow/shadow"))
                                .withFragmentShader(
                                        Identifier.fromNamespaceAndPath(
                                                "astralis-recode",
                                                "kawase_shadow/shadow_composite"))
                                .withVertexBinding(0, RenderVertexFormats.fullscreenFormat)
                                .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
                                .withColorTargetState(
                                        new ColorTargetState(BlendFunction.TRANSLUCENT))
                                .withCull(false)
                                .withBindGroupLayout(
                                        BindGroupLayout.builder()
                                                .withUniform(
                                                        "SphShadowCompositeParams",
                                                        UniformType.UNIFORM_BUFFER)
                                                .withSampler("u_Texture")
                                                .withSampler("u_OriginalTexture")
                                                .build())
                                .build());
    }
}
