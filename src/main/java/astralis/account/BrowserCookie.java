package astralis.account;

import java.util.Locale;

public record BrowserCookie(String domain, boolean includeSubdomains, String name, String value) {

    public boolean matchesHost(String host) {
        return this.domain.equalsIgnoreCase(host)
                || this.includeSubdomains
                        && host.toLowerCase(Locale.ROOT).endsWith("." + this.domain);
    }

    public String getDomain() {
        return this.domain;
    }

    public boolean isIncludeSubdomains() {
        return this.includeSubdomains;
    }

    public String getName() {
        return this.name;
    }

    public String getValue() {
        return this.value;
    }
}
