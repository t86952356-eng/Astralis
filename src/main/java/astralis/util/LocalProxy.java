package astralis.util;

import com.astralis.scripting.InstanceAccess;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class LocalProxy implements InstanceAccess {
    private static volatile String status = "Idle";
    private static volatile boolean ready;
    private static volatile boolean failed;
    private static volatile Thread bootstrapThread;
    private static volatile Process installProcess;
    private static volatile Process layerProcess;

    private LocalProxy() {}

    public static synchronized void startBootstrap() {

        if (!ready && (bootstrapThread == null || !bootstrapThread.isAlive())) {
            failed = false;

            status = "Preparing MiniBlox...";
            Runnable bootstrapTask = LocalProxy::runBootstrap;

            bootstrapThread = new Thread(bootstrapTask, "MiniBlox bootstrap");
            bootstrapThread.setDaemon(true);
            bootstrapThread.start();
        }
    }

    private static void waitForPortOpen(Process process, int port, String serviceName)
            throws InterruptedException, java.io.IOException {
        int attempts = 0;

        while (true) {

            if (attempts >= 600) {

                throw new IOException(serviceName + " did not start in time");
            }

            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedException();
            }

            if (!process.isAlive()) {

                throw new IOException(serviceName + " stopped unexpectedly");
            }

            if (isPortOpen(port)) {
                return;
            }

            TimeUnit.MILLISECONDS.sleep(100L);
            attempts++;
        }
    }

    private static void patchTranslationLayer(Path path) throws java.io.IOException {

        Path path2 = path.resolve("index.js");
        String indexSource = Files.readString(path2, StandardCharsets.UTF_8);

        String patchedIndexSource =
                indexSource.replace(
                        "if (skipKick > Date.now()) return;\n\t\tclient.end(reason);",
                        "if (skipKick > Date.now() || ClientSocket.socket.active) return;\n\t\tclient.end(reason);");
        if (!patchedIndexSource.equals(indexSource)) {
            Files.writeString(path2, patchedIndexSource, StandardCharsets.UTF_8);
        }

        Path path3 = path.resolve("miniblox");

        Path path4 = path3.resolve("main.js");
        String mainSource = Files.readString(path4, StandardCharsets.UTF_8);

        String patchedMainSource =
                mainSource.replace("reconnectionAttempts: 2,", "reconnectionAttempts: 10,");
        if (!patchedMainSource.equals(mainSource)) {
            Files.writeString(path4, patchedMainSource, StandardCharsets.UTF_8);
        }
    }

    private static boolean isPortOpen(int port) {

        try (Socket socket = new Socket()) {

            InetSocketAddress address = new InetSocketAddress("127.0.0.1", port);
            socket.connect(address, 200);
            return true;
        } catch (IOException ignored) {
            return false;
        }
    }

    private static void installDependencies(Path path, Path path2)
            throws InterruptedException, java.io.IOException {

        if (!Files.isRegularFile(path2.resolve(".dependencies-ready"))) {

            status = "Installing MiniBlox dependencies...";
            String npmCommand;
            if (path.isAbsolute()) {
                Path path3 = path.getParent();

                npmCommand = path3.resolve("npm.cmd").toString();
            } else {

                npmCommand = "npm.cmd";
            }

            String npmExecutable = npmCommand;
            String[] command = new String[] {npmExecutable, null, null, null, null};

            command[1] = "install";

            command[2] = "--omit=dev";

            command[3] = "--no-audit";

            command[4] = "--no-fund";
            installProcess =
                    new ProcessBuilder(command)
                            .directory(path2.toFile())
                            .redirectErrorStream(true)
                            .start();

            pumpProcessOutput(installProcess, "Installing dependencies");
            if (installProcess.waitFor() != 0) {

                throw new IOException("npm install failed");
            }

            Path path4 = path2.resolve(".dependencies-ready");

            Files.writeString(path4, "ready", StandardCharsets.UTF_8);
        }
    }

    public static synchronized void stop() {

        ready = false;
        if (bootstrapThread != null) {
            bootstrapThread.interrupt();
        }

        terminateProcess(installProcess);
        terminateProcess(layerProcess);

        installProcess = null;
        layerProcess = null;
        status = "Stopped";
    }

    private static void pumpProcessOutput(Process process, String label) {

        String line;
        try (BufferedReader reader = process.inputReader(StandardCharsets.UTF_8)) {
            while ((line = reader.readLine()) != null) {
                if (!line.isBlank()) {

                    String cleanedLine = line.replaceAll("\\u001B\\[[;\\d]*m", "");

                    status = label + ": " + cleanedLine;
                }
            }
        } catch (IOException ignored) {
        }
    }

    private static void runBootstrap() {

        try {

            if (!isPortOpen(25565)) {
                Path path = mc.gameDirectory.toPath();

                Path path2 = path.resolve("astralis");

                Path path3 = path2.resolve("miniblox");
                Files.createDirectories(path3);

                Path path4 = ensureNode(path3);
                Path path5 = ensureTranslationLayer(path3);
                installDependencies(path4, path5);
                startLayer(path4, path5);
                status = "MiniBlox is ready";
                ready = true;
                return;
            }

            status = "MiniBlox is ready";
            ready = true;
        } catch (InterruptedException ignored) {
            terminateProcess(installProcess);
            terminateProcess(layerProcess);
            Thread.currentThread().interrupt();
            return;
        } catch (Exception e) {
            terminateProcess(installProcess);
            terminateProcess(layerProcess);
            failed = true;
            String message = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();

            status = "Failed: " + message;
            return;
        } finally {
            installProcess = null;
        }
    }

    private static void startLayer(Path path, Path path2)
            throws InterruptedException, java.io.IOException {

        status = "Starting the MiniBlox layer...";
        String[] command = new String[] {path.toString(), null};

        command[1] = "index.js";
        Process process =
                new ProcessBuilder(command)
                        .directory(path2.toFile())
                        .redirectErrorStream(true)
                        .start();
        layerProcess = process;
        Runnable outputPump =
                () -> {
                    pumpProcessOutput(process, "MiniBlox");
                };

        Thread outputThread = new Thread(outputPump, "MiniBlox output");
        outputThread.setDaemon(true);
        outputThread.start();
        waitForPortOpen(process, 25565, "MiniBlox layer");
    }

    public static boolean isConnectedViaProxy() {

        if (ready && mc.player != null && mc.level != null && mc.getCurrentServer() != null) {

            if ("127.0.0.1:25565".equals(mc.getCurrentServer().ip)) {
                return true;
            }
        }

        return false;
    }

    private static void deleteRecursively(Path path) throws java.io.IOException {

        try (Stream<Path> stream = Files.walk(path)) {
            for (Path path2 : stream.sorted(Comparator.reverseOrder()).toList()) {
                Files.delete(path2);
            }
        }
    }

    private static void terminateProcess(Process process) {
        if (process != null && process.isAlive()) {
            process.destroy();

            try {

                if (!process.waitFor(2L, TimeUnit.SECONDS)) {
                    process.destroyForcibly();

                    process.waitFor(2L, TimeUnit.SECONDS);
                }
            } catch (InterruptedException ignored) {
                process.destroyForcibly();
                Thread.currentThread().interrupt();
            }
        }
    }

    public static boolean hasFailed() {
        return failed;
    }

    private static HttpURLConnection openCheckedConnection(String url) throws java.io.IOException {
        HttpURLConnection connection = (HttpURLConnection) URI.create(url).toURL().openConnection();
        int httpStatus;

        connection.setConnectTimeout(15000);
        connection.setReadTimeout(60000);

        connection.setRequestProperty("User-Agent", "Astralis MiniBlox bootstrap");
        connection.setInstanceFollowRedirects(true);
        int responseCode = connection.getResponseCode();
        if (responseCode >= 400) {
            httpStatus = connection.getResponseCode();

            throw new IOException("download failed with HTTP " + httpStatus);
        } else {
            return connection;
        }
    }

    public static String getStatus() {
        return status;
    }

    public static boolean isReady() {
        return ready;
    }

    private static Path ensureNode(Path path) throws java.io.IOException {

        if (isCompatibleNode(Path.of("node"))) {

            status = "Using installed Node.js";

            return Path.of("node");
        }

        Path path2 = path.resolve("node");

        Path path3 = findFile(path2, "node.exe");
        if (path3 != null && isCompatibleNode(path3)) {

            status = "Using cached Node.js";
            return path3;
        }

        String osName = System.getProperty("os.name").toLowerCase(Locale.ROOT);

        if (!osName.contains("win")) {

            throw new IOException("automatic Node.js installation currently supports Windows only");
        }

        status = "Finding the latest Node.js LTS...";

        String osArch = System.getProperty("os.arch").toLowerCase(Locale.ROOT);

        String selectedPlatform;
        if (osArch.contains("aarch64")) {

            selectedPlatform = "win-arm64-zip";
        } else {

            selectedPlatform = "win-x64-zip";
        }

        String platformKey = selectedPlatform;

        JsonArray jsonArray =
                JsonParser.parseString(downloadString("https://nodejs.org/dist/index.json"))
                        .getAsJsonArray();
        JsonObject jsonObject = null;

        for (JsonElement jsonElement : jsonArray) {
            JsonObject jsonObject2 = jsonElement.getAsJsonObject();

            if (jsonObject2.getAsJsonPrimitive("lts").isString()) {

                if (jsonObject2.getAsJsonArray("files").contains(new JsonPrimitive(platformKey))) {
                    jsonObject = jsonObject2;
                    break;
                }
            }
        }

        if (jsonObject == null) {

            throw new IOException("no compatible Node.js LTS release was found");
        }

        if (Files.exists(path2)) {
            deleteRecursively(path2);
        }

        Files.createDirectories(path2);

        String version = jsonObject.get("version").getAsString();
        String platformName = platformKey.substring(0, platformKey.length() - 4);

        String archiveName = "node-" + version + "-" + platformName + ".zip";

        Path path4 = path.resolve("node.zip");

        status = "Downloading Node.js " + version + "...";

        downloadFile("https://nodejs.org/dist/" + version + "/" + archiveName, path4);

        status = "Installing Node.js...";
        extractZip(path4, path2, false);
        Files.deleteIfExists(path4);

        Path path5 = findFile(path2, "node.exe");
        if (path5 == null) {

            throw new IOException("Node.js archive did not contain node.exe");
        } else {
            return path5;
        }
    }

    private static void extractZip(Path path, Path path2, boolean stripTopLevel)
            throws java.io.IOException {

        ZipEntry entry;
        try (ZipInputStream zipStream = new ZipInputStream(Files.newInputStream(path))) {
            while ((entry = zipStream.getNextEntry()) != null) {
                String entryName = entry.getName();

                String relativePath = entryName.replace('\\', '/');
                if (stripTopLevel) {

                    int separatorIndex = relativePath.indexOf(47);
                    if (separatorIndex < 0) {
                        continue;
                    }

                    relativePath = relativePath.substring(separatorIndex + 1);
                }

                if (!relativePath.isEmpty()) {
                    Path path3 = path2.resolve(relativePath).normalize();
                    if (!path3.startsWith(path2)) {
                        String unsafeEntryName = entry.getName();

                        throw new IOException("unsafe ZIP entry: " + unsafeEntryName);
                    }

                    if (entry.isDirectory()) {
                        Files.createDirectories(path3);
                    } else {
                        Files.createDirectories(path3.getParent());
                        Files.copy(zipStream, path3, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }
        }
    }

    private static Path ensureTranslationLayer(Path path) throws java.io.IOException {

        Path path2 = path.resolve("layer");
        if (Files.isRegularFile(path2.resolve("index.js"))) {
            if (Files.isRegularFile(path2.resolve("package.json"))) {

                status = "Using cached MiniBlox layer";
                patchTranslationLayer(path2);
                return path2;
            }
        }

        if (Files.exists(path2)) {
            deleteRecursively(path2);
        }

        Files.createDirectories(path2);
        Path path3 = path.resolve("layer.zip");

        status = "Downloading the MiniBlox layer...";

        downloadFile(
                "https://github.com/7GrandDadPGN/MinibloxTranslationLayer/archive/refs/heads/main.zip",
                path3);

        status = "Installing the MiniBlox layer...";
        extractZip(path3, path2, true);
        Files.deleteIfExists(path3);
        if (Files.isRegularFile(path2.resolve("index.js"))) {
            if (Files.isRegularFile(path2.resolve("package.json"))) {
                patchTranslationLayer(path2);
                return path2;
            }
        }

        throw new IOException("translation layer archive was incomplete");
    }

    private static void downloadFile(String url, Path path) throws java.io.IOException {
        HttpURLConnection connection = openCheckedConnection(url);

        try (InputStream input = connection.getInputStream()) {
            Files.copy(input, path, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            connection.disconnect();
        }
    }

    private static Path findFile(Path path, String fileName) throws java.io.IOException {
        if (!Files.exists(path)) {
            return null;
        }

        try (Stream<Path> stream = Files.walk(path)) {
            return stream.filter(
                            path2 ->
                                    Files.isRegularFile(path2)
                                            && path2.getFileName()
                                                    .toString()
                                                    .equalsIgnoreCase(fileName))
                    .findFirst()
                    .orElse(null);
        }
    }

    public static synchronized void restart() {
        stop();
        bootstrapThread = null;
        startBootstrap();
    }

    private static String downloadString(String url) throws java.io.IOException {
        HttpURLConnection connection = openCheckedConnection(url);

        try (InputStream input = connection.getInputStream()) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } finally {
            connection.disconnect();
        }
    }

    private static boolean isCompatibleNode(Path path) {

        try {
            String[] command = new String[] {path.toString(), null};

            command[1] = "--version";
            Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
            String versionLine = process.inputReader(StandardCharsets.UTF_8).readLine();
            if (process.waitFor(10L, TimeUnit.SECONDS)
                    && process.exitValue() == 0
                    && versionLine != null) {
                String versionString = versionLine.replaceFirst("^v", "");
                int majorVersion = Integer.parseInt(versionString.split("\\.")[0]);
                if (majorVersion >= 22) {
                    return true;
                }
            }

            return false;
        } catch (Exception ignored) {
            return false;
        }
    }
}
