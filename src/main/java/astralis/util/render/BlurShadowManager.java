package astralis.util.render;

import astralis.Astralis;
import astralis.module.impl.Effects;

import com.astralis.scripting.InstanceAccess;
import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;

import java.awt.Color;
import java.util.function.Consumer;

public class BlurShadowManager implements InstanceAccess {
    private RenderTarget blurOutputTarget;
    private Render2DContext uiRenderer;
    private long cachedFrameId;
    private int cachedIterations;
    private int cachedWindowWidth;
    private int cachedWindowHeight;
    private int cachedScaleBits;
    private boolean fullResolution;
    private boolean cacheValid;

    public boolean isFullResolution() {
        return this.fullResolution;
    }

    public void blurRectangle(float x, float y, float width, float height) {
        this.blurRoundedRectangle(x, y, width, height, 0.0F);
    }

    public void updateBlurSnapshot(
            Render2DContext renderer, int iterations, float downsampleFactor) {
        this.updateBlurSnapshot(renderer, iterations, downsampleFactor, false);
    }

    public void drawLiquidGlassRectangle(
            float x, float y, float width, float height, float radius, Color color) {
        this.uiRenderer.runCustom(
                () -> {
                    this.uiRenderer
                            .getLiquidGlassRenderer()
                            .renderWithDefaults(
                                    this.blurOutputTarget != null
                                            ? this.blurOutputTarget.getColorTexture()
                                            : null,
                                    x,
                                    y,
                                    width,
                                    height,
                                    radius,
                                    color);
                });
    }

    public int getCachedIterations() {
        return this.cachedIterations;
    }

    public long getCachedFrameId() {
        return this.cachedFrameId;
    }

    public void blurRoundedRectangle(float x, float y, float width, float height, float radius) {
        this.blurRoundedRectangleWithAlpha(x, y, width, height, radius, 1.0F);
    }

    public void renderRecordedShadows() {
        if (this.uiRenderer != null) {
            Effects effects = Astralis.getInstance().getModuleManager().getModule(Effects.class);
            if (effects.isEnabled() && effects.shadow.getValue()) {
                this.uiRenderer
                        .getShadowRenderer()
                        .renderRecordedShapes(
                                ColorUtil.withAlpha(
                                        Color.black,
                                        (int) Math.min(225.0F, effects.shadowAlpha.getValue())),
                                effects.shadowIterations.getValue().intValue(),
                                effects.shadowDownsampleFactor.getValue());
            }
        }
    }

    public void invalidateCache() {
        this.cacheValid = false;
    }

    public void beginFrame(Render2DContext renderer) {
        this.uiRenderer = renderer;
        Effects effects = Astralis.getInstance().getModuleManager().getModule(Effects.class);
        if (effects.isEnabled() && effects.blur.getValue()) {
            this.updateBlurSnapshot(
                    renderer,
                    effects.blurIterations.getValue().intValue(),
                    effects.blurDownsampleFactor.getValue());
        }
    }

    public void updateBlurSnapshot(
            Render2DContext renderer,
            int iterations,
            float downsampleFactor,
            boolean forceFullResolution) {
        this.uiRenderer = renderer;
        int windowWidth = mc.getWindow().getWidth();
        int windowHeight = mc.getWindow().getHeight();
        int scaledWidth = Math.max(1, (int) (windowWidth * downsampleFactor));
        int scaledHeight = Math.max(1, (int) (windowHeight * downsampleFactor));
        boolean blurPipelineValid =
                AstralisPipelines.isPipelineValid(AstralisPipelines.separableBlur);
        int targetWidth = !forceFullResolution && blurPipelineValid ? scaledWidth : windowWidth;
        int targetHeight = !forceFullResolution && blurPipelineValid ? scaledHeight : windowHeight;
        int scaleBits = Float.floatToIntBits(downsampleFactor);
        if (this.blurOutputTarget == null) {

            this.blurOutputTarget =
                    new TextureTarget(
                            "Sph Blur Output",
                            targetWidth,
                            targetHeight,
                            false,
                            GpuFormat.RGBA8_UNORM);
        } else if (this.blurOutputTarget.width != targetWidth
                || this.blurOutputTarget.height != targetHeight) {
            this.blurOutputTarget.resize(targetWidth, targetHeight);
        }

        if (!this.cacheValid
                || this.cachedFrameId != renderer.getFrameCounter()
                || this.cachedIterations != iterations
                || this.cachedWindowWidth != windowWidth
                || this.cachedWindowHeight != windowHeight
                || this.cachedScaleBits != scaleBits
                || this.fullResolution != forceFullResolution) {
            renderer.flush();
            RenderTarget renderTarget = renderer.getRenderTarget();
            renderer.setRenderTarget(this.blurOutputTarget);
            renderer.getBlurRenderer()
                    .blurRoundedRegion(
                            mc.gameRenderer.mainRenderTarget().getColorTexture(),
                            0.0F,
                            0.0F,
                            mc.getWindow().getGuiScaledWidth(),
                            mc.getWindow().getGuiScaledHeight(),
                            0.0F,
                            iterations,
                            downsampleFactor);
            renderer.setRenderTarget(renderTarget);
            this.cachedFrameId = renderer.getFrameCounter();
            this.cachedIterations = iterations;
            this.cachedWindowWidth = windowWidth;
            this.cachedWindowHeight = windowHeight;
            this.cachedScaleBits = scaleBits;
            this.fullResolution = forceFullResolution;
            this.cacheValid = true;
        }
    }

