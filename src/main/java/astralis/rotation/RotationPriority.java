package astralis.rotation;

import lombok.Generated;

public enum RotationPriority {
    STANDARD(0),
    MEDIUM(1),
    HIGH(2);

    private final int priorityLevel;

    RotationPriority(int priorityLevel) {
        this.priorityLevel = priorityLevel;
    }

    @Generated
    /** Returns the integer rank used to resolve competing rotation submissions. */
    public int getPriorityLevel() {
        return this.priorityLevel;
    }
}
