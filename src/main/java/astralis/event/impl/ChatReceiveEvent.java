package astralis.event.impl;

import astralis.event.CancellableEvent;

import net.minecraft.network.chat.Component;

/** Posted for incoming system, player and disguised chat messages; cancelling hides the message. */
public class ChatReceiveEvent extends CancellableEvent {
    private Component message;
    private boolean overlay;

    public ChatReceiveEvent(Component component, boolean overlay) {
        super();
        this.message = component;
        this.overlay = overlay;
    }

    public void setMessage(Component component) {
        this.message = component;
    }

    public void setOverlay(boolean overlay) {
        this.overlay = overlay;
    }

    public Component getMessage() {
        return this.message;
    }

    public boolean isOverlay() {
        return this.overlay;
    }
}
