package astralis.util;

public enum OperatingSystem {
    WIN("Windows"),
    MAC("MacOS"),
    LNX("Linux"),
    UNK("Unknown");

    private final String displayName;

    OperatingSystem(String displayName) {
        this.displayName = displayName;
    }

    String getDisplayName() {
        return this.displayName;
    }
}
