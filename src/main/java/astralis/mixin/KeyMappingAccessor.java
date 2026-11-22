package astralis.mixin;

import com.mojang.blaze3d.platform.InputConstants.Key;

import org.spongepowered.asm.mixin.gen.Accessor;

@org.spongepowered.asm.mixin.Mixin(net.minecraft.client.KeyMapping.class)
public interface KeyMappingAccessor {
    @Accessor("key")
    Key getBoundKey();

    @Accessor("clickCount")
    int getTimesPressed();

    @Accessor("clickCount")
    void setTimesPressed(int timesPressed);
}
