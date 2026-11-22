package astralis.util.inventory;

import astralis.event.EventStage;
import astralis.event.EventTarget;
import astralis.event.impl.TickEvent;
import astralis.util.InventoryUtil;
import astralis.util.Listener;

public class DelayedSlotRestorer extends Listener {
    private static int pendingSlot = -1;

    public static void scheduleSlotRestore(int slot) {
        pendingSlot = slot;
    }

    @EventTarget
    public void onTick(TickEvent tickEvent) {
        if (tickEvent.getStage() == EventStage.PRE) {
            if (pendingSlot != -1) {
                InventoryUtil.setSlot(pendingSlot);
                pendingSlot = -1;
            }
        }
    }
}
