package astralis.util.render;

import astralis.util.render.font.MsdfFont;
import astralis.util.render.font.MsdfGlyph;

import com.mojang.blaze3d.IndexType;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;

import it.unimi.dsi.fastutil.ints.Int2FloatMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;

import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.Vec3;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryUtil;

import java.awt.Color;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.function.Supplier;

public final class WorldTextRenderer {
    private static final int MAX_GLYPH_QUADS = 4096;
    private static final int FLOATS_PER_VERTEX = 11;
    private static final int VERTICES_PER_QUAD = 4;
    private static final int INDICES_PER_QUAD = 6;
    private static final int PARAMS_BUFFER_SIZE = 144;
    private final Render2DContext uiRenderer;
    private final FloatBuffer vertexData;
    private final GpuBuffer vertexBuffer;
    private final GpuBuffer indexBuffer;
    private final GpuBuffer paramsBuffer;
    private final ByteBuffer paramsData;

    public WorldTextRenderer(Render2DContext context) {

        this.uiRenderer = context;

        this.vertexData = MemoryUtil.memAllocFloat(180224);
        GpuDevice gpuDevice = RenderSystem.getDevice();
        Supplier vertexLabel = () -> "Sph World Text VBO";

        this.vertexBuffer = gpuDevice.createBuffer(vertexLabel, 40, 720896L);

        ByteBuffer indexData = MemoryUtil.memAlloc(49152);
        int quad = 0;

        while (true) {

            if (quad >= 4096) {
                indexData.flip();
                Supplier indexLabel = () -> "Sph World Text EBO";

                this.indexBuffer = gpuDevice.createBuffer(indexLabel, 72, indexData);
                MemoryUtil.memFree(indexData);
                Supplier paramsLabel = () -> "Sph World Text Params UBO";

                this.paramsBuffer = gpuDevice.createBuffer(paramsLabel, 136, 144L);

                this.paramsData = MemoryUtil.memAlloc(144);
                return;
            }

            short baseVertex = (short) (quad * 4);
            indexData
                    .putShort(baseVertex)
                    .putShort((short) (baseVertex + 1))
                    .putShort((short) (baseVertex + 2));
            indexData
                    .putShort((short) (baseVertex + 2))
                    .putShort((short) (baseVertex + 3))
                    .putShort(baseVertex);
            quad++;
        }
    }

    private void appendGlyphQuad(
            Vec3 vector3,
            Vector3f vector3f,
            Vector3f vector3f2,
            float x,
            float y,
            float width,
            float height,
            MsdfGlyph glyph,
            Color color,
            Color color2) {
        this.appendVertex(
                vector3,
                vector3f,
                vector3f2,
                x,
                y,
                glyph.getULeft(),
                glyph.getVTop(),
                color,
                0.0F,
                0.0F);
        this.appendVertex(
                vector3,
                vector3f,
                vector3f2,
                x,
                y - height,
                glyph.getULeft(),
                glyph.getVBottom(),
                color,
                0.0F,
                0.0F);
        this.appendVertex(
                vector3,
                vector3f,
                vector3f2,
                x + width,
                y - height,
                glyph.getURight(),
                glyph.getVBottom(),
                color2,
                0.0F,
                0.0F);
        this.appendVertex(
                vector3,
                vector3f,
                vector3f2,
                x + width,
                y,
                glyph.getURight(),
                glyph.getVTop(),
                color2,
                0.0F,
                0.0F);
    }

    public void destroy() {
        this.vertexBuffer.close();
        this.indexBuffer.close();
        this.paramsBuffer.close();
        MemoryUtil.memFree(this.vertexData);
        MemoryUtil.memFree(this.paramsData);
    }

    public void renderWithThickness(
            MsdfFont font,
            List<WorldText> texts,
            CameraRenderState cameraRenderState,
            float thickness) {
        this.render(font, texts, cameraRenderState, null, null, thickness);
    }

