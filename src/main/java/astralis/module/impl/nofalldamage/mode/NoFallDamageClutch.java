package astralis.module.impl.nofalldamage.mode;

import astralis.event.EventStage;
import astralis.event.EventTarget;
import astralis.event.impl.TickEvent;
import astralis.module.impl.NoFallDamage;
import astralis.rotation.RotationManager;
import astralis.setting.Mode;
import astralis.util.InventoryUtil;
import astralis.util.player.PlayerSimulator;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;

public class NoFallDamageClutch extends Mode<NoFallDamage> {
    private PlayerSimulator simulation;

    public NoFallDamageClutch(NoFallDamage noFallDamage) {
        super("Clutch", noFallDamage);
    }

    @EventTarget
    public void onTick(TickEvent tickEvent) {

        this.simulation = new PlayerSimulator();
        if (this.simulation.getDummyPlayer() != null) {
            if (tickEvent.getStage() == EventStage.PRE && !mc.player.isInWater()) {
                for (int tick = 0; tick < 5; tick++) {
                    this.simulation.getDummyPlayer().tick();
                    if (this.simulation.getDummyPlayer().onGround()) {
                        if (mc.player.fallDistance > 3.0) {
                            int bucketSlot = -1;
                            int slot = 0;

                            while (true) {

                                if (slot >= 9) {
                                    break;
                                }

                                if (mc.player.getInventory().getItem(slot).is(Items.WATER_BUCKET)) {
                                    bucketSlot = slot;
                                    break;
                                }

                                slot++;
                            }

                            if (bucketSlot != -1) {
                                slot = mc.player.getInventory().getSelectedSlot();
                                RotationManager.getInstance()
                                        .submit(mc.player.getYRot(), 90.0F, 360.0F, 360.0F);
                                InventoryUtil.setSlot(bucketSlot);
                                mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
                                mc.player.swing(InteractionHand.MAIN_HAND);
                                InventoryUtil.setSlot(slot);
                            }
                        }
                        break;
                    }
                }
            }
        }
    }
}
