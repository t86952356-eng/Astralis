package astralis.util.render.sky;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline.Builder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;

import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;

import java.util.Optional;
import java.util.function.Function;

public final class SkyPipelines {
    private static Function<SkyBlendMode, RenderPipeline> pipelineCache;

    private SkyPipelines() {}

    public static RenderPipeline getPipeline(SkyBlendMode blendMode) {
        return pipelineCache.apply(blendMode);
    }

    private static RenderPipeline createPipeline(SkyBlendMode blendMode) {
        Builder builder = RenderPipeline.builder();

        String modeName = blendMode.name().toLowerCase();

        Builder locatedBuilder =
                builder.withLocation(
                        Identifier.fromNamespaceAndPath(
                                "astralis-recode", "pipeline/pack_sky_" + modeName));

        Builder vertexBuilder = locatedBuilder.withVertexShader("core/position_tex");

        Builder fragmentBuilder =
                vertexBuilder
                        .withFragmentShader("core/position_tex")
                        .withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION)
                        .withBindGroupLayout(BindGroupLayouts.SAMPLER0);
        Optional blendFunction = Optional.ofNullable(blendMode.getBlendFunction());
        return fragmentBuilder
                .withColorTargetState(new ColorTargetState(blendFunction, GpuFormat.RGBA8_UNORM, 7))
                .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX)
                .withPrimitiveTopology(PrimitiveTopology.QUADS)
                .build();
    }

    static {
        pipelineCache = Util.memoize(SkyPipelines::createPipeline);
    }
}
