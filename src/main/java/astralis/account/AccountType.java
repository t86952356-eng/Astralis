package astralis.account;

public enum AccountType {
    MICROSOFT("Microsoft"),
    CRACKED("Cracked"),
    COOKIE("Cookie"),
    TOKEN("Token"),
    REFRESH("Refresh");

    final String displayName;

    AccountType(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return this.displayName;
    }
}
