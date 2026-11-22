package astralis.scripting;

import com.astralis.scripting.api.script.ScriptDependency;
import com.astralis.scripting.api.script.ScriptPackage;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.IDN;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Stream;

import javax.lang.model.SourceVersion;

public final class ScriptScanner {
    private ScriptScanner() {}

    static List<ScriptCandidate> scanScripts(Path path) throws java.io.IOException {

        List<Path> scriptFiles;
        try (Stream<Path> fileStream = Files.list(path)) {
            scriptFiles =
                    fileStream
                            .sorted(Comparator.comparing(path2 -> path2.getFileName().toString()))
                            .toList();
        }

        ArrayList<ScriptCandidate> candidates = new ArrayList<>();

        for (Path path3 : scriptFiles) {
            if (Files.isSymbolicLink(path3)) {
                String linkName = String.valueOf(path3.getFileName());

                throw new IOException("Script entries cannot be symbolic links: " + linkName);
            }

            if (Files.isRegularFile(path3, LinkOption.NOFOLLOW_LINKS)) {
                String fileName = path3.getFileName().toString();

                if (fileName.endsWith(".java")) {
                    String scriptName =
                            ScriptManager.validateScriptName(
                                    path3.getFileName()
                                            .toString()
                                            .substring(
                                                    0,
                                                    path3.getFileName().toString().length() - 5));

                    byte[] sourceBytes = readFileLimited(path3, 1048576L, "Script source");
                    List sourceFiles =
                            List.of(
                                    new ScriptSourceFile(
                                            path3.getFileName().toString(), sourceBytes));

                    candidates.add(
                            new ScriptCandidate(
                                    scriptName,
                                    scriptName,
                                    scriptName,
                                    sourceFiles,
                                    new ScriptPackage(
                                            scriptName,
                                            scriptName,
                                            "0",
                                            "legacy",
                                            Set.of(),
                                            Set.of(),
                                            List.of(),
                                            Map.of()),
                                    computePackageRevision(
                                            List.of(
                                                    Map.entry(
                                                            path3.getFileName().toString(),
                                                            sourceBytes))),
                                    true));
                    continue;
                }
            }

            if (Files.isDirectory(path3, LinkOption.NOFOLLOW_LINKS)) {
                if (!path3.toRealPath().equals(path3.toAbsolutePath().normalize())) {
                    String junctionName = String.valueOf(path3.getFileName());

                    throw new IOException(
                            "Script package directories cannot be junctions: " + junctionName);
                }

                if (Files.exists(path3.resolve("script.json"), LinkOption.NOFOLLOW_LINKS)) {
                    candidates.add(readPackage(path, path3));
                }
            }
        }

        int candidateCount = candidates.size();

        if (candidateCount > 128) {

            throw new IOException(
                    "The scripts directory cannot contain more than 128 scripts and packages.");
        }

        LinkedHashSet seenIds = new LinkedHashSet();
        LinkedHashSet seenNames = new LinkedHashSet();

        for (ScriptCandidate candidate : candidates) {
            if (!seenIds.add(
                    Normalizer.normalize(candidate.getId(), Form.NFC).toLowerCase(Locale.ROOT))) {
                String duplicateId = candidate.getId();

                throw new IOException("Duplicate script id: " + duplicateId);
            }

            if (!seenNames.add(
                    Normalizer.normalize(candidate.getName(), Form.NFC).toLowerCase(Locale.ROOT))) {
                String duplicateName = candidate.getName();

                throw new IOException("Duplicate script name: " + duplicateName);
            }
        }

        return List.copyOf(candidates);
    }

