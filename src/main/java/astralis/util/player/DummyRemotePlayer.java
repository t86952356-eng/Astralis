package astralis.util.player;

import com.astralis.scripting.InstanceAccess;
import com.mojang.authlib.GameProfile;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import org.jspecify.annotations.NonNull;

import java.util.Objects;

public class DummyRemotePlayer extends RemotePlayer {

    DummyRemotePlayer(PlayerSimulator simulator, ClientLevel clientLevel, GameProfile profile) {
        Objects.requireNonNull(simulator);
        super(clientLevel, profile);
        this.xxa = ((astralis.mixin.LivingEntityAccessor) InstanceAccess.mc.player).getXxa();
        this.zza = ((astralis.mixin.LivingEntityAccessor) InstanceAccess.mc.player).getZza();
    }

    @Override
    public void aiStep() {
        this.travel(new Vec3(this.xxa, this.yya, this.zza));
    }

    @Override
    public boolean isSilent() {
        return true;
    }

    @Override
    public void playSound(SoundEvent soundEvent, float volume, float pitch) {}

    @Override
    protected void playStepSound(BlockPos blockPos, BlockState blockState) {}

    @Override
    protected void spawnSprintParticle() {}

    @Override
    public void push(@NonNull Entity entity) {}

    @Override
    public void doPush(@NonNull Entity entity) {}
}
