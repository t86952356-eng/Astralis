package astralis.resourcepack.migration;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringJoiner;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

public final class BlockstateMigration {
    private static Set<String> legacyBlockstateProperties;
    private static Map<String, String> blockModelRenames;

    public static void fixShortGrass(Path path) throws java.io.IOException {

        Path path2 = path.resolve("assets/minecraft");

        Path path3 = path2.resolve("blockstates");

        Path path4 = path3.resolve("grass.json");

        Path path5 = path2.resolve("models/block/short_grass.json");
        if (Files.isRegularFile(path4) || Files.isRegularFile(path5)) {
            Path path6 = path2.resolve("textures/block/grass.png");
            Path path7 = path2.resolve("textures/block/short_grass.png");
            if (Files.isRegularFile(path6) && !Files.exists(path7)) {
                Files.move(path6, path7);
            }

            if (Files.isRegularFile(path5)) {
                try {
                    JsonObject jsonObject = PackJsonUtil.readJsonFile(path5);
                    if (jsonObject != null) {
                        JsonObject jsonObject2;
                        label63:
                        {
                            if (jsonObject.has("textures")) {
                                if (jsonObject.get("textures").isJsonObject()) {
                                    jsonObject2 = jsonObject.getAsJsonObject("textures");
                                    break label63;
                                }
                            }

                            jsonObject2 = null;
                        }

                        JsonObject jsonObject3 = jsonObject2;
                        if (jsonObject3 != null) {
                            if (!jsonObject3.has("cross")) {
                                if (modelUsesTextureVariable(jsonObject, "cross")) {

                                    jsonObject3.addProperty("cross", "minecraft:block/short_grass");
                                    PackJsonUtil.writeJsonFile(path5, jsonObject);
                                }
                            }
                        }
                    }
                } catch (Exception ignored) {
                }
            }

            if (Files.isRegularFile(path4)) {
                try {
                    JsonObject jsonObject4 = PackJsonUtil.readJsonFile(path4);
                    if (jsonObject4 != null) {

                        retargetBlockModel(jsonObject4, "grass", "short_grass");
                        PackJsonUtil.writeJsonFile(path3.resolve("short_grass.json"), jsonObject4);
                        Files.delete(path4);
                    }
                } catch (Exception ignored) {
                }
            }
        }
    }

    static boolean modelUsesTextureVariable(JsonObject jsonObject, String variableName) {

        JsonElement jsonElement = jsonObject.get("elements");
        if (jsonElement != null && jsonElement.isJsonArray()) {
            String variableRef = "#" + variableName;

            for (JsonElement jsonElement2 : jsonElement.getAsJsonArray()) {
                if (jsonElement2.isJsonObject()) {
                    JsonObject jsonObject2 = jsonElement2.getAsJsonObject();
                    JsonElement jsonElement3 = jsonObject2.get("faces");
                    if (jsonElement3 != null && jsonElement3.isJsonObject()) {
                        for (Entry faceEntry : jsonElement3.getAsJsonObject().entrySet()) {
                            if (((JsonElement) faceEntry.getValue()).isJsonObject()) {
                                JsonObject jsonObject3 =
                                        ((JsonElement) faceEntry.getValue()).getAsJsonObject();
                                JsonElement jsonElement4 = jsonObject3.get("texture");
                                if (jsonElement4 != null
                                        && jsonElement4.isJsonPrimitive()
                                        && variableRef.equals(jsonElement4.getAsString())) {
                                    return true;
                                }
                            }
                        }
                    }
                }
            }

            return false;
        } else {
            return false;
        }
    }

    static void retargetBlockModel(JsonObject jsonObject, String oldName, String newName) {

        String newModel = "minecraft:block/" + newName;
        retargetModelReferences(jsonObject, oldName, newModel);
    }

    static void retargetModelReferences(JsonElement jsonElement, String oldName, String newModel) {
        if (jsonElement != null) {
            if (jsonElement.isJsonObject()) {
                JsonObject jsonObject = jsonElement.getAsJsonObject();

                JsonElement jsonElement2 = jsonObject.get("model");
                if (jsonElement2 != null && jsonElement2.isJsonPrimitive()) {
                    String modelRef = jsonElement2.getAsString();

                    String modelPathValue;
                    if (modelRef.indexOf(58) >= 0) {

                        modelPathValue = modelRef.substring(modelRef.indexOf(58) + 1);
                    } else {
                        modelPathValue = modelRef;
                    }

                    String modelPath = modelPathValue;

                    if (modelPath.equals("block/" + oldName) || modelPath.equals(oldName)) {

                        jsonObject.addProperty("model", newModel);
                    }
                }

                for (Entry childEntry : jsonObject.entrySet()) {
                    retargetModelReferences((JsonElement) childEntry.getValue(), oldName, newModel);
                }
            } else if (jsonElement.isJsonArray()) {
                for (JsonElement jsonElement3 : jsonElement.getAsJsonArray()) {
                    retargetModelReferences(jsonElement3, oldName, newModel);
                }
            }
        }
    }

