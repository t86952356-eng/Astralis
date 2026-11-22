package astralis.util.render;

import com.mojang.blaze3d.opengl.GlStateManager;

public class GlStateUtil {

    public static void setDepthFunc(int func) {
        GlStateManager._depthFunc(func);
    }

    public static void disableDepthTest() {
        GlStateManager._disableDepthTest();
    }

    public static void enableCull() {
        GlStateManager._enableCull();
    }

    public static void setColorMask(boolean red, boolean green, boolean blue, boolean alpha) {

        int rgbMask = (red ? 1 : 0) | (green ? 2 : 0) | (blue ? 4 : 0);
        byte alphaMask;
        if (alpha) {

            alphaMask = 8;
        } else {
            alphaMask = 0;
        }

        int mask = rgbMask | alphaMask;
        GlStateManager._colorMask(mask);
    }

    public static void disableCull() {
        GlStateManager._disableCull();
    }

    public static void setDepthMask(boolean enabled) {
        GlStateManager._depthMask(enabled);
    }

    public static void disableBlend() {
        GlStateManager._disableBlend(0);
    }

    public static void enableDepthTest() {
        GlStateManager._enableDepthTest();
    }

    public static void enableBlend() {
        GlStateManager._enableBlend(0);
    }

    public static void setBlendFuncSeparate(int srcRgb, int dstRgb, int srcAlpha, int dstAlpha) {
        GlStateManager._blendFuncSeparate(srcRgb, dstRgb, srcAlpha, dstAlpha);
    }
}