    public void drawBlurredRoundedRectangle(
            float x, float y, float width, float height, float radius, float alpha) {
        this.drawBlurredRectangle(
                x, y, width, height, radius, radius, radius, radius, alpha, false);
    }

    public void recreateOutputTarget(int width, int height) {
        if (this.blurOutputTarget != null) {
            this.blurOutputTarget.destroyBuffers();
        }

        this.blurOutputTarget =
                new TextureTarget("Sph Blur Output", width, height, false, GpuFormat.RGBA8_UNORM);
        this.cacheValid = false;
    }

    public void drawBlurredRectangle(
            float x,
            float y,
            float width,
            float height,
            float topLeftRadius,
            float topRightRadius,
            float bottomRightRadius,
            float bottomLeftRadius,
            float alpha,
            boolean outerAntialiasOnly) {
        if (this.uiRenderer != null) {
            if (this.blurOutputTarget != null) {
                float targetWidth = this.uiRenderer.getTargetWidth();
                float targetHeight = this.uiRenderer.getTargetHeight();
                float guiScale = this.uiRenderer.getGuiScale();
                float uvLeft = x * guiScale / targetWidth;
                float uvTop = 1.0F - y * guiScale / targetHeight;
                float uvRight = (x + width) * guiScale / targetWidth;
                float uvBottom = 1.0F - (y + height) * guiScale / targetHeight;
                Render2DContext renderer = this.uiRenderer;
                TexturedRectShape shape =
                        TexturedRectShape.obtain()
                                .setBounds(x, y, width, height)
                                .setCornerRadii(
                                        topLeftRadius,
                                        topRightRadius,
                                        bottomRightRadius,
                                        bottomLeftRadius)
                                .setOuterAntialiasOnly(outerAntialiasOnly)
                                .setTexture(this.blurOutputTarget.getColorTexture())
                                .setUv(uvLeft, uvTop, uvRight, uvBottom);

                renderer.drawTexture(
                        shape.setColor(new Color(255, 255, 255, (int) (alpha * 255.0F))));
            }
        }
    }

    public void blurRoundedRectangleWithAlpha(
            float x, float y, float width, float height, float radius, float alpha) {
        if (this.uiRenderer != null) {
            Effects effects = Astralis.getInstance().getModuleManager().getModule(Effects.class);
            if (effects.isEnabled() && effects.blur.getValue()) {
                this.drawBlurredRoundedRectangle(x, y, width, height, radius, alpha);
            }
        }
    }

    public int getCachedScaleBits() {
        return this.cachedScaleBits;
    }

    public void drawClearLiquidGlassRectangle(
            float x, float y, float width, float height, float radius) {

        this.drawLiquidGlassRectangle(x, y, width, height, radius, new Color(255, 255, 255, 0));
    }

    public void recreateOutputTargetAtWindowSize() {

        this.blurOutputTarget =
                new TextureTarget(
                        "Sph Blur Output",
                        mc.getWindow().getWidth(),
                        mc.getWindow().getHeight(),
                        false,
                        GpuFormat.RGBA8_UNORM);
        this.cacheValid = false;
    }

    public void renderShadowShapes(Consumer<Render2DContext> shapeDrawer) {
        if (this.uiRenderer != null) {
            Effects effects = Astralis.getInstance().getModuleManager().getModule(Effects.class);
            if (effects.isEnabled() && effects.shadow.getValue()) {
                this.uiRenderer
                        .getShadowRenderer()
                        .renderShapes(
                                shapeDrawer,
                                ColorUtil.withAlpha(
                                        Color.black,
                                        (int) Math.min(225.0F, effects.shadowAlpha.getValue())),
                                effects.shadowIterations.getValue().intValue(),
                                effects.shadowDownsampleFactor.getValue());
            }
        }
    }

    public void blurRectangleWithRadii(
            float x,
            float y,
            float width,
            float height,
            float topLeftRadius,
            float topRightRadius,
            float bottomRightRadius,
            float bottomLeftRadius,
            boolean outerAntialiasOnly) {
        if (this.uiRenderer != null) {
            Effects effects = Astralis.getInstance().getModuleManager().getModule(Effects.class);
            if (effects.isEnabled() && effects.blur.getValue()) {
                this.drawBlurredRectangle(
                        x,
                        y,
                        width,
                        height,
                        topLeftRadius,
                        topRightRadius,
                        bottomRightRadius,
                        bottomLeftRadius,
                        1.0F,
                        outerAntialiasOnly);
            }
        }
    }

    public RenderTarget getBlurOutputTarget() {
        return this.blurOutputTarget;
    }

    public Render2DContext getRenderer() {
        return this.uiRenderer;
    }

    public int getCachedWindowWidth() {
        return this.cachedWindowWidth;
    }

    public int getCachedWindowHeight() {
        return this.cachedWindowHeight;
    }

    public boolean isCacheValid() {
        return this.cacheValid;
    }
}
