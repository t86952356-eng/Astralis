package astralis.util.render;

import com.mojang.blaze3d.pipeline.RenderTarget;

/** Retains and updates the rendering context for the current render target. */
public class RenderContextHolder {
    private Render2DContext renderContext;

    /** Returns the current render context, which is null before first initialization. */
    public Render2DContext getRenderContext() {
        return this.renderContext;
    }

    /** Initializes the renderer on first use, then updates its target, dimensions, and scale. */
    public void updateRenderContext(RenderTarget renderTarget, int width, int height, float scale) {
        if (this.renderContext == null) {
            this.renderContext = new Render2DContext(renderTarget, width, height, scale);
            this.renderContext.initRenderers();
        } else {
            this.renderContext.beginFrame(renderTarget, width, height, scale);
        }
    }

    /** Replaces the retained render context. */
    public void setRenderContext(Render2DContext renderContext) {
        this.renderContext = renderContext;
    }
}
