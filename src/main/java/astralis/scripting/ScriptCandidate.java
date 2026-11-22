package astralis.scripting;

import com.astralis.scripting.api.script.ScriptPackage;

import java.util.List;

public record ScriptCandidate(
        String id,
        String name,
        String entrypoint,
        List<ScriptSourceFile> sources,
        ScriptPackage scriptPackage,
        String revision,
        boolean singleFile) {
    public ScriptCandidate(
            String id,
            String name,
            String entrypoint,
            List<ScriptSourceFile> sources,
            ScriptPackage scriptPackage,
            String revision,
            boolean singleFile) {
        sources = List.copyOf(sources);
        this.id = id;
        this.name = name;
        this.entrypoint = entrypoint;
        this.sources = sources;
        this.scriptPackage = scriptPackage;
        this.revision = revision;
        this.singleFile = singleFile;
    }

    public List<ScriptSourceFile> getSources() {
        return this.sources;
    }

    public String getEntrypoint() {
        return this.entrypoint;
    }

    public boolean isSingleFile() {
        return this.singleFile;
    }

    public String getId() {
        return this.id;
    }

    public String getRevision() {
        return this.revision;
    }

    public ScriptPackage getScriptPackage() {
        return this.scriptPackage;
    }

    public String getName() {
        return this.name;
    }
}
