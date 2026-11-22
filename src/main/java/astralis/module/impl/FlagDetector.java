package astralis.module.impl;

import astralis.event.EventTarget;
import astralis.event.impl.PacketEvent;
import astralis.module.Module;
import astralis.util.ChatUtil;

import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.phys.Vec3;

import pub.astralis.client.module.core.Category;

public class FlagDetector extends Module {
    public FlagDetector() {

        super(MODULE_ID, "", "", Category.Misc);
    }

    @EventTarget
    public void onPacket(PacketEvent packetEvent) {

        if (packetEvent.getPacket()
                instanceof ClientboundPlayerPositionPacket clientboundPlayerPositionPacket) {
            PositionMoveRotation positionMoveRotation = clientboundPlayerPositionPacket.change();
            Vec3 vector3 =
                    new Vec3(
                            positionMoveRotation.position().x(),
                            positionMoveRotation.position().y(),
                            positionMoveRotation.position().z());
            if (mc.player.position().distanceTo(vector3) <= 4.0) {

                ChatUtil.print("Flag detected!");
            }
        }
    }

    private static final String MODULE_ID = "62fe5e87c76e8d2d9d0a214c50dd3dac";
}
