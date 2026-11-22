package astralis.resourcepack.migration;

import java.awt.HeadlessException;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class CookieFileImporter {
    private static Set<String> cookieFileExtensions;

    private CookieFileImporter() {}

    public static List<File> collectClipboardFiles(String fallbackText) {
        ArrayList files = new ArrayList();

        try {
            Transferable clipboardContents =
                    Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
            if (clipboardContents != null
                    && clipboardContents.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                Object transferData =
                        clipboardContents.getTransferData(DataFlavor.javaFileListFlavor);
                if (transferData instanceof List) {
                    for (Object item : (List) transferData) {
                        if (item instanceof File file2 && file2.isFile()) {
                            files.add(file2);
                        }
                    }
                }
            }
        } catch (HeadlessException | IOException | UnsupportedFlavorException ignored) {
        }

        return !files.isEmpty() ? dedupeFilesByPath(files) : parseFilePathLines(fallbackText);
    }

    public static List<File> parseFilePathsFromText(String text) {
        return parseFilePathLines(text);
    }

    private static List<File> parseFilePathLines(String text) {
        ArrayList files = new ArrayList();
        if (text != null && !text.isBlank()) {
            for (String rawLine : text.replace("\r", "").split("\n")) {
                String line = rawLine.trim();
                if (!line.isEmpty() && !line.startsWith("#")) {
                    try {

                        if (line.startsWith("file://")) {
                            File file2 = new File(new URI(line));
                            if (file2.isFile()) {
                                files.add(file2);
                            }
                            continue;
                        }
                    } catch (URISyntaxException | IllegalArgumentException ignored) {
                    }

                    File file3 = new File(line.replace("\"", ""));
                    if (file3.isFile()) {
                        files.add(file3);
                    }
                }
            }

            return dedupeFilesByPath(files);
        } else {
            return files;
        }
    }

    public static List<File> importCookieFiles(List<File> files, File file2)
            throws java.io.IOException {
        if (files != null && !files.isEmpty()) {
            Path path = file2.toPath();

            Path path2 = path.resolve("astralis");

            Path path3 = path2.resolve("cookies");
            Files.createDirectories(path3);
            ArrayList importedFiles = new ArrayList();
            long[] totalBytes = new long[] {0L};

            for (File file3 : dedupeFilesByPath(files)) {
                if (file3 != null && file3.isFile()) {
                    if (isZipFile(file3)) {
                        extractCookieFilesFromZip(
                                file3.toPath(), path3, importedFiles, totalBytes, 0);
                    } else {
                        importedFiles.add(
                                copyCookieFile(file3.toPath(), path3, file3.getName(), totalBytes));
                    }
                }
            }

            if (importedFiles.isEmpty()) {

                throw new IOException("No cookie files found in the selected file");
            } else {
                return importedFiles;
            }
        } else {

            throw new IOException("No files were supplied");
        }
    }

    private static void extractCookieFilesFromZip(
            Path path, Path path2, List<File> importedFiles, long[] totalBytes, int depth)
            throws java.io.IOException {
        long entrySize;
        if (depth > 2) {

            throw new IOException("Nested ZIP depth is too large");
        }

        try (ZipFile zipFile = new ZipFile(path.toFile())) {
            Enumeration entries = zipFile.entries();

            while (entries.hasMoreElements()) {
                ZipEntry entry = (ZipEntry) entries.nextElement();
                if (!entry.isDirectory() && isSafeZipEntryName(entry.getName())) {
                    entrySize = entry.getSize();

                    if (entrySize > 33554432L) {

                        throw new IOException("A cookie file is too large");
                    }

                    String fileName = Path.of(entry.getName()).getFileName().toString();
                    String lowerName = fileName.toLowerCase();

                    if (lowerName.endsWith(".zip")) {

                        Path path3 = Files.createTempFile(path2, "nested-", ".zip");

                        try {
                            try (InputStream nestedInput = zipFile.getInputStream(entry);
                                    OutputStream nestedOutput = Files.newOutputStream(path3); ) {
                                copyWithSizeLimit(
                                        nestedInput, nestedOutput, entry.getSize(), totalBytes);
                            }

                            extractCookieFilesFromZip(
                                    path3, path2, importedFiles, totalBytes, depth + 1);
                        } finally {
                            Files.deleteIfExists(path3);
                        }
                    } else if (isCookieFileName(fileName)) {
                        Path path4 = createUniqueDestination(path2, fileName);

                        try (InputStream entryInput = zipFile.getInputStream(entry);
                                OutputStream entryOutput = Files.newOutputStream(path4); ) {
                            copyWithSizeLimit(entryInput, entryOutput, entry.getSize(), totalBytes);
                        }

                        importedFiles.add(path4.toFile());
                    }
                }
            }
        }
    }

    private static File copyCookieFile(Path path, Path path2, String fileName, long[] totalBytes)
            throws java.io.IOException {
        long fileSize;
        fileSize = Files.size(path);

        if (fileSize > 33554432L) {

            throw new IOException("Cookie file is too large");
        }

        Path path3 = createUniqueDestination(path2, fileName);

        try (InputStream input = Files.newInputStream(path);
                OutputStream output = Files.newOutputStream(path3); ) {
            copyWithSizeLimit(input, output, Files.size(path), totalBytes);
        }

        return path3.toFile();
    }

    private static void copyWithSizeLimit(
            InputStream input, OutputStream output, long declaredSize, long[] totalBytes)
            throws java.io.IOException {
        long importedTotal;

        if (declaredSize > 33554432L) {

            throw new IOException("Cookie file is too large");
        }

        byte[] buffer = new byte[8192];
        long copiedBytes = 0L;

        while (true) {
            int bytesRead;
            if ((bytesRead = input.read(buffer)) == -1) {
                return;
            }

            copiedBytes += bytesRead;
            totalBytes[0] += bytesRead;

            if (copiedBytes > 33554432L) {
                break;
            }

            importedTotal = totalBytes[0];

            if (importedTotal > 134217728L) {
                break;
            }

            output.write(buffer, 0, bytesRead);
        }

        throw new IOException("Imported cookie data is too large");
    }

    private static Path createUniqueDestination(Path path, String fileName) {
        String safeName = sanitizeFileName(fileName);
        return path.resolve(UUID.randomUUID() + "-" + safeName);
    }

    private static String sanitizeFileName(String fileName) {

        String sanitized = fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
        String result;
        if (sanitized.isBlank()) {

            result = "cookies.txt";
        } else {
            result = sanitized;
        }

        return result;
    }

    private static boolean isZipFile(File file2) {

        try (InputStream input = Files.newInputStream(file2.toPath())) {
            int firstByte = input.read();

            if (firstByte == 80) {
                int secondByte = input.read();

                if (secondByte == 75) {
                    int thirdByte = input.read();
                    int fourthByte = input.read();
                    if (thirdByte != 3 || fourthByte != 4) {
                        if (thirdByte == 5) {

                            if (fourthByte == 6) {
                                return true;
                            }
                        }

                        if (thirdByte == 7) {

                            if (fourthByte == 8) {
                                return true;
                            }
                        }

                        return false;
                    }

                    return true;
                }
            }

            return false;
        } catch (IOException ignored) {
            return false;
        }
    }

    private static boolean isCookieFileName(String fileName) {
        String lowerName = fileName.toLowerCase();

        return lowerName.contains("cookie")
                ? true
                : cookieFileExtensions.stream().anyMatch(lowerName::endsWith);
    }

    private static boolean isSafeZipEntryName(String entryName) {

        try {
            Path path = Path.of(entryName);
            if (!path.isAbsolute() && !entryName.contains("\\")) {
                Path path2 = path.normalize();

                if (!path2.startsWith("..")) {
                    return true;
                }
            }

            return false;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static List<File> dedupeFilesByPath(List<File> files) {
        ArrayList uniqueFiles = new ArrayList();
        HashSet seenPaths = new HashSet();
        Iterator iterator = files.iterator();

        while (iterator.hasNext()) {
            File file2 = (File) iterator.next();
            if (file2 != null && seenPaths.add(file2.getAbsolutePath())) {
                uniqueFiles.add(file2);
            }
        }

        return uniqueFiles;
    }

    static {
        cookieFileExtensions = Set.of(".txt", ".json", ".csv", ".cookie", ".cookies", ".dat");
    }
}
