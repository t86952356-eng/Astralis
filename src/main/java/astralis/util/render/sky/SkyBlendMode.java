package astralis.util.render.sky;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.platform.BlendFactor;

import org.joml.Vector4f;

public enum SkyBlendMode {
    ADD(
            intensity -> new Vector4f(1.0F, 1.0F, 1.0F, intensity),
            new BlendFunction(BlendFactor.SRC_ALPHA, BlendFactor.ONE)),
    SUBTRACT(
            intensity -> new Vector4f(intensity, intensity, intensity, 1.0F),
            new BlendFunction(BlendFactor.ONE_MINUS_DST_COLOR, BlendFactor.ZERO)),
    MULTIPLY(
            intensity -> new Vector4f(intensity, intensity, intensity, intensity),
            new BlendFunction(BlendFactor.DST_COLOR, BlendFactor.ONE_MINUS_SRC_ALPHA)),
    DODGE(
            intensity -> new Vector4f(intensity, intensity, intensity, 1.0F),
            new BlendFunction(BlendFactor.ONE, BlendFactor.ONE)),
    BURN(
            intensity -> new Vector4f(intensity, intensity, intensity, 1.0F),
            new BlendFunction(BlendFactor.ZERO, BlendFactor.ONE_MINUS_SRC_COLOR)),
    SCREEN(
            intensity -> new Vector4f(intensity, intensity, intensity, 1.0F),
            new BlendFunction(BlendFactor.ONE, BlendFactor.ONE_MINUS_SRC_COLOR)),
    REPLACE(intensity -> new Vector4f(1.0F, 1.0F, 1.0F, intensity), null),
    OVERLAY(
            intensity -> new Vector4f(intensity, intensity, intensity, 1.0F),
            new BlendFunction(BlendFactor.DST_COLOR, BlendFactor.SRC_COLOR)),
    ALPHA(
            intensity -> new Vector4f(1.0F, 1.0F, 1.0F, intensity),
            new BlendFunction(BlendFactor.SRC_ALPHA, BlendFactor.ONE_MINUS_SRC_ALPHA));

    private final BlendColorFunction colorFunction;
    private final BlendFunction blendFunction;

    SkyBlendMode(BlendColorFunction colorFunction, BlendFunction blendFunction) {
        this.colorFunction = colorFunction;
        this.blendFunction = blendFunction;
    }

    public Vector4f getColor(float intensity) {
        return this.colorFunction.getColor(intensity);
    }

    public BlendFunction getBlendFunction() {
        return this.blendFunction;
    }

    public static SkyBlendMode fromNameOrDefault(String name, SkyBlendMode fallback) {
        SkyBlendMode[] modes = values();
        int length = modes.length;

        for (int i = 0; i < length; i++) {
            SkyBlendMode mode = modes[i];
            if (mode.name().equalsIgnoreCase(name)) {
                return mode;
            }
        }

        return fallback;
    }
}
