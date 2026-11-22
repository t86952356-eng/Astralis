package astralis.scripting.service;

import com.astralis.scripting.api.task.ScriptTask;

public final class ScheduledTask implements ScriptTask {
    public final Runnable callback;
    public final int interval;
    public long dueTick;
    public boolean cancelled;
    public boolean complete;

    public ScheduledTask(Runnable callback, long dueTick, int interval) {
        this.callback = callback;
        this.dueTick = dueTick;
        this.interval = interval;
    }

    @Override
    public void cancel() {
        this.cancelled = true;
    }

    @Override
    public boolean isCancelled() {
        return this.cancelled;
    }

    @Override
    public boolean isComplete() {
        return this.complete;
    }
}
