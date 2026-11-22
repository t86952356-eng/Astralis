package astralis.util.render;

public final class SavedGlState {
    private final int[] activeTexture = new int[1];
    private final int[] program = new int[1];
    private final int[] textureBinding = new int[1];
    private final int[] samplerBinding = new int[1];
    private final int[] arrayBuffer = new int[1];
    private final int[] vertexArray = new int[1];
    private final int[] polygonMode = new int[2];
    private final int[] viewport = new int[4];
    private final int[] scissorBox = new int[4];
    private final int[] blendSrcRgb = new int[1];
    private final int[] blendDstRgb = new int[1];
    private final int[] blendSrcAlpha = new int[1];
    private final int[] blendDstAlpha = new int[1];
    private final int[] blendEquationRgb = new int[1];
    private final int[] blendEquationAlpha = new int[1];
    private final int[] pixelUnpackBuffer = new int[1];
    private final int[] unpackAlignment = new int[1];
    private final int[] unpackRowLength = new int[1];
    private final int[] unpackSkipPixels = new int[1];
    private final int[] unpackSkipRows = new int[1];
    private final int[] packSwapBytes = new int[1];
    private final int[] packLsbFirst = new int[1];
    private final int[] packRowLength = new int[1];
    private final int[] packImageHeight = new int[1];
    private final int[] packSkipPixels = new int[1];
    private final int[] packSkipRows = new int[1];
    private final int[] packSkipImages = new int[1];
    private final int[] packAlignment = new int[1];
    private final int[] unpackSwapBytes = new int[1];
    private final int[] unpackLsbFirst = new int[1];
    private final int[] unpackImageHeight = new int[1];
    private final int[] unpackSkipImages = new int[1];
    private boolean blendEnabled = false;
    private boolean cullFaceEnabled = false;
    private boolean depthTestEnabled = false;
    private boolean stencilTestEnabled = false;
    private boolean scissorTestEnabled = false;
    private boolean primitiveRestartEnabled = false;
    private boolean depthMaskEnabled;

    public int[] getVertexArray() {
        return this.vertexArray;
    }

    public int[] getArrayBuffer() {
        return this.arrayBuffer;
    }

    public int[] getSamplerBinding() {
        return this.samplerBinding;
    }

    public int[] getTextureBinding() {
        return this.textureBinding;
    }

    public int[] getProgram() {
        return this.program;
    }

    public int[] getActiveTexture() {
        return this.activeTexture;
    }

    public int[] getPolygonMode() {
        return this.polygonMode;
    }

    public int[] getViewport() {
        return this.viewport;
    }

    public int[] getScissorBox() {
        return this.scissorBox;
    }

    public int[] getBlendSrcRgb() {
        return this.blendSrcRgb;
    }

    public int[] getBlendDstRgb() {
        return this.blendDstRgb;
    }

    public int[] getBlendSrcAlpha() {
        return this.blendSrcAlpha;
    }

    public int[] getBlendDstAlpha() {
        return this.blendDstAlpha;
    }

    public int[] getBlendEquationRgb() {
        return this.blendEquationRgb;
    }

    public int[] getBlendEquationAlpha() {
        return this.blendEquationAlpha;
    }

    public int[] getPixelUnpackBuffer() {
        return this.pixelUnpackBuffer;
    }

    public int[] getUnpackAlignment() {
        return this.unpackAlignment;
    }

    public int[] getUnpackRowLength() {
        return this.unpackRowLength;
    }

    public int[] getUnpackSkipPixels() {
        return this.unpackSkipPixels;
    }

    public int[] getUnpackSkipRows() {
        return this.unpackSkipRows;
    }

    public int[] getPackSwapBytes() {
        return this.packSwapBytes;
    }

    public int[] getPackLsbFirst() {
        return this.packLsbFirst;
    }

    public int[] getPackRowLength() {
        return this.packRowLength;
    }

    public int[] getPackImageHeight() {
        return this.packImageHeight;
    }

    public int[] getPackSkipPixels() {
        return this.packSkipPixels;
    }

    public int[] getPackSkipRows() {
        return this.packSkipRows;
    }

    public int[] getPackSkipImages() {
        return this.packSkipImages;
    }

    public int[] getPackAlignment() {
        return this.packAlignment;
    }

    public int[] getUnpackSwapBytes() {
        return this.unpackSwapBytes;
    }

    public int[] getUnpackLsbFirst() {
        return this.unpackLsbFirst;
    }

    public int[] getUnpackImageHeight() {
        return this.unpackImageHeight;
    }

    public int[] getUnpackSkipImages() {
        return this.unpackSkipImages;
    }

    public boolean isBlendEnabled() {
        return this.blendEnabled;
    }

    public void setBlendEnabled(boolean enabled) {
        this.blendEnabled = enabled;
    }

    public boolean isCullFaceEnabled() {
        return this.cullFaceEnabled;
    }

    public void setCullFaceEnabled(boolean enabled) {
        this.cullFaceEnabled = enabled;
    }

    public boolean isDepthTestEnabled() {
        return this.depthTestEnabled;
    }

    public void setDepthTestEnabled(boolean enabled) {
        this.depthTestEnabled = enabled;
    }

    public boolean isStencilTestEnabled() {
        return this.stencilTestEnabled;
    }

    public void setStencilTestEnabled(boolean enabled) {
        this.stencilTestEnabled = enabled;
    }

    public boolean isScissorTestEnabled() {
        return this.scissorTestEnabled;
    }

    public void setScissorTestEnabled(boolean enabled) {
        this.scissorTestEnabled = enabled;
    }

    public boolean isPrimitiveRestartEnabled() {
        return this.primitiveRestartEnabled;
    }

    public void setPrimitiveRestartEnabled(boolean enabled) {
        this.primitiveRestartEnabled = enabled;
    }

    public boolean isDepthMaskEnabled() {
        return this.depthMaskEnabled;
    }

    public void setDepthMaskEnabled(boolean enabled) {
        this.depthMaskEnabled = enabled;
    }
}
