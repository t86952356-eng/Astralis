package com.astralis.scripting.api.hud;

import com.astralis.scripting.api.entity.ScriptEntity;
import com.astralis.scripting.api.render.RenderContext;

@FunctionalInterface
public interface TargetHudRenderer {
    void render(RenderContext context, ScriptEntity target, int slot);
}
