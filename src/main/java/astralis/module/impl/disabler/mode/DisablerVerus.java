package astralis.module.impl.disabler.mode;

import astralis.event.EventTarget;
import astralis.event.impl.PacketEvent;
import astralis.module.impl.Disabler;
import astralis.setting.Mode;
import astralis.setting.ModeSetting;
import astralis.util.InventoryUtil;
import astralis.util.PacketUtil;

import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.PosRot;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec3;

public class DisablerVerus extends Mode<Disabler> {
    private final ModeSetting mode;
    private int packetCount;

    public DisablerVerus(Disabler disabler) {

        super("Verus", disabler);

        ModeSetting modeSetting = new ModeSetting("5e7a9015ea584af4a04a409297fa5600");
        ModeSetting modeSetting2 = modeSetting.addMode("Timer");
        this.mode = modeSetting2.setDefault("Timer");
    }

    @EventTarget
    public void onPacket(PacketEvent packetEvent) {
        if (packetEvent.getPacket()
                instanceof ServerboundMovePlayerPacket serverboundMovePlayerPacket) {
            packetEvent.setCancelled(true);
            PacketUtil.sendSilent(
                    new PosRot(
                            new Vec3(
                                    serverboundMovePlayerPacket.getX(mc.player.getX()),
                                    serverboundMovePlayerPacket.getY(mc.player.getY()),
                                    serverboundMovePlayerPacket.getZ(mc.player.getZ())),
                            serverboundMovePlayerPacket.getYRot(mc.player.getYRot()),
                            serverboundMovePlayerPacket.getXRot(mc.player.getXRot()),
                            serverboundMovePlayerPacket.isOnGround(),
                            mc.player.horizontalCollision));
            if (this.packetCount % 3 == 0) {
                PacketUtil.send(
                        new ServerboundUseItemPacket(
                                InventoryUtil.isHoldingSword()
                                        ? InteractionHand.OFF_HAND
                                        : InteractionHand.MAIN_HAND,
                                0,
                                0.0F,
                                0.0F));
            }

            this.packetCount++;
        }
    }

    @Override
    public void onEnable() {
        this.packetCount = 0;
    }
}
