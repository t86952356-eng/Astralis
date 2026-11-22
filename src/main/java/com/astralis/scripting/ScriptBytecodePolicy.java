package com.astralis.scripting;

import astralis.scripting.ScriptManager;

import java.nio.file.Path;

public final class ScriptBytecodePolicy {
    private ScriptBytecodePolicy() {}

    public static void validate(Path path) throws java.io.IOException {
        Path path2 = path;
        ScriptManager.validateCompiledClasses(path2);
    }
}
