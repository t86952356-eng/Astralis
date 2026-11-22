package com.astralis.scripting.api.world;

public record ScriptScoreboardEntry(String name, int score) {
    public ScriptScoreboardEntry(String name, int score) {
        if (name == null) {
            throw new IllegalArgumentException("Scoreboard entry names cannot be null.");
        }

        this.name = name;
        this.score = score;
    }
}
