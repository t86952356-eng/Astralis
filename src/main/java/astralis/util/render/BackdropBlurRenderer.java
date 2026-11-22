package astralis.util.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;

public class BackdropBlurRenderer extends AbstractBlurRenderer {
    public BackdropBlurRenderer(Render2DContext context) {
        super(context);
    }

    public void blurRegion(
            GpuTexture gpuTexture,
            float x,
            float y,
            float width,
            float height,
            int radius,
            float scale) {
        this.blurRoundedRegion(gpuTexture, x, y, width, height, 0.0F, radius, scale);
    }

    @Override
    protected RenderPipeline getDefaultPipeline() {
        return AstralisPipelines.kawaseBlur;
    }

    public void blurRoundedRegion(
            GpuTexture gpuTexture,
            float x,
            float y,
            float width,
            float height,
            float cornerRadius,
            int radius,
            float scale) {
        if (gpuTexture != null) {
            if (this.isBlurAvailable()) {
                this.transformPoint(x, y);
                float transformedX = this.scratchVector.x;
                float transformedY = this.scratchVector.y;
                this.transformVector(width, height);
                float transformedWidth = this.scratchVector.x;
                float transformedHeight = this.scratchVector.y;
                this.transformVector(radius, 0.0F);
                float transformedRadius = this.scratchVector.x;
                int targetWidth = this.getRenderContext().getTargetWidth();
                int targetHeight = this.getRenderContext().getTargetHeight();
                double guiScale = this.getRenderContext().getGuiScale();
                int blurWidth = (int) (targetWidth * scale);
                int blurHeight = (int) (targetHeight * scale);
                this.resizeBlurTargets(blurWidth, blurHeight);
                int pixelX = (int) (transformedX * guiScale);
                int pixelWidth = (int) (transformedWidth * guiScale);
                int pixelHeight = (int) (transformedHeight * guiScale);
                int pixelY = (int) (targetHeight - transformedY * guiScale - pixelHeight);
                GpuTextureView gpuTextureView = this.getOrCreateTextureView(gpuTexture);
                if (gpuTextureView != null) {
                    RenderTarget renderTarget = this.getRenderContext().getRenderTarget();
                    if (renderTarget.width != blurWidth
                            || renderTarget.height != blurHeight
                            || !this.trySeparableBlur(
                                    gpuTextureView,
                                    Math.max(1, (int) transformedRadius),
                                    blurWidth,
                                    blurHeight,
                                    1.0F / targetWidth,
                                    1.0F / targetHeight,
                                    1.0F / blurWidth,
                                    1.0F / blurHeight,
                                    renderTarget,
                                    true)) {
                        RenderTarget renderTarget2 =
                                this.blurTexture(
                                        gpuTextureView,
                                        Math.max(1, (int) transformedRadius),
                                        blurWidth,
                                        blurHeight,
                                        1.0F / targetWidth,
                                        1.0F / targetHeight,
                                        1.0F / blurWidth,
                                        1.0F / blurHeight);
                        this.drawFinalKawasePass(
                                renderTarget2,
                                this.getRenderContext().getRenderTarget(),
                                1.0F / blurWidth,
                                1.0F / blurHeight,
                                pixelX + pixelWidth * 0.5F,
                                pixelY + pixelHeight * 0.5F,
                                pixelWidth * 0.5F,
                                pixelHeight * 0.5F,
                                cornerRadius * (float) guiScale,
                                pixelX,
                                pixelY,
                                pixelWidth,
                                pixelHeight,
                                true);
                    }
                }
            }
        }
    }
}