    public static void fixBlockstateVariants(Path path) throws java.io.IOException {

        Path path2 = path.resolve("assets");
        if (Files.isDirectory(path2)) {
            try (Stream<Path> assetStream = Files.walk(path2)) {
                for (Path path3 :
                        assetStream
                                .filter(path4 -> Files.isRegularFile(path4))
                                .filter(
                                        path5 -> {
                                            String fileName = path5.getFileName().toString();

                                            return fileName.endsWith(".json");
                                        })
                                .filter(
                                        path6 -> {
                                            if (path6.getParent() != null) {

                                                if ("blockstates"
                                                        .equals(
                                                                path6.getParent()
                                                                        .getFileName()
                                                                        .toString())) {
                                                    return true;
                                                }
                                            }

                                            return false;
                                        })
                                .toList()) {
                    fixBlockstateFileVariants(path3);
                }
            }
        }
    }

    static void fixBlockstateFileVariants(Path path) throws java.io.IOException {

        JsonObject jsonObject;
        try {
            jsonObject = PackJsonUtil.readJsonFile(path);
        } catch (Exception ignored) {
            return;
        }

        if (jsonObject != null) {

            if (jsonObject.has("variants")) {

                if (jsonObject.get("variants").isJsonObject()) {

                    JsonObject jsonObject2 = jsonObject.getAsJsonObject("variants");
                    boolean modified = false;

                    if (jsonObject2.has("axis=none")) {

                        jsonObject2.remove("axis=none");
                        modified = true;
                    }

                    if (jsonObject2.has("normal")) {

                        JsonElement jsonElement = jsonObject2.get("normal");

                        jsonObject2.remove("normal");
                        if (jsonObject2.isEmpty()) {
                            jsonObject2.add("", jsonElement);
                        }

                        modified = true;
                    }

                    JsonObject jsonObject3 = new JsonObject();
                    boolean variantsChanged = false;

                    for (Entry entry : jsonObject2.entrySet()) {
                        String key = (String) entry.getKey();
                        String strippedKey = stripLegacyProperties(key);
                        if (!strippedKey.equals(key)) {
                            variantsChanged = true;

                            PackJsonUtil.logger.info(
                                    "[PackMigrator88] stripped legacy blockstate property from variant '{}' in {}",
                                    key,
                                    path.getFileName());
                        }

                        if (jsonObject3.has(strippedKey)) {
                            variantsChanged = true;

                            PackJsonUtil.logger.info(
                                    "[PackMigrator88] dropped duplicate variant '{}' after legacy property strip in {}",
                                    strippedKey,
                                    path.getFileName());
                        } else {
                            jsonObject3.add(strippedKey, (JsonElement) entry.getValue());
                        }
                    }

                    if (variantsChanged) {
                        jsonObject2 = jsonObject3;

                        jsonObject.add("variants", jsonObject3);
                        modified = true;
                    }

                    for (Entry variantEntry : jsonObject2.entrySet()) {
                        if (((JsonElement) variantEntry.getValue()).isJsonObject()) {
                            JsonObject jsonObject4 =
                                    ((JsonElement) variantEntry.getValue()).getAsJsonObject();
                            String[] booleanKeys = new String[1];

                            booleanKeys[0] = "uvlock";

                            for (String booleanKey : booleanKeys) {
                                JsonElement jsonElement2 = jsonObject4.get(booleanKey);
                                if (jsonElement2 != null && jsonElement2.isJsonPrimitive()) {
                                    String stringValue = jsonElement2.getAsString();

                                    if (stringValue.equalsIgnoreCase("true")) {
                                        jsonObject4.addProperty(booleanKey, true);
                                        modified = true;
                                        continue;
                                    }
                                }

                                if (jsonElement2 != null && jsonElement2.isJsonPrimitive()) {
                                    String stringValue = jsonElement2.getAsString();

                                    if (stringValue.equalsIgnoreCase("false")) {
                                        jsonObject4.addProperty(booleanKey, false);
                                        modified = true;
                                    }
                                }
                            }

                            for (String rotationKey : new String[] {"x", "y"}) {
                                JsonElement jsonElement3 = jsonObject4.get(rotationKey);
                                if (jsonElement3 != null
                                        && jsonElement3.isJsonPrimitive()
                                        && jsonElement3.getAsJsonPrimitive().isString()) {
                                    try {
                                        jsonObject4.addProperty(
                                                rotationKey,
                                                Integer.parseInt(jsonElement3.getAsString()));
                                        modified = true;
                                    } catch (NumberFormatException ignored) {
                                    }
                                }
                            }
                        }
                    }

                    if (modified) {
                        PackJsonUtil.writeJsonFile(path, jsonObject);
                    }

                    return;
                }
            }
        }
    }

