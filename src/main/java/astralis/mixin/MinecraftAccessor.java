package astralis.mixin;

import net.minecraft.client.User;
import net.minecraft.client.multiplayer.ProfileKeyPairManager;
import net.minecraft.server.Services;

import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@org.spongepowered.asm.mixin.Mixin(net.minecraft.client.Minecraft.class)
public interface MinecraftAccessor {
    @Accessor("rightClickDelay")
    int getItemUseCooldown();

    @Accessor("rightClickDelay")
    void setItemUseCooldown(int itemUseCooldown);

    @Mutable
    @Accessor("services")
    void setServices(Services services);

    @Mutable
    @Accessor("profileKeyPairManager")
    void setProfileKeyPairManager(ProfileKeyPairManager profileKeyPairManager);

    @Invoker
    void callStartUseItem();

    @Mutable
    @Accessor
    void setUser(User user);
}
