package astralis.event.impl;

import astralis.event.Event;

/**
 * Posted after {@code ClientPacketListener.handleLogin}, with the join time in epoch milliseconds.
 */
public class GameJoinEvent implements Event {
    private final long joinTimeMillis;

    public GameJoinEvent(long joinTimeMillis) {
        this.joinTimeMillis = joinTimeMillis;
    }

    public long getJoinTimeMillis() {
        return this.joinTimeMillis;
    }
}
