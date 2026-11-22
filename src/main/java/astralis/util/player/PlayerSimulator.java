package astralis.util.player;

import com.astralis.scripting.InstanceAccess;
import com.mojang.authlib.GameProfile;

import lombok.Generated;

import net.minecraft.client.player.RemotePlayer;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public class PlayerSimulator implements InstanceAccess {
    private final RemotePlayer dummyPlayer;

    public PlayerSimulator() {

        if (mc.level == null) {
            this.dummyPlayer = null;
        } else {
            this.dummyPlayer =
                    new DummyRemotePlayer(
                            this, mc.level, new GameProfile(UUID.randomUUID(), "Dummy Player"));
            this.dummyPlayer.setXRot(mc.player.getXRot());
            this.dummyPlayer.setYRot(mc.player.getYRot());
            this.dummyPlayer.setPos(mc.player.position());
            this.dummyPlayer.setOnGround(mc.player.onGround());
            this.dummyPlayer.setDeltaMovement(mc.player.getDeltaMovement());
            this.dummyPlayer.fallDistance = mc.player.fallDistance;
            this.dummyPlayer.setSprinting(mc.player.isSprinting());
            this.dummyPlayer.setSpeed(mc.player.getSpeed());
            this.dummyPlayer.setShiftKeyDown(mc.player.isShiftKeyDown());
            this.dummyPlayer.xo = mc.player.xo;
            this.dummyPlayer.yo = mc.player.yo;
            this.dummyPlayer.zo = mc.player.zo;
            this.dummyPlayer.setHealth(mc.player.getHealth());
            this.dummyPlayer.setAbsorptionAmount(mc.player.getAbsorptionAmount());
            this.dummyPlayer.setYHeadRot(mc.player.getYHeadRot());
            this.dummyPlayer.yBodyRot = mc.player.yBodyRot;
            this.dummyPlayer.setPose(mc.player.getPose());
            this.dummyPlayer.setSwimming(mc.player.isSwimming());
            this.dummyPlayer.getAbilities().flying = mc.player.getAbilities().flying;
            this.dummyPlayer.getInventory().replaceWith(mc.player.getInventory());
            this.dummyPlayer
                    .getInventory()
                    .setSelectedSlot(mc.player.getInventory().getSelectedSlot());
            this.dummyPlayer.getFoodData().setFoodLevel(mc.player.getFoodData().getFoodLevel());
            this.dummyPlayer
                    .getFoodData()
                    .setSaturation(mc.player.getFoodData().getSaturationLevel());
            mc.player.getActiveEffects().forEach(this.dummyPlayer::addEffect);
        }
    }

    public Vec3 simulateTicks(int ticks) {
        int tick = 0;

        while (tick < ticks) {
            this.dummyPlayer.tick();
            tick++;
        }

        return this.dummyPlayer.position();
    }

    @Generated
    public RemotePlayer getDummyPlayer() {
        return this.dummyPlayer;
    }
}
