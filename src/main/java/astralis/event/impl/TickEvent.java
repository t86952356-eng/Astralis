package astralis.event.impl;

import astralis.event.EventStage;
import astralis.event.StagedEvent;

public class TickEvent extends StagedEvent {

    public TickEvent(EventStage stage) {
        super(stage);
    }
}
