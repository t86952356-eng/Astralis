package astralis.event;

public abstract class StagedEvent extends CancellableEvent {
    public EventStage stage;

    public StagedEvent(EventStage stage) {
        this.stage = stage;
    }

    public EventStage getStage() {
        return this.stage;
    }
}
