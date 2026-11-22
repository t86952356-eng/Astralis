package astralis.mixin;

import com.mojang.blaze3d.systems.GpuDeviceBackend;

import org.spongepowered.asm.mixin.gen.Accessor;

@org.spongepowered.asm.mixin.Mixin(com.mojang.blaze3d.systems.GpuDevice.class)
public interface GpuDeviceAccessor {
    @Accessor("backend")
    GpuDeviceBackend getBackend();
}
