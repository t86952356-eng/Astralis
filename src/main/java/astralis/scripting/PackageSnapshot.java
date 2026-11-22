package astralis.scripting;

import java.nio.file.Path;

public record PackageSnapshot(Path directory, StagedScript[] scripts) {
    public PackageSnapshot(Path directory, StagedScript[] scripts) {
        scripts = (StagedScript[]) scripts.clone();
        this.directory = directory;
        this.scripts = scripts;
    }

    public StagedScript[] scripts() {
        return (StagedScript[]) this.scripts.clone();
    }
}
