package astralis.resourcepack;

import astralis.mixin.PackResourcesAccessor;
import astralis.resourcepack.migration.PackMigrator;

import com.agentdid127.resourcepack.forwards.ForwardsPackConverter;
import com.agentdid127.resourcepack.library.utilities.Util;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;

import net.minecraft.server.packs.FilePackResources.FileResourcesSupplier;
import net.minecraft.server.packs.PathPackResources.PathResourcesSupplier;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.Pack.ResourcesSupplier;
import net.minecraft.server.packs.repository.PackRepository;

import org.slf4j.Logger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class PackAutoConverter {
    public static int currentPackFormat;
    private static int converterTargetProtocol;
    private static int maxLegacyPackFormat;
    public static int cacheVersion;
    private static ConcurrentHashMap<Path, ReentrantLock> conversionLocks;
    private static ScheduledExecutorService prewarmExecutor;
    private static Gson gson;
    private static Logger logger;
    private static Path settingsPath;
    private static volatile boolean converterEnabled;

    static {
        maxLegacyPackFormat = 64;
        converterTargetProtocol = 772;
        cacheVersion = 10;
        currentPackFormat = 88;
        conversionLocks = new ConcurrentHashMap<>();
        prewarmExecutor =
                Executors.newSingleThreadScheduledExecutor(
                        runnable -> {
                            Thread thread = new Thread(runnable, "astralis-pack-prewarm");
                            thread.setDaemon(true);
                            return thread;
                        });
        gson = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
        logger = LogUtils.getLogger();
        settingsPath = Path.of("resources", "astralis-converted", "settings.json");
        converterEnabled = loadConverterEnabled();
    }

    private PackAutoConverter() {}

    private static boolean hasPackMcmeta(Path path) {
        if (Files.isDirectory(path)) {

            return Files.isRegularFile(path.resolve("pack.mcmeta"));
        }

        String fileName = path.getFileName().toString();

        if (fileName.endsWith(".zip")) {
            try (ZipFile zipFile = new ZipFile(path.toFile())) {

                return zipFile.getEntry("pack.mcmeta") != null;
            } catch (IOException ignored) {
                return false;
            }
        } else {
            return false;
        }
    }

    private static String computePackHash(Path packPath) throws java.io.IOException {

        MessageDigest digest;
        try {

            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {

            throw new IllegalStateException("SHA-256 unavailable", e);
        }

        if (Files.isDirectory(packPath)) {
            try (Stream<Path> pathStream = Files.walk(packPath).sorted()) {
                pathStream.forEach(
                        file -> {
                            if (Files.isRegularFile(file)) {
                                try {
                                    digest.update(
                                            packPath.relativize(file)
                                                    .toString()
                                                    .getBytes(StandardCharsets.UTF_8));
                                    digest.update(new byte[] {0});
                                    digest.update(Files.readAllBytes(file));
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        });
            }
        } else {
            try (InputStream inputStream = Files.newInputStream(packPath)) {

                byte[] buffer = new byte[8192];

                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    digest.update(buffer, 0, bytesRead);
                }
            }
        }

        String fullHash = HexFormat.of().formatHex(digest.digest());

        return fullHash.substring(0, 16);
    }

    private static JsonObject readPackSection(Path path) {

        try {
            InputStream inputStream;
            if (Files.isDirectory(path)) {

                Path path2 = path.resolve("pack.mcmeta");
                if (!Files.exists(path2)) {
                    return null;
                }

                inputStream = Files.newInputStream(path2);
            } else {
                String fileName = path.getFileName().toString();

                if (!fileName.endsWith(".zip")) {
                    return null;
                }

                ZipFile zipFile = new ZipFile(path.toFile());

                ZipEntry entry = zipFile.getEntry("pack.mcmeta");
                if (entry == null) {
                    zipFile.close();
                    return null;
                }

                inputStream = zipFile.getInputStream(entry);
            }

            JsonObject jsonObject = parsePackSection(inputStream);
            inputStream.close();
            return jsonObject;
        } catch (IOException ignored) {
            return null;
        }
    }

    public static void prewarmRepository(PackRepository packRepository) {
        if (packRepository != null) {
            try {
                for (Pack pack : packRepository.getAvailablePacks()) {
                    Path path = getSupplierPath(((PackResourcesAccessor) pack).getResources());
                    if (path != null) {
                        String pathString = path.toString();

                        if (pathString.contains("resourcepacks") && isOutdatedPack(path)) {
                            prewarmExecutor.execute(
                                    () -> {
                                        try {
                                            convertIfOutdated(path);
                                        } catch (Exception e) {

                                            logger.warn(
                                                    "[PackAutoConverter] prewarm failed for {}",
                                                    path,
                                                    e);
                                        }
                                    });
                        }
                    }
                }
            } catch (Exception e) {

                logger.warn("[PackAutoConverter] prewarm scheduling failed", e);
            }
        }
    }

    private static void zipDirectory(Path path, Path path2) throws java.io.IOException {
        PackMigrator.zipTree(path, path2);
    }

    private static JsonObject parsePackSection(InputStream inputStream) {

        try {
            JsonObject jsonObject =
                    JsonParser.parseReader(new InputStreamReader(inputStream)).getAsJsonObject();

            return jsonObject.getAsJsonObject("pack");
        } catch (Exception ignored) {
            return null;
        }
    }

    public static boolean isConverterEnabled() {
        return converterEnabled;
    }

    private static Integer asIntegerOrNull(JsonElement jsonElement) {
        if (jsonElement != null && jsonElement.isJsonPrimitive()) {
            try {
                return jsonElement.getAsInt();
            } catch (NumberFormatException
                    | UnsupportedOperationException
                    | IllegalStateException ignored) {
                return null;
            }
        } else {
            return null;
        }
    }

    private static boolean isOutdatedFormat(JsonObject jsonObject) {

        Integer packFormat = asIntegerOrNull(jsonObject.get("pack_format"));

        Integer minFormat = asIntegerOrNull(firstArrayElement(jsonObject.get("min_format")));

        boolean hasMaxFormat =
                asIntegerOrNull(firstArrayElement(jsonObject.get("max_format"))) != null;
        if (packFormat == null && minFormat == null) {
            return true;
        }

        if (packFormat != null) {
            int packFormatValue = packFormat;

            if (packFormatValue > 64 && (minFormat == null || !hasMaxFormat)) {
                return true;
            }
        }

        int effectiveFormat = packFormat != null ? packFormat : minFormat;

        return effectiveFormat < 88;
    }

    private static void pruneOldRevisions(Path path) {
        Path path2 = path.getParent();
        if (path2 != null) {
            String currentName = path.getFileName().toString();

            try (Stream<Path> children = Files.list(path2)) {
                children.filter(
                                path3 -> {
                                    boolean staleDirectory;
                                    boolean staleFileFlag;
                                    label41:
                                    {
                                        String name = path3.getFileName().toString();
                                        staleDirectory =
                                                name.startsWith("v")
                                                        && !name.equals(currentName)
                                                        && Files.isDirectory(path3);
                                        if (!name.equals(currentName)) {

                                            if (name.contains("__") && Files.isRegularFile(path3)) {
                                                staleFileFlag = true;
                                                break label41;
                                            }
                                        }

                                        staleFileFlag = false;
                                    }

                                    boolean staleFile = staleFileFlag;
                                    return staleDirectory || staleFile;
                                })
                        .forEach(
                                path4 -> {
                                    try {
                                        deleteRecursive(path4);
                                    } catch (Exception ignored) {
                                    }
                                });
            } catch (Exception ignored) {
            }
        }
    }

    private static void extractZip(Path path, Path path2) throws java.io.IOException {

        try (ZipFile zipFile = new ZipFile(path.toFile())) {
            Enumeration entries = zipFile.entries();

            while (entries.hasMoreElements()) {
                ZipEntry entry = (ZipEntry) entries.nextElement();
                if (!entry.isDirectory()) {
                    Path path3 = path2.resolve(entry.getName()).normalize();
                    if (path3.startsWith(path2)) {
                        Files.createDirectories(path3.getParent());

                        try (InputStream inputStream = zipFile.getInputStream(entry)) {
                            Files.copy(inputStream, path3);
                        }
                    }
                }
            }
        }
    }

    public static void setConverterEnabled(boolean enabled) {
        converterEnabled = enabled;

        try {
            Files.createDirectories(settingsPath.getParent());
            JsonObject jsonObject = new JsonObject();

            jsonObject.addProperty("converterEnabled", enabled);
            Files.writeString(settingsPath, gson.toJson(jsonObject), StandardCharsets.UTF_8);
        } catch (IOException e) {

            logger.warn("[PackAutoConverter] could not persist converter setting", e);
        }
    }

    private static void restoreGuiSheets(Path path, Map<Path, byte[]> sheets) {
        Iterator iterator = sheets.entrySet().iterator();

        while (iterator.hasNext()) {
            Entry entry = (Entry) iterator.next();
            Path path2 = path.resolve((Path) entry.getKey());
            if (!Files.isRegularFile(path2)) {
                try {
                    Files.createDirectories(path2.getParent());
                    Files.write(path2, (byte[]) entry.getValue());
                } catch (IOException e) {

                    logger.warn("[PackAutoConverter] could not restore {}", path2, e);
                }
            }
        }
    }

    private static Path convertLocked(Path path) {

        try {
            JsonObject jsonObject = readPackSection(path);
            if (jsonObject != null && !isOutdatedFormat(jsonObject)) {
                return path;
            }

            if (jsonObject == null && !hasPackMcmeta(path)) {
                return path;
            }

            Integer rawPackFormat;
            if (jsonObject == null) {
                rawPackFormat = null;
            } else {

                rawPackFormat = asIntegerOrNull(jsonObject.get("pack_format"));
            }

            Integer packFormat = rawPackFormat;
            String sourceVersion = packFormat == null ? null : packFormatToVersion(packFormat);
            int sourceProtocol =
                    sourceVersion != null ? Util.getVersionProtocol(gson, sourceVersion) : -1;
            String zipCheckName = path.getFileName().toString();

            boolean isZip = zipCheckName.endsWith(".zip");
            String fileName = path.getFileName().toString();
            String baseName = isZip ? fileName.substring(0, fileName.length() - 4) : fileName;

            Path path2 = Files.createTempDirectory("astralis_rpc_");

            try {
                Path path3 = path2.resolve(fileName);
                if (isZip) {
                    Files.copy(path, path3);
                } else {
                    copyDirectory(path, path3);
                }

                Map guiSheets = snapshotGuiSheets(path3);

                String[] cacheSegments = new String[2];

                cacheSegments[0] = "astralis-converted";

                cacheSegments[1] = "v10";
                Path path4 = Path.of("resources", cacheSegments);
                Files.createDirectories(path4);
                String packHash = computePackHash(path);
                String rawSuffix;
                if (isZip) {
                    rawSuffix = ".zip";
                } else {
                    rawSuffix = "";
                }

                String suffix = rawSuffix;
                String hash = packHash;
                Path path5 = path4.resolve(baseName + "__" + hash + suffix);
                pruneStaleCache(path4, baseName, packHash);
                if (Files.exists(path5)) {
                    return path5;
                }

                short targetProtocol = 772;
                if (sourceProtocol > 0 && sourceProtocol < targetProtocol) {

                    new ForwardsPackConverter(
                                    gson,
                                    sourceProtocol,
                                    targetProtocol,
                                    "none",
                                    path2,
                                    false,
                                    new SilentPrintStream(System.out))
                            .runDir();
                }

                Path path6;
                if (isZip) {
                    path6 = path2.resolve(baseName + "_converted.zip");
                } else {
                    path6 = path2.resolve(baseName + "_converted");
                }

                Path path7 = path6;
                Path path8;
                if (!Files.exists(path7)) {
                    path8 = path3;
                } else if (isZip) {
                    Path path9 = path2.resolve(baseName + "_migrate");
                    extractZip(path7, path9);
                    path8 = path9;
                } else {
                    path8 = path7;
                }

                PackMigrator.migratePack(path8);
                restoreGuiSheets(path8, guiSheets);
                if (isZip) {
                    if (path8 != path7) {
                        zipDirectory(path8, path7);
                    }
                } else {
                    try (Stream<Path> fileStream = Files.walk(path7)) {
                        long fileCount =
                                fileStream.filter(path10 -> Files.isRegularFile(path10)).count();
                        if (fileCount == 0L) {
                            return path;
                        }
                    }
                }

                storeInCache(path7, path5, isZip);
                pruneOldRevisions(path4);
                return path5;
            } finally {
                deleteRecursive(path2);
            }
        } catch (Exception e) {

            logger.error("[PackAutoConverter] Failed to convert resource pack {}", path, e);
            return path;
        }
    }

    private static void deleteRecursive(Path path) {

        try {
            if (!Files.exists(path)) {
                return;
            }

            try (Stream<Path> pathStream = Files.walk(path)) {
                pathStream
                        .sorted(Comparator.reverseOrder())
                        .forEach(
                                path2 -> {
                                    try {
                                        Files.delete(path2);
                                    } catch (IOException ignored) {
                                    }
                                });
            }
        } catch (IOException ignored) {
        }
    }

    private static boolean loadConverterEnabled() {

        try {
            if (!Files.isRegularFile(settingsPath)) {
                return true;
            }

            JsonObject jsonObject =
                    JsonParser.parseString(Files.readString(settingsPath, StandardCharsets.UTF_8))
                            .getAsJsonObject();

            JsonElement jsonElement = jsonObject.get("converterEnabled");
            return jsonElement == null
                    || !jsonElement.isJsonPrimitive()
                    || jsonElement.getAsBoolean();
        } catch (Exception ignored) {
            return true;
        }
    }

    private static Map<Path, byte[]> snapshotGuiSheets(Path path) throws java.io.IOException {

        HashMap sheets = new HashMap();
        if (Files.isDirectory(path)) {
            try (Stream<Path> pathStream = Files.walk(path)) {
                pathStream
                        .filter(path2 -> Files.isRegularFile(path2))
                        .filter(
                                path3 -> {
                                    return "textures"
                                            .equals(
                                                    path3.getParent()
                                                            .getParent()
                                                            .getFileName()
                                                            .toString());
                                })
                        .filter(
                                path4 -> {
                                    return "gui".equals(path4.getParent().getFileName().toString());
                                })
                        .forEach(
                                path5 -> {
                                    try {
                                        sheets.put(
                                                path.relativize(path5), Files.readAllBytes(path5));
                                    } catch (IOException e) {

                                        logger.warn(
                                                "[PackAutoConverter] could not snapshot {}",
                                                path5,
                                                e);
                                    }
                                });
            } catch (IOException e) {

                logger.warn("[PackAutoConverter] sheet snapshot walk failed for {}", path, e);
            }

            return sheets;
        } else {
            String fileName = path.getFileName().toString();

            if (!fileName.endsWith(".zip")) {
                return sheets;
            }

            try (ZipFile zipFile = new ZipFile(path.toFile())) {
                Enumeration entries = zipFile.entries();

                while (entries.hasMoreElements()) {
                    ZipEntry entry = (ZipEntry) entries.nextElement();
                    if (!entry.isDirectory()) {
                        String entryName = entry.getName();

                        if (entryName.endsWith(".png")) {
                            Path path6 = Path.of(entryName);
                            if (path6.getNameCount() == 5) {

                                if ("assets".equals(path6.getName(0).toString())) {

                                    if ("textures".equals(path6.getName(2).toString())) {

                                        if ("gui".equals(path6.getName(3).toString())) {
                                            ByteArrayOutputStream outputStream =
                                                    new ByteArrayOutputStream();

                                            try (InputStream inputStream =
                                                    zipFile.getInputStream(entry)) {
                                                inputStream.transferTo(outputStream);
                                            }

                                            sheets.put(path6, outputStream.toByteArray());
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            return sheets;
        }
    }

    private static String packFormatToVersion(int packFormat) {

        if (packFormat <= 1) {

            return "1.8.9";
        } else if (packFormat <= 2) {

            return "1.10.2";
        } else if (packFormat <= 3) {

            return "1.12.2";
        } else if (packFormat <= 4) {

            return "1.14.4";
        } else if (packFormat <= 5) {

            return "1.16.1";
        } else {

            if (packFormat <= 6) {

                return "1.16.5";
            } else {

                if (packFormat <= 7) {

                    return "1.17.1";
                } else {

                    if (packFormat <= 8) {

                        return "1.18.2";
                    } else {

                        if (packFormat <= 9) {

                            return "1.19.2";
                        } else {

                            if (packFormat <= 12) {

                                return "1.19.3";
                            } else {

                                if (packFormat <= 13) {

                                    return "1.19.4";
                                } else {

                                    if (packFormat <= 15) {

                                        return "1.20.1";
                                    } else {

                                        if (packFormat <= 18) {

                                            return "1.20.2";
                                        } else {

                                            if (packFormat <= 22) {

                                                return "1.20.4";
                                            } else {

                                                if (packFormat <= 32) {

                                                    return "1.20.6";
                                                } else {

                                                    if (packFormat <= 34) {

                                                        return "1.21.1";
                                                    } else {

                                                        if (packFormat <= 42) {

                                                            return "1.21.3";
                                                        } else {

                                                            if (packFormat <= 46) {

                                                                return "1.21.4";
                                                            } else {

                                                                if (packFormat <= 55) {

                                                                    return "1.21.5";
                                                                } else {

                                                                    if (packFormat <= 63) {

                                                                        return "1.21.6";
                                                                    } else {

                                                                        if (packFormat <= 64) {

                                                                            return "1.21.8";
                                                                        } else {
                                                                            return null;
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private static void storeInCache(Path path, Path path2, boolean isZip)
            throws java.io.IOException {
        if (isZip) {
            String targetName = String.valueOf(path2.getFileName());

            Path path3 = path2.resolveSibling(targetName + ".tmp");
            Files.move(path, path3, StandardCopyOption.REPLACE_EXISTING);
            Files.move(path3, path2, StandardCopyOption.REPLACE_EXISTING);
        } else {
            copyDirectory(path, path2);
        }
    }

    private static void copyDirectory(Path path, Path path2) throws java.io.IOException {
        Files.walk(path)
                .forEach(
                        path3 -> {
                            try {
                                Files.copy(path3, path2.resolve(path.relativize(path3)));
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        });
    }

    private static JsonElement firstArrayElement(JsonElement jsonElement) {
        return jsonElement != null
                        && jsonElement.isJsonArray()
                        && !jsonElement.getAsJsonArray().isEmpty()
                ? jsonElement.getAsJsonArray().get(0)
                : null;
    }

    private static void pruneStaleCache(Path path, String baseName, String hash)
            throws java.io.IOException {

        try (Stream<Path> pathStream = Files.list(path)) {
            pathStream.forEach(
                    path2 -> {
                        String name = path2.getFileName().toString();

                        if (name.startsWith(baseName + "__")) {

                            if (!name.contains("__" + hash)) {
                                try {
                                    deleteRecursive(path2);
                                } catch (Exception ignored) {
                                }

                                return;
                            }
                        }
                    });
        }
    }

    public static boolean isOutdatedPack(Path path) {
        JsonObject jsonObject = readPackSection(path);
        return jsonObject == null ? hasPackMcmeta(path) : isOutdatedFormat(jsonObject);
    }

    public static Path convertIfOutdated(Path path) {
        ReentrantLock lock = conversionLocks.computeIfAbsent(path, path2 -> new ReentrantLock());
        lock.lock();

        try {
            return convertLocked(path);
        } finally {
            lock.unlock();
        }
    }

    public static ResourcesSupplier createResourcesSupplier(Path path) {
        String fileName = path.getFileName().toString();

        return fileName.endsWith(".zip")
                ? new FileResourcesSupplier(path)
                : new PathResourcesSupplier(path);
    }

    public static Path getSupplierPath(ResourcesSupplier resourcesSupplier) {
        if (resourcesSupplier instanceof FileResourcesSupplier fileResourcesSupplier) {
            return ((astralis.mixin.FileResourcesSupplierAccessor) fileResourcesSupplier)
                    .getContent()
                    .toPath();
        } else {
            return resourcesSupplier instanceof PathResourcesSupplier pathResourcesSupplier
                    ? ((astralis.mixin.PathResourcesSupplierAccessor) pathResourcesSupplier)
                            .getContent()
                    : null;
        }
    }
}
