package astralis.scripting;

public record StagingOutcome(
        PackageSnapshot incomingSources,
        CompilationSnapshot incomingCompilation,
        PackageSnapshot rollbackSources,
        CompilationSnapshot rollbackCompilation,
        ReloadResult failure) {}
