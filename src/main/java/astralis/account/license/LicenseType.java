package astralis.account.license;

import lombok.Generated;

public enum LicenseType {
    WEEKLY("Weekly"),
    MONTHLY("Monthly"),
    LIFETIME("Lifetime");

    private final String displayName;

    LicenseType(String displayName) {
        this.displayName = displayName;
    }

    public static LicenseType fromName(String name) {
        LicenseType[] types = values();
        int length = types.length;

        for (int i = 0; i < length; i++) {
            LicenseType type = types[i];
            if (type.displayName.equalsIgnoreCase(name)) {
                return type;
            }
        }

        throw new IllegalArgumentException("Unknown license type: " + name);
    }

    @Generated
    public String getDisplayName() {
        return this.displayName;
    }

    static {
    }
}
