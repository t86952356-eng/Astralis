package astralis.util.render.sky;

import org.joml.Vector4f;

@FunctionalInterface
public interface BlendColorFunction {
    Vector4f getColor(float intensity);
}
