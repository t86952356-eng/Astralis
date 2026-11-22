package astralis.network.hypixel;

import net.hypixel.data.type.ServerType;

import org.jetbrains.annotations.Nullable;

public record HypixelLocation(
        String serverName,
        @Nullable ServerType serverType,
        @Nullable String lobbyName,
        @Nullable String mode,
        @Nullable String map) {

    public HypixelLocation(
            String serverName,
            @Nullable ServerType serverType,
            @Nullable String lobbyName,
            @Nullable String mode,
            @Nullable String map) {
        this.serverName = serverName;
        this.serverType = serverType;
        this.lobbyName = lobbyName;
        this.mode = mode;
        this.map = map;
    }

    public boolean isInLobby() {
        return this.lobbyName != null;
    }

    public String getServerName() {
        return this.serverName;
    }

    @Nullable
    public ServerType getServerType() {
        return this.serverType;
    }

    @Nullable
    public String getLobbyName() {
        return this.lobbyName;
    }

    @Nullable
    public String getMode() {
        return this.mode;
    }

    @Nullable
    public String getMap() {
        return this.map;
    }
}
