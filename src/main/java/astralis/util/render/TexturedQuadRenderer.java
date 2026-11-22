package astralis.util.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;

import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;

import net.minecraft.server.packs.resources.ResourceManager;

import java.util.Arrays;

public class TexturedQuadRenderer extends AbstractQuadBatchRenderer {
    private static final int MAX_TEXTURE_SLOTS = 12;
    private static final int FLOATS_PER_VERTEX = 17;
    private static final float BYTE_TO_FLOAT = 0.003921569F;
    private final GpuTexture[] slotTextures;
    private final GpuTextureView[] slotViews;
    private final GpuSampler[] slotSamplers;
    private int slotCount;
    private final GpuSampler linearSampler;
    private final GpuSampler nearestSampler;
    private final Reference2ObjectMap<GpuTexture, GpuTextureView> textureViews;
    private static final String[] TEXTURE_UNIFORM_NAMES =
            new String[] {
                "u_Tex0", "u_Tex1", "u_Tex2", "u_Tex3", "u_Tex4", "u_Tex5", "u_Tex6", "u_Tex7",
                "u_Tex8", "u_Tex9", "u_Tex10", "u_Tex11"
            };

    public TexturedQuadRenderer(ResourceManager resourceManager, Render2DContext context) {

        super(resourceManager, context, 17);

        this.slotTextures = new GpuTexture[12];

        this.slotViews = new GpuTextureView[12];

        this.slotSamplers = new GpuSampler[12];
        this.slotCount = 0;

        this.textureViews = new Reference2ObjectOpenHashMap<>(32);
        this.linearSampler =
                RenderSystem.getSamplerCache()
                        .getSampler(
                                AddressMode.CLAMP_TO_EDGE,
                                AddressMode.CLAMP_TO_EDGE,
                                FilterMode.LINEAR,
                                FilterMode.LINEAR,
                                false);
        this.nearestSampler =
                RenderSystem.getSamplerCache()
                        .getSampler(
                                AddressMode.CLAMP_TO_EDGE,
                                AddressMode.CLAMP_TO_EDGE,
                                FilterMode.NEAREST,
                                FilterMode.NEAREST,
                                false);
    }

    @Override
    public void releaseResources() {
        super.releaseResources();

        for (GpuTextureView gpuTextureView : this.textureViews.values()) {
            if (!gpuTextureView.isClosed()) {
                gpuTextureView.close();
            }
        }

        this.textureViews.clear();
    }

    @Override
    protected RenderPipeline getRenderPipeline() {
        return AstralisPipelines.textureQuad;
    }

    @Override
    protected void bindPassResources(RenderPass renderPass) {
        GpuTextureView gpuTextureView = null;
        GpuSampler gpuSampler = null;

        for (int i = 0; i < this.slotCount; i++) {
            if (this.slotViews[i] != null) {
                gpuTextureView = this.slotViews[i];
                gpuSampler = this.slotSamplers[i];
                break;
            }
        }

        if (gpuTextureView != null) {
            int slot = 0;

            while (true) {

                if (slot >= 12) {
                    return;
                }

                if (slot < this.slotCount && this.slotViews[slot] != null) {
                    renderPass.bindTexture(
                            TEXTURE_UNIFORM_NAMES[slot],
                            this.slotViews[slot],
                            this.slotSamplers[slot]);
                } else {
                    renderPass.bindTexture(TEXTURE_UNIFORM_NAMES[slot], gpuTextureView, gpuSampler);
                }

                slot++;
            }
        }
    }

