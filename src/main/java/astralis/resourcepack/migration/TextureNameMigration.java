package astralis.resourcepack.migration;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringJoiner;
import java.util.stream.Stream;

public final class TextureNameMigration {
    static Map<String, String> legacyBlockNames;
    static Map<String, String> legacyItemNames;

    public static void loadLegacyNameMaps() {

        legacyBlockNames = loadLegacyNameMap("blocks");

        legacyItemNames = loadLegacyNameMap("items");
    }

    static Map<String, String> loadLegacyNameMap(String mapName) {
        LinkedHashMap nameMap = new LinkedHashMap();

        try {

            try (InputStreamReader reader =
                    new InputStreamReader(
                            openClasspathResource("/forwards/" + mapName + ".json"),
                            StandardCharsets.UTF_8)) {
                JsonObject jsonObject = JsonParser.parseReader(reader).getAsJsonObject();

                for (Entry sectionEntry : jsonObject.entrySet()) {
                    if (((JsonElement) sectionEntry.getValue()).isJsonObject()) {
                        for (Entry nameEntry :
                                ((JsonElement) sectionEntry.getValue())
                                        .getAsJsonObject()
                                        .entrySet()) {
                            if (((JsonElement) nameEntry.getValue()).isJsonPrimitive()) {
                                nameMap.put(
                                        (String) nameEntry.getKey(),
                                        ((JsonElement) nameEntry.getValue()).getAsString());
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {

            PackJsonUtil.logger.warn(
                    "[PackMigrator88] legacy tile-name map '{}' unavailable: {}",
                    mapName,
                    e.toString());
        }

        return nameMap;
    }

    static InputStream openClasspathResource(String resourcePath) throws java.io.IOException {
        InputStream resource =
                PackMigrator.class.getResourceAsStream(
                        resourcePath == null || resourcePath.startsWith("/")
                                ? resourcePath
                                : "/astralis/obf/" + resourcePath);
        if (resource == null) {

            throw new IOException("classpath resource not found: " + resourcePath);
        } else {
            return resource;
        }
    }

    public static void renamePluralTextureDirs(Path path) throws java.io.IOException {

        Path path2 = path.resolve("assets");
        if (Files.isDirectory(path2)) {
            try (Stream<Path> namespaceStream = Files.list(path2)) {
                for (Path path3 :
                        namespaceStream.filter(path4 -> Files.isDirectory(path4)).toList()) {
                    Path path5 = path3.resolve("textures");
                    if (Files.isDirectory(path5)) {
                        Path path6 = path5.resolve("blocks");
                        if (Files.isDirectory(path6)) {

                            moveOrMergeDirectory(path6, path5.resolve("block"));
                        }

                        Path path7 = path5.resolve("items");
                        if (Files.isDirectory(path7)) {

                            moveOrMergeDirectory(path7, path5.resolve("item"));
                        }
                    }
                }
            }
        }
    }

    static void moveOrMergeDirectory(Path path, Path path2) throws java.io.IOException {
        if (Files.exists(path2)) {
            try (Stream<Path> childStream = Files.list(path)) {
                for (Path path3 : childStream.toList()) {
                    Path path4 = path2.resolve(path3.getFileName().toString());
                    Files.createDirectories(path4.getParent());
                    Files.move(path3, path4, StandardCopyOption.REPLACE_EXISTING);
                }
            }

            Files.deleteIfExists(path);
        } else {
            Files.move(path, path2);
        }
    }

    public static void rewriteTextureReferences(Path path) throws java.io.IOException {

        Path path2 = path.resolve("assets");
        if (Files.isDirectory(path2)) {
            try (Stream<Path> assetStream = Files.walk(path2)) {
                assetStream
                        .filter(path3 -> Files.isRegularFile(path3))
                        .filter(
                                path4 -> {
                                    String pathString = path4.toString();

                                    String normalizedPath = pathString.replace('\\', '/');

                                    return normalizedPath.endsWith(".json");
                                })
                        .filter(
                                path5 -> {
                                    String pathString = path5.toString();

                                    String normalizedPath = pathString.replace('\\', '/');

                                    return normalizedPath.contains("/models/");
                                })
                        .forEach(TextureNameMigration::rewriteModelTextureRefs);
            }
        }
    }

    static void rewriteModelTextureRefs(Path path) {

        try {
            JsonObject jsonObject =
                    JsonParser.parseReader(
                                    new InputStreamReader(
                                            Files.newInputStream(path), StandardCharsets.UTF_8))
                            .getAsJsonObject();
            if (rewriteTexturesInJson(jsonObject, findTexturesDirectory(path))) {
                Files.writeString(
                        path, PackJsonUtil.gson.toJson(jsonObject), StandardCharsets.UTF_8);
            }
        } catch (Exception ignored) {
        }
    }

    static Path findTexturesDirectory(Path path) {

        Path path2 = path.toAbsolutePath().normalize();
        Path path3 = path2.getParent();

        while (path3 != null) {
            Path path4 = path3.getParent();
            if (path4 != null) {

                if ("assets".equals(path4.getFileName().toString())) {

                    return path3.resolve("textures");
                }
            }

            path3 = path4;
        }

        return null;
    }

    static boolean rewriteTexturesInJson(JsonObject jsonObject, Path path) {
        boolean modified = false;

        for (Entry entry : new ArrayList<>(jsonObject.entrySet())) {
            JsonElement jsonElement = (JsonElement) entry.getValue();
            String key = (String) entry.getKey();

            if (key.equals("textures") && jsonElement.isJsonObject()) {
                JsonObject jsonObject2 = jsonElement.getAsJsonObject();

                for (Entry textureEntry : new ArrayList<>(jsonObject2.entrySet())) {
                    if (((JsonElement) textureEntry.getValue()).isJsonPrimitive()) {
                        String resolved =
                                resolveTextureReference(
                                        ((JsonElement) textureEntry.getValue()).getAsString(),
                                        path);
                        if (!resolved.equals(
                                ((JsonElement) textureEntry.getValue()).getAsString())) {
                            jsonObject2.addProperty((String) textureEntry.getKey(), resolved);
                            modified = true;
                        }
                    }
                }
            } else if (jsonElement.isJsonObject()) {
                modified |= rewriteTexturesInJson(jsonElement.getAsJsonObject(), path);
            } else if (jsonElement.isJsonArray()) {
                for (JsonElement jsonElement2 : jsonElement.getAsJsonArray()) {
                    if (jsonElement2.isJsonObject()) {
                        modified |= rewriteTexturesInJson(jsonElement2.getAsJsonObject(), path);
                    }
                }
            }
        }

        return modified;
    }

    static String resolveTextureReference(String reference, Path path) {
        if (path == null) {
            return renamePluralTexturePath(reference);
        }

        String namespace = null;
        String texturePath = reference;

        int colonIndex = reference.indexOf(58);
        if (colonIndex >= 0) {
            namespace = reference.substring(0, colonIndex);
            texturePath = reference.substring(colonIndex + 1);
        }

        while (texturePath.startsWith("/")) {
            texturePath = texturePath.substring(1);
        }

        if (texturePath.isEmpty()) {
            return reference;
        }

        boolean hasNamespace = namespace != null;
        if (namespace == null) {
            namespace = path.getParent().getFileName().toString();
        }

        Path path2 = path;
        if (!namespace.equals(path.getParent().getFileName().toString())) {
            Path path3 = path.getParent().getParent().resolve(namespace);

            path2 = path3.resolve("textures");
        }

        ArrayList<String> candidates;
        String singularPath;
        String mappedPath;
        label99:
        {
            candidates = new ArrayList();
            singularPath = singularizePathSegments(texturePath);
            mappedPath = mapLegacyTileName(singularPath);
            candidates.add(texturePath);
            candidates.add(singularPath);
            candidates.add(mappedPath);
            String firstSegment = getFirstPathSegment(texturePath);

            if (!firstSegment.equals("item")) {

                if (!firstSegment.equals("block")) {
                    break label99;
                }
            }

            String strippedPath = stripFirstPathSegment(texturePath);
            String strippedSingular = singularizePathSegments(strippedPath);
            candidates.add(strippedPath);
            candidates.add(strippedSingular);
            candidates.add(mapLegacyTileName(strippedSingular));
        }

        for (String candidate : candidates) {

            if (Files.isRegularFile(path2.resolve(candidate + ".png"))) {
                return hasNamespace ? namespace + ":" + candidate : candidate;
            }
        }

        boolean legacyMappedValue;
        String fallbackPath;
        label88:
        {
            fallbackPath = mappedPath;
            if (mappedPath.equals(singularPath)) {

                if (!texturePath.contains("/blocks/")) {

                    if (!texturePath.contains("/items/")) {
                        legacyMappedValue = false;
                        break label88;
                    }
                }
            }

            legacyMappedValue = true;
        }

        boolean legacyMapped = legacyMappedValue;
        label82:
        if (legacyMapped) {
            String mappedFirstSegment = getFirstPathSegment(fallbackPath);

            if (!mappedFirstSegment.equals("item")) {
                String recheckedSegment = getFirstPathSegment(fallbackPath);

                if (!recheckedSegment.equals("block")) {
                    break label82;
                }
            }

            fallbackPath = stripFirstPathSegment(fallbackPath);
        }

        if (fallbackPath.equals(texturePath)) {
            return reference;
        } else {
            return hasNamespace ? namespace + ":" + fallbackPath : fallbackPath;
        }
    }

    static String renamePluralTexturePath(String texturePath) {
        String result;
        label27:
        {
            result = texturePath;

            if (!result.contains("blocks/")) {

                if (!result.contains("items/")) {
                    break label27;
                }
            }

            String absoluteBlocks = result.replace("/textures/blocks/", "/textures/block/");

            String absoluteItems = absoluteBlocks.replace("/textures/items/", "/textures/item/");

            String relativeBlocks = absoluteItems.replace("textures/blocks/", "textures/block/");

            result = relativeBlocks.replace("textures/items/", "textures/item/");
        }

        if (!result.startsWith("blocks/")) {

            if (!result.startsWith("items/")) {
                return result;
            }
        }

        String blockPrefixFixed = result.replaceFirst("^blocks/", "block/");

        return blockPrefixFixed.replaceFirst("^items/", "item/");
    }

    static String singularizePathSegments(String texturePath) {
        StringJoiner joiner = new StringJoiner("/");

        for (String segment : texturePath.split("/", -1)) {

            if (segment.equals("blocks")) {

                joiner.add("block");
            } else {

                if (segment.equals("items")) {

                    joiner.add("item");
                } else {
                    joiner.add(segment);
                }
            }
        }

        return joiner.toString();
    }

    static String mapLegacyTileName(String tileName) {

        int slashIndex = tileName.lastIndexOf(47);
        String baseName = slashIndex < 0 ? tileName : tileName.substring(slashIndex + 1);
        String mappedName = legacyBlockNames.get(baseName);
        if (mappedName == null) {
            mappedName = legacyItemNames.get(baseName);
        }

        if (mappedName == null) {
            return tileName;
        } else {
            return slashIndex < 0 ? mappedName : tileName.substring(0, slashIndex + 1) + mappedName;
        }
    }

    static String getFirstPathSegment(String texturePath) {

        int slashIndex = texturePath.indexOf(47);
        return slashIndex < 0 ? texturePath : texturePath.substring(0, slashIndex);
    }

    static String stripFirstPathSegment(String texturePath) {

        int slashIndex = texturePath.indexOf(47);
        return slashIndex < 0 ? texturePath : texturePath.substring(slashIndex + 1);
    }

    static {
        legacyBlockNames = Map.of();
        legacyItemNames = Map.of();
    }
}
