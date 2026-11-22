package astralis.resourcepack.migration;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Stream;

public final class ModelMigration {
    private static Map<String, String> legacyBlockParentRenames =
            Map.ofEntries(
                    Map.entry("block/column_side", "block/cube_column_horizontal"),
                    Map.entry("block/column_end", "block/cube_column"),
                    Map.entry("block/andesite_smooth", "block/polished_andesite"),
                    Map.entry("block/diorite_smooth", "block/polished_diorite"),
                    Map.entry("block/granite_smooth", "block/polished_granite"),
                    Map.entry("block/anvil_undamaged", "block/anvil"),
                    Map.entry("block/anvil_slightly_damaged", "block/chipped_anvil"),
                    Map.entry("block/anvil_very_damaged", "block/damaged_anvil"),
                    Map.entry("block/brown_mushroom_block_cap_all", "block/brown_mushroom_block"),
                    Map.entry("block/red_mushroom_block_cap_all", "block/red_mushroom_block"),
                    Map.entry(
                            "block/cobblestone_wall_mossy_inventory",
                            "block/mossy_cobblestone_wall_inventory"),
                    Map.entry("block/farmland_dry", "block/farmland"),
                    Map.entry("block/farmland_wet", "block/farmland_moist"),
                    Map.entry("block/half_slab_acacia", "block/acacia_slab"),
                    Map.entry("block/half_slab_birch", "block/birch_slab"),
                    Map.entry("block/half_slab_brick", "block/brick_slab"),
                    Map.entry("block/half_slab_cobblestone", "block/cobblestone_slab"),
                    Map.entry("block/half_slab_dark_oak", "block/dark_oak_slab"),
                    Map.entry("block/half_slab_jungle", "block/jungle_slab"),
                    Map.entry("block/half_slab_nether_brick", "block/nether_brick_slab"),
                    Map.entry("block/half_slab_oak", "block/oak_slab"),
                    Map.entry("block/half_slab_quartz", "block/quartz_slab"),
                    Map.entry("block/half_slab_red_sandstone", "block/red_sandstone_slab"),
                    Map.entry("block/half_slab_sandstone", "block/sandstone_slab"),
                    Map.entry("block/half_slab_spruce", "block/spruce_slab"),
                    Map.entry("block/half_slab_stone", "block/stone_slab"),
                    Map.entry("block/half_slab_stone_brick", "block/stone_brick_slab"),
                    Map.entry("block/hardened_clay", "block/terracotta"),
                    Map.entry("block/hardened_clay_white", "block/white_terracotta"),
                    Map.entry("block/hardened_clay_orange", "block/orange_terracotta"),
                    Map.entry("block/hardened_clay_magenta", "block/magenta_terracotta"),
                    Map.entry("block/hardened_clay_light_blue", "block/light_blue_terracotta"),
                    Map.entry("block/hardened_clay_yellow", "block/yellow_terracotta"),
                    Map.entry("block/hardened_clay_lime", "block/lime_terracotta"),
                    Map.entry("block/hardened_clay_pink", "block/pink_terracotta"),
                    Map.entry("block/hardened_clay_gray", "block/gray_terracotta"),
                    Map.entry("block/hardened_clay_silver", "block/light_gray_terracotta"),
                    Map.entry("block/hardened_clay_cyan", "block/cyan_terracotta"),
                    Map.entry("block/hardened_clay_purple", "block/purple_terracotta"),
                    Map.entry("block/hardened_clay_blue", "block/blue_terracotta"),
                    Map.entry("block/hardened_clay_brown", "block/brown_terracotta"),
                    Map.entry("block/hardened_clay_green", "block/green_terracotta"),
                    Map.entry("block/hardened_clay_red", "block/red_terracotta"),
                    Map.entry("block/hardened_clay_black", "block/black_terracotta"),
                    Map.entry(
                            "block/heavy_pressure_plate_inventory",
                            "block/heavy_weighted_pressure_plate"),
                    Map.entry(
                            "block/light_pressure_plate_inventory",
                            "block/light_weighted_pressure_plate"),
                    Map.entry("block/stone_pressure_plate_inventory", "block/stone_pressure_plate"),
                    Map.entry("block/wooden_pressure_plate_inventory", "block/oak_pressure_plate"),
                    Map.entry("block/wooden_button_inventory", "block/oak_button"),
                    Map.entry("block/lit_furnace", "block/furnace_on"),
                    Map.entry("block/lit_pumpkin", "block/jack_o_lantern"),
                    Map.entry("block/lit_redstone_lamp", "block/redstone_lamp_on"),
                    Map.entry("block/mob_spawner_cage", "block/spawner"),
                    Map.entry("block/quartz_lines", "block/quartz_pillar"),
                    Map.entry("block/red_sandstone_chiseled", "block/chiseled_red_sandstone"),
                    Map.entry("block/silver_wool", "block/light_gray_wool"),
                    Map.entry("block/snow", "block/snow_block"),
                    Map.entry("block/stonebrick_chiseled", "block/chiseled_stone_bricks"),
                    Map.entry("block/stonebrick_mossy", "block/mossy_stone_bricks"),
                    Map.entry("block/stonebrick_cracked", "block/cracked_stone_bricks"),
                    Map.entry("block/wooden_trapdoor_bottom", "block/oak_trapdoor_bottom"));

