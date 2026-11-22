package com.astralis.scripting.api.entity;

public record ScriptNetworkPlayer(
        String uuid, String name, String displayName, int ping, String gameMode) {
    public ScriptNetworkPlayer(
            String uuid, String name, String displayName, int ping, String gameMode) {
        if (uuid != null && name != null && displayName != null && gameMode != null) {
            this.uuid = uuid;
            this.name = name;
            this.displayName = displayName;
            this.ping = ping;
            this.gameMode = gameMode;
        } else {
            throw new IllegalArgumentException("Network player fields cannot be null.");
        }
    }

    public String getUuid() {
        return this.uuid;
    }

    public String getName() {
        return this.name;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public int getPing() {
        return this.ping;
    }

    public String getGameMode() {
        return this.gameMode;
    }
}
