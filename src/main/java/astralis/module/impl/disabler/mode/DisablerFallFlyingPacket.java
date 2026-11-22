package astralis.module.impl.disabler.mode;

import astralis.event.EventStage;
import astralis.event.EventTarget;
import astralis.event.impl.TickEvent;
import astralis.module.impl.Disabler;
import astralis.setting.Mode;
import astralis.util.PacketUtil;

import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket.Action;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class DisablerFallFlyingPacket extends Mode<Disabler> {
    public DisablerFallFlyingPacket(Disabler disabler) {
        super("Fall flying packet", disabler);
    }

    @EventTarget
    public void onTick(TickEvent tickEvent) {
        if (tickEvent.getStage() != EventStage.POST) {
            ItemStack itemStack = mc.player.getItemBySlot(EquipmentSlot.CHEST);
            if (!itemStack.isEmpty() && itemStack.getItem() == Items.ELYTRA) {
                PacketUtil.sendSilent(
                        new ServerboundPlayerCommandPacket(mc.player, Action.START_FALL_FLYING));
            }
        }
    }
}
