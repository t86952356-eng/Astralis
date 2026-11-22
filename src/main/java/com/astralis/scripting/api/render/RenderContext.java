package com.astralis.scripting.api.render;

import astralis.Astralis;
import astralis.util.render.BlurShadowManager;
import astralis.util.render.FontManager;
import astralis.util.render.Render2DContext;
import astralis.util.render.RoundedRectShape;
import astralis.util.render.TexturedRectShape;
import astralis.util.render.font.SizedFont;
import astralis.util.render.font.TextDrawRequest;

import com.astralis.scripting.InstanceAccess;
import com.astralis.scripting.api.entity.ScriptEntity;
import com.astralis.scripting.api.math.ScriptVector2;
import com.astralis.scripting.api.math.ScriptVector3;
import com.mojang.blaze3d.textures.GpuTexture;

import net.minecraft.client.player.AbstractClientPlayer;

import org.joml.Matrix4f;
import org.joml.Vector4f;

public class RenderContext implements InstanceAccess, AutoCloseable {
    private final Render2DContext drawContext;
    private final Matrix4f projectionMatrix;
    private final double cameraX;
    private final double cameraY;
    private final double cameraZ;
    private int poseDepth;
    private int scissorDepth;
    private int maskState;
    private int operationCount;
    private boolean closed;

    public RenderContext(
            Object contextObject,
            Object projection,
            double cameraX,
            double cameraY,
            double cameraZ) {
        if (contextObject instanceof Render2DContext render2DContext) {
            if (projection != null && !(projection instanceof Matrix4f)) {
                throw new IllegalArgumentException("Expected a projection matrix.");
            }

            this.drawContext = render2DContext;
            this.projectionMatrix = (Matrix4f) projection;
            this.cameraX = cameraX;
            this.cameraY = cameraY;
            this.cameraZ = cameraZ;
        } else {
            throw new IllegalArgumentException("Expected a Sapphire render context.");
        }
    }

    public RectBuilder rect(double x, double y, double width, double height) {
        this.validateRenderValues(x, y, width, height);
        if (!(width <= 0.0) && !(height <= 0.0) && !(width > 1000000.0) && !(height > 1000000.0)) {
            return new RectBuilder(this, (float) x, (float) y, (float) width, (float) height);
        } else {
            throw new IllegalArgumentException(
                    "Rectangle dimensions must be positive and at most 1,000,000.");
        }
    }

    public RectBuilder rect(ScriptVector2 position, ScriptVector2 size) {
        return this.rect(position.x(), position.y(), size.x(), size.y());
    }

    public TextBuilder text(String text, double x, double y) {
        this.validateRenderValues(x, y);
        if (text != null && text.length() <= 8192) {
            return new TextBuilder(this, text, (float) x, (float) y);
        } else {
            throw new IllegalArgumentException(
                    "Script text must contain at most 8,192 characters.");
        }
    }

    public TextBuilder text(String text, double x, double y, String fontName, double size) {
        return this.text(text, x, y).font(fontName).size(size);
    }

    public float getTextWidth(String text, String fontName, float size) {
        this.ensureActive();
        SizedFont resolvedFont = this.resolveFont(fontName, size);
        String measuredText = text;
        SizedFont sizedFont = resolvedFont;
        return sizedFont.getStringWidth(measuredText);
    }

    public float getLineHeight(String fontName, float size) {
        this.ensureActive();
        SizedFont sizedFont = this.resolveFont(fontName, size);
        return sizedFont.getLineHeight();
    }

    public void push() {
        this.ensureActive();
        this.drawContext.pushPose();
        this.poseDepth++;
    }

    public void pop() {
        this.ensureActive();
        if (this.poseDepth == 0) {
            throw new IllegalStateException("Cannot pop outside this script render scope.");
        }

        this.drawContext.popPose();
        this.poseDepth--;
    }

    public void translate(double x, double y) {
        this.validateRenderValues(x, y);
        this.drawContext.translate((float) x, (float) y);
    }

    public void translate(double x, double y, double z) {
        this.validateRenderValues(x, y, z);
        this.drawContext.translate3D((float) x, (float) y, (float) z);
    }

    public void scale(double factor) {
        this.validateRenderValues(factor);
        if (Math.abs(factor) > 1024.0) {
            throw new IllegalArgumentException("Render scale cannot exceed 1,024.");
        }

        this.drawContext.scaleUniform((float) factor);
    }

    public void scale(double scaleX, double scaleY) {
        this.validateRenderValues(scaleX, scaleY);
        if (!(Math.abs(scaleX) > 1024.0) && !(Math.abs(scaleY) > 1024.0)) {
            this.drawContext.scale((float) scaleX, (float) scaleY, 1.0F);
        } else {
            throw new IllegalArgumentException("Render scale cannot exceed 1,024.");
        }
    }

