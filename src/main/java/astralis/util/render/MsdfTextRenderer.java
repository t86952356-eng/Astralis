package astralis.util.render;

import astralis.util.render.font.MsdfFont;
import astralis.util.render.font.MsdfGlyph;
import astralis.util.render.font.RetainedFontRun;
import astralis.util.render.font.TextDrawRequest;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;

import it.unimi.dsi.fastutil.ints.Int2FloatMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;

import net.minecraft.server.packs.resources.ResourceManager;

import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Supplier;

public class MsdfTextRenderer extends AbstractQuadBatchRenderer {
    public static ShapePaint defaultPaint;
    public MsdfFont currentFont;
    public GpuTexture currentFontTexture;
    public GpuSampler currentFontSampler;
    private GpuTextureView currentFontTextureView;
    public float distanceRange;
    public float atlasWidth;
    public float atlasHeight;
    public final ShapePaint currentPaint;
    private final GpuBuffer paramsBuffer;
    private final ByteBuffer paramsData;
    private final List<RetainedTextBatch> retainedBatches;

    public MsdfTextRenderer(ResourceManager resourceManager, Render2DContext renderContext) {

        super(resourceManager, renderContext, 8);
        this.currentFont = null;
        this.currentFontTexture = null;
        this.currentFontSampler = null;
        this.currentFontTextureView = null;
        this.distanceRange = 8.0F;
        this.atlasWidth = 512.0F;
        this.atlasHeight = 512.0F;
        this.currentPaint = new ShapePaint();
        this.retainedBatches = new ArrayList<>();
        GpuDevice gpuDevice = RenderSystem.getDevice();
        Supplier labelSupplier = () -> "Sph MSDF Params UBO";

        this.paramsBuffer = gpuDevice.createBuffer(labelSupplier, 136, 80L);
        this.paramsData = MemoryUtil.memAlloc(80);
    }

    @Override
    public void releaseResources() {
        Iterator batchIterator = this.retainedBatches.iterator();

        while (batchIterator.hasNext()) {
            RetainedTextBatch batch = (RetainedTextBatch) batchIterator.next();
            batch.dispose();
        }

        this.retainedBatches.clear();
        super.releaseResources();
        this.paramsBuffer.close();
        MemoryUtil.memFree(this.paramsData);
    }

    @Override
    protected RenderPipeline getRenderPipeline() {
        return AstralisPipelines.msdfText;
    }

    @Override
    protected void bindPassResources(RenderPass renderPass) {

        renderPass.setUniform("SphMsdfParams", this.paramsBuffer.slice(0L, 80L));
        if (this.currentFontTextureView != null && this.currentFontSampler != null) {
            renderPass.bindTexture(
                    "u_Texture", this.currentFontTextureView, this.currentFontSampler);
        }
    }

    public RetainedTextBatch createRetainedBatch() {
        RetainedTextBatch batch = new RetainedTextBatch(this);
        this.retainedBatches.add(batch);
        return batch;
    }

    private void addGlyphQuad(
            float x,
            float y,
            float width,
            float height,
            float uLeft,
            float vTop,
            float uRight,
            float vBottom,
            float red,
            float green,
            float blue,
            float alpha) {
        this.putVertexPosition(x, y);
        this.vertexFloats.put(uLeft).put(vTop).put(red).put(green).put(blue).put(alpha);
        this.putVertexPosition(x, y + height);
        this.vertexFloats.put(uLeft).put(vBottom).put(red).put(green).put(blue).put(alpha);
        this.putVertexPosition(x + width, y + height);
        this.vertexFloats.put(uRight).put(vBottom).put(red).put(green).put(blue).put(alpha);
        this.putVertexPosition(x + width, y);
        this.vertexFloats.put(uRight).put(vTop).put(red).put(green).put(blue).put(alpha);
        this.quadCount++;
    }

    @Override
    protected void afterDraw() {
        this.currentFontTextureView = null;
    }

