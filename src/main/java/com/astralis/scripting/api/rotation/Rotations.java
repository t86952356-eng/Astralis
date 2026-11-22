package com.astralis.scripting.api.rotation;

import com.astralis.scripting.api.math.ScriptRotation;

public interface Rotations {
    void clear();

    RotationHandle register(String name, int priority, RotationStrategy strategy);

    RotationHandle request(String name, float yaw, float pitch, int ticks);

    ScriptRotation getSentRotation();
}
