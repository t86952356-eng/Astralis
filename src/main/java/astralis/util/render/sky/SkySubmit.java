package astralis.util.render.sky;

import net.minecraft.resources.Identifier;

import org.joml.Matrix4f;
import org.joml.Vector4f;

public record SkySubmit(
        Identifier texture,
        SkyBlendMode blendMode,
        Matrix4f transform,
        Vector4f colorModulator,
        float alpha) {
    public Identifier getTexture() {
        return this.texture;
    }

    public SkyBlendMode getBlendMode() {
        return this.blendMode;
    }

    public Matrix4f getTransform() {
        return this.transform;
    }

    public Vector4f getColorModulator() {
        return this.colorModulator;
    }

    public float getAlpha() {
        return this.alpha;
    }
}