    public void recordText(TextDrawRequest request, RetainedFontRun run) {
        float guiScale = this.getRenderContext().getGuiScale();
        float scaledFontSize = request.getFontSize() * guiScale;
        float scaledBoxWidth = request.getBoxWidth() * guiScale;
        float scaledBoxHeight = request.getBoxHeight() * guiScale;
        float scaledX = request.getX() * guiScale;
        float scaledY = request.getY() * guiScale;
        float cursorX =
                request.isCentered() && request.getBoxWidth() != 0.0F
                        ? scaledX
                                + (scaledBoxWidth
                                                - request.getFont()
                                                        .getStringWidth(
                                                                request.getText(), scaledFontSize))
                                        / 2.0F
                        : scaledX;
        float textY =
                request.isCentered() && request.getBoxHeight() != 0.0F
                        ? scaledY
                                + (scaledBoxHeight
                                                - request.getFont()
                                                        .getScaledLineHeight(scaledFontSize))
                                        / 2.0F
                        : scaledY;
        int baseColor = request.getPaint().getTopLeftColor();

        int alphaBits = baseColor >> 24;

        float baseAlpha = (alphaBits & 0xFF) / 255.0F;

        int redBits = baseColor >> 16;

        float baseRed = (redBits & 0xFF) / 255.0F;

        int greenBits = baseColor >> 8;

        float baseGreen = (greenBits & 0xFF) / 255.0F;

        float baseBlue = (baseColor & 0xFF) / 255.0F;
        float red = baseRed;
        float green = baseGreen;
        float blue = baseBlue;
        float alpha = baseAlpha;
        float baselineY = textY + request.getFont().getAscender() * scaledFontSize;
        Int2ObjectMap glyphs = request.getFont().getGlyphs();
        Int2ObjectMap kerningTable = request.getFont().getKerningTable();
        String text = request.getText();
        if (text != null) {
            int previousChar = -1;

            for (int index = 0; index < text.length(); index++) {
                char character = text.charAt(index);

                if (character == 167 && index + 1 < text.length()) {
                    ChatColorCode colorCode =
                            ChatColorCode.fromCode(Character.toLowerCase(text.charAt(index + 1)));
                    if (colorCode != null) {
                        if (colorCode == ChatColorCode.RESET) {
                            red = baseRed;
                            green = baseGreen;
                            blue = baseBlue;
                            alpha = baseAlpha;
                        } else {
                            int colorRgb = colorCode.getRgb();

                            int colorRedBits = colorRgb >> 16;

                            red = (colorRedBits & 0xFF) / 255.0F;

                            int colorGreenBits = colorRgb >> 8;

                            green = (colorGreenBits & 0xFF) / 255.0F;

                            blue = (colorRgb & 0xFF) / 255.0F;
                            alpha = baseAlpha;
                        }
                    }

                    index++;
                    previousChar = -1;
                } else {
                    MsdfGlyph glyph = (MsdfGlyph) glyphs.get(character);
                    if (glyph != null) {
                        if (previousChar != -1) {
                            Int2FloatMap kerningPairs =
                                    (Int2FloatMap) kerningTable.get(previousChar);
                            if (kerningPairs != null && kerningPairs.containsKey(character)) {
                                cursorX += kerningPairs.get(character) * scaledFontSize;
                            }
                        }

                        if (glyph.isRenderable()) {
                            float glyphX = cursorX + glyph.getPlaneLeft() * scaledFontSize;
                            float glyphY = baselineY - glyph.getPlaneTop() * scaledFontSize;
                            float glyphWidth = glyph.getPlaneWidth() * scaledFontSize;
                            float glyphHeight = glyph.getPlaneHeight() * scaledFontSize;
                            if (request.hasShadow()) {
                                this.addGlyphQuadToRun(
                                        run,
                                        glyphX + 1.0F,
                                        glyphY + 1.0F,
                                        glyphWidth,
                                        glyphHeight,
                                        glyph,
                                        red * 0.25F,
                                        green * 0.25F,
                                        blue * 0.25F,
                                        alpha);
                            }

                            this.addGlyphQuadToRun(
                                    run,
                                    glyphX,
                                    glyphY,
                                    glyphWidth,
                                    glyphHeight,
                                    glyph,
                                    red,
                                    green,
                                    blue,
                                    alpha);
                        }

                        cursorX += glyph.getAdvance() * scaledFontSize;
                        previousChar = character;
                    }
                }
            }
        }
    }

    @Override
    protected void prepareDraw() {
        if (this.currentFont != null) {
            try {
                this.currentFontTextureView = this.currentFont.getTextureView();
            } catch (IllegalStateException ignored) {
                this.currentFont = null;
                this.currentFontTexture = null;
                this.currentFontTextureView = null;
                this.quadCount = 0;
            }
        }
    }

