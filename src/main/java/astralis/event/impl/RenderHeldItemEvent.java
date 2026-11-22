package astralis.event.impl;

import astralis.event.Event;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

public class RenderHeldItemEvent implements Event {
    private ItemStack stack;
    private final InteractionHand hand;

    public RenderHeldItemEvent(ItemStack itemStack, InteractionHand interactionHand) {
        this.stack = itemStack;
        this.hand = interactionHand;
    }

    public InteractionHand getHand() {
        return this.hand;
    }

    public ItemStack getStack() {
        return this.stack;
    }

    public void setStack(ItemStack itemStack) {
        this.stack = itemStack;
    }
}
