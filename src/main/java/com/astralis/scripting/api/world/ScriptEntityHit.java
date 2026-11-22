package com.astralis.scripting.api.world;

import com.astralis.scripting.api.entity.ScriptEntity;
import com.astralis.scripting.api.math.ScriptVector3;

public record ScriptEntityHit(ScriptEntity entity, ScriptVector3 position, double distanceSquared) {
    public ScriptEntityHit(ScriptEntity entity, ScriptVector3 position, double distanceSquared) {
        if (entity == null || position == null) {
            throw new IllegalArgumentException(
                    "Entity raycast results require an entity and hit position.");
        }

        if (Double.isFinite(distanceSquared) && !(distanceSquared < 0.0)) {
            this.entity = entity;
            this.position = position;
            this.distanceSquared = distanceSquared;
        } else {
            throw new IllegalArgumentException(
                    "Entity raycast distance must be finite and non-negative.");
        }
    }
}