    public static void applyParticleFallbacks(Path path) throws java.io.IOException {

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
                                            String pathString = path6.toString();

                                            String normalizedPath = pathString.replace('\\', '/');

                                            return normalizedPath.contains("/models/");
                                        })
                                .toList()) {
                    applyParticleFallbackToModel(path3);
                }
            }
        }
    }

    static void applyParticleFallbackToModel(Path path) throws java.io.IOException {

        JsonObject jsonObject;
        try {
            jsonObject = PackJsonUtil.readJsonFile(path);
        } catch (Exception ignored) {
            return;
        }

        if (jsonObject != null) {

            if (jsonObject.has("elements")) {

                if (jsonObject.get("elements").isJsonArray()) {

                    if (jsonObject.has("parent")) {

                        if (jsonObject.get("parent").isJsonPrimitive()) {
                            return;
                        }
                    }

                    JsonObject jsonObject2;
                    label71:
                    {
                        if (jsonObject.has("textures")) {

                            if (jsonObject.get("textures").isJsonObject()) {

                                jsonObject2 = jsonObject.getAsJsonObject("textures");
                                break label71;
                            }
                        }

                        jsonObject2 = null;
                    }

                    JsonObject jsonObject3 = jsonObject2;
                    if (jsonObject3 != null) {

                        if (!jsonObject3.has("particle")) {
                            String particleTexture = null;

                            for (Entry textureEntry : jsonObject3.entrySet()) {
                                JsonElement jsonElement = (JsonElement) textureEntry.getValue();
                                if (jsonElement.isJsonPrimitive()
                                        && !jsonElement.getAsString().startsWith("#")) {
                                    particleTexture = jsonElement.getAsString();
                                    break;
                                }
                            }

                            if (particleTexture == null) {
                                return;
                            }

                            jsonObject3.addProperty("particle", particleTexture);
                            PackJsonUtil.writeJsonFile(path, jsonObject);

                            PackJsonUtil.logger.info(
                                    "[PackMigrator88] set particle texture of {} to leftover slot '{}'",
                                    path.getFileName(),
                                    particleTexture);
                            return;
                        }
                    }

                    return;
                }
            }
        }
    }

    public static void fixUnresolvedFaceTextures(Path path) throws java.io.IOException {

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
                                            String pathString = path6.toString();

                                            String normalizedPath = pathString.replace('\\', '/');

                                            return normalizedPath.contains("/models/");
                                        })
                                .toList()) {
                    fixUnresolvedFaceTexturesInModel(path3);
                }
            }
        }
    }

    static void fixUnresolvedFaceTexturesInModel(Path path) throws java.io.IOException {

        JsonObject jsonObject;
        try {
            jsonObject = PackJsonUtil.readJsonFile(path);
        } catch (Exception ignored) {
            return;
        }

        if (jsonObject != null) {

            if (jsonObject.has("elements")) {

                if (jsonObject.get("elements").isJsonArray()) {
                    JsonObject jsonObject2;
                    label168:
                    {
                        if (jsonObject.has("textures")) {

                            if (jsonObject.get("textures").isJsonObject()) {

                                jsonObject2 = jsonObject.getAsJsonObject("textures");
                                break label168;
                            }
                        }

                        jsonObject2 = null;
                    }

                    JsonObject jsonObject3 = jsonObject2;
                    if (jsonObject3 == null) {
                        return;
                    }

                    LinkedHashSet<String> missingSlots = new LinkedHashSet();
                    String firstTexture = null;

                    for (Entry textureEntry : jsonObject3.entrySet()) {
                        if (((JsonElement) textureEntry.getValue()).isJsonPrimitive()
                                && firstTexture == null
                                && !((JsonElement) textureEntry.getValue())
                                        .getAsString()
                                        .startsWith("#")) {
                            firstTexture = ((JsonElement) textureEntry.getValue()).getAsString();
                        }
                    }

                    for (JsonElement jsonElement : jsonObject.getAsJsonArray("elements")) {
                        if (jsonElement.isJsonObject()) {
                            JsonObject jsonObject4 = jsonElement.getAsJsonObject();

                            JsonElement jsonElement2 = jsonObject4.get("faces");
                            if (jsonElement2 != null && jsonElement2.isJsonObject()) {
                                for (Entry faceEntry : jsonElement2.getAsJsonObject().entrySet()) {
                                    if (((JsonElement) faceEntry.getValue()).isJsonObject()) {
                                        JsonObject jsonObject5 =
                                                ((JsonElement) faceEntry.getValue())
                                                        .getAsJsonObject();

                                        JsonElement jsonElement3 = jsonObject5.get("texture");
                                        if (jsonElement3 != null
                                                && jsonElement3.isJsonPrimitive()) {
                                            String textureRef = jsonElement3.getAsString();
                                            if (textureRef.startsWith("#")) {
                                                String slotName = textureRef.substring(1);
                                                JsonElement jsonElement4 =
                                                        jsonObject3.get(slotName);
                                                if ((jsonElement4 == null
                                                                || !jsonElement4.isJsonPrimitive()
                                                                || jsonElement4
                                                                        .getAsString()
                                                                        .startsWith("#"))
                                                        && (jsonElement4 == null
                                                                || !jsonElement4.isJsonPrimitive()
                                                                || !jsonObject3.has(
                                                                        jsonElement4
                                                                                .getAsString()
                                                                                .substring(1)))) {
                                                    missingSlots.add(slotName);
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (missingSlots.isEmpty()) {
                        return;
                    }

                    String fallbackSlot =
                            firstTexture != null
                                    ? findTextureSlotName(jsonObject3, firstTexture)
                                    : null;

                    for (String missingSlot : missingSlots) {
                        String modelFileName = path.getFileName().toString();

                        String modelName = modelFileName.replace(".json", "");
                        Path path2 = path.getParent();

                        Path path3 = path2.resolveSibling("textures");

                        if (Files.isRegularFile(path3.resolve(modelName + ".png"))) {
                            jsonObject3.addProperty(missingSlot, modelName);
                        } else {
                            if (fallbackSlot == null) {
                                continue;
                            }

                            jsonObject3.addProperty(missingSlot, firstTexture);
                        }

                        PackJsonUtil.logger.info(
                                "[PackMigrator88] remapped dangling face texture '#{}' in {} to '{}'",
                                missingSlot,
                                path.getFileName(),
                                jsonObject3.get(missingSlot).getAsString());
                    }

                    PackJsonUtil.writeJsonFile(path, jsonObject);
                    return;
                }
            }
        }
    }

    static String findTextureSlotName(JsonObject jsonObject, String texture) {

        for (Entry textureEntry : jsonObject.entrySet()) {
            if (((JsonElement) textureEntry.getValue()).isJsonPrimitive()
                    && texture.equals(((JsonElement) textureEntry.getValue()).getAsString())) {
                return (String) textureEntry.getKey();
            }
        }

        return "particle";
    }

    public static void retargetBuiltinEntityParents(Path path) throws java.io.IOException {

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
                                            String pathString = path6.toString();

                                            String normalizedPath = pathString.replace('\\', '/');

                                            return normalizedPath.contains("/models/");
                                        })
                                .toList()) {
                    retargetBuiltinEntityParentInModel(path3);
                }
            }
        }
    }

    static void retargetBuiltinEntityParentInModel(Path path) throws java.io.IOException {

        JsonObject jsonObject;
        try {
            jsonObject = PackJsonUtil.readJsonFile(path);
        } catch (Exception ignored) {
            return;
        }

        if (jsonObject != null) {

            if (jsonObject.has("parent")) {

                if (jsonObject.get("parent").isJsonPrimitive()) {

                    String parent = jsonObject.get("parent").getAsString();

                    String parentPathValue;
                    if (parent.indexOf(58) >= 0) {

                        parentPathValue = parent.substring(parent.indexOf(58) + 1);
                    } else {
                        parentPathValue = parent;
                    }

                    String parentPath = parentPathValue;

                    if (!parentPath.equals("builtin/entity")) {
                        return;
                    }

                    boolean elementsPresent;
                    label53:
                    {
                        if (jsonObject.has("elements")) {

                            if (jsonObject.get("elements").isJsonArray()) {

                                if (!jsonObject.getAsJsonArray("elements").isEmpty()) {
                                    elementsPresent = true;
                                    break label53;
                                }
                            }
                        }

                        elementsPresent = false;
                    }

                    boolean hasElements = elementsPresent;
                    if (hasElements) {

                        jsonObject.remove("parent");

                        PackJsonUtil.logger.info(
                                "[PackMigrator88] removed builtin/entity parent from element model {} (kept custom geometry)",
                                path.getFileName());
                    } else {

                        jsonObject.addProperty("parent", "minecraft:item/generated");

                        PackJsonUtil.logger.info(
                                "[PackMigrator88] retargeted builtin/entity parent of {} to minecraft:item/generated",
                                path.getFileName());
                    }

                    PackJsonUtil.writeJsonFile(path, jsonObject);
                    return;
                }
            }
        }
    }

    public static void fixLegacyModelParents(Path path) throws java.io.IOException {

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
                                            String pathString = path6.toString();

                                            String normalizedPath = pathString.replace('\\', '/');

                                            return normalizedPath.contains("/models/");
                                        })
                                .toList()) {
                    fixLegacyModelParentInModel(path3);
                }
            }
        }
    }

    static void fixLegacyModelParentInModel(Path path) throws java.io.IOException {

        JsonObject jsonObject;
        try {
            jsonObject = PackJsonUtil.readJsonFile(path);
        } catch (Exception ignored) {
            return;
        }

        if (jsonObject != null) {

            if (jsonObject.has("parent")) {

                if (jsonObject.get("parent").isJsonPrimitive()) {

                    String parent = jsonObject.get("parent").getAsString();

                    String namespace = "minecraft";
                    String parentPath = parent;

                    int colonIndex = parent.indexOf(58);
                    if (colonIndex >= 0) {
                        namespace = parent.substring(0, colonIndex);
                        parentPath = parent.substring(colonIndex + 1);
                    }

                    if (namespace.equals("minecraft")) {

                        if (parentPath.startsWith("block/")) {
                            String renamedParent = legacyBlockParentRenames.get(parentPath);
                            if (renamedParent == null) {
                                return;
                            }

                            Path path2;
                            for (path2 = path.toAbsolutePath().normalize();
                                    path2.getParent() != null;
                                    path2 = path2.getParent()) {

                                if ("assets".equals(path2.getParent().getFileName().toString())) {
                                    break;
                                }
                            }

                            if (path2.getParent() == null) {
                                return;
                            }

                            Path path3 = path2.resolve(namespace);

                            Path path4 = path3.resolve("models");

                            if (Files.isRegularFile(path4.resolve(parentPath + ".json"))) {
                                return;
                            }

                            jsonObject.addProperty("parent", "minecraft:" + renamedParent);

                            PackJsonUtil.logger.info(
                                    "[PackMigrator88] remapped legacy model parent '{}' -> 'minecraft:{}' in {}",
                                    parent,
                                    renamedParent,
                                    path.getFileName());
                            PackJsonUtil.writeJsonFile(path, jsonObject);
                            return;
                        }
                    }

                    return;
                }
            }
        }
    }
}
