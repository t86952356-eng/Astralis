package astralis.scripting;

public record TargetedStagingOutcome(
        PackageSnapshot incomingSources,
        CompilationSnapshot incomingCompilation,
        PackageSnapshot rollbackSources,
        CompilationSnapshot rollbackCompilation,
        TargetedStagingFailure failure) {}
