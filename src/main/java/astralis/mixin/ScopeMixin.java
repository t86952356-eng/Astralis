package astralis.mixin;

import net.minecraft.util.parsing.packrat.Scope;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Scope.class)
public abstract class ScopeMixin implements astralis.integration.mixin.ScopeDepthExtension {
    @Unique private int astralis$depth;

    @Override
    public int astralis$increaseDepth() {
        return ++this.astralis$depth;
    }

    @Override
    public int astralis$decreaseDepth() {
        return this.astralis$depth--;
    }
}
