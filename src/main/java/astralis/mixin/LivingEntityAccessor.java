package astralis.mixin;

import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.Vec3;

import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@org.spongepowered.asm.mixin.Mixin(net.minecraft.world.entity.LivingEntity.class)
public interface LivingEntityAccessor {
    @Invoker("wouldNotSuffocateAtTargetPose")
    boolean callWouldNotSuffocateAtTargetPose(Pose pose);

    @Accessor("xxa")
    float getXxa();

    @Accessor("zza")
    float getZza();

    @Accessor
    int getNoJumpDelay();

    @Accessor
    void setNoJumpDelay(int noJumpDelay);

    @Invoker
    void callTravelInAir(Vec3 vector3);
}
