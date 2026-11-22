package astralis.mixin;

import astralis.Astralis;
import astralis.integration.mixin.ParticleExtension;
import astralis.module.impl.Particles;

import net.minecraft.client.particle.SingleQuadParticle;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(SingleQuadParticle.class)
public abstract class SingleQuadParticleMixin {
    @ModifyArg(
            method =
                    "extractRotatedQuad(Lnet/minecraft/client/renderer/state/level/QuadParticleRenderState;Lorg/joml/Quaternionf;FFFF)V",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/client/renderer/state/level/QuadParticleRenderState;add(Lnet/minecraft/client/particle/SingleQuadParticle$Layer;FFFFFFFFFFFFII)V"),
            index = 13)
    private int modifyParticleColor(int color) {
        Particles particles = Astralis.getInstance().getModuleManager().getModule(Particles.class);
        if (particles != null && particles.isEnabled()) {
            ParticleExtension particleExtension = (ParticleExtension) (Object) this;
            return particles.computeParticleColor(
                    color,
                    particleExtension.astralis$getAge(),
                    ((SingleQuadParticle) (Object) this).getLifetime());
        } else {
            return color;
        }
    }
}
