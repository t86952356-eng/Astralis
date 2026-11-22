package com.astralis.scripting.api.task;

public interface Tasks {
    void clear();

    ScriptTask afterTicks(int delayTicks, Runnable task);

    ScriptTask everyTicks(int intervalTicks, Runnable task);
}
