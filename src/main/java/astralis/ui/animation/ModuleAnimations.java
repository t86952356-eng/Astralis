package astralis.ui.animation;

import astralis.module.Module;
import astralis.util.Easing;

import java.util.HashMap;
import java.util.Map;

/** Caches one animation per module for enabled-state transitions. */
public final class ModuleAnimations {
    private final Map<Module, Animation> animationsByModule;

    public ModuleAnimations() {

        this.animationsByModule = new HashMap<>();
    }

    /** Returns the existing animation or creates a 250ms exponential easing animation. */
    public Animation getAnimation(Module module) {
        return this.animationsByModule.computeIfAbsent(
                module,
                ignoredModule -> {
                    return new Animation(Easing.EASE_OUT_EXPO, 250L);
                });
    }
}