    static String stripLegacyProperties(String variantKey) {
        if (!variantKey.isEmpty() && variantKey.contains("=")) {
            StringJoiner joiner = new StringJoiner(",");
            boolean stripped = false;

            for (String property : variantKey.split(",")) {

                int equalsIndex = property.indexOf(61);
                if (equalsIndex > 0
                        && legacyBlockstateProperties.contains(
                                property.substring(0, equalsIndex))) {
                    stripped = true;
                } else {
                    joiner.add(property);
                }
            }

            return stripped ? joiner.toString() : variantKey;
        } else {
            return variantKey;
        }
    }

    public static void fixBlockstateVariantModels(Path path) throws java.io.IOException {

        Path path2 = path.resolve("assets");
        if (Files.isDirectory(path2)) {
            try (Stream<Path> namespaceStream = Files.list(path2)) {
                for (Path path3 :
                        namespaceStream.filter(path4 -> Files.isDirectory(path4)).toList()) {
                    Path path5 = path3.resolve("blockstates");
                    if (Files.isDirectory(path5)) {
                        String namespace = path3.getFileName().toString();
                        Path path6 = path3.resolve("textures/block");
                        Path path7 = path3.resolve("models");

                        List<Path> blockstateFiles;
                        try (Stream<Path> blockstateStream = Files.list(path5)) {
                            blockstateFiles =
                                    blockstateStream
                                            .filter(
                                                    path8 -> {
                                                        String fileName =
                                                                path8.getFileName().toString();

                                                        return fileName.endsWith(".json");
                                                    })
                                            .toList();
                        }

                        for (Path path9 : blockstateFiles) {
                            repairBlockstateModelRefs(path9, namespace, path6, path7);
                        }
                    }
                }
            }
        }
    }

    static void repairBlockstateModelRefs(Path path, String namespace, Path path2, Path path3)
            throws java.io.IOException {

        JsonObject jsonObject;
        try {
            jsonObject = PackJsonUtil.readJsonFile(path);
        } catch (Exception ignored) {
            return;
        }

        if (jsonObject != null) {
            AtomicBoolean changed = new AtomicBoolean(false);

            if (jsonObject.has("variants")) {

                if (jsonObject.get("variants").isJsonObject()) {

                    JsonObject jsonObject2 = jsonObject.getAsJsonObject("variants");
                    JsonObject jsonObject3 = new JsonObject();
                    boolean droppedVariant = false;

                    for (Entry variantEntry : jsonObject2.entrySet()) {
                        JsonElement jsonElement = (JsonElement) variantEntry.getValue();
                        JsonElement jsonElement2 =
                                repairModelEntry(
                                        jsonElement, namespace, path2, path3, path, changed);
                        if (jsonElement2 == null) {
                            droppedVariant = true;

                            PackJsonUtil.logger.info(
                                    "[PackMigrator88] dropped blockstate variant '{}' (unresolvable model ref) in {}",
                                    variantEntry.getKey(),
                                    path.getFileName());
                        } else {
                            jsonObject3.add((String) variantEntry.getKey(), jsonElement2);
                        }
                    }

                    if (droppedVariant) {
                        changed.set(true);
                        if (jsonObject3.isEmpty()) {

                            if (namespace.equals("minecraft")) {
                                Files.deleteIfExists(path);

                                PackJsonUtil.logger.info(
                                        "[PackMigrator88] deleted blockstate {} left without variants — 26.2's own definition takes over",
                                        path.getFileName());
                                return;
                            }
                        }

                        jsonObject.add("variants", jsonObject3);
                    } else if (changed.get()) {

                        jsonObject.add("variants", jsonObject3);
                    }
                }
            }

            if (jsonObject.has("multipart")) {

                if (jsonObject.get("multipart").isJsonArray()) {

                    JsonArray jsonArray = jsonObject.getAsJsonArray("multipart");
                    JsonArray jsonArray2 = new JsonArray();
                    boolean droppedPart = false;

                    for (JsonElement jsonElement3 : jsonArray) {
                        JsonElement jsonElement4 =
                                repairModelEntry(
                                        jsonElement3, namespace, path2, path3, path, changed);
                        if (jsonElement4 == null) {
                            droppedPart = true;
                        } else {
                            jsonArray2.add(jsonElement4);
                        }
                    }

                    if (droppedPart) {

                        jsonObject.add("multipart", jsonArray2);
                        changed.set(true);
                    }
                }
            }

            if (changed.get()) {
                PackJsonUtil.writeJsonFile(path, jsonObject);
            }
        }
    }