    private int appendTextQuads(
            MsdfFont font, WorldText worldText, int quadCount, float thickness) {
        long rawLayerCount;
        float fontSize = worldText.getSize();
        float cursorX = -font.getStringWidth(worldText.getText(), fontSize) * 0.5F;
        Int2ObjectMap glyphs = font.getGlyphs();
        Int2ObjectMap kerningTable = font.getKerningTable();
        Vec3 vector3 = Vec3.directionFromRotation(0.0F, worldText.getYaw());
        Vector3f vector3f = new Vector3f((float) (-vector3.z), 0.0F, (float) vector3.x);
        Vector3f vector3f2 = new Vector3f(0.0F, 1.0F, 0.0F);
        int previousChar = -1;

        for (int i = 0; i < worldText.getText().length(); i++) {
            char character = worldText.getText().charAt(i);
            MsdfGlyph glyph = (MsdfGlyph) glyphs.get(character);
            if (glyph != null) {
                if (previousChar != -1) {
                    Int2FloatMap kerningRow = (Int2FloatMap) kerningTable.get(previousChar);
                    if (kerningRow != null && kerningRow.containsKey(character)) {
                        cursorX += kerningRow.get(character) * fontSize;
                    }
                }

                if (glyph.isRenderable()) {

                    if (quadCount == 4096) {
                        return quadCount;
                    }

                    float glyphX = cursorX + glyph.getPlaneLeft() * fontSize;
                    float glyphY = glyph.getPlaneTop() * fontSize;
                    float glyphWidth = glyph.getPlaneWidth() * fontSize;
                    float glyphHeight = glyph.getPlaneHeight() * fontSize;
                    rawLayerCount = (int) Math.ceil(thickness / 0.0015F) + 1;

                    int layerCount = Math.clamp(rawLayerCount, 1, 32);
                    int endQuadCount = quadCount + layerCount;

                    if (endQuadCount > 4096) {
                        return quadCount;
                    }

                    for (int layer = layerCount - 1; layer >= 0; layer--) {
                        float depthOffset = thickness * layer / Math.max(1, layerCount - 1);
                        Color color =
                                layer == 0
                                        ? worldText.getStartColor()
                                        : this.darkenColor(worldText.getStartColor());
                        Color color2 =
                                layer == 0
                                        ? worldText.getEndColor()
                                        : this.darkenColor(worldText.getEndColor());
                        this.appendGlyphQuad(
                                worldText.getPosition().add(vector3.scale(depthOffset)),
                                vector3f,
                                vector3f2,
                                glyphX,
                                glyphY,
                                glyphWidth,
                                glyphHeight,
                                glyph,
                                color,
                                color2);
                        quadCount++;
                    }
                }

                cursorX += glyph.getAdvance() * fontSize;
                previousChar = character;
            }
        }

        return quadCount;
    }

