package astralis.ui.inventory;

import astralis.event.EventTarget;
import astralis.event.impl.ContainerScreenRenderEvent;
import astralis.mixin.AbstractContainerScreenAccessor;
import astralis.util.Listener;
import astralis.util.render.ColorUtil;
import astralis.util.render.RoundedRectShape;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;

import java.awt.Color;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

public class SlotHighlightManager extends Listener {
    private final Map<Integer, SlotHighlight> activeHighlights = new HashMap<>();

    public void highlightSlot(int slotIndex, Color color) {
        this.activeHighlights.put(slotIndex, new SlotHighlight(System.currentTimeMillis(), color));
    }

    @EventTarget
    public void onContainerScreenRender(ContainerScreenRenderEvent event) {
        if (!this.activeHighlights.isEmpty()) {
            if (mc.gui.screen() instanceof AbstractContainerScreen abstractContainerScreen) {
                long currentTimeMillis = System.currentTimeMillis();
                AbstractContainerScreenAccessor screenAccessor =
                        (AbstractContainerScreenAccessor) abstractContainerScreen;
                int leftPos = screenAccessor.getLeftPos();
                int topPos = screenAccessor.getTopPos();
                Iterator iterator = this.activeHighlights.entrySet().iterator();

                while (iterator.hasNext()) {
                    Entry entry = (Entry) iterator.next();
                    long elapsedMillis =
                            currentTimeMillis
                                    - ((SlotHighlight) entry.getValue()).getStartTimeMillis();
                    if (elapsedMillis >= 400L) {
                        iterator.remove();
                    } else {
                        int highlightedSlotIndex = (Integer) entry.getKey();
                        if (highlightedSlotIndex >= 0
                                && highlightedSlotIndex < mc.player.containerMenu.slots.size()) {
                            float progress = (float) elapsedMillis / 400.0F;
                            int alpha = (int) (150.0F * (1.0F - progress) * (1.0F - progress));
                            if (alpha > 0) {
                                Slot slot = mc.player.containerMenu.slots.get(highlightedSlotIndex);
                                event.getContext()
                                        .drawRect(
                                                RoundedRectShape.obtain()
                                                        .setBounds(
                                                                leftPos + slot.x,
                                                                topPos + slot.y,
                                                                16.0F,
                                                                16.0F)
                                                        .setRadius(8.0F * (1.0F - progress))
                                                        .setColor(
                                                                ColorUtil.withAlpha(
                                                                        ((SlotHighlight)
                                                                                        entry
                                                                                                .getValue())
                                                                                .getColor(),
                                                                        alpha)));
                            }
                        }
                    }
                }
            }
        }
    }
}
