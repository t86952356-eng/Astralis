package astralis.resourcepack.migration;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.stream.Stream;

import javax.imageio.ImageIO;

public final class PackHousekeepingMigration {

    public static void rewritePackMcmeta(Path path) {

        Path path2 = path.resolve("pack.mcmeta");
        if (Files.isRegularFile(path2)) {
            JsonObject jsonObject;
            try {
                jsonObject =
                        JsonParser.parseString(
                                        PackJsonUtil.stripJsonComments(
                                                Files.readString(path2, StandardCharsets.UTF_8)))
                                .getAsJsonObject();
            } catch (Exception ignored) {
                PackJsonUtil.logger.warn(
                        "[PackMigrator88] pack.mcmeta unreadable, replacing with minimal valid one: {}",
                        path.getFileName());
                JsonObject jsonObject2 = new JsonObject();
                JsonObject jsonObject3 = new JsonObject();

                jsonObject3.addProperty("description", "Converted pack");

                jsonObject3.add("min_format", createFormatArray(88));

                jsonObject3.add("max_format", createFormatArray(88));
                jsonObject2.add("pack", jsonObject3);

                try {
                    PackJsonUtil.writeJsonFile(path2, jsonObject2);
                } catch (IOException e) {
                    PackJsonUtil.logger.warn(
                            "[PackMigrator88] failed to write minimal pack.mcmeta: {}",
                            e.toString());
                }

                return;
            }

            JsonObject jsonObject4;
            label88:
            {
                if (jsonObject.has("pack")) {
                    if (jsonObject.get("pack").isJsonObject()) {
                        jsonObject4 = jsonObject.getAsJsonObject("pack");
                        break label88;
                    }
                }

                jsonObject4 = null;
            }

            JsonObject jsonObject5 = jsonObject4;
            JsonElement jsonElement;
            if (jsonObject5 != null) {
                jsonElement = jsonObject5.get("description");
            } else {
                jsonElement = null;
            }

            JsonElement jsonElement2 = jsonElement;
            JsonObject jsonObject6;
            if (jsonElement2 != null && jsonElement2.isJsonObject()) {
                jsonObject6 = jsonElement2.getAsJsonObject();
            } else if (jsonElement2 != null && jsonElement2.isJsonPrimitive()) {
                JsonObject jsonObject7 = new JsonObject();
                jsonObject7.addProperty("text", jsonElement2.getAsString());
                jsonObject6 = jsonObject7;
            } else {
                jsonObject6 = null;
            }

            if (jsonObject5 != null) {
                if (jsonObject5.has("overlays")) {
                    PackJsonUtil.logger.warn(
                            "[PackMigrator88] overlays section dropped from pack.mcmeta (26.2 incompatible)");
                }
            }

            JsonObject jsonObject8 = new JsonObject();
            JsonObject jsonObject9 = new JsonObject();
            if (jsonObject6 != null) {
                jsonObject9.add("description", jsonObject6);
            } else {

                jsonObject9.addProperty("description", "Converted pack");
            }

            jsonObject9.add("min_format", createFormatArray(88));

            jsonObject9.add("max_format", createFormatArray(88));
            jsonObject8.add("pack", jsonObject9);
            if (jsonObject.has("language")) {

                jsonObject8.add("language", jsonObject.get("language"));
            }

            try {
                Files.writeString(
                        path2, PackJsonUtil.gson.toJson(jsonObject8), StandardCharsets.UTF_8);
            } catch (IOException e) {
                PackJsonUtil.logger.warn(
                        "[PackMigrator88] failed to rewrite pack.mcmeta: {}", e.toString());
            }
        }
    }

    private static JsonArray createFormatArray(int format) {
        JsonArray jsonArray = new JsonArray();
        jsonArray.add(format);
        return jsonArray;
    }

    public static void sanitizeStrictJson(Path path) throws java.io.IOException {

        Path path2 = path.resolve("assets");
        if (Files.isDirectory(path2)) {
            try (Stream<Path> assetStream = Files.walk(path2)) {
                assetStream
                        .filter(path3 -> Files.isRegularFile(path3))
                        .filter(
                                path4 -> {
                                    String fileName = path4.getFileName().toString();

                                    return fileName.endsWith(".json");
                                })
                        .filter(PackHousekeepingMigration::isStrictJsonResource)
                        .forEach(PackHousekeepingMigration::stripCommentsFromFile);
            }
        }
    }

    static boolean isStrictJsonResource(Path path) {
        String pathString = path.toString();

        String normalizedPath = pathString.replace('\\', '/');
        int assetsIndex = normalizedPath.indexOf("assets/");
        if (assetsIndex < 0) {
            return false;
        }

        String relativePath = normalizedPath.substring(assetsIndex + "assets/".length());
        if (!relativePath.contains("/blockstates/")) {
            if (!relativePath.contains("/atlases/")) {
                if (!relativePath.contains("/items/")) {
                    return false;
                }
            }
        }

        return true;
    }

