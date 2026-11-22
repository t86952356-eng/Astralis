package astralis.util.render;

import com.mojang.blaze3d.opengl.GlStateManager;

import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL45;

public class GlStateSnapshot {
    private final int glVersion;
    private final SavedGlState savedState;

    public GlStateSnapshot(int glVersion) {
        this.glVersion = glVersion;
        this.savedState = new SavedGlState();
    }

    public final GlStateSnapshot capture() {
        int glValue;
        label50:
        {
            GL45.glGetIntegerv(34016, this.savedState.getActiveTexture());
            GlStateManager._activeTexture(33984);
            GL45.glGetIntegerv(35725, this.savedState.getProgram());
            GL45.glGetIntegerv(32873, this.savedState.getTextureBinding());
            glValue = this.glVersion;
            if (this.glVersion < 330) {
                glValue = GL.getCapabilities().GL_ARB_sampler_objects ? 1 : 0;
                if (glValue == 0) {
                    break label50;
                }
            }

            glValue = 35097;
            GL45.glGetIntegerv(glValue, this.savedState.getSamplerBinding());
        }

        GL45.glGetIntegerv(34964, this.savedState.getArrayBuffer());
        GL45.glGetIntegerv(34229, this.savedState.getVertexArray());
        glValue = this.glVersion;
        int versionThreshold = 200;
        if (glValue >= versionThreshold) {
            GL45.glGetIntegerv(2880, this.savedState.getPolygonMode());
        }

        GL45.glGetIntegerv(2978, this.savedState.getViewport());
        GL45.glGetIntegerv(3088, this.savedState.getScissorBox());
        GL45.glGetIntegerv(32969, this.savedState.getBlendSrcRgb());
        GL45.glGetIntegerv(32968, this.savedState.getBlendDstRgb());
        GL45.glGetIntegerv(32971, this.savedState.getBlendSrcAlpha());
        GL45.glGetIntegerv(32970, this.savedState.getBlendDstAlpha());
        GL45.glGetIntegerv(32777, this.savedState.getBlendEquationRgb());
        GL45.glGetIntegerv(34877, this.savedState.getBlendEquationAlpha());
        this.savedState.setBlendEnabled(GL45.glIsEnabled(3042));
        this.savedState.setCullFaceEnabled(GL45.glIsEnabled(2884));
        this.savedState.setDepthTestEnabled(GL45.glIsEnabled(2929));
        this.savedState.setStencilTestEnabled(GL45.glIsEnabled(2960));
        this.savedState.setScissorTestEnabled(GL45.glIsEnabled(3089));
        glValue = this.glVersion;
        versionThreshold = 310;
        if (glValue >= versionThreshold) {
            this.savedState.setPrimitiveRestartEnabled(GL45.glIsEnabled(36765));
        }

        this.savedState.setDepthMaskEnabled(GL45.glGetBoolean(2930));
        GL45.glGetIntegerv(35055, this.savedState.getPixelUnpackBuffer());
        GlStateManager._glBindBuffer(35052, 0);
        GL45.glGetIntegerv(3328, this.savedState.getPackSwapBytes());
        GL45.glGetIntegerv(3329, this.savedState.getPackLsbFirst());
        GL45.glGetIntegerv(3330, this.savedState.getPackRowLength());
        GL45.glGetIntegerv(3332, this.savedState.getPackSkipPixels());
        GL45.glGetIntegerv(3331, this.savedState.getPackSkipRows());
        GL45.glGetIntegerv(3333, this.savedState.getPackAlignment());
        GL45.glGetIntegerv(3312, this.savedState.getUnpackSwapBytes());
        GL45.glGetIntegerv(3313, this.savedState.getUnpackLsbFirst());
        GL45.glGetIntegerv(3317, this.savedState.getUnpackAlignment());
        GL45.glGetIntegerv(3314, this.savedState.getUnpackRowLength());
        GL45.glGetIntegerv(3316, this.savedState.getUnpackSkipPixels());
        GL45.glGetIntegerv(3315, this.savedState.getUnpackSkipRows());
        glValue = this.glVersion;
        versionThreshold = 120;
        if (glValue >= versionThreshold) {
            GL45.glGetIntegerv(32876, this.savedState.getPackImageHeight());
            GL45.glGetIntegerv(32875, this.savedState.getPackSkipImages());
            GL45.glGetIntegerv(32878, this.savedState.getUnpackImageHeight());
            GL45.glGetIntegerv(32877, this.savedState.getUnpackSkipImages());
        }

        GlStateManager._pixelStore(3317, 1);
        GlStateManager._pixelStore(3314, 0);
        GlStateManager._pixelStore(3316, 0);
        glValue = 3315;
        versionThreshold = 0;
        GlStateManager._pixelStore(glValue, versionThreshold);
        return this;
    }