    public void rotateZ(double degrees) {
        this.validateRenderValues(degrees);
        this.drawContext.rotateZ((float) degrees);
    }

    public void line(
            double startX, double startY, double endX, double endY, double width, int color) {
        this.validateRenderValues(startX, startY, endX, endY, width);
        if (!(width <= 0.0) && !(width > 4096.0)) {
            double deltaX = endX - startX;
            double deltaY = endY - startY;
            this.push();

            try {
                this.translate(startX, startY);
                this.rotateZ(Math.toDegrees(Math.atan2(deltaY, deltaX)));
                this.rect(0.0, -width / 2.0, Math.hypot(deltaX, deltaY), width).color(color).draw();
            } finally {
                this.pop();
            }
        } else {
            throw new IllegalArgumentException("Line width must be positive and at most 4,096.");
        }
    }

    public void line(ScriptVector2 start, ScriptVector2 end, double width, ScriptColor color) {
        this.line(start.x(), start.y(), end.x(), end.y(), width, color.argb());
    }

    public void worldText(
            String text, double x, double y, double z, double size, int color, boolean shadow) {
        this.validateRenderValues(x, y, z, size);
        double[] screenPosition = this.worldToScreen(x, y, z);
        if (screenPosition != null) {
            TextBuilder textBuilder =
                    this.text(text, screenPosition[0], screenPosition[1]).size(size).color(color);
            if (shadow) {
                textBuilder.shadow();
            }

            textBuilder.draw();
        }
    }

    public boolean drawPlayerHead(
            ScriptEntity entity, double x, double y, double size, double radius, int color) {
        this.validateRenderValues(x, y, size, radius);
        if (entity == null
                || size <= 0.0
                || size > 1000000.0
                || radius < 0.0
                || radius > Math.min(size / 2.0, 4096.0)) {
            throw new IllegalArgumentException("Player head bounds and radius are invalid.");
        } else if (mc.level == null) {
            return false;
        } else if (mc.level.getEntity(entity.getId())
                        instanceof AbstractClientPlayer abstractClientPlayer
                && abstractClientPlayer.getUUID().toString().equals(entity.getUuid())) {
            GpuTexture gpuTexture =
                    mc.getTextureManager()
                            .getTexture(abstractClientPlayer.getSkin().body().texturePath())
                            .getTexture();
            this.countDrawOperation();
            this.drawContext.drawTexture(
                    TexturedRectShape.obtain()
                            .setBounds((float) x, (float) y, (float) size, (float) size)
                            .setTexture(gpuTexture)
                            .setUv(0.125F, 0.125F, 0.25F, 0.25F)
                            .setRadius((float) radius)
                            .useNearestFilter()
                            .setColor(color));
            this.countDrawOperation();
            this.drawContext.drawTexture(
                    TexturedRectShape.obtain()
                            .setBounds((float) x, (float) y, (float) size, (float) size)
                            .setTexture(gpuTexture)
                            .setUv(0.625F, 0.125F, 0.75F, 0.25F)
                            .setRadius((float) radius)
                            .useNearestFilter()
                            .setColor(color));
            return true;
        } else {
            return false;
        }
    }

    public void scissor(double x, double y, double width, double height) {
        this.validateRenderValues(x, y, width, height);
        if (!(width <= 0.0) && !(height <= 0.0) && !(width > 1000000.0) && !(height > 1000000.0)) {
            this.drawContext.beginScissor((float) x, (float) y, (float) width, (float) height);
            this.scissorDepth++;
        } else {
            throw new IllegalArgumentException(
                    "Scissor dimensions must be positive and at most 1,000,000.");
        }
    }

    public void endScissor() {
        this.ensureActive();
        if (this.scissorDepth == 0) {
            throw new IllegalStateException(
                    "Cannot end a scissor outside this script render scope.");
        }

        this.drawContext.endScissor();
        this.scissorDepth--;
    }

    public void startMaskWrite() {
        this.ensureActive();
        if (this.maskState != 0) {
            throw new IllegalStateException("A depth mask is already active.");
        }

        this.drawContext.beginDepthWrite();
        this.maskState = 1;
    }

    public void startMaskRead() {
        this.ensureActive();
        if (this.maskState != 1) {
            throw new IllegalStateException("Start mask writing before reading it.");
        }

        this.drawContext.beginDepthRead();
        this.maskState = 2;
    }

    public void endMask() {
        this.ensureActive();
        if (this.maskState == 0) {
            throw new IllegalStateException("No depth mask is active.");
        }

        this.drawContext.endDepth();
        this.maskState = 0;
    }

    public double getWidth() {
        return this.drawContext.getTargetWidth() / this.drawContext.getGuiScale();
    }

    public double getHeight() {
        return this.drawContext.getTargetHeight() / this.drawContext.getGuiScale();
    }

