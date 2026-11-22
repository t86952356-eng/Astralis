package astralis.scripting.service;

import com.astralis.scripting.api.rotation.RotationHandle;
import com.astralis.scripting.api.rotation.RotationStrategy;

public final class RotationRegistration implements RotationHandle {
    public final String name;
    private final int priority;
    public final RotationStrategy strategy;
    public boolean active = true;

    public RotationRegistration(String name, int priority, RotationStrategy strategy) {
        this.name = name;
        this.priority = priority;
        this.strategy = strategy;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public int getPriority() {
        return this.priority;
    }

    @Override
    public boolean isActive() {
        return this.active;
    }

    @Override
    public void remove() {
        this.active = false;
    }
}