    private void addGlyphQuadToRun(
            RetainedFontRun run,
            float x,
            float y,
            float width,
            float height,
            MsdfGlyph glyph,
            float red,
            float green,
            float blue,
            float alpha) {

        run.ensureCapacity(32);
        this.putTransformedPosition(run.vertexData, x, y);
        run.vertexData
                .put(glyph.getULeft())
                .put(glyph.getVTop())
                .put(red)
                .put(green)
                .put(blue)
                .put(alpha);
        this.putTransformedPosition(run.vertexData, x, y + height);
        run.vertexData
                .put(glyph.getULeft())
                .put(glyph.getVBottom())
                .put(red)
                .put(green)
                .put(blue)
                .put(alpha);
        this.putTransformedPosition(run.vertexData, x + width, y + height);
        run.vertexData
                .put(glyph.getURight())
                .put(glyph.getVBottom())
                .put(red)
                .put(green)
                .put(blue)
                .put(alpha);
        this.putTransformedPosition(run.vertexData, x + width, y);
        run.vertexData
                .put(glyph.getURight())
                .put(glyph.getVTop())
                .put(red)
                .put(green)
                .put(blue)
                .put(alpha);
        run.quadCount++;
    }

    @Override
    protected void uploadExtraBuffers(CommandEncoder commandEncoder) {

        ByteBuffer buffer = this.paramsData;
        buffer.putFloat(0, this.distanceRange);
        buffer.putFloat(4, 0.0F);

        buffer.putFloat(8, this.atlasWidth);

        buffer.putFloat(12, this.atlasHeight);

        buffer.putInt(16, this.currentPaint.getPaintType().getShaderId());

        buffer.putFloat(20, 0.0F);

        buffer.putFloat(24, 0.0F);

        buffer.putFloat(28, 0.0F);
        int startColor = this.currentPaint.getTopLeftColor();

        int redBits = startColor >> 16;

        buffer.putFloat(32, (redBits & 0xFF) * 0.003921569F);

        int greenBits = startColor >> 8;

        buffer.putFloat(36, (greenBits & 0xFF) * 0.003921569F);

        buffer.putFloat(40, (startColor & 0xFF) * 0.003921569F);

        int alphaBits = startColor >> 24;

        buffer.putFloat(44, (alphaBits & 0xFF) * 0.003921569F);
        if (this.currentPaint.getPaintType() == PaintMode.LINEAR_GRADIENT) {
            int endColor = this.currentPaint.getTopRightColor();

            int endRedBits = endColor >> 16;

            buffer.putFloat(48, (endRedBits & 0xFF) * 0.003921569F);

            int endGreenBits = endColor >> 8;

            buffer.putFloat(52, (endGreenBits & 0xFF) * 0.003921569F);

            buffer.putFloat(56, (endColor & 0xFF) * 0.003921569F);

            int endAlphaBits = endColor >> 24;

            buffer.putFloat(60, (endAlphaBits & 0xFF) * 0.003921569F);
            float guiScale = this.getRenderContext().getGuiScale();

            buffer.putFloat(64, this.currentPaint.getGradientStartX() * guiScale);

            buffer.putFloat(68, this.currentPaint.getGradientStartY() * guiScale);

            buffer.putFloat(72, this.currentPaint.getGradientEndX() * guiScale);

            buffer.putFloat(76, this.currentPaint.getGradientEndY() * guiScale);
        } else {

            int offset = 48;

            while (true) {

                if (offset >= 80) {
                    break;
                }

                buffer.putFloat(offset, 0.0F);
                offset += 4;
            }
        }

        ByteBuffer rewoundBuffer = buffer.position(0);

        rewoundBuffer.limit(80);
        commandEncoder.writeToBuffer(this.paramsBuffer.slice(), buffer);
    }

