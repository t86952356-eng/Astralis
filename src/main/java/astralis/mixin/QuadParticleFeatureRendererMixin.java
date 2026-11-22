package astralis.mixin;

import astralis.Astralis;
import astralis.module.impl.Particles;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.textures.GpuTextureView;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.feature.FeatureFrameContext;
import net.minecraft.client.renderer.feature.QuadParticleFeatureRenderer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(QuadParticleFeatureRenderer.class)
public class QuadParticleFeatureRendererMixin {
    @Inject(method = "executeGroup", at = @At("HEAD"))
    private void copyDepthToGlowTarget(
            FeatureFrameContext featureFrameContext,
            int groupIndex,
            List submits,
            boolean translucent,
            CallbackInfo callbackInfo) {
        Particles particles = Astralis.getInstance().getModuleManager().getModule(Particles.class);
        if (particles != null && particles.isGlowActive() && !submits.isEmpty()) {
            Minecraft minecraft2 = Minecraft.getInstance();
            particles.copyDepthOnce(minecraft2.gameRenderer.mainRenderTarget());
        }
    }

    @Redirect(
            method = "executeGroup",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lcom/mojang/blaze3d/pipeline/RenderTarget;getColorTextureView()Lcom/mojang/blaze3d/textures/GpuTextureView;"))
    private GpuTextureView redirectColorTextureToGlowTarget(RenderTarget renderTarget) {
        Particles particles = Astralis.getInstance().getModuleManager().getModule(Particles.class);
        RenderTarget renderTarget2 = particles == null ? null : particles.getGlowTarget();
        return renderTarget2 != null && particles.isGlowActive()
                ? renderTarget2.getColorTextureView()
                : renderTarget.getColorTextureView();
    }

    @Redirect(
            method = "executeGroup",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lcom/mojang/blaze3d/pipeline/RenderTarget;getDepthTextureView()Lcom/mojang/blaze3d/textures/GpuTextureView;"))
    private GpuTextureView redirectDepthTextureToGlowTarget(RenderTarget renderTarget) {
        Particles particles = Astralis.getInstance().getModuleManager().getModule(Particles.class);
        RenderTarget renderTarget2 = particles == null ? null : particles.getGlowTarget();
        return renderTarget2 != null && particles.isGlowActive()
                ? renderTarget2.getDepthTextureView()
                : renderTarget.getDepthTextureView();
    }
}
