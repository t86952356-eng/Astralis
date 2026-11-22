package astralis.scripting;

import java.nio.file.Path;

public record CompilationSnapshot(Path directory, CompiledScript[] scripts) {
    public CompilationSnapshot(Path directory, CompiledScript[] scripts) {
        scripts = (CompiledScript[]) scripts.clone();
        this.directory = directory;
        this.scripts = scripts;
    }

    public CompiledScript[] scripts() {
        return (CompiledScript[]) this.scripts.clone();
    }
}