    public final GlStateSnapshot restore() {
        int glValue;
        label122:
        {
            GlStateManager._glUseProgram(this.savedState.getProgram()[0]);
            GlStateManager._bindTexture(this.savedState.getTextureBinding()[0]);
            glValue = this.glVersion;
            int versionThreshold = 330;
            if (glValue < versionThreshold) {
                glValue = GL.getCapabilities().GL_ARB_sampler_objects ? 1 : 0;
                if (glValue == 0) {
                    break label122;
                }
            }

            glValue = 0;
            versionThreshold = this.savedState.getSamplerBinding()[0];
            GL45.glBindSampler(glValue, versionThreshold);
        }

        GlStateManager._activeTexture(this.savedState.getActiveTexture()[0]);
        GlStateManager._glBindVertexArray(this.savedState.getVertexArray()[0]);
        GlStateManager._glBindBuffer(34962, this.savedState.getArrayBuffer()[0]);
        GL45.glBlendEquationSeparate(
                this.savedState.getBlendEquationRgb()[0],
                this.savedState.getBlendEquationAlpha()[0]);
        GlStateManager._blendFuncSeparate(
                this.savedState.getBlendSrcRgb()[0],
                this.savedState.getBlendDstRgb()[0],
                this.savedState.getBlendSrcAlpha()[0],
                this.savedState.getBlendDstAlpha()[0]);
        glValue = this.savedState.isBlendEnabled() ? 1 : 0;
        if (glValue != 0) {
            GlStateManager._enableBlend(0);
        } else {
            glValue = 0;
            GlStateManager._disableBlend(glValue);
        }

        if (this.savedState.isCullFaceEnabled()) {
            GlStateManager._enableCull();
        } else {
            GlStateManager._disableCull();
        }

        if (this.savedState.isDepthTestEnabled()) {
            GlStateManager._enableDepthTest();
        } else {
            GlStateManager._disableDepthTest();
        }

        glValue = this.savedState.isStencilTestEnabled() ? 1 : 0;
        if (glValue != 0) {
            GL45.glEnable(2960);
        } else {
            glValue = 2960;
            GL45.glDisable(glValue);
        }

        if (this.savedState.isScissorTestEnabled()) {
            GlStateManager._enableScissorTest();
        } else {
            GlStateManager._disableScissorTest();
        }

        glValue = this.glVersion;
        int versionThreshold = 310;
        if (glValue >= versionThreshold) {
            glValue = this.savedState.isPrimitiveRestartEnabled() ? 1 : 0;
            if (glValue != 0) {
                GL45.glEnable(36765);
            } else {
                glValue = 36765;
                GL45.glDisable(glValue);
            }
        }

        glValue = this.glVersion;
        versionThreshold = 200;
        if (glValue >= versionThreshold) {
            GlStateManager._polygonMode(1032, this.savedState.getPolygonMode()[0]);
        }

        GlStateManager._viewport(
                this.savedState.getViewport()[0],
                this.savedState.getViewport()[1],
                this.savedState.getViewport()[2],
                this.savedState.getViewport()[3]);
        GlStateManager._scissorBox(
                this.savedState.getScissorBox()[0],
                this.savedState.getScissorBox()[1],
                this.savedState.getScissorBox()[2],
                this.savedState.getScissorBox()[3]);
        GlStateManager._pixelStore(3328, this.savedState.getPackSwapBytes()[0]);
        GlStateManager._pixelStore(3329, this.savedState.getPackLsbFirst()[0]);
        GlStateManager._pixelStore(3330, this.savedState.getPackRowLength()[0]);
        GlStateManager._pixelStore(3332, this.savedState.getPackSkipPixels()[0]);
        GlStateManager._pixelStore(3331, this.savedState.getPackSkipRows()[0]);
        GlStateManager._pixelStore(3333, this.savedState.getPackAlignment()[0]);
        GlStateManager._glBindBuffer(35052, this.savedState.getPixelUnpackBuffer()[0]);
        GlStateManager._pixelStore(3312, this.savedState.getUnpackSwapBytes()[0]);
        GlStateManager._pixelStore(3313, this.savedState.getUnpackLsbFirst()[0]);
        GlStateManager._pixelStore(3317, this.savedState.getUnpackAlignment()[0]);
        GlStateManager._pixelStore(3314, this.savedState.getUnpackRowLength()[0]);
        GlStateManager._pixelStore(3316, this.savedState.getUnpackSkipPixels()[0]);
        GlStateManager._pixelStore(3315, this.savedState.getUnpackSkipRows()[0]);
        glValue = this.glVersion;
        versionThreshold = 120;
        if (glValue >= versionThreshold) {
            GlStateManager._pixelStore(32876, this.savedState.getPackImageHeight()[0]);
            GlStateManager._pixelStore(32875, this.savedState.getPackSkipImages()[0]);
            GlStateManager._pixelStore(32878, this.savedState.getUnpackImageHeight()[0]);
            GlStateManager._pixelStore(32877, this.savedState.getUnpackSkipImages()[0]);
        }

        glValue = this.savedState.isDepthMaskEnabled() ? 1 : 0;
        GlStateManager._depthMask(glValue != 0);
        return this;
    }
}
