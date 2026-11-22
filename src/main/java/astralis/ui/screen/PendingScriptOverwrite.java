package astralis.ui.screen;

public record PendingScriptOverwrite(
        String name, String source, String project, String revision, long expiresAtMillis) {
    public String getName() {
        return this.name;
    }

    public String getSource() {
        return this.source;
    }

    public String getProject() {
        return this.project;
    }

    public String getRevision() {
        return this.revision;
    }

    public long getExpiresAtMillis() {
        return this.expiresAtMillis;
    }
}