    public void addTexture(TexturedRectShape shape) {

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
        float halfWidth = width * 0.5F;
        float halfHeight = height * 0.5F;
        GpuSampler gpuSampler = shape.isNearestFilter() ? this.nearestSampler : this.linearSampler;
        int slotIndex = this.acquireSlot(shape.getTexture(), gpuSampler);
        float slotCoord = slotIndex + 0.001F;
        int topLeftColor = shape.getTopLeftColor();
        int topRightColor = shape.getTopRightColor();
        int bottomRightColor = shape.getBottomRightColor();
        int bottomLeftColor = shape.getBottomLeftColor();

        int topLeftRedShifted = topLeftColor >> 16;

        float topLeftRed = (topLeftRedShifted & 0xFF) * 0.003921569F;

        int topLeftGreenShifted = topLeftColor >> 8;

        float topLeftGreen = (topLeftGreenShifted & 0xFF) * 0.003921569F;

        float topLeftBlue = (topLeftColor & 0xFF) * 0.003921569F;

        int topLeftAlphaShifted = topLeftColor >>> 24;

        float topLeftAlpha = (topLeftAlphaShifted & 0xFF) * 0.003921569F;

        int topRightRedShifted = topRightColor >> 16;

        float topRightRed = (topRightRedShifted & 0xFF) * 0.003921569F;

        int topRightGreenShifted = topRightColor >> 8;

        float topRightGreen = (topRightGreenShifted & 0xFF) * 0.003921569F;

        float topRightBlue = (topRightColor & 0xFF) * 0.003921569F;

        int topRightAlphaShifted = topRightColor >>> 24;

        float topRightAlpha = (topRightAlphaShifted & 0xFF) * 0.003921569F;

        int bottomRightRedShifted = bottomRightColor >> 16;

        float bottomRightRed = (bottomRightRedShifted & 0xFF) * 0.003921569F;

        int bottomRightGreenShifted = bottomRightColor >> 8;

        float bottomRightGreen = (bottomRightGreenShifted & 0xFF) * 0.003921569F;

        float bottomRightBlue = (bottomRightColor & 0xFF) * 0.003921569F;

        int bottomRightAlphaShifted = bottomRightColor >>> 24;

        float bottomRightAlpha = (bottomRightAlphaShifted & 0xFF) * 0.003921569F;

        int bottomLeftRedShifted = bottomLeftColor >> 16;

        float bottomLeftRed = (bottomLeftRedShifted & 0xFF) * 0.003921569F;

        int bottomLeftGreenShifted = bottomLeftColor >> 8;

        float bottomLeftGreen = (bottomLeftGreenShifted & 0xFF) * 0.003921569F;

        float bottomLeftBlue = (bottomLeftColor & 0xFF) * 0.003921569F;

        int bottomLeftAlphaShifted = bottomLeftColor >>> 24;

        float bottomLeftAlpha = (bottomLeftAlphaShifted & 0xFF) * 0.003921569F;
        float uMin = shape.getUMin();
        float vMin = shape.getVMin();
        float uMax = shape.getUMax();
        float vMax = shape.getVMax();
        this.putTexturedVertex(
                x,
                y,
                topLeftRed,
                topLeftGreen,
                topLeftBlue,
                topLeftAlpha,
                uMin,
                vMin,
                width,
                height,
                topLeftRadius,
                topRightRadius,
                bottomRightRadius,
                bottomLeftRadius,
                -halfWidth,
                -halfHeight,
                slotCoord);
        this.putTexturedVertex(
                x,
                y + height,
                bottomLeftRed,
                bottomLeftGreen,
                bottomLeftBlue,
                bottomLeftAlpha,
                uMin,
                vMax,
                width,
                height,
                topLeftRadius,
                topRightRadius,
                bottomRightRadius,
                bottomLeftRadius,
                -halfWidth,
                halfHeight,
                slotCoord);
        this.putTexturedVertex(
                x + width,
                y + height,
                bottomRightRed,
                bottomRightGreen,
                bottomRightBlue,
                bottomRightAlpha,
                uMax,
                vMax,
                width,
                height,
                topLeftRadius,
                topRightRadius,
                bottomRightRadius,
                bottomLeftRadius,
                halfWidth,
                halfHeight,
                slotCoord);
        this.putTexturedVertex(
                x + width,
                y,
                topRightRed,
                topRightGreen,
                topRightBlue,
                topRightAlpha,
                uMax,
                vMin,
                width,
                height,
                topLeftRadius,
                topRightRadius,
                bottomRightRadius,
                bottomLeftRadius,
                halfWidth,
                -halfHeight,
                slotCoord);
        this.quadCount++;
    }

    @Override
    protected void afterDraw() {
        Arrays.fill(this.slotViews, 0, this.slotCount, null);
        Arrays.fill(this.slotTextures, 0, this.slotCount, null);
        Arrays.fill(this.slotSamplers, 0, this.slotCount, null);
        this.slotCount = 0;
    }

    private int acquireSlot(GpuTexture gpuTexture, GpuSampler gpuSampler) {
        int i = 0;

        while (i < this.slotCount) {
            if (this.slotTextures[i] == gpuTexture && this.slotSamplers[i] == gpuSampler) {
                return i;
            }

            i++;
        }

        if (this.slotCount >= 12) {
            this.flush();
        }

        this.slotTextures[this.slotCount] = gpuTexture;
        this.slotSamplers[this.slotCount] = gpuSampler;
        return this.slotCount++;
    }

    @Override
    protected void prepareDraw() {
        int i = 0;

        while (i < this.slotCount) {
            GpuTexture gpuTexture = this.slotTextures[i];
            label43:
            if (gpuTexture != null) {
                GpuTextureView gpuTextureView = this.textureViews.get(gpuTexture);
                if (gpuTextureView == null || gpuTextureView.isClosed() || gpuTexture.isClosed()) {
                    if (gpuTextureView != null && !gpuTextureView.isClosed()) {
                        gpuTextureView.close();
                    }

                    if (gpuTexture.isClosed()) {
                        this.textureViews.remove(gpuTexture);
                        this.slotViews[i] = null;
                        break label43;
                    }

                    gpuTextureView = RenderSystem.getDevice().createTextureView(gpuTexture);
                    this.textureViews.put(gpuTexture, gpuTextureView);
                }

                this.slotViews[i] = gpuTextureView;
            }

            i++;
        }
    }

    private void putTexturedVertex(
            float x,
            float y,
            float red,
            float green,
            float blue,
            float alpha,
            float u,
            float v,
            float rectWidth,
            float rectHeight,
            float topLeftRadius,
            float topRightRadius,
            float bottomRightRadius,
            float bottomLeftRadius,
            float localX,
            float localY,
            float slot) {
        this.putVertexPosition(x, y);
        this.vertexFloats.put(red).put(green).put(blue).put(alpha);
        this.vertexFloats.put(u).put(v);
        this.vertexFloats.put(rectWidth).put(rectHeight);
        this.vertexFloats
                .put(topLeftRadius)
                .put(topRightRadius)
                .put(bottomRightRadius)
                .put(bottomLeftRadius);
        this.vertexFloats.put(localX).put(localY);
        this.vertexFloats.put(slot);
    }
}
