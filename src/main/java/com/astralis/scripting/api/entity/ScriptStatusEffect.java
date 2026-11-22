package com.astralis.scripting.api.entity;

public record ScriptStatusEffect(
        String id, int amplifier, int durationTicks, boolean ambient, boolean visible) {
    public ScriptStatusEffect(
            String id, int amplifier, int durationTicks, boolean ambient, boolean visible) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Status effects require an identifier.");
        }

        if (amplifier >= 0 && durationTicks >= 0) {
            this.id = id;
            this.amplifier = amplifier;
            this.durationTicks = durationTicks;
            this.ambient = ambient;
            this.visible = visible;
        } else {
            throw new IllegalArgumentException("Status-effect values cannot be negative.");
        }
    }
}
