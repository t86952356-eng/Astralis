package astralis.util.render;

import astralis.network.websocket.ChannelInterest;
import astralis.util.render.font.TextDrawRequest;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;

import net.minecraft.client.Minecraft;

import org.joml.Matrix4fStack;
import org.joml.Vector4f;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public class Render2DContext {
    private static final long FNV_OFFSET_BASIS = -3750763034362895579L;
    private static final long FNV_PRIME = 1099511628211L;
    private final Matrix4fStack matrixStack;
    private final Deque<ScissorRect> scissorStack;
    private final List<DrawCommand> commands;
    private final List<RoundedRectShape> deferredRects;
    private final List<TextDrawRequest> deferredTexts;
    private final Map<Object, RetainedTextBatch> retainedBatches;
    private int commandCount;
    private int deferDepth;
    private boolean recording;
    private boolean shadowMaskPass;
    private boolean hashing;
    private RetainedTextBatch activeRetainedBatch;
    private long drawHash;
    private long frameCounter;
    private RenderTarget renderTarget;
    private int targetWidth;
    private int targetHeight;
    private float guiScale;
    private RendererRegistry renderers;
    private ChannelInterest depthMode;
    private final Vector4f scissorTopLeft;
    private final Vector4f scissorTopRight;
    private final Vector4f scissorBottomRight;
    private final Vector4f scissorBottomLeft;
    private AbstractQuadBatchRenderer activeRenderer;
    private static Boolean openGlBackend;

    public Render2DContext(
            RenderTarget renderTarget, int targetWidth, int targetHeight, float guiScale) {

        this.matrixStack = new Matrix4fStack(32);

        this.scissorStack = new ArrayDeque<>(16);

        this.commands = new ArrayList<>(512);

        this.deferredRects = new ArrayList<>(128);

        this.deferredTexts = new ArrayList<>(128);
        this.retainedBatches = new IdentityHashMap<>();
        this.depthMode = ChannelInterest.NONE;
        this.scissorTopLeft = new Vector4f();
        this.scissorTopRight = new Vector4f();
        this.scissorBottomRight = new Vector4f();
        this.scissorBottomLeft = new Vector4f();
        this.beginFrame(renderTarget, targetWidth, targetHeight, guiScale);
    }

    private RoundedRectRenderer getRectRenderer() {
        return this.renderers.getRectRenderer();
    }

    public ChannelInterest getDepthMode() {
        return this.depthMode;
    }

    private void mixInt(int value) {
        this.drawHash ^= value;

        this.drawHash *= 1099511628211L;
    }

    public Vector4f getScissorBottomLeft() {
        return this.scissorBottomLeft;
    }

    public void endDeferred() {
        if (!this.recording && this.deferDepth != 0) {
            this.deferDepth--;
            if (this.deferDepth == 0) {
                this.flushDeferred();
            }
        }
    }

    public long endHashing() {
        this.hashing = false;
        return this.drawHash;
    }

    private void mixFloat(float value) {
        this.mixInt(Float.floatToRawIntBits(value));
    }

    public void drawRect(RoundedRectShape shape) {
        if (this.recording) {
            this.addCommand(DrawCommandType.RECTANGLE).payload = shape.copy();
        } else if (this.deferDepth > 0) {
            this.deferredRects.add(shape);
        } else {
            if (this.hashing) {
                this.hashRect(shape);
            }

            this.switchRenderer(this.getRectRenderer());
            this.getRectRenderer().addRect(shape);
        }
    }

    private void replayInternal(boolean shadowMask) {
        this.flush();
        this.resetState();
        this.shadowMaskPass = shadowMask;

        for (int index = 0; index < this.commandCount; index++) {
            DrawCommand command = this.commands.get(index);
            switch (command.commandType) {
                case RECTANGLE:
                    RoundedRectShape shape = (RoundedRectShape) command.payload;
                    if (!shadowMask || shape.getShadowAlpha() >= 0) {
                        this.drawRect(shape);
                    }
                    break;
                case TEXT:
                    if (!shadowMask) {
                        this.drawText((TextDrawRequest) command.payload);
                    }
                    break;
                case TEXTURE:
                    if (!shadowMask) {
                        this.drawTexture((TexturedRectShape) command.payload);
                    }
                    break;
                case PUSH:
                    this.pushPose();
                    break;
                case POP:
                    this.popPose();
                    break;
                case TRANSLATE:
                    this.translate3D(command.param1, command.param2, command.param3);
                    break;
                case SCALE:
                    this.scale(command.param1, command.param2, command.param3);
                    break;
                case ROTATE:
                    this.rotate(command.param1, command.param2, command.param3, command.param4);
                    break;
                case SCISSOR_START:
                    this.beginScissor(
                            command.param1, command.param2, command.param3, command.param4);
                    break;
                case SCISSOR_END:
                    this.endScissor();
                    break;
                case DEPTH_WRITE:
                    if (!shadowMask) {
                        this.beginDepthWrite();
                    }
                    break;
                case DEPTH_READ:
                    if (!shadowMask) {
                        this.beginDepthRead();
                    }
                    break;
                case DEPTH_END:
                    if (!shadowMask) {
                        this.endDepth();
                    }
                    break;
                case CUSTOM:
                    if (!shadowMask) {
                        this.runCustom((Runnable) command.payload);
                    }
                    break;
                case FLUSH:
                    this.flush();
            }
        }

        if (shadowMask) {
            this.flush();
        }

        this.shadowMaskPass = false;
        this.resetState();
    }

    public void initRenderers() {
        this.renderers = new RendererRegistry(Minecraft.getInstance().getResourceManager(), this);
    }

    public boolean hasShadowRects() {

        for (int index = 0; index < this.commandCount; index++) {
            DrawCommand command = this.commands.get(index);
            if (command.commandType == DrawCommandType.RECTANGLE
                    && ((RoundedRectShape) command.payload).getShadowAlpha() >= 0) {
                return true;
            }
        }

        return false;
    }

    private void hashText(TextDrawRequest request) {
        this.mixInt(2);
        this.hashTransformAndScissor();
        this.mixInt(request.getText() == null ? 0 : request.getText().hashCode());
        this.mixInt(System.identityHashCode(request.getFont()));
        this.mixFloat(request.getX());
        this.mixFloat(request.getY());
        this.mixFloat(request.getFontSize());
        this.mixInt(request.isCentered() ? 1 : 0);
        this.mixFloat(request.getBoxWidth());
        this.mixFloat(request.getBoxHeight());
        this.mixInt(request.hasShadow() ? 1 : 0);
        this.mixInt(request.getPaint().getPaintType().ordinal());
        this.mixInt(request.getPaint().getTopLeftColor());
        this.mixInt(request.getPaint().getTopRightColor());
        this.mixInt(request.getPaint().getBottomRightColor());
        this.mixInt(request.getPaint().getBottomLeftColor());
        this.mixFloat(request.getPaint().getGradientStartX());
        this.mixFloat(request.getPaint().getGradientStartY());
        this.mixFloat(request.getPaint().getGradientEndX());
        this.mixFloat(request.getPaint().getGradientEndY());
    }

    public ScissorRect getCurrentScissor() {
        return this.scissorStack.isEmpty() ? null : this.scissorStack.peek();
    }

    public void endScissor() {
        if (this.recording) {
            this.addCommand(DrawCommandType.SCISSOR_END);
        } else {
            this.flushDeferred();
            if (this.activeRenderer != null) {
                this.activeRenderer.flush();
            }

            if (!this.scissorStack.isEmpty()) {
                this.scissorStack.pop();
            }
        }
    }

    public void endRecording() {
        this.recording = false;
    }

    public Vector4f getScissorBottomRight() {
        return this.scissorBottomRight;
    }

    public void rotateZ(float degrees) {
        if (this.recording) {
            DrawCommand command = this.addCommand(DrawCommandType.ROTATE);
            command.param1 = degrees;
            command.param4 = 1.0F;
        } else {
            this.matrixStack.rotateZ((float) Math.toRadians(degrees));
        }
    }

    public void resetState() {
        this.matrixStack.clear();
        this.matrixStack.identity();
        this.scissorStack.clear();
        this.deferredRects.clear();
        this.deferredTexts.clear();
        this.deferDepth = 0;
        this.activeRetainedBatch = null;
    }

    public static boolean isOpenGlBackend() {
        if (openGlBackend == null) {
            openGlBackend = RenderSystem.getDevice().getDeviceInfo().backendName().equals("OpenGL");
        }

        return openGlBackend;
    }

    public void destroyRenderers() {
        if (this.renderers != null) {
            this.renderers.releaseAll();
            this.retainedBatches.clear();
        }
    }

    public void beginRecording() {
        this.flush();
        this.commandCount = 0;
        this.recording = true;
    }

    public long getFrameCounter() {
        return this.frameCounter;
    }

    public RendererRegistry getRenderers() {
        return this.renderers;
    }

    public RenderTarget getRenderTarget() {
        return this.renderTarget;
    }

    public void beginDepthWrite() {
        if (this.recording) {
            this.addCommand(DrawCommandType.DEPTH_WRITE);
        } else {
            this.flush();
            this.depthMode = ChannelInterest.WRITE;
            GpuTexture gpuTexture = this.renderTarget.getDepthTexture();
            if (gpuTexture != null) {
                RenderSystem.getDevice().createCommandEncoder().clearDepthTexture(gpuTexture, 1.0);
            }
        }
    }

    public List<TextDrawRequest> getDeferredTexts() {
        return this.deferredTexts;
    }

    public void drawText(TextDrawRequest request) {
        if (this.recording) {
            this.addCommand(DrawCommandType.TEXT).payload = request.copy();
        } else if (this.activeRetainedBatch == null
                || !this.activeRetainedBatch.tryCapture(request)) {
            if (this.deferDepth > 0) {
                this.deferredTexts.add(request);
            } else {
                if (this.hashing) {
                    this.hashText(request);
                }

                this.switchRenderer(this.getTextRenderer());
                this.getTextRenderer().drawText(request);
            }
        }
    }

    public void replayCommands() {
        this.replayInternal(false);
    }

    public SandboxShaderRenderer getSandboxRenderer() {
        return this.renderers != null ? this.renderers.getSandboxRenderer() : null;
    }

    public void translate(float x, float y) {
        if (this.recording) {
            DrawCommand command = this.addCommand(DrawCommandType.TRANSLATE);
            command.param1 = x;
            command.param2 = y;
        } else {
            this.matrixStack.translate(x * this.guiScale, y * this.guiScale, 0.0F);
        }
    }

    public Vector4f getScissorTopLeft() {
        return this.scissorTopLeft;
    }

    public void endFrame() {

        int savedDeferDepth = this.deferDepth;
        this.deferDepth = 0;
        this.flushDeferred();
        this.deferDepth = savedDeferDepth;
        if (this.activeRenderer != null) {
            this.activeRenderer.endBatch();
            this.activeRenderer = null;
        }

        this.hashing = false;
        RoundedRectShape.resetPool();
        TexturedRectShape.resetPool();
        TextDrawRequest.resetPool();
        this.commandCount = 0;
        this.deferDepth = 0;
        if (isOpenGlBackend()) {
            GlStateUtil.enableDepthTest();

            GlStateUtil.setDepthFunc(515);
            GlStateUtil.setDepthMask(true);

            GlStateUtil.enableBlend();

            GlStateUtil.setBlendFuncSeparate(770, 771, 1, 0);
            GlStateUtil.setColorMask(true, true, true, true);
            GlStateUtil.disableCull();
        }
    }

    public void scaleUniform(float scale) {
        if (this.recording) {
            DrawCommand command = this.addCommand(DrawCommandType.SCALE);
            command.param1 = scale;
            command.param2 = scale;
            command.param3 = 1.0F;
        } else {
            this.matrixStack.scale(scale, scale, 1.0F);
        }
    }

    public Map<Object, RetainedTextBatch> getRetainedBatches() {
        return this.retainedBatches;
    }

    public void beginDeferred() {
        if (!this.recording) {
            if (this.deferDepth == 0) {
                this.flush();
            }

            this.deferDepth++;
        }
    }

    public AbstractQuadBatchRenderer getActiveRenderer() {
        return this.activeRenderer;
    }

    public BackdropBlurRenderer getBlurRenderer() {
        return this.renderers != null ? this.renderers.getBlurRenderer() : null;
    }

    public void runCustom(Runnable action) {
        if (this.recording) {
            this.addCommand(DrawCommandType.CUSTOM).payload = action;
        } else {
            this.flushDeferred();
            this.flush();
            action.run();
        }
    }

    private void hashRect(RoundedRectShape shape) {
        this.mixInt(1);
        this.hashTransformAndScissor();
        this.mixFloat(shape.getX());
        this.mixFloat(shape.getY());
        this.mixFloat(shape.getWidth());
        this.mixFloat(shape.getHeight());
        this.mixFloat(shape.getTopLeftRadius());
        this.mixFloat(shape.getTopRightRadius());
        this.mixFloat(shape.getBottomRightRadius());
        this.mixFloat(shape.getBottomLeftRadius());
        this.mixFloat(shape.getOutlineWidth());
        this.mixInt(shape.getShadowAlpha());
        this.mixInt(shape.isOuterAntialiasOnly() ? 1 : 0);
        this.mixInt(shape.getTopLeftColor());
        this.mixInt(shape.getTopRightColor());
        this.mixInt(shape.getBottomRightColor());
        this.mixInt(shape.getBottomLeftColor());
        this.mixInt(shape.getOutlineTopLeftColor());
        this.mixInt(shape.getOutlineTopRightColor());
        this.mixInt(shape.getOutlineBottomRightColor());
        this.mixInt(shape.getOutlineBottomLeftColor());
    }

    public void beginDepthRead() {
        if (this.recording) {
            this.addCommand(DrawCommandType.DEPTH_READ);
        } else {
            this.flush();
            this.depthMode = ChannelInterest.READ;
        }
    }

    public void popPose() {
        if (this.recording) {
            this.addCommand(DrawCommandType.POP);
        } else {
            this.matrixStack.popMatrix();
        }
    }

    private MsdfTextRenderer getTextRenderer() {
        return this.renderers.getTextRenderer();
    }

    public int getTargetHeight() {
        return this.targetHeight;
    }

    public void flush() {
        if (this.recording) {
            this.addCommand(DrawCommandType.FLUSH);
        } else {
            this.flushDeferred();
            if (this.activeRenderer != null) {
                this.activeRenderer.endBatch();
                this.activeRenderer = null;
            }
        }
    }

    public void rotate(float degrees, float axisX, float axisY, float axisZ) {
        if (this.recording) {
            DrawCommand command = this.addCommand(DrawCommandType.ROTATE);
            command.param1 = degrees;
            command.param2 = axisX;
            command.param3 = axisY;
            command.param4 = axisZ;
        } else {
            this.matrixStack.rotate((float) Math.toRadians(degrees), axisX, axisY, axisZ);
        }
    }

    public void beginFrame(
            RenderTarget renderTarget, int targetWidth, int targetHeight, float guiScale) {
        this.renderTarget = renderTarget;
        this.targetWidth = targetWidth;
        this.targetHeight = targetHeight;
        this.guiScale = guiScale;
        this.frameCounter++;
        this.resetState();
    }

    public long getDrawHash() {
        return this.drawHash;
    }

    public void drawTexture(TexturedRectShape shape) {
        if (this.recording) {
            this.addCommand(DrawCommandType.TEXTURE).payload = shape.copy();
        } else {
            this.flushDeferred();
            if (this.hashing) {
                this.hashTexture(shape);
            }

            this.switchRenderer(this.getTextureRenderer());
            this.getTextureRenderer().addTexture(shape);
        }
    }

    public float getGuiScale() {
        return this.guiScale;
    }

    public int getTargetWidth() {
        return this.targetWidth;
    }

    public void setRenderTarget(RenderTarget renderTarget) {
        if (this.renderTarget != renderTarget) {
            this.flush();
            this.renderTarget = renderTarget;
        }
    }

    public ShadowRenderer getShadowRenderer() {
        return this.renderers != null ? this.renderers.getShadowRenderer() : null;
    }

    public LiquidGlassRenderer getLiquidGlassRenderer() {
        return this.renderers != null ? this.renderers.getLiquidGlassRenderer() : null;
    }

    public void setGuiScale(float guiScale) {
        this.guiScale = guiScale;
    }

    public SkyShaderRenderer getSkyRenderer() {
        return this.renderers != null ? this.renderers.getSkyRenderer() : null;
    }

    public boolean isShadowMaskPass() {
        return this.shadowMaskPass;
    }

    public void replayShadowMask() {
        this.replayInternal(true);
    }

    private void hashRectMask(RoundedRectShape shape) {
        if (shape.getShadowAlpha() >= 0) {
            this.mixInt(1);
            this.hashTransformAndScissor();
            this.mixFloat(shape.getX());
            this.mixFloat(shape.getY());
            this.mixFloat(shape.getWidth());
            this.mixFloat(shape.getHeight());
            this.mixFloat(shape.getTopLeftRadius());
            this.mixFloat(shape.getTopRightRadius());
            this.mixFloat(shape.getBottomRightRadius());
            this.mixFloat(shape.getBottomLeftRadius());
            this.mixInt(shape.getShadowAlpha());
            this.mixInt(shape.isOuterAntialiasOnly() ? 1 : 0);
        }
    }

    public void endRetainedText(Object key) {
        if (!this.recording && this.activeRetainedBatch == this.retainedBatches.get(key)) {
            RetainedTextBatch batch = this.activeRetainedBatch;
            this.activeRetainedBatch = null;
            this.flush();
            batch.finishAndDraw();
        }
    }

    public void translate3D(float x, float y, float z) {
        if (this.recording) {
            DrawCommand command = this.addCommand(DrawCommandType.TRANSLATE);
            command.param1 = x;
            command.param2 = y;
            command.param3 = z;
        } else {
            this.matrixStack.translate(x * this.guiScale, y * this.guiScale, z * this.guiScale);
        }
    }

    private DrawCommand addCommand(DrawCommandType commandType) {
        DrawCommand command;
        if (this.commandCount == this.commands.size()) {
            command = new DrawCommand();
            this.commands.add(command);
        } else {
            command = this.commands.get(this.commandCount);
        }

        this.commandCount++;
        command.commandType = commandType;
        command.payload = null;
        command.param1 = command.param2 = command.param3 = command.param4 = 0.0F;
        return command;
    }

    private void switchRenderer(AbstractQuadBatchRenderer renderer) {
        if (this.activeRenderer != renderer) {
            if (this.activeRenderer != null) {
                this.activeRenderer.endBatch();
            }

            this.activeRenderer = renderer;
            this.activeRenderer.beginBatch();
        }
    }

    public void scale(float x, float y, float z) {
        if (this.recording) {
            DrawCommand command = this.addCommand(DrawCommandType.SCALE);
            command.param1 = x;
            command.param2 = y;
            command.param3 = z;
        } else {
            this.matrixStack.scale(x, y, z);
        }
    }

    private void flushDeferred() {
        if (!this.deferredRects.isEmpty() || !this.deferredTexts.isEmpty()) {
            int savedDeferDepth = this.deferDepth;
            this.deferDepth = 0;

            for (RoundedRectShape shape : this.deferredRects) {
                this.drawRect(shape);
            }

            for (TextDrawRequest request : this.deferredTexts) {
                this.drawText(request);
            }

            this.deferredRects.clear();
            this.deferredTexts.clear();
            this.deferDepth = savedDeferDepth;
        }
    }

    public void pushPose() {
        if (this.recording) {
            this.addCommand(DrawCommandType.PUSH);
        } else {
            this.matrixStack.pushMatrix();
        }
    }

    public void beginScissor(float x, float y, float width, float height) {
        if (this.recording) {
            DrawCommand command = this.addCommand(DrawCommandType.SCISSOR_START);
            command.param1 = x;
            command.param2 = y;
            command.param3 = width;
            command.param4 = height;
        } else {
            this.flushDeferred();
            if (this.activeRenderer != null) {
                this.activeRenderer.flush();
            }

            float scaledX = x * this.guiScale;
            float scaledY = y * this.guiScale;
            float scaledWidth = width * this.guiScale;
            float scaledHeight = height * this.guiScale;
            this.scissorTopLeft.set(scaledX, scaledY, 0.0F, 1.0F);
            this.scissorTopRight.set(scaledX + scaledWidth, scaledY, 0.0F, 1.0F);
            this.scissorBottomRight.set(scaledX + scaledWidth, scaledY + scaledHeight, 0.0F, 1.0F);
            this.scissorBottomLeft.set(scaledX, scaledY + scaledHeight, 0.0F, 1.0F);
            this.matrixStack.transform(this.scissorTopLeft);
            this.matrixStack.transform(this.scissorTopRight);
            this.matrixStack.transform(this.scissorBottomRight);
            this.matrixStack.transform(this.scissorBottomLeft);
            float minX =
                    Math.min(
                            Math.min(this.scissorTopLeft.x, this.scissorTopRight.x),
                            Math.min(this.scissorBottomRight.x, this.scissorBottomLeft.x));
            float minY =
                    Math.min(
                            Math.min(this.scissorTopLeft.y, this.scissorTopRight.y),
                            Math.min(this.scissorBottomRight.y, this.scissorBottomLeft.y));
            float maxX =
                    Math.max(
                            Math.max(this.scissorTopLeft.x, this.scissorTopRight.x),
                            Math.max(this.scissorBottomRight.x, this.scissorBottomLeft.x));
            float maxY =
                    Math.max(
                            Math.max(this.scissorTopLeft.y, this.scissorTopRight.y),
                            Math.max(this.scissorBottomRight.y, this.scissorBottomLeft.y));
            int scissorX = (int) minX;
            int scissorWidth = (int) (maxX - minX);
            int scissorHeight = (int) (maxY - minY);
            int scissorY = (int) (this.targetHeight - maxY);
            if (scissorWidth < 0) {
                scissorWidth = 0;
            }

            if (scissorHeight < 0) {
                scissorHeight = 0;
            }

            if (!this.scissorStack.isEmpty()) {
                ScissorRect parentScissor = this.scissorStack.peek();
                int clippedX = Math.max(scissorX, parentScissor.x());
                int clippedY = Math.max(scissorY, parentScissor.y());
                int clippedWidth =
                        Math.min(scissorX + scissorWidth, parentScissor.x() + parentScissor.width())
                                - clippedX;
                int clippedHeight =
                        Math.min(
                                        scissorY + scissorHeight,
                                        parentScissor.y() + parentScissor.height())
                                - clippedY;
                scissorX = clippedX;
                scissorY = clippedY;
                scissorWidth = Math.max(0, clippedWidth);
                scissorHeight = Math.max(0, clippedHeight);
            }

            this.scissorStack.push(
                    new ScissorRect(scissorX, scissorY, scissorWidth, scissorHeight));
        }
    }

    public Deque<ScissorRect> getScissorStack() {
        return this.scissorStack;
    }

    public int getCommandCount() {
        return this.commandCount;
    }

    private void hashTransformAndScissor() {
        this.mixFloat(this.matrixStack.m00());
        this.mixFloat(this.matrixStack.m01());
        this.mixFloat(this.matrixStack.m02());
        this.mixFloat(this.matrixStack.m03());
        this.mixFloat(this.matrixStack.m10());
        this.mixFloat(this.matrixStack.m11());
        this.mixFloat(this.matrixStack.m12());
        this.mixFloat(this.matrixStack.m13());
        this.mixFloat(this.matrixStack.m20());
        this.mixFloat(this.matrixStack.m21());
        this.mixFloat(this.matrixStack.m22());
        this.mixFloat(this.matrixStack.m23());
        this.mixFloat(this.matrixStack.m30());
        this.mixFloat(this.matrixStack.m31());
        this.mixFloat(this.matrixStack.m32());
        this.mixFloat(this.matrixStack.m33());
        ScissorRect scissor = this.getCurrentScissor();
        if (scissor == null) {
            this.mixInt(0);
        } else {
            this.mixInt(1);
            this.mixInt(scissor.getX());
            this.mixInt(scissor.getY());
            this.mixInt(scissor.getWidth());
            this.mixInt(scissor.getHeight());
        }
    }

    public boolean isRecording() {
        return this.recording;
    }

    public WorldTextRenderer getWorldTextRenderer() {
        return this.renderers != null ? this.renderers.getWorldTextRenderer() : null;
    }

    public List<DrawCommand> getCommands() {
        return this.commands;
    }

    public boolean isHashing() {
        return this.hashing;
    }

    private void hashTexture(TexturedRectShape shape) {
        this.mixInt(3);
        this.hashTransformAndScissor();
        this.mixFloat(shape.getX());
        this.mixFloat(shape.getY());
        this.mixFloat(shape.getWidth());
        this.mixFloat(shape.getHeight());
        this.mixFloat(shape.getUMin());
        this.mixFloat(shape.getVMin());
        this.mixFloat(shape.getUMax());
        this.mixFloat(shape.getVMax());
        this.mixFloat(shape.getTopLeftRadius());
        this.mixFloat(shape.getTopRightRadius());
        this.mixFloat(shape.getBottomRightRadius());
        this.mixFloat(shape.getBottomLeftRadius());
        this.mixInt(shape.getVariant());
        this.mixInt(System.identityHashCode(shape.getTexture()));
        this.mixInt(shape.getTopLeftColor());
        this.mixInt(shape.getTopRightColor());
        this.mixInt(shape.getBottomRightColor());
        this.mixInt(shape.getBottomLeftColor());
        this.mixInt(shape.isNearestFilter() ? 1 : 0);
        this.mixInt(shape.isOuterAntialiasOnly() ? 1 : 0);
    }

    public EntityGlowRenderer getEntityGlowRenderer() {
        return this.renderers != null ? this.renderers.getEntityGlowRenderer() : null;
    }

    public List<RoundedRectShape> getDeferredRects() {
        return this.deferredRects;
    }

    public int getDeferDepth() {
        return this.deferDepth;
    }

    public void endDepth() {

        if (this.recording) {
            this.addCommand(DrawCommandType.DEPTH_END);
        } else {
            this.flush();
            this.depthMode = ChannelInterest.NONE;
            if (isOpenGlBackend()) {
                GlStateUtil.disableDepthTest();
                GlStateUtil.setDepthMask(true);

                GlStateUtil.setDepthFunc(515);
            }
        }
    }

    public long computeShadowMaskHash() {
        this.resetState();

        this.drawHash = -3750763034362895579L;
        this.hashing = true;

        for (int index = 0; index < this.commandCount; index++) {
            DrawCommand command = this.commands.get(index);
            switch (command.commandType) {
                case RECTANGLE:
                    this.hashRectMask((RoundedRectShape) command.payload);
                case TEXT:
                case TEXTURE:
                default:
                    break;
                case PUSH:
                    this.pushPose();
                    break;
                case POP:
                    this.popPose();
                    break;
                case TRANSLATE:
                    this.translate3D(command.param1, command.param2, command.param3);
                    break;
                case SCALE:
                    this.scale(command.param1, command.param2, command.param3);
                    break;
                case ROTATE:
                    this.rotate(command.param1, command.param2, command.param3, command.param4);
                    break;
                case SCISSOR_START:
                    this.beginScissor(
                            command.param1, command.param2, command.param3, command.param4);
                    break;
                case SCISSOR_END:
                    this.endScissor();
            }
        }

        this.hashing = false;
        this.resetState();
        return this.drawHash;
    }

    public void beginRetainedText(Object key, boolean invalidate) {
        if (!this.recording) {
            this.flushDeferred();
            this.flush();
            this.activeRetainedBatch =
                    this.retainedBatches.computeIfAbsent(
                            key, ignored -> this.getTextRenderer().createRetainedBatch());
            if (invalidate) {
                this.activeRetainedBatch.invalidate();
            }

            this.activeRetainedBatch.beginRecording();
        }
    }

    public void beginHashing() {

        this.drawHash = -3750763034362895579L;
        this.hashing = true;
    }

    public Matrix4fStack getMatrixStack() {
        return this.matrixStack;
    }

    public RetainedTextBatch getActiveRetainedBatch() {
        return this.activeRetainedBatch;
    }

    public Vector4f getScissorTopRight() {
        return this.scissorTopRight;
    }

    private TexturedQuadRenderer getTextureRenderer() {
        return this.renderers.getTextureRenderer();
    }
}
