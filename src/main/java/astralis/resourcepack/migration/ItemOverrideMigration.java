package astralis.resourcepack.migration;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Stream;

public final class ItemOverrideMigration {
    private static Set<String> rangePredicates;

    public static void migrateItemOverrides(Path path) throws java.io.IOException {
        int fileNameLength;

        Path path2 = path.resolve("assets");
        if (Files.isDirectory(path2)) {
            try (Stream<Path> namespaceStream = Files.list(path2)) {
                for (Path path3 :
                        namespaceStream.filter(path4 -> Files.isDirectory(path4)).toList()) {
                    Path path5 = path3.resolve("models/item");
                    if (Files.isDirectory(path5)) {
                        String namespace = path3.getFileName().toString();

                        List<Path> itemModelFiles;
                        try (Stream<Path> modelStream = Files.list(path5)) {
                            itemModelFiles =
                                    modelStream
                                            .filter(
                                                    path6 -> {
                                                        String fileName =
                                                                path6.getFileName().toString();

                                                        return fileName.endsWith(".json");
                                                    })
                                            .toList();
                        }

                        for (Path path7 : itemModelFiles) {
                            String modelFileName = path7.getFileName().toString();
                            fileNameLength = modelFileName.length();

                            String itemName =
                                    modelFileName.substring(0, fileNameLength - ".json".length());
                            migrateItemModel(path2, path3, namespace, itemName);
                        }
                    }
                }
            }
        }
    }

    static void migrateItemModel(Path path, Path path2, String namespace, String itemName)
            throws java.io.IOException {

        Path path3 = path2.resolve("models/item/" + itemName + ".json");

        JsonObject jsonObject;
        try {
            jsonObject =
                    JsonParser.parseReader(
                                    new InputStreamReader(
                                            Files.newInputStream(path3), StandardCharsets.UTF_8))
                            .getAsJsonObject();
        } catch (Exception ignored) {
            return;
        }

        if (jsonObject.has("overrides")) {

            JsonArray jsonArray = jsonObject.getAsJsonArray("overrides");

            jsonObject.remove("overrides");
            Files.writeString(path3, PackJsonUtil.gson.toJson(jsonObject), StandardCharsets.UTF_8);
            if (!jsonArray.isEmpty()) {
                JsonElement jsonElement = createItemModelDefinition(namespace, itemName);

                for (int index = jsonArray.size() - 1; index >= 0; index--) {
                    JsonElement jsonElement2 = jsonArray.get(index);
                    if (jsonElement2.isJsonObject()) {
                        JsonObject jsonObject2;
                        JsonObject jsonObject3;
                        label73:
                        {
                            jsonObject2 = jsonElement2.getAsJsonObject();

                            if (jsonObject2.has("predicate")) {

                                if (jsonObject2.get("predicate").isJsonObject()) {

                                    jsonObject3 = jsonObject2.getAsJsonObject("predicate");
                                    break label73;
                                }
                            }

                            jsonObject3 = null;
                        }

                        JsonObject jsonObject4 = jsonObject3;

                        JsonElement jsonElement3 = jsonObject2.get("model");
                        if (jsonObject4 != null
                                && jsonElement3 != null
                                && jsonElement3.isJsonPrimitive()
                                && jsonObject4.entrySet().size() == 1) {
                            Entry predicateEntry = jsonObject4.entrySet().iterator().next();
                            String modelRef = jsonElement3.getAsString();
                            modelFileExists(path, modelRef);
                            JsonElement jsonElement4 =
                                    buildPredicateDispatch(
                                            (String) predicateEntry.getKey(),
                                            (JsonElement) predicateEntry.getValue(),
                                            modelRef,
                                            jsonElement);
                            if (jsonElement4 != null) {
                                jsonElement = jsonElement4;
                            }
                        } else if (jsonObject4 != null) {
                            jsonObject4.entrySet();
                        }
                    }
                }

                JsonObject jsonObject5 = new JsonObject();

                jsonObject5.add("model", jsonElement);
                writeItemDefinition(path2, itemName, jsonObject5);
            } else {

                if (!namespace.equals("minecraft")) {
                    writeItemDefinition(
                            path2, itemName, createItemModelDefinition(namespace, itemName));
                }
            }
        }
    }

    static boolean modelFileExists(Path path, String modelRef) {

        String namespace = "minecraft";
        String modelPath = modelRef;

        int colonIndex = modelRef.indexOf(58);
        if (colonIndex >= 0) {
            namespace = modelRef.substring(0, colonIndex);
            modelPath = modelRef.substring(colonIndex + 1);
        }

        Path path2 = path.resolve(namespace);
        Path path3 = path2.resolve("models");

        Path path4 = path3.resolve(modelPath + ".json");
        return Files.isRegularFile(path4);
    }

    static JsonObject createItemModelDefinition(String namespace, String itemName) {
        JsonObject jsonObject = new JsonObject();

        jsonObject.addProperty("type", "minecraft:model");

        jsonObject.addProperty("model", namespace + ":item/" + itemName);
        return jsonObject;
    }

    static void writeItemDefinition(Path path, String itemName, JsonObject jsonObject)
            throws java.io.IOException {

        Path path2 = path.resolve("items");
        Files.createDirectories(path2);

        Files.writeString(
                path2.resolve(itemName + ".json"),
                PackJsonUtil.gson.toJson(jsonObject),
                StandardCharsets.UTF_8);
    }

    static JsonElement buildPredicateDispatch(
            String predicate, JsonElement jsonElement, String modelRef, JsonElement jsonElement2) {
        if (rangePredicates.contains(predicate) && jsonElement.isJsonPrimitive()) {
            JsonObject jsonObject = new JsonObject();

            jsonObject.addProperty("type", "minecraft:range_dispatch");
            JsonObject jsonObject2 = new JsonObject();

            jsonObject2.addProperty("type", "minecraft:" + predicate);

            jsonObject.add("property", jsonObject2);
            JsonArray jsonArray = new JsonArray();
            JsonObject jsonObject3 = new JsonObject();

            jsonObject3.addProperty("threshold", jsonElement.getAsFloat());

            jsonObject3.add("model", createModelNode(modelRef));
            jsonArray.add(jsonObject3);

            jsonObject.add("entries", jsonArray);

            jsonObject.add("fallback", jsonElement2);
            return jsonObject;
        } else {

            if (predicate.equals("damaged")) {
                JsonObject jsonObject4 = new JsonObject();

                jsonObject4.addProperty("type", "minecraft:condition");
                JsonObject jsonObject5 = new JsonObject();

                jsonObject5.addProperty("type", "minecraft:damaged");

                jsonObject4.add("property", jsonObject5);

                jsonObject4.add("on_true", createModelNode(modelRef));

                jsonObject4.add("on_false", jsonElement2);
                return jsonObject4;
            } else {
                return null;
            }
        }
    }

    static JsonObject createModelNode(String modelRef) {
        JsonObject jsonObject = new JsonObject();

        jsonObject.addProperty("type", "minecraft:model");

        if (modelRef.indexOf(58) < 0) {

            jsonObject.addProperty("model", "minecraft:" + modelRef);
        } else {

            jsonObject.addProperty("model", modelRef);
        }

        return jsonObject;
    }

    static {
        rangePredicates = Set.of("damage", "custom_model_data");
    }
}
