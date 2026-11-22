package astralis.scripting.service;

import com.astralis.scripting.Script;
import com.astralis.scripting.api.task.ScriptTask;
import com.astralis.scripting.api.task.Tasks;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class ScriptTaskService implements Tasks {
    private final List<ScheduledTask> tasks = new ArrayList<>();
    private final Script script;
    private long currentTick;
    private boolean destroyed;

    public ScriptTaskService(Script script) {
        this.script = script;
    }

    @Override
    public synchronized void clear() {
        this.tasks.forEach(ScheduledTask::cancel);
        this.tasks.clear();
    }

    @Override
    public synchronized ScriptTask afterTicks(int delayTicks, Runnable callback) {
        return this.scheduleTask(delayTicks, 0, callback);
    }

    @Override
    public synchronized ScriptTask everyTicks(int intervalTicks, Runnable callback) {
        return this.scheduleTask(intervalTicks, intervalTicks, callback);
    }

    public synchronized void destroy() {
        this.destroyed = true;
        this.clear();
    }

    private ScriptTask scheduleTask(int delayTicks, int intervalTicks, Runnable callback) {
        if (!this.destroyed && this.script.isRuntimeActive()) {
            if (delayTicks >= 1) {

                if (delayTicks <= 72000) {
                    int taskCount = this.tasks.size();

                    if (taskCount >= 128) {

                        throw new IllegalStateException(
                                "A script cannot schedule more than 128 tasks.");
                    }

                    ScheduledTask task =
                            new ScheduledTask(
                                    Objects.requireNonNull(callback, "callback"),
                                    this.currentTick + delayTicks,
                                    intervalTicks);
                    this.tasks.add(task);
                    return task;
                }
            }

            throw new IllegalArgumentException("Task delays must be between 1 and 72,000 ticks.");
        } else {

            throw new IllegalStateException("The script task service is not active.");
        }
    }

    public synchronized void tick() {
        if (!this.destroyed) {
            this.currentTick++;

            for (ScheduledTask dueTask :
                    this.tasks.stream()
                            .filter(task -> !task.cancelled && task.dueTick <= this.currentTick)
                            .toList()) {
                if (!dueTask.cancelled) {
                    try {
                        dueTask.callback.run();
                    } catch (Throwable e) {

                        this.script.reportError("tasks", e);
                    }

                    if (dueTask.interval > 0 && !dueTask.cancelled) {
                        dueTask.dueTick = this.currentTick + dueTask.interval;
                    } else {
                        dueTask.complete = true;
                    }
                }
            }

            this.tasks.removeIf(task -> task.cancelled || task.complete);
        }
    }
}
