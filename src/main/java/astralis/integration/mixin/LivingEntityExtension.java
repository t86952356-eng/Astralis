package astralis.integration.mixin;

public interface LivingEntityExtension {
    void astralis$setHeadPitch(float headPitch);

    void astralis$setHeadYaw(float headYaw);

    boolean astralis$isInInventory();

    float astralis$getHeadPitch();

    float astralis$getPrevHeadPitch();

    float astralis$getHeadYaw();

    float astralis$getPrevHeadYaw();
}
