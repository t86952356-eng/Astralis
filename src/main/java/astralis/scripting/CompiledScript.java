package astralis.scripting;

import java.nio.file.Path;

public record CompiledScript(StagedScript script, Path outputDirectory, boolean cached) {}
