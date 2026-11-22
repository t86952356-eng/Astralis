package com.astralis.scripting.api.script;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public final class ScriptPackage {
    private final String id;
    private final String name;
    private final String version;
    private final String apiVersion;
    private final Set<String> permissions;
    private final Set<String> requestedHttpDomains;
    private final List<ScriptDependency> dependencies;
    private final Map<String, byte[]> assets;

    public ScriptPackage(
            String id,
            String name,
            String version,
            String apiVersion,
            Set<String> permissions,
            Set<String> requestedHttpDomains,
            List<ScriptDependency> dependencies,
            Map<String, byte[]> assets) {
        this.id = id;
        this.name = name;
        this.version = version;
        this.apiVersion = apiVersion;
        this.permissions = Set.copyOf(permissions);
        this.requestedHttpDomains = Set.copyOf(requestedHttpDomains);
        this.dependencies = List.copyOf(dependencies);
        this.assets =
                assets.entrySet().stream()
                        .collect(
                                Collectors.toUnmodifiableMap(
                                        Entry::getKey,
                                        entry -> (byte[]) ((byte[]) entry.getValue()).clone()));
    }

    public String name() {
        return this.name;
    }

    public String version() {
        return this.version;
    }

    public String id() {
        return this.id;
    }

    public Set<String> permissions() {
        return this.permissions;
    }

    public Optional<byte[]> readAsset(String assetName) {
        byte[] data = this.assets.get(assetName);
        return data == null ? Optional.empty() : Optional.of((byte[]) data.clone());
    }

    public Set<String> assetNames() {
        return this.assets.keySet();
    }

    public boolean hasAsset(String assetName) {
        return this.assets.containsKey(assetName);
    }

    public Set<String> requestedHttpDomains() {
        return this.requestedHttpDomains;
    }

    public List<ScriptDependency> dependencies() {
        return this.dependencies;
    }

    public String apiVersion() {
        return this.apiVersion;
    }

    public Optional<String> readTextAsset(String assetName) {
        byte[] data = this.assets.get(assetName);
        return data == null
                ? Optional.empty()
                : Optional.of(new String(data, StandardCharsets.UTF_8));
    }
}