    public double getCameraX() {
        return this.cameraX;
    }

    public double getCameraY() {
        return this.cameraY;
    }

    public double getCameraZ() {
        return this.cameraZ;
    }

    public void blurRect(double x, double y, double width, double height, double radius) {
        this.validateRenderValues(x, y, width, height, radius);
        if (!(width <= 0.0)
                && !(height <= 0.0)
                && !(width > 1000000.0)
                && !(height > 1000000.0)
                && !(radius < 0.0)
                && !(radius > Math.min(Math.min(width, height) / 2.0, 4096.0))) {
            this.countDrawOperation();
            Astralis astralisInstance = Astralis.getInstance();
            BlurShadowManager effects = astralisInstance.getRenderEffects();
            float rawX = (float) x;
            float rawY = (float) y;
            float rawWidth = (float) width;
            float rawHeight = (float) height;
            float blurRadius = (float) radius;
            float blurHeight = rawHeight;
            float blurWidth = rawWidth;
            float blurY = rawY;
            float blurX = rawX;
            BlurShadowManager blurManager = effects;
            blurManager.blurRoundedRectangle(blurX, blurY, blurWidth, blurHeight, blurRadius);
        } else {
            throw new IllegalArgumentException("Blur bounds and radius are invalid.");
        }
    }

    public double[] worldToScreen(double x, double y, double z) {
        this.ensureActive();
        if (this.projectionMatrix != null
                && Double.isFinite(x)
                && Double.isFinite(y)
                && Double.isFinite(z)) {
            Vector4f vector4f =
                    new Vector4f(
                            (float) (x - this.cameraX),
                            (float) (y - this.cameraY),
                            (float) (z - this.cameraZ),
                            1.0F);
            this.projectionMatrix.transform(vector4f);
            if (vector4f.w <= 0.0F) {
                return null;
            } else {
                float ndcX = vector4f.x / vector4f.w;
                float ndcY = vector4f.y / vector4f.w;
                float ndcZ = vector4f.z / vector4f.w;
                if (ndcX < -1.5F || ndcX > 1.5F || ndcY < -1.5F || ndcY > 1.5F) {
                    return null;
                } else {
                    return Float.isFinite(ndcZ) && !(ndcZ < -1.0F) && !(ndcZ > 1.0F)
                            ? new double[] {
                                (ndcX + 1.0F) * 0.5F * mc.getWindow().getGuiScaledWidth(),
                                (1.0F - ndcY) * 0.5F * mc.getWindow().getGuiScaledHeight()
                            }
                            : null;
                }
            }
        } else {
            return null;
        }
    }

    public ScriptVector2 worldToScreen(ScriptVector3 position) {
        double[] screenPosition = this.worldToScreen(position.x(), position.y(), position.z());
        return screenPosition != null
                ? new ScriptVector2(screenPosition[0], screenPosition[1])
                : null;
    }

    @Override
    public void close() {
        if (!this.closed) {
            if (this.maskState != 0) {
                this.drawContext.endDepth();
            }

            while (this.scissorDepth > 0) {
                this.drawContext.endScissor();
                this.scissorDepth--;
            }

            while (this.poseDepth > 0) {
                this.drawContext.popPose();
                this.poseDepth--;
            }

            this.maskState = 0;
            this.closed = true;
        }
    }

    void submitRect(RoundedRectShape shape) {
        this.countDrawOperation();
        this.drawContext.drawRect(shape);
    }

    void submitText(TextDrawRequest request) {
        this.countDrawOperation();
        this.drawContext.drawText(request);
    }

    SizedFont resolveFont(String fontName, float size) {
        this.ensureActive();
        if (fontName == null
                || !fontName.matches("(Tahoma|Inter Medium|Product Sans (Regular|Medium|Bold))")) {
            throw new IllegalArgumentException("Unsupported script font: " + fontName);
        } else if (Float.isFinite(size) && !(size < 1.0F) && !(size > 128.0F)) {
            float roundedSize = Math.round(size * 4.0F) / 4.0F;
            String resolvedName = fontName;
            return FontManager.getFont(resolvedName, roundedSize);
        } else {
            throw new IllegalArgumentException("Font size must be finite and between 1 and 128.");
        }
    }

    private void ensureActive() {
        if (this.closed) {
            throw new IllegalStateException("This render context is no longer active.");
        }
    }

    void validateRenderValues(double... values) {
        this.ensureActive();

        for (double value : values) {
            if (!Double.isFinite(value) || Math.abs(value) > 3.3554432E7) {
                throw new IllegalArgumentException(
                        "Render values must be finite and within supported bounds.");
            }
        }
    }

    private void countDrawOperation() {
        this.ensureActive();
        if (++this.operationCount > 10000) {
            throw new IllegalStateException(
                    "A script cannot draw more than 10,000 operations per callback.");
        }
    }
}
