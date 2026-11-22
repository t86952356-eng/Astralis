package astralis.module.impl.anticheat.check;

/** Stable categories used by check buffers, flag counts, cooldown maps, and display messages. */
public enum CheckType {
    KILLAURA("KillAura"),
    VELOCITY("Velocity"),
    SPEED("Speed"),
    FLY("Fly"),
    REACH("Reach"),
    BOT("Bot"),
    AUTOBLOCK("AutoBlock"),
    SCAFFOLD("Scaffold");

    private final String displayName;

    CheckType(String displayName) {
        this.displayName = displayName;
    }

    /** Returns the category label used in notifications and chat. */
    public String getDisplayName() {
        return this.displayName;
    }
}
