package astralis.util.render;

import astralis.network.websocket.ChannelInterest;

import com.mojang.blaze3d.pipeline.RenderPipeline;

import net.minecraft.server.packs.resources.ResourceManager;

public class RoundedRectRenderer extends AbstractQuadBatchRenderer {
    private static final int FLOATS_PER_VERTEX = 19;
    private static final float BYTE_TO_FLOAT = 0.003921569F;

    public RoundedRectRenderer(ResourceManager resourceManager, Render2DContext renderContext) {

        super(resourceManager, renderContext, 19);
    }

    @Override
    protected RenderPipeline getRenderPipeline() {

        return switch (DepthMaskModeSwitchMap.ORDINAL_TABLE[
                this.getRenderContext().getDepthMode().ordinal()]) {
            case 1 -> AstralisPipelines.rectangleDepthWrite;
            case 2 -> AstralisPipelines.rectangleDepthRead;
            default -> AstralisPipelines.rectangle;
        };
    }

    public void addRect(RoundedRectShape shape) {

        if (this.quadCount >= 10000) {
            this.flush();
        }

        float guiScale = this.getRenderContext().getGuiScale();
        float x = shape.getX() * guiScale;
        float y = shape.getY() * guiScale;
        float width = shape.getWidth() * guiScale;
        float height = shape.getHeight() * guiScale;
        float radiusSign = shape.isOuterAntialiasOnly() ? -1.0F : 1.0F;
        float topLeftRadius = shape.getTopLeftRadius() * guiScale * radiusSign;
        float topRightRadius = shape.getTopRightRadius() * guiScale * radiusSign;
        float bottomRightRadius = shape.getBottomRightRadius() * guiScale * radiusSign;
        float bottomLeftRadius = shape.getBottomLeftRadius() * guiScale * radiusSign;
        boolean shadowMaskPass = this.getRenderContext().isShadowMaskPass();
        float outlineWidth = shadowMaskPass ? 0.0F : shape.getOutlineWidth() * guiScale;
        float halfWidth = width * 0.5F;

        float halfHeight = height * 0.5F;
        int shadowAlpha = shape.getShadowAlpha();

        int maskColor = 16777215 | shadowAlpha << 24;
        int topLeftColor = shadowMaskPass ? maskColor : shape.getTopLeftColor();
        int topRightColor = shadowMaskPass ? maskColor : shape.getTopRightColor();
        int bottomRightColor = shadowMaskPass ? maskColor : shape.getBottomRightColor();
        int bottomLeftColor = shadowMaskPass ? maskColor : shape.getBottomLeftColor();

        int topLeftRedBits = topLeftColor >> 16;

        float topLeftRed = (topLeftRedBits & 0xFF) * 0.003921569F;

        int topLeftGreenBits = topLeftColor >> 8;

        float topLeftGreen = (topLeftGreenBits & 0xFF) * 0.003921569F;

        float topLeftBlue = (topLeftColor & 0xFF) * 0.003921569F;

        int topLeftAlphaBits = topLeftColor >>> 24;

        float topLeftAlpha = (topLeftAlphaBits & 0xFF) * 0.003921569F;

        int topRightRedBits = topRightColor >> 16;

        float topRightRed = (topRightRedBits & 0xFF) * 0.003921569F;

        int topRightGreenBits = topRightColor >> 8;

        float topRightGreen = (topRightGreenBits & 0xFF) * 0.003921569F;

        float topRightBlue = (topRightColor & 0xFF) * 0.003921569F;

        int topRightAlphaBits = topRightColor >>> 24;

        float topRightAlpha = (topRightAlphaBits & 0xFF) * 0.003921569F;

        int bottomRightRedBits = bottomRightColor >> 16;

        float bottomRightRed = (bottomRightRedBits & 0xFF) * 0.003921569F;

        int bottomRightGreenBits = bottomRightColor >> 8;

        float bottomRightGreen = (bottomRightGreenBits & 0xFF) * 0.003921569F;

        float bottomRightBlue = (bottomRightColor & 0xFF) * 0.003921569F;

        int bottomRightAlphaBits = bottomRightColor >>> 24;

        float bottomRightAlpha = (bottomRightAlphaBits & 0xFF) * 0.003921569F;

        int bottomLeftRedBits = bottomLeftColor >> 16;

        float bottomLeftRed = (bottomLeftRedBits & 0xFF) * 0.003921569F;

        int bottomLeftGreenBits = bottomLeftColor >> 8;

        float bottomLeftGreen = (bottomLeftGreenBits & 0xFF) * 0.003921569F;

        float bottomLeftBlue = (bottomLeftColor & 0xFF) * 0.003921569F;

        int bottomLeftAlphaBits = bottomLeftColor >>> 24;

        float bottomLeftAlpha = (bottomLeftAlphaBits & 0xFF) * 0.003921569F;
        int outlineTopLeftColor = shape.getOutlineTopLeftColor();
        int outlineTopRightColor = shape.getOutlineTopRightColor();
        int outlineBottomRightColor = shape.getOutlineBottomRightColor();
        int outlineBottomLeftColor = shape.getOutlineBottomLeftColor();

        int outlineTopLeftRedBits = outlineTopLeftColor >> 16;

        float outlineTopLeftRed = (outlineTopLeftRedBits & 0xFF) * 0.003921569F;

        int outlineTopLeftGreenBits = outlineTopLeftColor >> 8;

        float outlineTopLeftGreen = (outlineTopLeftGreenBits & 0xFF) * 0.003921569F;

        float outlineTopLeftBlue = (outlineTopLeftColor & 0xFF) * 0.003921569F;

        int outlineTopLeftAlphaBits = outlineTopLeftColor >>> 24;

        float outlineTopLeftAlpha = (outlineTopLeftAlphaBits & 0xFF) * 0.003921569F;

        int outlineTopRightRedBits = outlineTopRightColor >> 16;

        float outlineTopRightRed = (outlineTopRightRedBits & 0xFF) * 0.003921569F;

        int outlineTopRightGreenBits = outlineTopRightColor >> 8;

        float outlineTopRightGreen = (outlineTopRightGreenBits & 0xFF) * 0.003921569F;

        float outlineTopRightBlue = (outlineTopRightColor & 0xFF) * 0.003921569F;

        int outlineTopRightAlphaBits = outlineTopRightColor >>> 24;

        float outlineTopRightAlpha = (outlineTopRightAlphaBits & 0xFF) * 0.003921569F;

        int outlineBottomRightRedBits = outlineBottomRightColor >> 16;

        float outlineBottomRightRed = (outlineBottomRightRedBits & 0xFF) * 0.003921569F;

        int outlineBottomRightGreenBits = outlineBottomRightColor >> 8;

        float outlineBottomRightGreen = (outlineBottomRightGreenBits & 0xFF) * 0.003921569F;

        float outlineBottomRightBlue = (outlineBottomRightColor & 0xFF) * 0.003921569F;

        int outlineBottomRightAlphaBits = outlineBottomRightColor >>> 24;

        float outlineBottomRightAlpha = (outlineBottomRightAlphaBits & 0xFF) * 0.003921569F;

        int outlineBottomLeftRedBits = outlineBottomLeftColor >> 16;

        float outlineBottomLeftRed = (outlineBottomLeftRedBits & 0xFF) * 0.003921569F;

        int outlineBottomLeftGreenBits = outlineBottomLeftColor >> 8;

        float outlineBottomLeftGreen = (outlineBottomLeftGreenBits & 0xFF) * 0.003921569F;

        float outlineBottomLeftBlue = (outlineBottomLeftColor & 0xFF) * 0.003921569F;

        int outlineBottomLeftAlphaBits = outlineBottomLeftColor >>> 24;

        float outlineBottomLeftAlpha = (outlineBottomLeftAlphaBits & 0xFF) * 0.003921569F;
        this.putRectVertex(
                x,
                y,
                topLeftRed,
                topLeftGreen,
                topLeftBlue,
                topLeftAlpha,
                outlineTopLeftRed,
                outlineTopLeftGreen,
                outlineTopLeftBlue,
                outlineTopLeftAlpha,
                width,
                height,
                topLeftRadius,
                topRightRadius,
                bottomRightRadius,
                bottomLeftRadius,
                outlineWidth,
                -halfWidth,
                -halfHeight);
        this.putRectVertex(
                x,
                y + height,
                bottomLeftRed,
                bottomLeftGreen,
                bottomLeftBlue,
                bottomLeftAlpha,
                outlineBottomLeftRed,
                outlineBottomLeftGreen,
                outlineBottomLeftBlue,
                outlineBottomLeftAlpha,
                width,
                height,
                topLeftRadius,
                topRightRadius,
                bottomRightRadius,
                bottomLeftRadius,
                outlineWidth,
                -halfWidth,
                halfHeight);
        this.putRectVertex(
                x + width,
                y + height,
                bottomRightRed,
                bottomRightGreen,
                bottomRightBlue,
                bottomRightAlpha,
                outlineBottomRightRed,
                outlineBottomRightGreen,
                outlineBottomRightBlue,
                outlineBottomRightAlpha,
                width,
                height,
                topLeftRadius,
                topRightRadius,
                bottomRightRadius,
                bottomLeftRadius,
                outlineWidth,
                halfWidth,
                halfHeight);
        this.putRectVertex(
                x + width,
                y,
                topRightRed,
                topRightGreen,
                topRightBlue,
                topRightAlpha,
                outlineTopRightRed,
                outlineTopRightGreen,
                outlineTopRightBlue,
                outlineTopRightAlpha,
                width,
                height,
                topLeftRadius,
                topRightRadius,
                bottomRightRadius,
                bottomLeftRadius,
                outlineWidth,
                halfWidth,
                -halfHeight);
        this.quadCount++;
    }

    @Override
    protected boolean usesDepthAttachment() {
        return this.getRenderContext().getDepthMode() != ChannelInterest.NONE;
    }

    private void putRectVertex(
            float x,
            float y,
            float red,
            float green,
            float blue,
            float alpha,
            float outlineRed,
            float outlineGreen,
            float outlineBlue,
            float outlineAlpha,
            float width,
            float height,
            float topLeftRadius,
            float topRightRadius,
            float bottomRightRadius,
            float bottomLeftRadius,
            float outlineWidth,
            float localX,
            float localY) {
        this.putVertexPosition(x, y);
        this.vertexFloats.put(red).put(green).put(blue).put(alpha);
        this.vertexFloats.put(width).put(height);
        this.vertexFloats
                .put(topLeftRadius)
                .put(topRightRadius)
                .put(bottomRightRadius)
                .put(bottomLeftRadius);
        this.vertexFloats.put(outlineRed).put(outlineGreen).put(outlineBlue).put(outlineAlpha);
        this.vertexFloats.put(outlineWidth);
        this.vertexFloats.put(localX).put(localY);
    }
}