    static JsonElement repairModelEntry(
            JsonElement jsonElement,
            String namespace,
            Path path,
            Path path2,
            Path path3,
            AtomicBoolean changed)
            throws java.io.IOException {
        if (jsonElement == null) {
            return null;
        }

        if (jsonElement.isJsonObject()) {
            JsonObject jsonObject = jsonElement.getAsJsonObject();

            if (jsonObject.has("apply")) {

                if (jsonObject.has("when")) {

                    JsonElement jsonElement2 =
                            repairModelEntry(
                                    jsonObject.get("apply"),
                                    namespace,
                                    path,
                                    path2,
                                    path3,
                                    changed);
                    if (jsonElement2 == null) {
                        return null;
                    }

                    jsonObject.add("apply", jsonElement2);
                    changed.set(true);
                    return jsonObject;
                }
            }

            JsonElement jsonElement3 = jsonObject.get("model");
            if (jsonElement3 != null && jsonElement3.isJsonPrimitive()) {
                String resolvedModel =
                        resolveModelReference(
                                jsonElement3.getAsString(), namespace, path, path2, path3);
                if (resolvedModel == null) {
                    return null;
                }

                if (!resolvedModel.equals(jsonElement3.getAsString())) {

                    jsonObject.addProperty("model", resolvedModel);
                    changed.set(true);

                    PackJsonUtil.logger.info(
                            "[PackMigrator88] repointed blockstate model ref '{}' -> '{}' in {}",
                            jsonElement3.getAsString(),
                            resolvedModel,
                            path3.getFileName());
                }

                return jsonObject;
            } else {
                return jsonObject;
            }
        } else if (jsonElement.isJsonArray()) {
            JsonArray jsonArray = new JsonArray();

            for (JsonElement jsonElement4 : jsonElement.getAsJsonArray()) {
                JsonElement jsonElement5 =
                        repairModelEntry(jsonElement4, namespace, path, path2, path3, changed);
                if (jsonElement5 != null) {
                    jsonArray.add(jsonElement5);
                }
            }

            if (jsonArray.isEmpty()) {
                return null;
            }

            if (jsonArray.size() != jsonElement.getAsJsonArray().size()) {
                changed.set(true);
            }

            return jsonArray;
        } else {
            return jsonElement;
        }
    }

    static String resolveModelReference(
            String modelRef, String defaultNamespace, Path path, Path path2, Path path3)
            throws java.io.IOException {
        String namespace = defaultNamespace;
        String modelPath = modelRef;

        int colonIndex = modelRef.indexOf(58);
        if (colonIndex >= 0) {
            namespace = modelRef.substring(0, colonIndex);
            modelPath = modelRef.substring(colonIndex + 1);
        }

        while (modelPath.startsWith("/")) {
            modelPath = modelPath.substring(1);
        }

        if (!modelPath.startsWith("block/")) {

            modelPath = "block/" + modelPath;
        }

        if (Files.isRegularFile(path2.resolve(modelPath + ".json"))) {
            return modelRef;
        }

        if (namespace.equals("minecraft") && vanillaModelExists(modelPath)) {
            return modelRef;
        }

        String resolved = null;

        if (namespace.equals("minecraft")) {
            resolved = blockModelRenames.get(modelPath);
            if (resolved == null && path != null && Files.isDirectory(path)) {

                String blockName = modelPath.substring(modelPath.lastIndexOf(47) + 1);
                String texture = findBlockTexture(blockName, path);
                if (texture != null) {

                    generateMissingModel(
                            path2.resolve(modelPath + ".json"), modelPath, texture, path);
                    resolved = modelPath;
                }
            }
        }

        if (resolved == null) {

            PackJsonUtil.logger.info(
                    "[PackMigrator88] blockstate model ref '{}' in {} unresolvable — entry dropped",
                    modelRef,
                    path3.getFileName());
            return null;
        } else {

            return "minecraft:" + resolved;
        }
    }

