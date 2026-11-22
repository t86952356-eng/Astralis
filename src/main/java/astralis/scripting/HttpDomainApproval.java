package astralis.scripting;

import java.util.Set;

public record HttpDomainApproval(String revision, Set<String> domains) {
    public HttpDomainApproval(String revision, Set<String> domains) {
        domains = Set.copyOf(domains);
        this.revision = revision;
        this.domains = domains;
    }
}