    private static byte[] readFileLimited(Path path, long maxBytes, String description)
            throws java.io.IOException {
        if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {

            throw new IOException(description + " is not a regular file.");
        }

        try (SeekableByteChannel channel =
                Files.newByteChannel(path, StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS)) {
            if (channel.size() > maxBytes) {

                throw new IOException(description + " exceeds its size limit.");
            }

            ByteBuffer buffer = ByteBuffer.allocate(Math.toIntExact(maxBytes + 1L));

            while (buffer.hasRemaining()) {
                int bytesRead = channel.read(buffer);
                if (bytesRead < 0) {
                    break;
                }

                if (bytesRead == 0) {

                    throw new IOException(description + " read did not make progress.");
                }
            }

            if (buffer.position() > maxBytes) {

                throw new IOException(description + " exceeds its size limit.");
            } else {
                byte[] contents = new byte[buffer.position()];
                buffer.flip();
                buffer.get(contents);
                return contents;
            }
        }
    }

    private static void validateEntrypoint(String entrypoint) throws java.io.IOException {

        for (String segment : entrypoint.split("\\.", -1)) {
            if (!SourceVersion.isIdentifier(segment) || SourceVersion.isKeyword(segment)) {

                throw new IOException(
                        "Manifest entrypoint must be a fully-qualified Java class name.");
            }
        }
    }

    private static void verifyContained(Path path, Path path2) throws java.io.IOException {
        if (Files.isSymbolicLink(path2)) {

            throw new IOException("Symbolic links are not allowed in script packages.");
        }

        Path path3 = path.toRealPath();
        Path path4 = path2.toRealPath();
        if (!path4.startsWith(path3)) {

            throw new IOException("A script package path escaped its root.");
        }

        if (Files.isDirectory(path2, LinkOption.NOFOLLOW_LINKS)
                && !path4.equals(path2.toAbsolutePath().normalize())) {

            throw new IOException("Junctions are not allowed in script packages.");
        }
    }