    public void drawText(TextDrawRequest request) {
        float guiScale = this.getRenderContext().getGuiScale();
        float scaledFontSize = request.getFontSize() * guiScale;
        float scaledBoxWidth = request.getBoxWidth() * guiScale;
        float scaledBoxHeight = request.getBoxHeight() * guiScale;
        float scaledX = request.getX() * guiScale;
        float scaledY = request.getY() * guiScale;
        float startX =
                request.isCentered() && request.getBoxWidth() != 0.0F
                        ? scaledX
                                + (scaledBoxWidth
                                                - request.getFont()
                                                        .getStringWidth(
                                                                request.getText(), scaledFontSize))
                                        / 2.0F
                        : scaledX;
        float textY =
                request.isCentered() && request.getBoxHeight() != 0.0F
                        ? scaledY
                                + (scaledBoxHeight
                                                - request.getFont()
                                                        .getScaledLineHeight(scaledFontSize))
                                        / 2.0F
                        : scaledY;
        MsdfFont font = request.getFont();
        ShapePaint paint = request.getPaint();
        boolean standardPaint = paint.getPaintType() == PaintMode.STANDARD;
        boolean paintChanged =
                standardPaint
                        ? !this.currentPaint.isSamePaint(defaultPaint)
                        : !this.currentPaint.isSamePaint(paint);
        if (font.getTexture() != this.currentFontTexture || paintChanged) {
            this.flush();
            this.currentFont = font;
            this.currentFontTexture = font.getTexture();
            this.currentFontSampler = font.getSampler();
            this.distanceRange = font.getDistanceRange() > 0.0F ? font.getDistanceRange() : 4.0F;
            this.atlasWidth = font.getAtlasWidth();
            this.atlasHeight = font.getAtlasHeight();
            if (standardPaint) {
                this.currentPaint.copyFrom(defaultPaint);
            } else {
                this.currentPaint.copyFrom(paint);
            }
        }

        float red = 1.0F;
        float green = 1.0F;
        float blue = 1.0F;
        float alpha = 1.0F;
        if (standardPaint) {
            int baseColor = paint.getTopLeftColor();

            int alphaBits = baseColor >> 24;

            alpha = (alphaBits & 0xFF) / 255.0F;

            int redBits = baseColor >> 16;

            red = (redBits & 0xFF) / 255.0F;

            int greenBits = baseColor >> 8;

            green = (greenBits & 0xFF) / 255.0F;

            blue = (baseColor & 0xFF) / 255.0F;
        }

        float baseRed = red;
        float baseGreen = green;
        float baseBlue = blue;
        float baseAlpha = alpha;
        float cursorX = startX;
        float baselineY = textY + font.getAscender() * scaledFontSize;
        Int2ObjectMap glyphs = font.getGlyphs();
        Int2ObjectMap kerningTable = font.getKerningTable();
        String text = request.getText();
        if (text != null) {
            int length = text.length();
            int previousChar = -1;

            for (int index = 0; index < length; index++) {
                char character = text.charAt(index);

                if (character == 167 && index + 1 < length) {
                    char formatChar = Character.toLowerCase(text.charAt(index + 1));
                    ChatColorCode colorCode = ChatColorCode.fromCode(formatChar);
                    if (colorCode != null) {
                        if (colorCode == ChatColorCode.RESET) {
                            red = baseRed;
                            green = baseGreen;
                            blue = baseBlue;
                            alpha = baseAlpha;
                        } else {
                            int colorRgb = colorCode.getRgb();

                            int colorRedBits = colorRgb >> 16;

                            red = (colorRedBits & 0xFF) * 0.003921569F;

                            int colorGreenBits = colorRgb >> 8;

                            green = (colorGreenBits & 0xFF) * 0.003921569F;

                            blue = (colorRgb & 0xFF) * 0.003921569F;
                            alpha = baseAlpha;
                        }
                    }

                    index++;
                    previousChar = -1;
                } else {
                    MsdfGlyph glyph = (MsdfGlyph) glyphs.get(character);
                    if (glyph != null) {
                        if (previousChar != -1) {
                            Int2FloatMap kerningPairs =
                                    (Int2FloatMap) kerningTable.get(previousChar);
                            if (kerningPairs != null && kerningPairs.containsKey(character)) {
                                cursorX += kerningPairs.get(character) * scaledFontSize;
                            }
                        }

                        if (glyph.isRenderable()) {

                            if (this.quadCount >= 9999) {
                                this.flush();
                            }

                            float glyphX = cursorX + glyph.getPlaneLeft() * scaledFontSize;
                            float glyphY = baselineY - glyph.getPlaneTop() * scaledFontSize;
                            float glyphWidth = glyph.getPlaneWidth() * scaledFontSize;
                            float glyphHeight = glyph.getPlaneHeight() * scaledFontSize;
                            float uLeft = glyph.getULeft();
                            float vTop = glyph.getVTop();
                            float uRight = glyph.getURight();
                            float vBottom = glyph.getVBottom();
                            if (request.hasShadow()) {
                                this.addGlyphQuad(
                                        glyphX + 1.0F,
                                        glyphY + 1.0F,
                                        glyphWidth,
                                        glyphHeight,
                                        uLeft,
                                        vTop,
                                        uRight,
                                        vBottom,
                                        red * 0.25F,
                                        green * 0.25F,
                                        blue * 0.25F,
                                        alpha);
                            }

                            this.addGlyphQuad(
                                    glyphX,
                                    glyphY,
                                    glyphWidth,
                                    glyphHeight,
                                    uLeft,
                                    vTop,
                                    uRight,
                                    vBottom,
                                    red,
                                    green,
                                    blue,
                                    alpha);
                        }

                        cursorX += glyph.getAdvance() * scaledFontSize;
                        previousChar = character;
                    }
                }
            }
        }
    }

    @Override
    public void flush() {
        if (this.quadCount != 0) {
            super.flush();
        }
    }

    static {
        defaultPaint = new ShapePaint().setColor(-1);
    }
}
