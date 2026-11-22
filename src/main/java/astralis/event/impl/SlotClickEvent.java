package astralis.event.impl;

import astralis.event.CancellableEvent;

import lombok.Generated;

import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;

/**
 * Posted when a container screen is clicked with the left or right button (without shift);
 * cancelling consumes the click.
 */
public class SlotClickEvent extends CancellableEvent {
    private final Slot slot;
    private final int slotIndex;
    private final int button;
    private final ContainerInput containerInput;

    @Generated
    public Slot getSlot() {
        return this.slot;
    }

    @Generated
    public int getSlotIndex() {
        return this.slotIndex;
    }

    @Generated
    public int getButton() {
        return this.button;
    }

    @Generated
    public ContainerInput getContainerInput() {
        return this.containerInput;
    }

    @Generated
    public SlotClickEvent(Slot slot, int slotIndex, int button, ContainerInput containerInput) {
        this.slot = slot;
        this.slotIndex = slotIndex;
        this.button = button;
        this.containerInput = containerInput;
    }
}