    static void stripCommentsFromFile(Path path) {

        try {
            String content = Files.readString(path, StandardCharsets.UTF_8);
            String stripped = PackJsonUtil.stripJsonComments(content);
            if (!stripped.equals(content)) {
                Files.writeString(path, stripped, StandardCharsets.UTF_8);
            }
        } catch (IOException ignored) {
        }
    }

    public static void cleanupDetritus(Path path) {

        try {

            Path path2 = path.resolve("assets");
            if (!Files.isDirectory(path2)) {
                return;
            }

            try (Stream<Path> assetStream = Files.walk(path2)) {
                assetStream
                        .filter(path3 -> Files.isRegularFile(path3))
                        .forEach(PackHousekeepingMigration::cleanupMcmetaFile);
            }
        } catch (IOException ignored) {
        }
    }

    static void cleanupMcmetaFile(Path path) {
        String fileName = path.getFileName().toString();

        if (fileName.endsWith(".mcmeta")) {
            try {
                String content = Files.readString(path, StandardCharsets.UTF_8);
                String stripped = PackJsonUtil.stripJsonComments(content);
                JsonParser.parseString(stripped);
                if (!stripped.equals(content)) {
                    Files.writeString(path, stripped, StandardCharsets.UTF_8);
                }
            } catch (Exception exception) {
                try {
                    Files.delete(path);
                } catch (IOException ignored) {
                }
            }
        }
    }

    public static void stripPackFonts(Path path) throws java.io.IOException {

        Path path2 = path.resolve("assets/minecraft");

        deleteFontDirectory(path2.resolve("textures/font"));

        deleteFontDirectory(path2.resolve("font"));
    }

    static void deleteFontDirectory(Path path) throws java.io.IOException {
        if (Files.exists(path)) {
            try (Stream<Path> walkStream = Files.walk(path)) {
                for (Path path2 : walkStream.sorted(Comparator.reverseOrder()).toList()) {
                    Files.deleteIfExists(path2);
                }
            }

            PackJsonUtil.logger.info(
                    "[PackMigrator88] dropped {} so vanilla fonts stay in effect", path);
        }
    }

    public static void normalizePanorama(Path path) throws java.io.IOException {

        Path path2 = path.resolve("assets/minecraft/textures/gui/title/background");
        if (Files.isDirectory(path2)) {
            ArrayList facePaths = new ArrayList();
            int faceIndex = 0;

            while (true) {
                if (faceIndex >= 6) {
                    if (facePaths.size() < 2) {
                        return;
                    }

                    int minWidth = Integer.MAX_VALUE;

                    int minHeight = Integer.MAX_VALUE;
                    ImageIO.setUseCache(false);
                    int[][] sizes = new int[facePaths.size()][2];

                    for (int index = 0; index < facePaths.size(); index++) {
                        BufferedImage image = ImageIO.read(((Path) facePaths.get(index)).toFile());
                        if (image == null) {
                            return;
                        }

                        sizes[index][0] = image.getWidth();
                        sizes[index][1] = image.getHeight();
                        minWidth = Math.min(minWidth, sizes[index][0]);
                        minHeight = Math.min(minHeight, sizes[index][1]);
                    }

                    int targetSize = Math.min(minWidth, minHeight);
                    boolean allSquare = true;

                    for (int[] size : sizes) {
                        if (size[0] != targetSize || size[1] != targetSize) {
                            allSquare = false;
                            break;
                        }
                    }

                    if (allSquare) {
                        return;
                    }

                    for (int index = 0; index < facePaths.size(); index++) {
                        if (sizes[index][0] != targetSize || sizes[index][1] != targetSize) {
                            BufferedImage source =
                                    ImageIO.read(((Path) facePaths.get(index)).toFile());
                            int cropX = (sizes[index][0] - targetSize) / 2;
                            int cropY = (sizes[index][1] - targetSize) / 2;
                            BufferedImage cropped =
                                    source.getSubimage(cropX, cropY, targetSize, targetSize);
                            ImageIO.write(cropped, "png", ((Path) facePaths.get(index)).toFile());
                        }
                    }

                    PackJsonUtil.logger.info(
                            "[PackMigrator88] Normalized panorama faces of {} to {}x{}",
                            path.getFileName(),
                            targetSize,
                            targetSize);
                    return;
                }

                Path path3 = path2.resolve("panorama_" + faceIndex + ".png");
                if (Files.isRegularFile(path3)) {
                    facePaths.add(path3);
                }

                faceIndex++;
            }
        }
    }
}
