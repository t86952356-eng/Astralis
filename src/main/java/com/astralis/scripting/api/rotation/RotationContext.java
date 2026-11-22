package com.astralis.scripting.api.rotation;

import com.astralis.scripting.api.entity.ScriptEntity;
import com.astralis.scripting.api.math.ScriptRotation;

public record RotationContext(
        ScriptRotation base,
        ScriptRotation current,
        ScriptRotation targetRotation,
        ScriptEntity target,
        String source,
        float tickDelta) {}
