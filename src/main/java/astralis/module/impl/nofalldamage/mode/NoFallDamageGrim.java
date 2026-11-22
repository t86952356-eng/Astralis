package astralis.module.impl.nofalldamage.mode;

import astralis.event.EventTarget;
import astralis.event.impl.MotionEvent;
import astralis.module.impl.NoFallDamage;
import astralis.setting.Mode;
import astralis.util.PacketUtil;

import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket.Action;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

public class NoFallDamageGrim extends Mode<NoFallDamage> {
    private int airTicks;
    private boolean fallDamageExpected;

    public NoFallDamageGrim(NoFallDamage noFallDamage) {
        super("Grim", noFallDamage);
    }

    @EventTarget
    public void onMotion(MotionEvent motionEvent) {
        if (!mc.player.onGround()) {
            this.airTicks++;
            if (this.wouldTakeFallDamage(mc.player.fallDistance)) {
                this.fallDamageExpected = true;
            }
        } else if (this.fallDamageExpected && mc.player.verticalCollision && this.airTicks > 0) {
            Vec3 vector3 = mc.player.getDeltaMovement();
            if (vector3.x != 0.0 && vector3.z != 0.0) {
                PacketUtil.sendSilent(
                        new ServerboundPlayerCommandPacket(mc.player, Action.START_FALL_FLYING));
            }
        }
    }

    private boolean wouldTakeFallDamage(double fallDistance) {
        double excessDistance =
                fallDistance + 1.0E-6 - mc.player.getAttributeValue(Attributes.SAFE_FALL_DISTANCE);
        return Mth.floor(
                        excessDistance
                                * mc.player.getAttributeValue(Attributes.FALL_DAMAGE_MULTIPLIER))
                > 0;
    }

    @Override
    public void onEnable() {
        this.airTicks = 0;
        this.fallDamageExpected = false;
    }
}
