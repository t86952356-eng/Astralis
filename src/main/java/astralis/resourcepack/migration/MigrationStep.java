package astralis.resourcepack.migration;

import java.nio.file.Path;

@FunctionalInterface
public interface MigrationStep {
    void migrate(Path path) throws java.io.IOException;
}
