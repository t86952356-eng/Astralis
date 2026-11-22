package astralis.event.impl;

import astralis.event.Event;

public class ItemUseAnimationEvent implements Event {
    private boolean using;

    public boolean isUsing() {
        return this.using;
    }

    public void setUsing(boolean using) {
        this.using = using;
    }
}