    static boolean vanillaModelExists(String modelPath) {

        try {

            try (InputStream resource =
                    PackMigrator.class.getResourceAsStream(
                            "/assets/minecraft/models/" + modelPath + ".json")) {
                return resource != null;
            }
        } catch (IOException ignored) {
            return false;
        }
    }

    static String findBlockTexture(String blockName, Path path) {

        if (Files.isRegularFile(path.resolve(blockName + ".png"))) {
            return "minecraft:block/" + blockName;
        }

        int digitCount = 0;

        for (int index = blockName.length() - 1;
                index >= 0 && Character.isDigit(blockName.charAt(index));
                index--) {
            digitCount++;
        }

        if (digitCount > 0) {
            String baseName = blockName.substring(0, blockName.length() - digitCount);
            String digits = blockName.substring(blockName.length() - digitCount);

            for (Entry legacyEntry : TextureNameMigration.legacyBlockNames.entrySet()) {
                if (((String) legacyEntry.getValue()).equals(baseName)) {
                    String legacyName = (String) legacyEntry.getKey();

                    if (Files.isRegularFile(path.resolve(legacyName + digits + ".png"))) {
                        String keyValue = (String) legacyEntry.getKey();
                        String digitSuffix = digits;
                        String blockPrefix = keyValue;

                        return "minecraft:block/" + blockPrefix + digitSuffix;
                    }
                }
            }
        }

        return null;
    }

    static void generateMissingModel(Path path, String modelPath, String texture, Path path2)
            throws java.io.IOException {
        JsonObject jsonObject;
        JsonObject jsonObject2;
        label40:
        {
            jsonObject = new JsonObject();
            jsonObject2 = new JsonObject();

            String blockName = modelPath.substring(modelPath.lastIndexOf(47) + 1);
            if (!blockName.startsWith("waterlily")) {
                if (!blockName.startsWith("lily")) {
                    if (!blockName.startsWith("tallgrass")) {
                        if (!blockName.startsWith("fern")) {
                            if (!blockName.startsWith("sapling")) {
                                if (!blockName.startsWith("double_plant")) {
                                    String endTexture = findColumnEndTexture(blockName, path2);
                                    if (endTexture != null) {

                                        jsonObject.addProperty(
                                                "parent", "minecraft:block/cube_column");
                                        jsonObject2.addProperty("end", endTexture);
                                        jsonObject2.addProperty("side", texture);
                                    } else {

                                        jsonObject.addProperty(
                                                "parent", "minecraft:block/cube_all");
                                        jsonObject2.addProperty("all", texture);
                                    }
                                    break label40;
                                }
                            }
                        }
                    }

                    jsonObject.addProperty("parent", "minecraft:block/cross");
                    jsonObject2.addProperty("cross", texture);
                    break label40;
                }
            }

            jsonObject.addProperty("parent", "minecraft:block/lily_pad");
            jsonObject2.addProperty("texture", texture);
            jsonObject2.addProperty("particle", texture);
        }

        jsonObject.add("textures", jsonObject2);
        Files.createDirectories(path.getParent());
        PackJsonUtil.writeJsonFile(path, jsonObject);

        PackJsonUtil.logger.info(
                "[PackMigrator88] generated missing variant model {} (texture {})",
                modelPath,
                texture);
    }

    static String findColumnEndTexture(String blockName, Path path) {
        int digitCount = 0;

        for (int index = blockName.length() - 1;
                index >= 0 && Character.isDigit(blockName.charAt(index));
                index--) {
            digitCount++;
        }

        String baseName =
                digitCount > 0
                        ? blockName.substring(0, blockName.length() - digitCount)
                        : blockName;

        String topName = baseName + "_top";

        if (Files.isRegularFile(path.resolve(topName + ".png"))) {

            return "minecraft:block/" + topName;
        }

        String legacyTopName = TextureNameMigration.mapLegacyTileName(topName);
        if (!legacyTopName.equals(topName)) {

            if (Files.isRegularFile(path.resolve(legacyTopName + ".png"))) {

                return "minecraft:block/" + legacyTopName;
            }
        }

        return null;
    }

    static {
        legacyBlockstateProperties = Set.of("suspended");
        blockModelRenames = Map.of("block/birch_log_side", "block/birch_log_horizontal");
    }
}
