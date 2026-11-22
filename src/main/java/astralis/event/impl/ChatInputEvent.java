package astralis.event.impl;

import astralis.event.CancellableEvent;

/**
 * Posted for outgoing chat messages and commands (commands are prefixed with "/"); cancelling
 * prevents sending.
 */
public class ChatInputEvent extends CancellableEvent {
    private String message;

    public ChatInputEvent(String message) {
        this.message = message;
    }

    public String getMessage() {
        return this.message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
