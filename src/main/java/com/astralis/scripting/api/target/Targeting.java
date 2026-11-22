package com.astralis.scripting.api.target;

import com.astralis.scripting.api.entity.ScriptEntity;
import com.astralis.scripting.api.math.ScriptVector3;

public interface Targeting {
    TargetQuery query();

    ScriptVector3 aimPoint(ScriptEntity entity, double heightFraction);
}
