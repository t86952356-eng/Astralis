package astralis.mixin;

import astralis.Astralis;
import astralis.integration.mixin.ParticleExtension;
import astralis.module.impl.Particles;

import net.minecraft.client.particle.Particle;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Particle.class)
public abstract class ParticleMixin implements ParticleExtension {
    @Shadow protected int age;
    @Shadow protected float friction;
    @Shadow protected float gravity;
    @Shadow protected int lifetime;
    @Unique private boolean lifetimeApplied;
    @Unique private boolean captured;
    @Unique private float vanillaGravity;
    @Unique private float vanillaFriction;

    @Override
    public int astralis$getAge() {
        return this.age;
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void applyParticlePhysicsOverrides(CallbackInfo callbackInfo) {
        Particles particles = Astralis.getInstance().getModuleManager().getModule(Particles.class);
        if (particles != null && particles.isEnabled()) {
            if (!this.captured) {
                this.vanillaFriction = this.friction;
                this.vanillaGravity = this.gravity;
                this.captured = true;
            }

            this.friction = particles.applyFriction(this.vanillaFriction);
            this.gravity = particles.applyGravity(this.vanillaGravity);
            if (!this.lifetimeApplied) {
                this.lifetime = particles.applyLifetime(this.lifetime);
                this.lifetimeApplied = true;
            }
        }
    }
}
