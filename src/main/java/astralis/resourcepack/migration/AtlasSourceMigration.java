package astralis.resourcepack.migration;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

public final class AtlasSourceMigration {
    private static Set<String> vanillaTextureDirs =
            Set.of(
                    "block",
                    "item",
                    "particle",
                    "chest",
                    "painting",
                    "decorated_pot",
                    "map_decorations",
                    "banner_patterns",
                    "shield_patterns",
                    "shulker_boxes",
                    "armor_trims",
                    "gui",
                    "mob_effect",
                    "celestial",
                    "entity",
                    "environment",
                    "font",
                    "colormaps",
                    "colormap",
                    "misc",
                    "effect",
                    "hud",
                    "weapon",
                    "signs",
                    "map",
                    "models");
    private static String migrationAtlasFileName = "astralis_migration.json";

    public static void dedupeAtlasSources(Path path) throws java.io.IOException {

        Path path2 = path.resolve("assets");
        if (Files.isDirectory(path2)) {
            List<Path> atlasFiles;
            try (Stream<Path> assetStream = Files.walk(path2)) {
                atlasFiles =
                        assetStream
                                .filter(path3 -> Files.isRegularFile(path3))
                                .filter(
                                        path4 -> {
                                            String fileName = path4.getFileName().toString();

                                            return fileName.endsWith(".json");
                                        })
                                .filter(
                                        path5 -> {
                                            if (path5.getParent() != null) {

                                                if ("atlases"
                                                        .equals(
                                                                path5.getParent()
                                                                        .getFileName()
                                                                        .toString())) {
                                                    return true;
                                                }
                                            }

                                            return false;
                                        })
                                .toList();
            }

            for (Path path6 : atlasFiles) {
                dedupeAtlasFile(path6);
            }
        }
    }

    static void dedupeAtlasFile(Path path) throws java.io.IOException {

        JsonObject jsonObject;
        try {
            jsonObject =
                    JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8))
                            .getAsJsonObject();
        } catch (Exception ignored) {
            return;
        }

        if (jsonObject.has("sources")) {

            if (jsonObject.get("sources").isJsonArray()) {

                JsonArray jsonArray = jsonObject.getAsJsonArray("sources");
                JsonArray jsonArray2 = new JsonArray();
                boolean removedAny = false;

                for (JsonElement jsonElement : jsonArray) {
                    label76:
                    if (jsonElement.isJsonObject()) {
                        JsonObject jsonObject2;
                        String typeValue;
                        String sourceValue;
                        label74:
                        {
                            jsonObject2 = jsonElement.getAsJsonObject();

                            if (jsonObject2.has("type")) {

                                if (jsonObject2.get("type").isJsonPrimitive()) {

                                    typeValue = jsonObject2.get("type").getAsString();
                                    break label74;
                                }
                            }

                            typeValue = "";
                        }

                        String type = typeValue;

                        if (!type.equals("minecraft:directory")) {

                            if (!type.equals("directory")) {
                                break label76;
                            }
                        }

                        label65:
                        {
                            if (jsonObject2.has("source")) {

                                if (jsonObject2.get("source").isJsonPrimitive()) {

                                    sourceValue = jsonObject2.get("source").getAsString();
                                    break label65;
                                }
                            }

                            sourceValue = "";
                        }

                        String source = sourceValue;
                        if (vanillaTextureDirs.contains(
                                TextureNameMigration.getFirstPathSegment(source))) {
                            removedAny = true;
                            continue;
                        }
                    }

                    jsonArray2.add(jsonElement);
                }

                if (!removedAny) {
                    return;
                }

                if (jsonArray2.isEmpty()) {
                    Files.delete(path);
                    return;
                }

                jsonObject.add("sources", jsonArray2);
                Files.writeString(
                        path, PackJsonUtil.gson.toJson(jsonObject), StandardCharsets.UTF_8);
                return;
            }
        }
    }

    public static void emitAtlasSources(Path path) throws java.io.IOException {

        Path path2 = path.resolve("assets");
        if (Files.isDirectory(path2)) {
            JsonArray jsonArray = new JsonArray();

            try (Stream<Path> namespaceStream = Files.list(path2)) {
                for (Path path3 :
                        namespaceStream.filter(path4 -> Files.isDirectory(path4)).toList()) {
                    Path path5 = path3.resolve("textures");
                    if (Files.isDirectory(path5)) {
                        String namespace = path3.getFileName().toString();

                        List<Path> textureDirs;
                        try (Stream<Path> textureDirStream = Files.list(path5)) {
                            textureDirs =
                                    textureDirStream
                                            .filter(path6 -> Files.isDirectory(path6))
                                            .toList();
                        }

                        for (Path path7 : textureDirs) {
                            String dirName = path7.getFileName().toString();
                            if (!vanillaTextureDirs.contains(dirName)) {
                                boolean hasPng;
                                try (Stream<Path> fileStream = Files.walk(path7)) {
                                    hasPng =
                                            fileStream.anyMatch(
                                                    path8 -> {
                                                        String pathString = path8.toString();

                                                        return pathString.endsWith(".png");
                                                    });
                                }

                                if (hasPng) {
                                    JsonObject jsonObject = new JsonObject();

                                    jsonObject.addProperty("type", "minecraft:directory");
                                    jsonObject.addProperty("namespace", namespace);
                                    jsonObject.addProperty("prefix", dirName + "/");
                                    jsonObject.addProperty("source", dirName);
                                    jsonArray.add(jsonObject);
                                }
                            }
                        }
                    }
                }
            }

            if (!jsonArray.isEmpty()) {
                Path path9 = path2.resolve("minecraft/atlases");
                Files.createDirectories(path9);
                JsonObject jsonObject2 = new JsonObject();
                jsonObject2.add("sources", jsonArray);
                Files.writeString(
                        path9.resolve("astralis_migration.json"),
                        PackJsonUtil.gson.toJson(jsonObject2),
                        StandardCharsets.UTF_8);
            }
        }
    }
}
