package com.astralis.scripting.api.rotation;

import com.astralis.scripting.api.math.ScriptRotation;

@FunctionalInterface
public interface RotationStrategy {
    ScriptRotation apply(RotationContext context);
}
