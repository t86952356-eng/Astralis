package astralis.scripting;

import java.io.File;

public record StagedScript(ScriptCandidate candidate, File[] sourceFiles) {
    public StagedScript(ScriptCandidate candidate, File[] sourceFiles) {
        sourceFiles = (File[]) sourceFiles.clone();
        this.candidate = candidate;
        this.sourceFiles = sourceFiles;
    }

    public File[] sourceFiles() {
        return (File[]) this.sourceFiles.clone();
    }
}