    private static String computePackageRevision(List<Entry<String, byte[]>> entries) {

        try {

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(ByteBuffer.allocate(4).putInt(entries.size()).array());

            for (Entry entry : entries.stream().sorted(Entry.comparingByKey()).toList()) {
                byte[] keyBytes = ((String) entry.getKey()).getBytes(StandardCharsets.UTF_8);
                digest.update(ByteBuffer.allocate(4).putInt(keyBytes.length).array());
                digest.update(keyBytes);
                digest.update(
                        ByteBuffer.allocate(4).putInt(((byte[]) entry.getValue()).length).array());
                digest.update((byte[]) entry.getValue());
            }

            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {

            throw new IllegalStateException("SHA-256 is unavailable.", e);
        }
    }

    private static void validatePathSegments(Path path, String label) throws java.io.IOException {
        Iterator iterator = path.iterator();

        String segmentName;
        Path path2;
        do {
            if (!iterator.hasNext()) {
                return;
            }

            path2 = (Path) iterator.next();
            if (path2.toString().isBlank()) {
                break;
            }

            int segmentLength = path2.toString().length();

            if (segmentLength > 128) {
                break;
            }

            segmentName = path2.toString();

        } while (segmentName.indexOf(34) < 0
                && !path2.toString().codePoints().anyMatch(Character::isISOControl));

        throw new IOException(label + " paths contain unsupported characters.");
    }

    private static Set<String> readStringSet(
            JsonObject jsonObject, String fieldName, int maxEntries, int maxLength)
            throws java.io.IOException {
        if (!jsonObject.has(fieldName)) {
            return Set.of();
        }

        if (!jsonObject.get(fieldName).isJsonArray()) {

            throw new IOException("Manifest field " + fieldName + " must be an array.");
        }

        JsonArray jsonArray = jsonObject.getAsJsonArray(fieldName);
        if (jsonArray.size() > maxEntries) {

            throw new IOException("Manifest field " + fieldName + " contains too many entries.");
        }

        LinkedHashSet values = new LinkedHashSet();

        for (JsonElement jsonElement : jsonArray) {
            if (jsonElement.isJsonPrimitive() && jsonElement.getAsJsonPrimitive().isString()) {
                String value = jsonElement.getAsString();
                if (!value.isBlank() && value.length() <= maxLength) {
                    if (!values.add(value)) {

                        throw new IOException(
                                "Manifest field " + fieldName + " contains duplicates.");
                    }
                    continue;
                }

                throw new IOException(
                        "Manifest field " + fieldName + " contains an invalid value.");
            }

            throw new IOException("Manifest field " + fieldName + " must contain strings.");
        }

        return Set.copyOf(values);
    }

    private static ScriptCandidate readPackage(Path path, Path path2) throws java.io.IOException {
        verifyContained(path, path2);

        Path path3 = path2.resolve("script.json");
        verifyContained(path2, path3);

        byte[] manifestBytes = readFileLimited(path3, 65536L, "Script manifest");

        JsonObject jsonObject;
        try {
            jsonObject =
                    JsonParser.parseString(new String(manifestBytes, StandardCharsets.UTF_8))
                            .getAsJsonObject();
        } catch (RuntimeException e) {
            String directoryName = String.valueOf(path2.getFileName());

            throw new IOException("Invalid script manifest in " + directoryName, e);
        }

        String scriptId = readRequiredString(jsonObject, "id", 64);

        if (scriptId.matches("[a-z][a-z0-9.-]{0,63}") && !scriptId.endsWith(".")) {

            if (!scriptId.contains("..")) {

                String scriptName = readRequiredString(jsonObject, "name", 64);
                if (Normalizer.normalize(scriptName, Form.NFC).equals(scriptName)
                        && !scriptName.codePoints().anyMatch(Character::isISOControl)) {

                    String scriptVersion = readRequiredString(jsonObject, "version", 32);

                    if (!scriptVersion.matches("[0-9A-Za-z][0-9A-Za-z.+_-]{0,31}")) {

                        throw new IOException("Manifest version contains unsupported characters.");
                    }

                    String apiVersion = readRequiredString(jsonObject, "apiVersion", 32);

                    if (!apiVersion.matches("[0-9A-Za-z][0-9A-Za-z.+_-]{0,31}")) {

                        throw new IOException(
                                "Manifest API version contains unsupported characters.");
                    }

                    String entrypoint = readRequiredString(jsonObject, "entrypoint", 200);
                    validateEntrypoint(entrypoint);

                    Set<String> permissions = readStringSet(jsonObject, "permissions", 32, 64);

                    for (String permission : permissions) {

                        if (!permission.matches("[a-z][a-z0-9._-]{0,63}")) {

                            throw new IOException("Invalid permission name: " + permission);
                        }
                    }

                    LinkedHashSet httpDomains = new LinkedHashSet();

                    for (String domain : readStringSet(jsonObject, "httpDomains", 32, 253)) {
                        httpDomains.add(normalizeHttpDomain(domain));
                    }

                    if (!httpDomains.isEmpty()) {

                        if (!permissions.contains("http")) {

                            throw new IOException(
                                    "A package requesting HTTP domains must declare the http permission.");
                        }
                    }

                    ArrayList dependencies = new ArrayList();

                    if (jsonObject.has("dependencies")) {

                        if (!jsonObject.get("dependencies").isJsonArray()) {

                            throw new IOException("Manifest dependencies must be an array.");
                        }

                        int dependencyCount = jsonObject.getAsJsonArray("dependencies").size();

                        if (dependencyCount > 32) {

                            throw new IOException(
                                    "A package cannot declare more than 32 dependencies.");
                        }

                        for (JsonElement jsonElement : jsonObject.getAsJsonArray("dependencies")) {
                            if (!jsonElement.isJsonObject()) {

                                throw new IOException("Each dependency must be an object.");
                            }

                            JsonObject jsonObject2 = jsonElement.getAsJsonObject();

                            String dependencyId = readRequiredString(jsonObject2, "id", 64);
                            JsonObject jsonObject3 = jsonElement.getAsJsonObject();

                            String dependencyVersion =
                                    readRequiredString(jsonObject3, "version", 32);

                            if (dependencyId.matches("[a-z][a-z0-9.-]{0,63}")
                                    && !dependencyId.endsWith(".")) {

                                if (!dependencyId.contains("..")) {

                                    if (!dependencyVersion.matches(
                                            "[0-9A-Za-z][0-9A-Za-z.*+_-]{0,31}")) {

                                        throw new IOException(
                                                "Dependency version contains unsupported characters.");
                                    }

                                    dependencies.add(
                                            new ScriptDependency(dependencyId, dependencyVersion));
                                    continue;
                                }
                            }

                            throw new IOException(
                                    "Dependency id must be a lowercase dotted identifier.");
                        }
                    }

                    Path path4 = path2.resolve("src");
                    if (!Files.isDirectory(path4, LinkOption.NOFOLLOW_LINKS)) {

                        throw new IOException("A script package must contain a src directory.");
                    }

                    verifyContained(path2, path4);
                    ArrayList<ScriptSourceFile> sourceFiles = new ArrayList();
                    long totalSourceBytes = 0L;

                    try (Stream<Path> sourceWalk = Files.walk(path4)) {
                        for (Path path5 : sourceWalk.sorted().toList()) {
                            verifyContained(path4, path5);
                            if (!path5.equals(path4)
                                    && !Files.isDirectory(path5, LinkOption.NOFOLLOW_LINKS)) {
                                if (Files.isRegularFile(path5, LinkOption.NOFOLLOW_LINKS)) {
                                    String sourceFileName = path5.getFileName().toString();

                                    if (sourceFileName.endsWith(".java")) {
                                        int sourceDepth = path4.relativize(path5).getNameCount();

                                        if (sourceDepth > 8) {

                                            throw new IOException(
                                                    "Package source paths cannot exceed 8 levels.");
                                        }

                                        Path path6 = path4.relativize(path5);

                                        validatePathSegments(path6, "Package source");

                                        byte[] sourceBytes =
                                                readFileLimited(path5, 1048576L, "Script source");
                                        totalSourceBytes += sourceBytes.length;

                                        if (totalSourceBytes > 4194304L) {

                                            throw new IOException(
                                                    "Package Java sources cannot exceed 4 MiB in total.");
                                        }

                                        String relativeSourcePath =
                                                path4.relativize(path5).toString();

                                        sourceFiles.add(
                                                new ScriptSourceFile(
                                                        relativeSourcePath.replace('\\', '/'),
                                                        sourceBytes));
                                        continue;
                                    }
                                }

                                throw new IOException(
                                        "Package src may contain only Java source files.");
                            }
                        }
                    }

                    if (sourceFiles.isEmpty()) {

                        throw new IOException("A script package must contain Java sources.");
                    }

                    int sourceCount = sourceFiles.size();

                    if (sourceCount > 64) {

                        throw new IOException(
                                "A script package cannot contain more than 64 sources.");
                    }

                    LinkedHashMap assets = new LinkedHashMap();

                    Path path7 = path2.resolve("assets");
                    if (Files.exists(path7, LinkOption.NOFOLLOW_LINKS)) {
                        if (!Files.isDirectory(path7, LinkOption.NOFOLLOW_LINKS)) {

                            throw new IOException("Package assets must be a directory.");
                        }

                        verifyContained(path2, path7);
                        long totalAssetBytes = 0L;

                        try (Stream<Path> assetWalk = Files.walk(path7)) {
                            for (Path path8 : assetWalk.sorted().toList()) {
                                verifyContained(path7, path8);
                                if (!path8.equals(path7)
                                        && !Files.isDirectory(path8, LinkOption.NOFOLLOW_LINKS)) {
                                    if (!Files.isRegularFile(path8, LinkOption.NOFOLLOW_LINKS)) {

                                        throw new IOException(
                                                "Package assets must be regular files.");
                                    }

                                    int assetDepth = path7.relativize(path8).getNameCount();

                                    if (assetDepth > 8) {

                                        throw new IOException(
                                                "Package asset paths cannot exceed 8 levels.");
                                    }

                                    Path path9 = path7.relativize(path8);

                                    validatePathSegments(path9, "Package asset");

                                    byte[] assetBytes =
                                            readFileLimited(path8, 2097152L, "Script asset");
                                    totalAssetBytes += assetBytes.length;

                                    if (totalAssetBytes > 16777216L) {

                                        throw new IOException(
                                                "Package assets cannot exceed 16 MiB in total.");
                                    }

                                    String relativeAssetPath = path7.relativize(path8).toString();

                                    assets.put(relativeAssetPath.replace('\\', '/'), assetBytes);
                                }
                            }
                        }

                        int assetCount = assets.size();

                        if (assetCount > 128) {

                            throw new IOException("A package cannot contain more than 128 assets.");
                        }
                    }

                    ArrayList revisionEntries = new ArrayList();

                    revisionEntries.add(Map.entry("script.json", manifestBytes));
                    sourceFiles.forEach(
                            sourceFile -> {
                                String sourcePath = sourceFile.getRelativePath();

                                revisionEntries.add(
                                        Map.entry(
                                                "src/" + sourcePath, sourceFile.getContentCopy()));
                            });
                    assets.forEach(
                            (assetPath, assetContent) -> {
                                revisionEntries.add(Map.entry("assets/" + assetPath, assetContent));
                            });
                    return new ScriptCandidate(
                            scriptId,
                            scriptName,
                            entrypoint,
                            sourceFiles,
                            new ScriptPackage(
                                    scriptId,
                                    scriptName,
                                    scriptVersion,
                                    apiVersion,
                                    permissions,
                                    httpDomains,
                                    dependencies,
                                    assets),
                            computePackageRevision(revisionEntries),
                            false);
                }

                throw new IOException("Manifest name contains unsupported characters.");
            }
        }

        throw new IOException("Manifest id must be a lowercase dotted identifier.");
    }

    static String normalizeHttpDomain(String domain) throws java.io.IOException {

        if (domain != null && !domain.isBlank()) {
            String asciiDomain;
            try {
                asciiDomain = IDN.toASCII(domain.strip().toLowerCase(Locale.ROOT), 2);
            } catch (IllegalArgumentException e) {

                throw new IOException("Invalid HTTP domain: " + domain, e);
            }

            int length = asciiDomain.length();

            if (length <= 253 && !asciiDomain.startsWith(".") && !asciiDomain.endsWith(".")) {

                if (!asciiDomain.contains("..")
                        && !asciiDomain.contains(":")
                        && !asciiDomain.contains("/")) {

                    if (!asciiDomain.equals("localhost")) {

                        if (!asciiDomain.endsWith(".localhost")) {

                            if (!asciiDomain.matches("[0-9.]+") && asciiDomain.contains(".")) {

                                if (!Set.of(
                                                ".local",
                                                ".internal",
                                                ".lan",
                                                ".home",
                                                ".onion",
                                                ".arpa")
                                        .stream()
                                        .anyMatch(asciiDomain::endsWith)) {
                                    return asciiDomain;
                                }
                            }
                        }
                    }
                }
            }

            throw new IOException("Invalid public HTTPS domain: " + domain);
        } else {

            throw new IOException("HTTP domain cannot be empty.");
        }
    }

    private static String readRequiredString(JsonObject jsonObject, String fieldName, int maxLength)
            throws java.io.IOException {
        if (jsonObject.has(fieldName)
                && jsonObject.get(fieldName).isJsonPrimitive()
                && jsonObject.get(fieldName).getAsJsonPrimitive().isString()) {
            String value = jsonObject.get(fieldName).getAsString();
            if (!value.isBlank() && value.length() <= maxLength) {
                return value;
            }

            throw new IOException("Manifest field " + fieldName + " has an invalid length.");
        } else {

            throw new IOException("Manifest field " + fieldName + " must be a string.");
        }
    }
}
