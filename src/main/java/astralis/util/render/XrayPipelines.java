package astralis.util.render;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.RenderPipeline;

import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

public final class XrayPipelines {
    public static RenderPipeline translucentTerrainPipeline =
            RenderPipelines.register(
                    RenderPipeline.builder(RenderPipelines.TERRAIN_SNIPPET)
                            .withLocation(
                                    Identifier.fromNamespaceAndPath(
                                            "astralis-recode", "pipeline/xray_translucent_terrain"))
                            .withShaderDefine("ALPHA_CUTOUT", 0.1F)
                            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
                            .build());

    private XrayPipelines() {}
}