    public void render(
            MsdfFont font,
            List<WorldText> texts,
            CameraRenderState cameraRenderState,
            Color color,
            Color color2,
            float thickness) {
        if (!texts.isEmpty()
                && AstralisPipelines.isPipelineValid(AstralisPipelines.worldMsdfText)) {
            RenderTarget renderTarget = this.uiRenderer.getRenderTarget();
            if (renderTarget != null
                    && renderTarget.getDepthTextureView() != null
                    && !font.getTexture().isClosed()) {
                this.vertexData.clear();
                int quadCount = 0;

                for (WorldText worldText : texts) {
                    quadCount =
                            this.appendTextQuads(
                                    font,
                                    worldText,
                                    quadCount,
                                    Math.max(0.0F, thickness - 1.0F) * 0.01F);

                    if (quadCount == 4096) {
                        break;
                    }
                }

                if (quadCount != 0) {
                    this.vertexData.flip();
                    int byteSize = this.vertexData.remaining() * 4;
                    CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
                    commandEncoder.writeToBuffer(
                            this.vertexBuffer.slice(0L, byteSize),
                            MemoryUtil.memByteBuffer(
                                    MemoryUtil.memAddress(this.vertexData), byteSize));
                    this.paramsData.clear();
                    new Matrix4f(cameraRenderState.projectionMatrix)
                            .mul(cameraRenderState.viewRotationMatrix)
                            .get(this.paramsData);

                    this.paramsData.position(64);
                    this.paramsData
                            .putFloat((float) cameraRenderState.pos.x)
                            .putFloat((float) cameraRenderState.pos.y)
                            .putFloat((float) cameraRenderState.pos.z)
                            .putFloat(font.getDistanceRange());
                    this.paramsData
                            .putFloat(font.getAtlasWidth())
                            .putFloat(font.getAtlasHeight())
                            .putFloat(color != null && color2 != null ? 1.0F : 0.0F)
                            .putFloat(1.0F);
                    Color color3 = color == null ? Color.WHITE : color;
                    Color color4 = color2 == null ? Color.WHITE : color2;
                    this.paramsData
                            .putFloat(color3.getRed() / 255.0F)
                            .putFloat(color3.getGreen() / 255.0F)
                            .putFloat(color3.getBlue() / 255.0F)
                            .putFloat(color3.getAlpha() / 255.0F);
                    this.paramsData
                            .putFloat(color4.getRed() / 255.0F)
                            .putFloat(color4.getGreen() / 255.0F)
                            .putFloat(color4.getBlue() / 255.0F)
                            .putFloat(color4.getAlpha() / 255.0F);
                    this.paramsData
                            .putFloat((float) System.currentTimeMillis() / 1000.0F)
                            .putFloat(0.0F)
                            .putFloat(0.0F)
                            .putFloat(0.0F);
                    this.paramsData.flip();
                    commandEncoder.writeToBuffer(this.paramsBuffer.slice(), this.paramsData);
                    GpuTextureView gpuTextureView = font.getTextureView();

                    try (RenderPass renderPass =
                            commandEncoder.createRenderPass(
                                    () -> "Sph World Text",
                                    renderTarget.getColorTextureView(),
                                    Optional.empty(),
                                    renderTarget.getDepthTextureView(),
                                    OptionalDouble.empty())) {
                        renderPass.setPipeline(AstralisPipelines.worldMsdfText);

                        renderPass.setUniform("SphWorldTextParams", this.paramsBuffer.slice());
                        renderPass.setVertexBuffer(0, this.vertexBuffer.slice());
                        renderPass.setIndexBuffer(this.indexBuffer, IndexType.SHORT);

                        renderPass.bindTexture("u_Texture", gpuTextureView, font.getSampler());

                        renderPass.drawIndexed(quadCount * 6, 1, 0, 0, 0);
                    }
                }
            }
        }
    }

    public void renderTexts(
            MsdfFont font, List<WorldText> texts, CameraRenderState cameraRenderState) {
        this.render(font, texts, cameraRenderState, null, null, 1.0F);
    }

    public void renderWithColorOverride(
            MsdfFont font,
            List<WorldText> texts,
            CameraRenderState cameraRenderState,
            Color color,
            Color color2) {
        this.render(font, texts, cameraRenderState, color, color2, 1.0F);
    }

    private void appendVertex(
            Vec3 vector3,
            Vector3f vector3f,
            Vector3f vector3f2,
            float x,
            float y,
            float u,
            float v,
            Color color,
            float extraX,
            float extraY) {
        this.vertexData.put((float) (vector3.x + vector3f.x * x + vector3f2.x * y));
        this.vertexData.put((float) (vector3.y + vector3f.y * x + vector3f2.y * y));
        this.vertexData.put((float) (vector3.z + vector3f.z * x + vector3f2.z * y));
        this.vertexData.put(u).put(v);
        this.vertexData
                .put(color.getRed() / 255.0F)
                .put(color.getGreen() / 255.0F)
                .put(color.getBlue() / 255.0F)
                .put(color.getAlpha() / 255.0F);
        this.vertexData.put(extraX).put(extraY);
    }

    private Color darkenColor(Color color) {
        return new Color(
                Math.round(color.getRed() * 0.42F),
                Math.round(color.getGreen() * 0.42F),
                Math.round(color.getBlue() * 0.42F),
                color.getAlpha());
    }
}
