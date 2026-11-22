package astralis.mixin;

import astralis.Astralis;
import astralis.module.impl.Xray;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.QuadInstance;

import net.minecraft.client.renderer.block.BlockQuadOutput;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.BakedQuad.MaterialInfo;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.block.state.BlockState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ModelBlockRenderer.class)
public class ModelBlockRendererMixin {
    @WrapOperation(
            method = "putQuadWithTint",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/client/renderer/block/BlockQuadOutput;put(FFFLnet/minecraft/client/resources/model/geometry/BakedQuad;Lcom/mojang/blaze3d/vertex/QuadInstance;)V"))
    private void makeXrayQuadTranslucent(
            BlockQuadOutput blockQuadOutput,
            float x,
            float y,
            float z,
            BakedQuad bakedQuad,
            QuadInstance quadInstance,
            Operation original,
            @Local(argsOnly = true) BlockState blockState) {
        Xray xray = Astralis.getInstance().getModuleManager().getModule(Xray.class);
        if (xray != null && !xray.isTranslucencyInactive() && !xray.isSelectedBlock(blockState)) {
            int alpha = xray.getOpacityAlpha();

            for (int i = 0; i < 4; i++) {
                quadInstance.setColor(i, ARGB.color(alpha, quadInstance.getColor(i)));
            }

            MaterialInfo materialInfo2 = bakedQuad.materialInfo();
            BakedQuad bakedQuad2 =
                    materialInfo2.layer() == ChunkSectionLayer.TRANSLUCENT
                            ? bakedQuad
                            : new BakedQuad(
                                    bakedQuad.position0(),
                                    bakedQuad.position1(),
                                    bakedQuad.position2(),
                                    bakedQuad.position3(),
                                    bakedQuad.packedUV0(),
                                    bakedQuad.packedUV1(),
                                    bakedQuad.packedUV2(),
                                    bakedQuad.packedUV3(),
                                    bakedQuad.direction(),
                                    new MaterialInfo(
                                            materialInfo2.sprite(),
                                            ChunkSectionLayer.TRANSLUCENT,
                                            materialInfo2.itemRenderType(),
                                            materialInfo2.tintIndex(),
                                            materialInfo2.shade(),
                                            materialInfo2.lightEmission()));
            original.call(blockQuadOutput, x, y, z, bakedQuad2, quadInstance);
        } else {
            original.call(blockQuadOutput, x, y, z, bakedQuad, quadInstance);
        }
    }
}
