package astralis.scripting;

import astralis.Astralis;
import astralis.config.ConfigProfile;
import astralis.ui.screen.DropdownClickGuiScreen;
import astralis.ui.screen.PanelClickGuiScreen;
import astralis.util.ChatUtil;

import com.astralis.scripting.InstanceAccess;
import com.astralis.scripting.Script;
import com.astralis.scripting.ScriptInternals;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.metadata.ModOrigin;
import net.fabricmc.loader.api.metadata.ModOrigin.Kind;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent.CopyToClipboard;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent.ShowText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Type;
import org.objectweb.asm.signature.SignatureReader;

import pub.astralis.client.module.core.Category;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.Thread.Builder.OfVirtual;
import java.lang.reflect.Modifier;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.ProtectionDomain;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor.AbortPolicy;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.lang.model.SourceVersion;

public class ScriptManager implements InstanceAccess {
    private static final int MAX_CLASS_FILES = 256;
    private static final long MAX_CLASS_FILE_BYTES = 2097152L;
    private static final long MAX_TOTAL_CLASS_BYTES = 16777216L;
    private static final int MAX_CHAT_DIAGNOSTIC_LENGTH = 4096;
    private static final int MAX_COPY_TEXT_LENGTH = 65536;
    private final CopyOnWriteArrayList<Script> loadedScripts;
    private final CopyOnWriteArrayList<PendingScriptMessage> pendingMessages;
    private final ScriptEventListener eventListener;
    private final File scriptsDirectory;
    private final File statesFile;
    private final File legacyStatesFile;
    private final File permissionsFile;
    private final Path cacheDirectory;
    private final boolean cacheDirectorySafe;
    private final Map<String, Boolean> enabledStates;
    private final Map<String, ScriptCandidate> loadedCandidates;
    private final Map<Script, String> scriptIds;
    private final Map<String, HttpDomainApproval> httpApprovals;
    private ThreadPoolExecutor loaderExecutor;
    private volatile Path scriptingApiJar;
    private volatile String compileClasspath;
    private volatile String cachedFingerprint;
    private volatile long lifecycleGeneration;
    private volatile boolean initialized;
    private volatile boolean apiMismatchReported;
    private boolean unloadingAll;
    private boolean reloadRequested;
    private boolean loadInProgress;
    private int dispatchDepth;

    public ScriptManager() {

        this.loadedScripts = new CopyOnWriteArrayList<>();
        this.pendingMessages = new CopyOnWriteArrayList<>();
        this.enabledStates = new HashMap<>();
        this.loadedCandidates = new HashMap<>();
        this.scriptIds = new HashMap<>();
        this.httpApprovals = new HashMap<>();
        this.eventListener = new ScriptEventListener(this);

        this.scriptsDirectory = new File(mc.gameDirectory, "astralis/scripts");

        this.statesFile = new File(this.scriptsDirectory, "states.json");

        this.legacyStatesFile = new File(this.scriptsDirectory, "states.txt");

        this.permissionsFile = new File(this.scriptsDirectory, "permissions.json");
        Path path = this.scriptsDirectory.toPath();

        this.cacheDirectory = path.resolve(".cache");
        boolean cacheSafe = false;

        try {
            Files.createDirectories(this.cacheDirectory);
            cacheSafe =
                    this.cacheDirectory
                            .toRealPath()
                            .getParent()
                            .equals(this.scriptsDirectory.toPath().toRealPath());
            if (!cacheSafe) {
                Astralis.LOGGER.error(
                        "[ScriptManager] Compilation cache is not a direct scripts directory child");
            }
        } catch (IOException e) {
            Astralis.LOGGER.error("[ScriptManager] Failed to create scripts directory", e);
        }

        this.cacheDirectorySafe = cacheSafe;
    }

    public synchronized String overwriteScript(
            String scriptName, String source, String projectJson, String expectedRevision)
            throws IOException {
        Path path = this.resolveScriptsDirectory();
        Path path2 = this.resolveSourcePath(scriptName, path);
        byte[] originalBytes = readSourceBytes(path2);
        String newRevision = this.overwriteSource(scriptName, source, expectedRevision);

        try {
            this.writeEasyModeProject(scriptName, projectJson);
            return newRevision;
        } catch (IOException | RuntimeException e) {
            try {
                writeFileAtomically(path, path2, originalBytes);
            } catch (IOException suppressed) {
                e.addSuppressed(suppressed);
            }

            throw e;
        }
    }

    private void deleteStagingDirectories(StagingOutcome outcome) {

        for (Path path :
                new Path[] {
                    outcome.incomingCompilation() != null
                            ? outcome.incomingCompilation().directory()
                            : null,
                    outcome.rollbackCompilation() != null
                            ? outcome.rollbackCompilation().directory()
                            : null,
                    outcome.incomingSources() != null
                            ? outcome.incomingSources().directory()
                            : null,
                    outcome.rollbackSources() != null ? outcome.rollbackSources().directory() : null
                }) {
            if (path != null) {
                try {
                    this.deleteRecursively(path);
                } catch (IOException e) {

                    Astralis.LOGGER.warn(
                            "[ScriptManager] Could not remove script staging directory", e);
                }
            }
        }
    }

    private void validateScriptClass(Class<?> scriptClass, ClassLoader expectedLoader) {
        if (!Modifier.isPublic(scriptClass.getModifiers())) {

            throw new IllegalArgumentException("The script class must be public.");
        }

        if (Modifier.isAbstract(scriptClass.getModifiers())) {

            throw new IllegalArgumentException("The script class cannot be abstract.");
        }

        if (!Script.class.isAssignableFrom(scriptClass)) {

            throw new IllegalArgumentException("The script class must extend Script.");
        }

        if (scriptClass.getClassLoader() != expectedLoader) {

            throw new IllegalArgumentException(
                    "The script class conflicts with the client classpath.");
        }
    }

    private Path resolveEasyModeProjectPath(String scriptName, Path path) throws IOException {
        String safeName = validateScriptName(scriptName);

        Path path2 = path.resolve(safeName + ".astralis.json").normalize();
        if (!path2.getParent().equals(path)) {

            throw new IOException("Easy Mode project escaped the scripts directory.");
        } else if (Files.exists(path2, LinkOption.NOFOLLOW_LINKS) && Files.isSymbolicLink(path2)) {

            throw new IOException("Symbolic-link Easy Mode projects cannot be edited in game.");
        } else {
            return path2;
        }
    }

    private synchronized ReloadResult scheduleLoading(StagingKind kind) {
        if (!this.initialized || this.loaderExecutor == null || this.loaderExecutor.isShutdown()) {
            return ReloadResult.SCAN_FAILED;
        }

        if (this.loadInProgress) {
            this.reloadRequested = true;
            return ReloadResult.DEFERRED;
        }

        this.loadInProgress = true;
        long generation = this.lifecycleGeneration;
        List candidates =
                kind == StagingKind.RELOAD
                        ? List.copyOf(this.loadedCandidates.values())
                        : List.of();

        try {
            this.loaderExecutor.execute(() -> this.runLoading(kind, candidates, generation));

            Astralis.LOGGER.info(
                    "[ScriptManager] Queued {} staging on the background loader", kind);
            return ReloadResult.DEFERRED;
        } catch (RejectedExecutionException e) {
            this.loadInProgress = false;

            Astralis.LOGGER.error("[ScriptManager] Could not queue script loading", e);
            return ReloadResult.SCAN_FAILED;
        }
    }

    public synchronized EasyModeProjectContent readEasyModeProject(String scriptName)
            throws IOException {
        byte[] bytes =
                readSourceBytes(
                        this.resolveEasyModeProjectPath(
                                scriptName, this.resolveScriptsDirectory()));
        return new EasyModeProjectContent(
                StandardCharsets.UTF_8
                        .newDecoder()
                        .onMalformedInput(CodingErrorAction.REPORT)
                        .onUnmappableCharacter(CodingErrorAction.REPORT)
                        .decode(ByteBuffer.wrap(bytes))
                        .toString(),
                computeRevision(bytes));
    }

    private void loadPermissions() {
        long fileSize;
        this.httpApprovals.clear();
        if (this.permissionsFile.exists()) {
            try {
                label94:
                {
                    if (Files.isRegularFile(
                            this.permissionsFile.toPath(), LinkOption.NOFOLLOW_LINKS)) {
                        fileSize = Files.size(this.permissionsFile.toPath());

                        if (fileSize <= 262144L) {
                            JsonObject jsonObject =
                                    JsonParser.parseString(
                                                    Files.readString(
                                                            this.permissionsFile.toPath(),
                                                            StandardCharsets.UTF_8))
                                            .getAsJsonObject();
                            int scriptCount = jsonObject.size();

                            if (scriptCount > 256) {

                                throw new IOException(
                                        "Script permissions contain too many script entries.");
                            }

                            for (Entry entry : jsonObject.entrySet()) {
                                String scriptId = (String) entry.getKey();

                                if (scriptId.matches("[a-z][a-z0-9.-]{0,63}")
                                        && ((JsonElement) entry.getValue()).isJsonObject()) {
                                    JsonObject jsonObject2 =
                                            ((JsonElement) entry.getValue()).getAsJsonObject();

                                    if (jsonObject2.has("revision")) {

                                        if (jsonObject2.get("revision").isJsonPrimitive()) {

                                            if (jsonObject2
                                                    .get("revision")
                                                    .getAsJsonPrimitive()
                                                    .isString()) {

                                                String revision =
                                                        jsonObject2.get("revision").getAsString();

                                                if (revision.matches("[0-9a-f]{64}")) {

                                                    if (jsonObject2.has("domains")) {

                                                        if (jsonObject2.get("domains").isJsonArray()
                                                                && jsonObject2.size() == 2) {

                                                            int domainCount =
                                                                    jsonObject2
                                                                            .getAsJsonArray(
                                                                                    "domains")
                                                                            .size();

                                                            if (domainCount > 32) {

                                                                throw new IOException(
                                                                        "A script cannot approve more than 32 HTTP domains.");
                                                            }

                                                            LinkedHashSet domains =
                                                                    new LinkedHashSet();

                                                            for (JsonElement jsonElement :
                                                                    jsonObject2.getAsJsonArray(
                                                                            "domains")) {
                                                                if (!jsonElement.isJsonPrimitive()
                                                                        || !jsonElement
                                                                                .getAsJsonPrimitive()
                                                                                .isString()) {

                                                                    throw new IOException(
                                                                            "Approved HTTP domains must be strings.");
                                                                }

                                                                domains.add(
                                                                        ScriptScanner
                                                                                .normalizeHttpDomain(
                                                                                        jsonElement
                                                                                                .getAsString()));
                                                            }

                                                            Map approvals = this.httpApprovals;
                                                            String approvalScriptId =
                                                                    (String) entry.getKey();

                                                            approvals.put(
                                                                    approvalScriptId,
                                                                    new HttpDomainApproval(
                                                                            jsonObject2
                                                                                    .get("revision")
                                                                                    .getAsString(),
                                                                            domains));
                                                            continue;
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    throw new IOException(
                                            "Script permissions contain an invalid revision-bound approval.");
                                }

                                throw new IOException(
                                        "Script permissions contain an invalid entry.");
                            }
                            break label94;
                        }
                    }

                    throw new IOException(
                            "Script permissions must be a regular file no larger than 256 KiB.");
                }
            } catch (IOException | RuntimeException e) {
                this.httpApprovals.clear();

                Astralis.LOGGER.error("[ScriptManager] Failed to load script HTTP approvals", e);
            }
        }
    }

    public synchronized ScriptSourceContent readScriptSource(String scriptName) throws IOException {
        Path path = this.resolveSourcePath(scriptName, this.resolveScriptsDirectory());
        byte[] bytes = readSourceBytes(path);
        return new ScriptSourceContent(
                StandardCharsets.UTF_8
                        .newDecoder()
                        .onMalformedInput(CodingErrorAction.REPORT)
                        .onUnmappableCharacter(CodingErrorAction.REPORT)
                        .decode(ByteBuffer.wrap(bytes))
                        .toString(),
                computeRevision(bytes));
    }

    private CompiledScript compileAndPreflightScript(StagedScript stagedScript, Path path)
            throws IOException {
        Files.createDirectories(path);
        String cacheKey =
                stagedScript.candidate().getRevision()
                        + System.lineSeparator()
                        + this.compilationFingerprint();
        boolean restoredFromCache =
                this.restoreCachedOutput(stagedScript.candidate(), path, cacheKey);
        if (!restoredFromCache
                && !this.compileScriptSources(
                        stagedScript.sourceFiles(), path, stagedScript.candidate().getName())) {
            return null;
        }

        try (URLClassLoader classLoader =
                new URLClassLoader(
                        new URL[] {path.toUri().toURL()}, Script.class.getClassLoader())) {
            validateCompiledClasses(path);
            Class scriptClass =
                    Class.forName(
                            this.verifyEntrypointClass(
                                    path, stagedScript.candidate().getEntrypoint()),
                            false,
                            classLoader);
            this.validateScriptClass(scriptClass, classLoader);
            scriptClass.getConstructor();
        } catch (IOException | ReflectiveOperationException | RuntimeException e) {
            this.reportFailureWithCause(
                    stagedScript.candidate().getName(), "Preflight validation failed.", e);
            return null;
        } catch (LinkageError error) {
            if (ScriptApiCompatibility.isApiMismatch(error)) {
                this.reportApiMismatch(stagedScript.candidate().getName(), error);
            } else {
                this.reportFailureWithCause(
                        stagedScript.candidate().getName(), "Preflight validation failed.", error);
            }

            return null;
        }

        Files.writeString(
                path.resolve(".astralis-cache"),
                cacheKey,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE);
        return new CompiledScript(stagedScript, path, restoredFromCache);
    }

    private static String composeFailureMessage(String message, Throwable cause) {
        String baseMessage = messageOrDefault(message);
        if (cause == null) {
            return baseMessage;
        }

        String causeMessage = cause.getMessage();
        return causeMessage != null
                        && !causeMessage.isBlank()
                        && !baseMessage.contains(causeMessage)
                ? baseMessage + " " + causeMessage.strip()
                : baseMessage;
    }

    private static byte[] rewriteLegacyImports(byte[] sourceBytes) {
        String text = new String(sourceBytes, StandardCharsets.UTF_8);
        StringBuilder result = new StringBuilder(text.length());

        for (String line : text.split("(?<=\\R)", -1)) {
            int contentEnd;
            for (contentEnd = line.length(); contentEnd > 0; contentEnd--) {
                char lastChar = line.charAt(contentEnd - 1);

                if (lastChar != '\n') {
                    char lastCharCheck = line.charAt(contentEnd - 1);

                    if (lastCharCheck != '\r') {
                        break;
                    }
                }
            }

            String content = line.substring(0, contentEnd);
            String trimmed = content.stripLeading();

            String importTarget;
            if (trimmed.startsWith("import ") && trimmed.endsWith(";")) {

                importTarget = trimmed.substring("import ".length(), trimmed.length() - 1).trim();
            } else {
                importTarget = null;
            }

            String rewrittenLine;
            if (importTarget == null) {
                rewrittenLine = content;
            } else {
                label65:
                {
                    if (!importTarget.equals("pub.astralis.client.scripting.Script")) {

                        if (!importTarget.equals("static pub.astralis.client.scripting.Script")) {

                            if (!importTarget.equals("pub.astralis.scripting.Script")) {

                                if (!importTarget.equals("static pub.astralis.scripting.Script")) {

                                    if (!importTarget.startsWith(
                                            "pub.astralis.client.scripting.api.")) {

                                        if (!importTarget.startsWith(
                                                "static pub.astralis.client.scripting.api.")) {

                                            if (!importTarget.startsWith(
                                                    "pub.astralis.scripting.api.")) {

                                                if (!importTarget.startsWith(
                                                        "static pub.astralis.scripting.api.")) {
                                                    rewrittenLine = content;
                                                    break label65;
                                                }
                                            }
                                        }
                                    }

                                    String apiIndent =
                                            content.substring(
                                                    0, content.length() - trimmed.length());

                                    String clientApiReplaced =
                                            importTarget.replace(
                                                    "pub.astralis.client.scripting.api.",
                                                    "com.astralis.scripting.api.");

                                    String importPath =
                                            clientApiReplaced.replace(
                                                    "pub.astralis.scripting.api.",
                                                    "com.astralis.scripting.api.");

                                    rewrittenLine = apiIndent + "import " + importPath + ";";
                                    break label65;
                                }
                            }
                        }
                    }

                    String scriptIndent = content.substring(0, content.length() - trimmed.length());

                    String clientScriptReplaced =
                            importTarget.replace(
                                    "pub.astralis.client.scripting.Script",
                                    "com.astralis.scripting.Script");

                    String scriptImportPath =
                            clientScriptReplaced.replace(
                                    "pub.astralis.scripting.Script",
                                    "com.astralis.scripting.Script");

                    rewrittenLine = scriptIndent + "import " + scriptImportPath + ";";
                }
            }

            result.append(rewrittenLine).append(line.substring(contentEnd));
        }

        String rewritten = result.toString();
        return rewritten.equals(text) ? sourceBytes : rewritten.getBytes(StandardCharsets.UTF_8);
    }

    public synchronized void shutdown() {
        if (this.initialized) {
            this.requireClientThread();
            this.initialized = false;
            this.lifecycleGeneration++;
            this.reloadRequested = false;
            this.loadInProgress = false;
            if (this.loaderExecutor != null) {
                this.loaderExecutor.shutdownNow();

                try {

                    if (!this.loaderExecutor.awaitTermination(2L, TimeUnit.SECONDS)) {

                        Astralis.LOGGER.warn(
                                "[ScriptManager] Script loader did not stop within two seconds");
                    }
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }

                this.loaderExecutor = null;
            }

            this.saveStates();
            this.unloadAll();
            Astralis.getInstance().getEventBus().unregister(this.eventListener);
        }
    }

    private static byte[] readSourceBytes(Path path) throws IOException {
        long fileSize;

        if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {

            throw new IOException("Script source does not exist or is not a regular file.");
        }

        try (SeekableByteChannel channel =
                Files.newByteChannel(path, StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS)) {
            fileSize = channel.size();

            if (fileSize > 1048576L) {

                throw new IOException("Script source cannot exceed 1 MiB.");
            }

            ByteBuffer buffer = ByteBuffer.allocate(1048577);

            while (buffer.hasRemaining()) {
                int bytesRead = channel.read(buffer);
                if (bytesRead < 0) {
                    break;
                }

                if (bytesRead == 0) {

                    throw new IOException("Script source read did not make progress.");
                }
            }

            int totalBytes = buffer.position();

            if (totalBytes > 1048576) {

                throw new IOException("Script source cannot exceed 1 MiB.");
            } else {
                byte[] result = new byte[buffer.position()];
                buffer.flip();
                buffer.get(result);
                return result;
            }
        }
    }

    public synchronized void init() {
        if (!this.initialized) {
            this.requireClientThread();
            this.initialized = true;
            this.lifecycleGeneration++;
            this.loaderExecutor =
                    new ThreadPoolExecutor(
                            1,
                            1,
                            0L,
                            TimeUnit.MILLISECONDS,
                            new ArrayBlockingQueue<>(1),
                            runnable -> {
                                Thread thread = new Thread(runnable, "Astralis Script Loader");
                                thread.setDaemon(true);
                                return thread;
                            },
                            new AbortPolicy());
            Astralis.getInstance().getEventBus().register(this.eventListener);
            this.loadStates();
            this.loadPermissions();
            this.scheduleLoading(StagingKind.STARTUP);
        }
    }

    public synchronized void saveStates() {
        if (!this.unloadingAll) {
            for (Script script : this.loadedScripts) {
                if (ScriptInternals.getScriptModule(script) != null) {
                    this.enabledStates.put(
                            this.scriptIds.getOrDefault(script, script.getName()),
                            ((ScriptModule) ScriptInternals.getScriptModule(script)).isEnabled());
                }
            }

            try {
                Files.createDirectories(this.scriptsDirectory.toPath());
                JsonObject jsonObject = new JsonObject();
                this.enabledStates.entrySet().stream()
                        .sorted(Entry.comparingByKey())
                        .forEach(entry -> jsonObject.addProperty(entry.getKey(), entry.getValue()));
                Path path = this.scriptsDirectory.toPath();
                String statesFileName = this.statesFile.getName();

                Path path2 = Files.createTempFile(path, statesFileName, ".tmp");

                try {
                    Files.writeString(
                            path2,
                            new GsonBuilder().setPrettyPrinting().create().toJson(jsonObject),
                            StandardCharsets.UTF_8);

                    try {
                        Files.move(
                                path2,
                                this.statesFile.toPath(),
                                StandardCopyOption.REPLACE_EXISTING,
                                StandardCopyOption.ATOMIC_MOVE);
                    } catch (AtomicMoveNotSupportedException ignored) {
                        Files.move(
                                path2,
                                this.statesFile.toPath(),
                                StandardCopyOption.REPLACE_EXISTING);
                    }
                } finally {
                    Files.deleteIfExists(path2);
                }
            } catch (IOException e) {

                Astralis.LOGGER.error("[ScriptManager] Failed to save script states", e);
            }
        }
    }

    public synchronized String overwriteSource(
            String scriptName, String newSource, String expectedRevision) throws IOException {
        if (expectedRevision != null && !expectedRevision.isBlank()) {
            byte[] newBytes = encodeSource(newSource);
            Path path = this.resolveScriptsDirectory();
            this.ensureNameAvailable(path, validateScriptName(scriptName), true);
            Path path2 = this.resolveSourcePath(scriptName, path);
            if (!computeRevision(readSourceBytes(path2)).equals(expectedRevision)) {

                throw new IOException(
                        "Script source changed outside the workspace; reopen it before saving.");
            }

            Path path3 = writeTempFile(path, newBytes);

            try {
                if (!computeRevision(readSourceBytes(path2)).equals(expectedRevision)) {

                    throw new IOException(
                            "Script source changed outside the workspace; reopen it before saving.");
                }

                moveReplacing(path3, path2);
            } finally {
                Files.deleteIfExists(path3);
            }

            return computeRevision(newBytes);
        } else {

            throw new IllegalArgumentException("The expected source revision is required.");
        }
    }

    public synchronized void approveHttpDomain(String scriptId, String domain) throws IOException {
        ScriptCandidate candidate =
                ScriptScanner.scanScripts(this.resolveScriptsDirectory()).stream()
                        .filter(scanned -> scanned.getId().equals(scriptId))
                        .findFirst()
                        .orElseThrow(
                                () -> {
                                    return new IllegalArgumentException(
                                            "Unknown script id: " + scriptId);
                                });
        String normalizedDomain = ScriptScanner.normalizeHttpDomain(domain);
        if (!candidate.getScriptPackage().requestedHttpDomains().contains(normalizedDomain)) {

            throw new IllegalArgumentException("The script manifest does not request this domain.");
        }

        HttpDomainApproval existingApproval = this.httpApprovals.get(scriptId);
        LinkedHashSet approvedDomains =
                new LinkedHashSet<>(
                        existingApproval != null
                                        && existingApproval
                                                .revision()
                                                .equals(candidate.getRevision())
                                ? existingApproval.domains()
                                : Set.of());
        int domainCount = approvedDomains.size();

        if (domainCount >= 32 && !approvedDomains.contains(normalizedDomain)) {

            throw new IllegalArgumentException(
                    "A script cannot approve more than 32 HTTP domains.");
        }

        HttpDomainApproval previousApproval = this.httpApprovals.get(scriptId);
        approvedDomains.add(normalizedDomain);
        this.httpApprovals.put(
                scriptId, new HttpDomainApproval(candidate.getRevision(), approvedDomains));

        try {
            this.savePermissions();
        } catch (IOException e) {
            if (previousApproval == null) {
                this.httpApprovals.remove(scriptId);
            } else {
                this.httpApprovals.put(scriptId, previousApproval);
            }

            throw e;
        }
    }

    private void collectClassLoaderPaths(Set<String> paths, ClassLoader startLoader) {

        for (ClassLoader current = startLoader; current != null; current = current.getParent()) {
            if (current instanceof URLClassLoader urlLoader) {
                for (URL url : urlLoader.getURLs()) {
                    String protocol = url.getProtocol();

                    if (protocol.equals("file")) {
                        try {
                            paths.add(Path.of(url.toURI()).toString());
                        } catch (Exception ignored) {
                        }
                    }
                }
            }
        }
    }

    public synchronized void forEachActiveScript(Consumer<Script> action) {
        this.dispatchDepth++;

        try {
            for (Script script : this.loadedScripts) {
                if (ScriptInternals.getScriptModule(script) != null && script.isEnabled()) {
                    try {
                        action.accept(script);
                    } catch (Throwable e) {

                        script.reportError("event callback", e);
                        if (ScriptApiCompatibility.isApiMismatch(e)
                                && ((ScriptModule) ScriptInternals.getScriptModule(script))
                                        .isEnabled()) {
                            ((ScriptModule) ScriptInternals.getScriptModule(script))
                                    .setEnabled(false);
                        }
                    }
                }
            }
        } finally {
            this.dispatchDepth--;
            if (this.dispatchDepth == 0 && this.reloadRequested) {
                this.reloadRequested = false;
                this.reload();
            }
        }
    }

    private Path resolveScriptsDirectory() throws IOException {
        Files.createDirectories(this.scriptsDirectory.toPath());
        Path path = this.scriptsDirectory.toPath().toAbsolutePath().normalize().toRealPath();
        Path path2 = mc.gameDirectory.toPath().toAbsolutePath().normalize().toRealPath();

        Path path3 = path2.resolve("astralis");

        if (!path.equals(path3.resolve("scripts").normalize())) {

            throw new IOException("Scripts directory cannot be a symbolic link or junction.");
        } else {
            return path;
        }
    }

    public synchronized TargetedLoadResult load(String scriptName) {
        String baseName;
        if (!mc.isSameThread()) {
            mc.execute(() -> this.load(scriptName));
            return TargetedLoadResult.QUEUED;
        }

        if (!this.initialized || this.loaderExecutor == null || this.loaderExecutor.isShutdown()) {
            return TargetedLoadResult.UNAVAILABLE;
        }

        if (this.loadInProgress) {
            return TargetedLoadResult.BUSY;
        }

        if (scriptName != null && !scriptName.isBlank()) {
            int nameLength = scriptName.length();

            if (nameLength <= 69 && !scriptName.codePoints().anyMatch(Character::isISOControl)) {
                label47:
                {
                    String strippedName = scriptName.strip();
                    if (strippedName.length() > 5) {
                        int suffixStart = strippedName.length() - 5;

                        if (strippedName.regionMatches(true, suffixStart, ".java", 0, 5)) {
                            baseName = strippedName.substring(0, strippedName.length() - 5);
                            break label47;
                        }
                    }

                    baseName = strippedName;
                }

                String targetName = baseName;
                if (!targetName.isBlank()) {
                    int targetLength = targetName.length();

                    if (targetLength <= 64) {
                        this.loadInProgress = true;
                        long generation = this.lifecycleGeneration;
                        List candidates = List.copyOf(this.loadedCandidates.values());

                        try {
                            this.loaderExecutor.execute(
                                    () ->
                                            this.runTargetedLoading(
                                                    targetName, candidates, generation));

                            Astralis.LOGGER.info(
                                    "[ScriptManager] Queued targeted loading for {}", targetName);
                            return TargetedLoadResult.QUEUED;
                        } catch (RejectedExecutionException e) {
                            this.loadInProgress = false;

                            Astralis.LOGGER.error(
                                    "[ScriptManager] Could not queue targeted script loading", e);
                            return TargetedLoadResult.UNAVAILABLE;
                        }
                    }
                }

                throw new IllegalArgumentException(
                        "Script name must contain 1 to 64 printable characters.");
            }
        }

        throw new IllegalArgumentException(
                "Script name must contain no more than 64 printable characters.");
    }

    public static void validateTypeReference(Type type, Set<String> scriptClasses) {
        int sort = type.getSort();

        if (sort == 9) {
            validateTypeReference(type.getElementType(), scriptClasses);
        } else {
            int secondSort = type.getSort();

            if (secondSort == 10) {
                validateClassReference(type.getInternalName(), scriptClasses);
            } else {
                int thirdSort = type.getSort();

                if (thirdSort == 11) {
                    validateTypeReference(type.getReturnType(), scriptClasses);

                    for (Type argumentType : type.getArgumentTypes()) {
                        validateTypeReference(argumentType, scriptClasses);
                    }
                }
            }
        }
    }

    private void applyTargetedStagingOutcome(String scriptName, TargetedStagingOutcome outcome) {
        this.requireClientThread();
        if (outcome.failure() != null) {
            String failureMessage;
            switch (outcome.failure()) {
                case NOT_FOUND:
                    failureMessage =
                            "§c[Scripts] §fNo script named §b" + scriptName + "§f was found.";
                    break;
                case SCAN_FAILED:
                    failureMessage = "§c[Scripts] §fCould not scan the scripts folder.";
                    break;
                case PREFLIGHT_FAILED:
                    failureMessage =
                            "§c[Scripts] §f"
                                    + scriptName
                                    + " did not pass compilation; the active script was left unchanged.";
                    break;
                default:
                    throw new MatchException(null, null);
            }

            ChatUtil.print(failureMessage);
        } else {
            ScriptCandidate candidate = outcome.incomingSources().scripts()[0].candidate();
            Script existingScript =
                    this.loadedScripts.stream()
                            .filter(
                                    loaded ->
                                            this.scriptIds
                                                            .getOrDefault(loaded, loaded.getName())
                                                            .equalsIgnoreCase(candidate.getId())
                                                    || loaded.getName()
                                                            .equalsIgnoreCase(candidate.getName()))
                            .findFirst()
                            .orElse(null);
            boolean wasEnabled =
                    existingScript != null
                                    && ScriptInternals.getScriptModule(existingScript) != null
                            ? ((ScriptModule) ScriptInternals.getScriptModule(existingScript))
                                    .isEnabled()
                            : this.enabledStates.getOrDefault(candidate.getId(), false);
            ConfigProfile activeConfig =
                    Astralis.getInstance().getConfigManager().getActiveConfig();
            JsonObject jsonObject = activeConfig != null ? activeConfig.getCurrentConfig() : null;
            if (existingScript != null) {
                String existingId =
                        this.scriptIds.getOrDefault(existingScript, existingScript.getName());
                this.enabledStates.put(existingId, wasEnabled);
                if (ScriptInternals.getScriptModule(existingScript) != null
                        && ((ScriptModule) ScriptInternals.getScriptModule(existingScript))
                                .isEnabled()) {
                    try {
                        ((ScriptModule) ScriptInternals.getScriptModule(existingScript))
                                .setEnabled(false);
                    } catch (Throwable e) {

                        existingScript.reportError("onDisable", e);
                    }
                }

                try {
                    existingScript.onUnload();
                } catch (Throwable e) {

                    existingScript.reportError("onUnload", e);
                }

                Astralis.getInstance()
                        .getModuleManager()
                        .unregister((ScriptModule) ScriptInternals.getScriptModule(existingScript));
                this.loadedScripts.remove(existingScript);
                this.scriptIds.remove(existingScript);
                this.loadedCandidates.remove(existingId);
                ScriptInternals.setScriptModule(existingScript, null);
                ScriptInternals.destroyRuntime(existingScript);
            }

            this.loadCompiledScript(outcome.incomingCompilation().scripts()[0], wasEnabled);
            boolean loadedNew =
                    this.loadedScripts.stream()
                            .anyMatch(
                                    loaded ->
                                            this.scriptIds
                                                    .getOrDefault(loaded, loaded.getName())
                                                    .equals(candidate.getId()));
            if (!loadedNew && outcome.rollbackCompilation() != null) {
                this.loadCompiledScript(outcome.rollbackCompilation().scripts()[0], wasEnabled);
            }

            boolean rolledBack =
                    !loadedNew
                            && outcome.rollbackSources() != null
                            && this.loadedScripts.stream()
                                    .anyMatch(
                                            loaded ->
                                                    this.scriptIds
                                                            .getOrDefault(loaded, loaded.getName())
                                                            .equals(
                                                                    outcome.rollbackSources()
                                                                            .scripts()[0]
                                                                            .candidate()
                                                                            .getId()));
            if (activeConfig != null) {
                activeConfig.loadConfig(jsonObject);
            }

            this.saveStates();
            if (mc.gui.screen() instanceof DropdownClickGuiScreen dropdownScreen) {
                dropdownScreen.refreshModules();
            } else if (mc.gui.screen() instanceof PanelClickGuiScreen panelScreen) {
                panelScreen.refreshModuleCards();
            }

            String message;
            if (loadedNew) {
                String scriptDisplayName = candidate.getName();

                message = "§a[Scripts] §fLoaded §b" + scriptDisplayName + "§f.";
            } else {
                String failedName = candidate.getName();
                String suffix;
                if (rolledBack) {

                    suffix = "§f; the previous version was restored.";
                } else {

                    suffix = "§f.";
                }

                String messageSuffix = suffix;
                String failedScriptName = failedName;
                message = "§c[Scripts] §fCould not load §b" + failedScriptName + messageSuffix;
            }

            ChatUtil.print(message);
        }
    }

    private String verifyEntrypointClass(Path path, String entrypoint) {

        String relativePath = entrypoint.replace('.', File.separatorChar);

        Path path2 = path.resolve(relativePath + ".class").normalize();
        if (path2.startsWith(path) && Files.isRegularFile(path2, LinkOption.NOFOLLOW_LINKS)) {
            return entrypoint;
        }

        throw new IllegalArgumentException(
                "Compilation did not produce the declared entrypoint class.");
    }

    private void deleteRecursively(Path path) throws IOException {
        try (Stream<Path> walk = Files.walk(path)) {
            for (Path path2 : walk.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path2);
            }
        }
    }

    private void runTargetedLoading(
            String scriptName, List<ScriptCandidate> knownCandidates, long generation) {
        TargetedStagingOutcome outcome = null;
        boolean pendingCleanup = false /* VF: Semaphore variable */;

        label430:
        {
            label477:
            {
                label478:
                {
                    try {
                        pendingCleanup = true;
                        outcome = this.stageTargetedScript(scriptName, knownCandidates);
                        if (this.initialized) {
                            if (generation == this.lifecycleGeneration) {
                                if (!Thread.currentThread().isInterrupted()) {
                                    TargetedStagingOutcome finalOutcome = outcome;
                                    CompletableFuture applyFuture = new CompletableFuture();
                                    mc.execute(
                                            () -> {
                                                if (this.initialized
                                                        && generation == this.lifecycleGeneration) {
                                                    try {
                                                        this.applyTargetedStagingOutcome(
                                                                scriptName, finalOutcome);
                                                        applyFuture.complete(true);
                                                    } catch (Throwable e) {
                                                        applyFuture.completeExceptionally(e);
                                                    }
                                                } else {
                                                    applyFuture.complete(false);
                                                }
                                            });
                                    applyFuture.get();
                                    pendingCleanup = false;
                                    break label430;
                                }

                                pendingCleanup = false;
                            } else {
                                pendingCleanup = false;
                            }
                        } else {
                            pendingCleanup = false;
                        }
                        break label477;
                    } catch (InterruptedException ignored) {
                        Thread.currentThread().interrupt();
                        pendingCleanup = false;
                        break label478;
                    } catch (ExecutionException e) {

                        Astralis.LOGGER.error(
                                "[ScriptManager] Could not apply targeted script loading",
                                e.getCause());
                        pendingCleanup = false;
                    } finally {
                        if (pendingCleanup) {
                            if (outcome != null) {
                                for (Path path :
                                        new Path[] {
                                            outcome.incomingCompilation() != null
                                                    ? outcome.incomingCompilation().directory()
                                                    : null,
                                            outcome.rollbackCompilation() != null
                                                    ? outcome.rollbackCompilation().directory()
                                                    : null,
                                            outcome.incomingSources() != null
                                                    ? outcome.incomingSources().directory()
                                                    : null,
                                            outcome.rollbackSources() != null
                                                    ? outcome.rollbackSources().directory()
                                                    : null
                                        }) {
                                    if (path != null) {
                                        try {
                                            this.deleteRecursively(path);
                                        } catch (IOException e) {

                                            Astralis.LOGGER.warn(
                                                    "[ScriptManager] Could not remove targeted staging directory",
                                                    e);
                                        }
                                    }
                                }
                            }

                            if (this.initialized && generation == this.lifecycleGeneration) {
                                mc.execute(() -> this.finishLoading(generation));
                            }
                        }
                    }

                    if (outcome != null) {
                        for (Path path2 :
                                new Path[] {
                                    outcome.incomingCompilation() != null
                                            ? outcome.incomingCompilation().directory()
                                            : null,
                                    outcome.rollbackCompilation() != null
                                            ? outcome.rollbackCompilation().directory()
                                            : null,
                                    outcome.incomingSources() != null
                                            ? outcome.incomingSources().directory()
                                            : null,
                                    outcome.rollbackSources() != null
                                            ? outcome.rollbackSources().directory()
                                            : null
                                }) {
                            if (path2 != null) {
                                try {
                                    this.deleteRecursively(path2);
                                } catch (IOException e) {

                                    Astralis.LOGGER.warn(
                                            "[ScriptManager] Could not remove targeted staging directory",
                                            e);
                                }
                            }
                        }
                    }

                    if (this.initialized && generation == this.lifecycleGeneration) {
                        mc.execute(() -> this.finishLoading(generation));
                    }

                    return;
                }

                if (outcome != null) {
                    for (Path path3 :
                            new Path[] {
                                outcome.incomingCompilation() != null
                                        ? outcome.incomingCompilation().directory()
                                        : null,
                                outcome.rollbackCompilation() != null
                                        ? outcome.rollbackCompilation().directory()
                                        : null,
                                outcome.incomingSources() != null
                                        ? outcome.incomingSources().directory()
                                        : null,
                                outcome.rollbackSources() != null
                                        ? outcome.rollbackSources().directory()
                                        : null
                            }) {
                        if (path3 != null) {
                            try {
                                this.deleteRecursively(path3);
                            } catch (IOException e) {

                                Astralis.LOGGER.warn(
                                        "[ScriptManager] Could not remove targeted staging directory",
                                        e);
                            }
                        }
                    }
                }

                if (this.initialized && generation == this.lifecycleGeneration) {
                    mc.execute(() -> this.finishLoading(generation));
                }

                return;
            }

            if (outcome != null) {
                for (Path path4 :
                        new Path[] {
                            outcome.incomingCompilation() != null
                                    ? outcome.incomingCompilation().directory()
                                    : null,
                            outcome.rollbackCompilation() != null
                                    ? outcome.rollbackCompilation().directory()
                                    : null,
                            outcome.incomingSources() != null
                                    ? outcome.incomingSources().directory()
                                    : null,
                            outcome.rollbackSources() != null
                                    ? outcome.rollbackSources().directory()
                                    : null
                        }) {
                    if (path4 != null) {
                        try {
                            this.deleteRecursively(path4);
                        } catch (IOException e) {

                            Astralis.LOGGER.warn(
                                    "[ScriptManager] Could not remove targeted staging directory",
                                    e);
                        }
                    }
                }
            }

            if (this.initialized && generation == this.lifecycleGeneration) {
                mc.execute(() -> this.finishLoading(generation));
            }

            return;
        }

        if (outcome != null) {
            for (Path path5 :
                    new Path[] {
                        outcome.incomingCompilation() != null
                                ? outcome.incomingCompilation().directory()
                                : null,
                        outcome.rollbackCompilation() != null
                                ? outcome.rollbackCompilation().directory()
                                : null,
                        outcome.incomingSources() != null
                                ? outcome.incomingSources().directory()
                                : null,
                        outcome.rollbackSources() != null
                                ? outcome.rollbackSources().directory()
                                : null
                    }) {
                if (path5 != null) {
                    try {
                        this.deleteRecursively(path5);
                    } catch (IOException e) {

                        Astralis.LOGGER.warn(
                                "[ScriptManager] Could not remove targeted staging directory", e);
                    }
                }
            }
        }

        if (this.initialized && generation == this.lifecycleGeneration) {
            mc.execute(() -> this.finishLoading(generation));
        }
    }

    private void loadCompiledScript(CompiledScript compiledScript, boolean enabled) {

        try {
            this.loadScript(compiledScript.script(), enabled, compiledScript.outputDirectory());
        } catch (Throwable e) {
            String scriptName = compiledScript.script().candidate().getName();

            this.reportFailureWithCause(
                    scriptName, "An unexpected error occurred while loading the script.", e);
        }
    }

    private String getCompileClasspath() {
        if (this.compileClasspath != null) {
            return this.compileClasspath;
        }

        synchronized (this) {
            if (this.compileClasspath == null) {
                this.compileClasspath = this.buildCompileClasspath();
            }

            return this.compileClasspath;
        }
    }

    public static void validateClassReference(String internalName, Set<String> scriptClasses) {

        if (internalName == null
                || !internalName.startsWith("[") && !internalName.startsWith("(")) {
            if (internalName != null && internalName.length() == 1) {

                if ("VZCBSIFJD".contains(internalName)) {
                    return;
                }
            }

            if (internalName != null && !scriptClasses.contains(internalName)) {

                if (!internalName.equals("com/astralis/scripting/Script")
                        && !internalName.equals(Type.getInternalName(Category.class))) {

                    if (!internalName.startsWith("com/astralis/scripting/api/")) {

                        if (!internalName.startsWith("java/math/")) {

                            if (!internalName.startsWith("java/time/")) {

                                if (!internalName.startsWith("java/text/")) {

                                    if (!internalName.startsWith("java/util/function/")) {

                                        if (!internalName.startsWith("java/util/random/")) {

                                            if (!internalName.startsWith("java/util/stream/")) {

                                                if (!internalName.startsWith("java/util/regex/")) {

                                                    if (internalName.matches("java/util/[^/]+")) {

                                                        if (!internalName.startsWith(
                                                                "java/util/concurrent/")) {

                                                            if (!internalName.startsWith(
                                                                    "java/util/logging/")) {

                                                                if (!internalName.equals(
                                                                        "java/util/ServiceLoader")) {

                                                                    if (!internalName.equals(
                                                                            "java/util/ResourceBundle")) {

                                                                        if (!internalName.equals(
                                                                                "java/util/Formatter")) {

                                                                            if (!internalName
                                                                                    .equals(
                                                                                            "java/util/Timer")) {

                                                                                if (!internalName
                                                                                        .equals(
                                                                                                "java/util/TimerTask")) {
                                                                                    return;
                                                                                }
                                                                            }
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }

                                                    if (internalName.matches("java/lang/[^/]+")) {

                                                        String[] restrictedLangClasses =
                                                                new String[20];

                                                        restrictedLangClasses[0] =
                                                                "java/lang/Class";

                                                        restrictedLangClasses[1] =
                                                                "java/lang/ClassLoader";

                                                        restrictedLangClasses[2] = "java/lang/IO";

                                                        restrictedLangClasses[3] =
                                                                "java/lang/Module";

                                                        restrictedLangClasses[4] =
                                                                "java/lang/ModuleLayer";

                                                        restrictedLangClasses[5] =
                                                                "java/lang/Package";

                                                        restrictedLangClasses[6] =
                                                                "java/lang/Process";

                                                        restrictedLangClasses[7] =
                                                                "java/lang/ProcessBuilder";

                                                        restrictedLangClasses[8] =
                                                                "java/lang/ProcessHandle";

                                                        restrictedLangClasses[9] =
                                                                "java/lang/ProcessHandle$Info";

                                                        restrictedLangClasses[10] =
                                                                "java/lang/Runtime";

                                                        restrictedLangClasses[11] =
                                                                "java/lang/Runtime$Version";

                                                        restrictedLangClasses[12] =
                                                                "java/lang/SecurityManager";

                                                        restrictedLangClasses[13] =
                                                                "java/lang/StackTraceElement";

                                                        restrictedLangClasses[14] =
                                                                "java/lang/StackWalker";

                                                        restrictedLangClasses[15] =
                                                                "java/lang/System";

                                                        restrictedLangClasses[16] =
                                                                "java/lang/Thread";

                                                        restrictedLangClasses[17] =
                                                                "java/lang/ThreadGroup";

                                                        restrictedLangClasses[18] =
                                                                "java/lang/ThreadLocal";

                                                        restrictedLangClasses[19] =
                                                                "java/lang/InheritableThreadLocal";
                                                        if (!Set.of(restrictedLangClasses)
                                                                .contains(internalName)) {
                                                            return;
                                                        }
                                                    }

                                                    String className =
                                                            internalName.replace('/', '.');

                                                    throw new IllegalArgumentException(
                                                            "Script bytecode references a restricted class: "
                                                                    + className);
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
        } else {
            validateTypeReference(Type.getType(internalName), scriptClasses);
        }
    }

    public synchronized List<ScriptSource> getSources() {

        try {
            return ScriptScanner.scanScripts(this.resolveScriptsDirectory()).stream()
                    .map(
                            candidate -> {
                                Script loadedScript =
                                        this.loadedScripts.stream()
                                                .filter(
                                                        loaded ->
                                                                this.scriptIds
                                                                        .getOrDefault(
                                                                                loaded,
                                                                                loaded.getName())
                                                                        .equals(candidate.getId()))
                                                .findFirst()
                                                .orElse(null);
                                return new ScriptSource(
                                        candidate.getName(),
                                        loadedScript != null,
                                        loadedScript != null && loadedScript.isEnabled(),
                                        candidate.getRevision());
                            })
                    .toList();
        } catch (IOException e) {

            Astralis.LOGGER.warn("[ScriptManager] Could not scan script sources", e);
            return List.of();
        }
    }

    private void queueScriptMessage(
            String scriptName, String message, String copyText, ChatFormatting chatFormatting) {
        this.pendingMessages.add(
                new PendingScriptMessage(scriptName, message, copyText, chatFormatting));
        if (mc.isSameThread()) {
            this.flushPendingMessages();
        } else {
            mc.execute(this::flushPendingMessages);
        }
    }

    private void runLoading(StagingKind kind, List<ScriptCandidate> candidates, long generation) {

        try {
            StagingKind currentKind = kind;
            List currentCandidates = candidates;

            while (this.initialized
                    && generation == this.lifecycleGeneration
                    && !Thread.currentThread().isInterrupted()) {
                StagingOutcome outcome = this.stageScripts(currentKind, currentCandidates);
                if (!this.initialized
                        || generation != this.lifecycleGeneration
                        || Thread.currentThread().isInterrupted()) {
                    this.deleteStagingDirectories(outcome);
                    return;
                }

                CompletableFuture applyFuture = new CompletableFuture();
                StagingKind applyKind = currentKind;
                mc.execute(
                        () -> {
                            if (this.initialized && generation == this.lifecycleGeneration) {
                                try {
                                    this.applyStagingOutcome(applyKind, outcome);
                                    applyFuture.complete(true);
                                } catch (Throwable e) {
                                    applyFuture.completeExceptionally(e);
                                }
                            } else {
                                applyFuture.complete(false);
                            }
                        });

                try {
                    if (!(Boolean) applyFuture.get()) {
                        this.deleteStagingDirectories(outcome);
                        return;
                    }
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                    this.deleteStagingDirectories(outcome);
                    return;
                } catch (ExecutionException e) {

                    Astralis.LOGGER.error(
                            "[ScriptManager] Could not apply staged scripts", e.getCause());
                    this.deleteStagingDirectories(outcome);
                    return;
                }

                this.deleteStagingDirectories(outcome);
                CompletableFuture nextFuture = new CompletableFuture();
                mc.execute(
                        () -> {
                            synchronized (this) {
                                if (this.initialized && generation == this.lifecycleGeneration) {
                                    if (this.reloadRequested) {
                                        this.reloadRequested = false;
                                        nextFuture.complete(
                                                List.copyOf(this.loadedCandidates.values()));
                                    } else {
                                        this.loadInProgress = false;
                                        nextFuture.complete(null);
                                    }
                                } else {
                                    nextFuture.complete(null);
                                }
                            }
                        });

                try {
                    currentCandidates = (List) nextFuture.get();
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                    return;
                } catch (ExecutionException e) {

                    Astralis.LOGGER.error(
                            "[ScriptManager] Could not continue script loading", e.getCause());
                    return;
                }

                if (currentCandidates == null) {
                    return;
                }

                currentKind = StagingKind.RELOAD;
            }
        } finally {
            if (this.initialized && generation == this.lifecycleGeneration) {
                mc.execute(() -> this.finishLoading(generation));
            }
        }
    }

    public static void validateCompiledClasses(Path path) throws IOException {

        List<Path> classFiles;
        try (Stream<Path> walk = Files.walk(path)) {
            classFiles =
                    walk.filter(
                                    path2 -> {
                                        String fileName = path2.getFileName().toString();

                                        return fileName.endsWith(".class");
                                    })
                            .toList();
        }

        int classFileCount = classFiles.size();

        if (classFileCount > 256) {

            throw new IllegalArgumentException(
                    "A script cannot produce more than 256 class files.");
        }

        long totalBytes = 0L;

        for (Path path3 : classFiles) {
            long fileSize = Files.size(path3);

            if (fileSize > 2097152L) {

                throw new IllegalArgumentException("A compiled script class cannot exceed 2 MiB.");
            }

            totalBytes += fileSize;

            if (totalBytes > 16777216L) {

                throw new IllegalArgumentException("Compiled script output cannot exceed 16 MiB.");
            }
        }

        LinkedHashSet scriptClasses = new LinkedHashSet();

        for (Path path4 : classFiles) {
            ClassReader classReader = new ClassReader(Files.readAllBytes(path4));
            String className = classReader.getClassName();

            if (!className.startsWith("pub/astralis/")) {

                if (!className.startsWith("net/minecraft/")) {

                    if (!className.startsWith("injection/")) {

                        if (!className.startsWith("dev/sapphire/")) {

                            if (!className.startsWith("club/serenityutils/")) {

                                if (!className.startsWith("java/")) {

                                    if (!className.startsWith("javax/")) {

                                        if (!className.startsWith("jdk/")) {

                                            if (!className.startsWith("sun/")) {
                                                try {

                                                    Class.forName(
                                                            className.replace('/', '.'),
                                                            false,
                                                            Script.class.getClassLoader());

                                                    throw new IllegalArgumentException(
                                                            "A script class conflicts with the client classpath.");
                                                } catch (ClassNotFoundException ignored) {
                                                    scriptClasses.add(className);
                                                    continue;
                                                } catch (LinkageError e) {

                                                    throw new IllegalArgumentException(
                                                            "A script class conflicts with the client classpath.",
                                                            e);
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

            throw new IllegalArgumentException(
                    "Scripts cannot define classes in reserved packages.");
        }

        for (Path path5 : classFiles) {
            ClassReader constantPoolReader = new ClassReader(Files.readAllBytes(path5));
            char[] buffer = new char[constantPoolReader.getMaxStringLength()];

            for (int index = 1; index < constantPoolReader.getItemCount(); index++) {
                int offset = constantPoolReader.getItem(index);
                if (offset > 0) {
                    int tag = constantPoolReader.readByte(offset - 1);

                    if (tag == 7) {
                        validateConstantPoolClass(
                                constantPoolReader.readUTF8(offset, buffer), scriptClasses);
                    }
                }
            }

            ScriptClassValidator validator = new ScriptClassValidator(589824, scriptClasses);
            constantPoolReader.accept(validator, 6);
        }
    }

    public synchronized void createScript(String scriptName, String source, String projectJson)
            throws IOException {
        String validatedName = validateScriptName(scriptName);
        this.createSource(validatedName, source);

        try {
            this.writeEasyModeProject(validatedName, projectJson);
        } catch (IOException | RuntimeException e) {
            Files.deleteIfExists(
                    this.resolveSourcePath(validatedName, this.resolveScriptsDirectory()));
            throw e;
        }
    }

    private static byte[] encodeSource(String source) throws IOException {
        if (source == null) {

            throw new IllegalArgumentException("Script source is required.");
        } else {
            ByteBuffer encoded =
                    StandardCharsets.UTF_8
                            .newEncoder()
                            .onMalformedInput(CodingErrorAction.REPORT)
                            .onUnmappableCharacter(CodingErrorAction.REPORT)
                            .encode(CharBuffer.wrap(source));
            int byteCount = encoded.remaining();

            if (byteCount > 1048576) {

                throw new IOException("Script source cannot exceed 1 MiB.");
            } else {
                byte[] bytes = new byte[encoded.remaining()];
                encoded.get(bytes);
                return bytes;
            }
        }
    }

    private void writeEasyModeProject(String scriptName, String projectJson) throws IOException {
        byte[] projectBytes = encodeSource(projectJson);
        Path path = this.resolveScriptsDirectory();
        if (!Files.isRegularFile(
                this.resolveSourcePath(scriptName, path), LinkOption.NOFOLLOW_LINKS)) {

            throw new IOException("Easy Mode projects require a matching Java source.");
        }

        Path path2 = this.resolveEasyModeProjectPath(scriptName, path);
        Path path3 = writeTempFile(path, projectBytes);

        try {
            moveReplacing(path3, path2);
        } finally {
            Files.deleteIfExists(path3);
        }
    }

    private void loadScript(StagedScript stagedScript, boolean enableOnLoad, Path path) {
        this.requireClientThread();
        ScriptCandidate candidate = stagedScript.candidate();
        if (!this.cacheDirectorySafe) {
            String scriptName = candidate.getName();

            this.reportFailure(scriptName, "The compilation cache path is unsafe or unavailable.");
        } else if (!ScriptApiCompatibility.isScriptApiCompatible()) {
            String scriptName = candidate.getName();

            this.reportApiMismatch(
                    scriptName,
                    new NoSuchFieldError(
                            "The running Script API is missing draggables or screens."));
        } else if (this.loadedScripts.stream()
                .anyMatch(loaded -> loaded.getName().equalsIgnoreCase(candidate.getName()))) {
            String scriptName = candidate.getName();

            this.reportFailure(scriptName, "A script with this name is already loaded.");
        } else {
            Path path2 = this.cacheDirectory.resolve(candidate.getId()).normalize();
            if (!path2.equals(this.cacheDirectory) && path2.startsWith(this.cacheDirectory)) {
                try {
                    if (Files.exists(path2)) {
                        if (!path2.toRealPath()
                                .getParent()
                                .equals(this.cacheDirectory.toRealPath())) {
                            String scriptName = candidate.getName();

                            this.reportFailure(
                                    scriptName,
                                    "The script cache path resolves outside the cache directory.");
                            return;
                        }

                        this.deleteRecursively(path2);
                    }

                    try {
                        Files.move(path, path2, StandardCopyOption.ATOMIC_MOVE);
                    } catch (AtomicMoveNotSupportedException ignored) {
                        Files.move(path, path2);
                    }
                } catch (IOException e) {
                    String scriptName = candidate.getName();

                    this.reportFailureWithCause(
                            scriptName, "Could not promote the compiled script cache.", e);
                    return;
                }

                String entrypointClass;
                try {
                    validateCompiledClasses(path2);
                    entrypointClass = this.verifyEntrypointClass(path2, candidate.getEntrypoint());
                } catch (IOException e) {
                    String scriptName = candidate.getName();

                    this.reportFailureWithCause(
                            scriptName, "Could not locate the compiled script class.", e);
                    this.discardFailedOutput(path2, candidate.getName());
                    return;
                } catch (RuntimeException e) {
                    this.reportFailureWithCause(candidate.getName(), e.getMessage(), e);
                    this.discardFailedOutput(path2, candidate.getName());
                    return;
                }

                URLClassLoader classLoader;
                try {
                    classLoader =
                            new URLClassLoader(
                                    new URL[] {path2.toUri().toURL()},
                                    Script.class.getClassLoader());
                } catch (IOException e) {
                    String scriptName = candidate.getName();

                    this.reportFailureWithCause(
                            scriptName, "Could not create the script classloader.", e);
                    this.discardFailedOutput(path2, candidate.getName());
                    return;
                }

                Script script = null;
                ScriptModule scriptModule = null;
                boolean loadInvoked = false;
                boolean moduleRegistered = false;
                boolean scriptListed = false;

                try {
                    Class scriptClass = Class.forName(entrypointClass, false, classLoader);
                    this.validateScriptClass(scriptClass, classLoader);
                    script = (Script) scriptClass.getConstructor().newInstance();
                    ScriptInternals.setScriptPackage(script, candidate.getScriptPackage());
                    ScriptInternals.setAllowedHttpDomains(
                            script,
                            candidate.getScriptPackage().requestedHttpDomains().stream()
                                    .filter(this.approvedDomainsFor(candidate)::contains)
                                    .collect(Collectors.toUnmodifiableSet()));
                    ScriptInternals.activateRuntime(script, candidate.getName(), classLoader);
                    scriptModule = new ScriptModule(script);
                    ScriptInternals.setScriptModule(script, scriptModule);
                    ScriptInternals.invokeOnLoad(script);
                    loadInvoked = true;
                    ScriptInternals.applyModuleProperties(script, scriptModule);
                    Astralis.getInstance().getModuleManager().register(scriptModule);
                    moduleRegistered = true;
                    this.loadedScripts.add(script);
                    scriptListed = true;
                    this.scriptIds.put(script, candidate.getId());
                    this.loadedCandidates.put(candidate.getId(), candidate);
                    if (enableOnLoad) {
                        scriptModule.setEnabled(true);
                    }

                    Astralis.LOGGER.info(
                            "[ScriptManager] Loaded Java script: {} ({})",
                            candidate.getName(),
                            candidate.getId());
                    if (mc.gui.screen() instanceof DropdownClickGuiScreen dropdownScreen) {
                        dropdownScreen.refreshModules();
                    } else if (mc.gui.screen() instanceof PanelClickGuiScreen panelScreen) {
                        panelScreen.refreshModuleCards();
                    }
                } catch (Throwable e) {
                    if (scriptModule != null && scriptModule.isEnabled()) {
                        try {
                            scriptModule.setEnabled(false);
                        } catch (Throwable disableFailure) {
                            e.addSuppressed(disableFailure);
                        }
                    }

                    if (loadInvoked && script != null) {
                        try {
                            ScriptInternals.invokeOnUnload(script);
                        } catch (Throwable unloadFailure) {
                            e.addSuppressed(unloadFailure);
                        }
                    }

                    if (moduleRegistered) {
                        Astralis.getInstance().getModuleManager().unregister(scriptModule);
                    }

                    if (scriptListed) {
                        this.loadedScripts.remove(script);
                    }

                    this.scriptIds.remove(script);
                    this.loadedCandidates.remove(candidate.getId());
                    if (script != null) {
                        ScriptInternals.setScriptModule(script, null);
                        ScriptInternals.destroyRuntime(script);
                    }

                    try {
                        classLoader.close();
                    } catch (IOException closeFailure) {
                        e.addSuppressed(closeFailure);
                    }

                    if (ScriptApiCompatibility.isApiMismatch(e)) {
                        this.reportApiMismatch(candidate.getName(), e);
                    } else {
                        String scriptName = candidate.getName();

                        this.reportFailureWithCause(
                                scriptName, "Could not initialize the script.", e);
                    }

                    this.discardFailedOutput(path2, candidate.getName());
                }
            } else {
                String scriptName = candidate.getName();

                this.reportFailure(scriptName, "Invalid script id.");
            }
        }
    }

    public synchronized void loadStates() {
        long fileSize;
        this.enabledStates.clear();
        if (this.statesFile.exists()) {
            try {
                fileSize = Files.size(this.statesFile.toPath());

                if (fileSize > 1048576L) {

                    throw new IOException("Script state file exceeds 1 MiB.");
                }

                JsonObject jsonObject =
                        JsonParser.parseString(
                                        Files.readString(
                                                this.statesFile.toPath(), StandardCharsets.UTF_8))
                                .getAsJsonObject();
                int entryCount = jsonObject.size();

                if (entryCount > 4096) {

                    throw new IOException("Script state file contains too many entries.");
                }

                for (Entry entry : jsonObject.entrySet()) {
                    if (((JsonElement) entry.getValue()).isJsonPrimitive()
                            && ((JsonElement) entry.getValue()).getAsJsonPrimitive().isBoolean()) {
                        this.enabledStates.put(
                                (String) entry.getKey(),
                                ((JsonElement) entry.getValue()).getAsBoolean());
                    }
                }
            } catch (IOException | RuntimeException e) {
                this.enabledStates.clear();

                Astralis.LOGGER.error("[ScriptManager] Failed to load script states", e);

                try {
                    Path path = this.statesFile.toPath();
                    Path path2 = this.statesFile.toPath();
                    String statesFileName = this.statesFile.getName();
                    long timestamp = System.currentTimeMillis();

                    Files.move(
                            path, path2.resolveSibling(statesFileName + ".corrupt-" + timestamp));
                } catch (IOException renameFailure) {
                    e.addSuppressed(renameFailure);
                }
            }
        } else if (this.legacyStatesFile.exists()) {
            try {
                fileSize = Files.size(this.legacyStatesFile.toPath());

                if (fileSize > 1048576L) {

                    throw new IOException("Legacy script state file exceeds 1 MiB.");
                }

                for (String line :
                        Files.readAllLines(
                                this.legacyStatesFile.toPath(), StandardCharsets.UTF_8)) {
                    if (line.contains(":")) {
                        String[] parts = line.split(":", 2);
                        this.enabledStates.put(parts[0], Boolean.parseBoolean(parts[1]));
                    }
                }

                this.saveStates();

                Astralis.LOGGER.info("[ScriptManager] Migrated script states to states.json");
            } catch (IOException e) {

                Astralis.LOGGER.error("[ScriptManager] Failed to migrate legacy script states", e);
            }
        }
    }

    public synchronized ReloadResult reload() {
        if (!mc.isSameThread()) {
            mc.execute(this::reload);
            return ReloadResult.DEFERRED;
        } else if (this.dispatchDepth > 0) {
            this.reloadRequested = true;
            return ReloadResult.DEFERRED;
        } else {
            return this.scheduleLoading(StagingKind.RELOAD);
        }
    }

    private static Path writeTempFile(Path path, byte[] content) throws IOException {

        Path path2 = Files.createTempFile(path, "script-", ".tmp");

        try {
            Files.write(path2, content, StandardOpenOption.TRUNCATE_EXISTING);
            return path2;
        } catch (IOException e) {
            Files.deleteIfExists(path2);
            throw e;
        }
    }

    private void requireClientThread() {
        if (!mc.isSameThread()) {

            throw new IllegalStateException(
                    "Script lifecycle mutation must run on the client thread.");
        }
    }

    private boolean compileScriptSources(File[] sourceFiles, Path path, String scriptName) {

        String javaHome = System.getProperty("java.home");
        String[] javacPathParts = new String[2];

        javacPathParts[0] = "bin";

        String osName = System.getProperty("os.name").toLowerCase(Locale.ROOT);

        String javacName;
        if (osName.startsWith("win")) {

            javacName = "javac.exe";
        } else {

            javacName = "javac";
        }

        javacPathParts[1] = javacName;
        Path path2 = Path.of(javaHome, javacPathParts);
        if (!Files.isRegularFile(path2)) {

            this.reportFailure(
                    scriptName,
                    "The Java compiler is unavailable. Run Astralis with a full Java 25 JDK.");
            return false;
        }

        Path path3 = null;
        Process process = null;
        boolean pendingCleanup = false /* VF: Semaphore variable */;

        boolean errorResult;
        label265:
        {
            label266:
            {
                boolean abortedResult;
                label267:
                {
                    label268:
                    {
                        label269:
                        {
                            boolean succeeded;
                            try {
                                pendingCleanup = true;

                                path3 = Files.createTempFile(path, "javac-", ".args");
                                StringBuilder argumentsText = new StringBuilder();

                                String encodingName = StandardCharsets.UTF_8.name();

                                String classpath = this.getCompileClasspath();

                                for (String argument :
                                        List.of(
                                                "--release",
                                                "25",
                                                "-proc:none",
                                                "-encoding",
                                                encodingName,
                                                "-classpath",
                                                classpath,
                                                "-d",
                                                path.toString())) {

                                    StringBuilder openedQuote = argumentsText.append('"');
                                    String escapedBackslashes = argument.replace("\\", "\\\\");
                                    StringBuilder quotedArgument =
                                            openedQuote.append(
                                                    escapedBackslashes.replace("\"", "\\\""));

                                    quotedArgument.append('"').append(System.lineSeparator());
                                }

                                for (File file2 : sourceFiles) {

                                    StringBuilder openedFileQuote = argumentsText.append('"');
                                    String absolutePath = file2.getAbsolutePath();
                                    String escapedPath = absolutePath.replace("\\", "\\\\");
                                    StringBuilder quotedFile =
                                            openedFileQuote.append(
                                                    escapedPath.replace("\"", "\\\""));

                                    quotedFile.append('"').append(System.lineSeparator());
                                }

                                Files.writeString(path3, argumentsText, StandardCharsets.UTF_8);
                                String[] command =
                                        new String[] {path2.toString(), null, null, null};

                                command[1] = "-J-Xmx256m";

                                command[2] = "-J-XX:MaxMetaspaceSize=128m";
                                command[3] = "@" + path3.toAbsolutePath();
                                process =
                                        new ProcessBuilder(command)
                                                .redirectErrorStream(true)
                                                .start();
                                Process compilerProcess = process;
                                ByteArrayOutputStream outputBuffer = new ByteArrayOutputStream();
                                AtomicBoolean truncated = new AtomicBoolean();
                                AtomicReference readFailure = new AtomicReference();
                                OfVirtual threadBuilder = Thread.ofVirtual();

                                Thread readerThread =
                                        threadBuilder
                                                .name("Astralis script compiler output")
                                                .start(
                                                        () -> {
                                                            try (InputStream stream =
                                                                    compilerProcess
                                                                            .getInputStream()) {

                                                                byte[] chunk = new byte[8192];

                                                                int bytesRead;
                                                                while ((bytesRead =
                                                                                stream.read(chunk))
                                                                        >= 0) {

                                                                    int remaining =
                                                                            1048576
                                                                                    - outputBuffer
                                                                                            .size();
                                                                    if (remaining > 0) {
                                                                        outputBuffer.write(
                                                                                chunk,
                                                                                0,
                                                                                Math.min(
                                                                                        bytesRead,
                                                                                        remaining));
                                                                    }

                                                                    if (bytesRead > remaining) {
                                                                        truncated.set(true);
                                                                    }
                                                                }
                                                            } catch (IOException e) {
                                                                readFailure.set(e);
                                                            }
                                                        });

                                if (!process.waitFor(30L, TimeUnit.SECONDS)) {
                                    process.destroyForcibly();

                                    process.waitFor(5L, TimeUnit.SECONDS);

                                    readerThread.join(5000L);

                                    this.reportFailure(
                                            scriptName,
                                            "Compilation exceeded the 30 second limit.");
                                    abortedResult = false;
                                    pendingCleanup = false;
                                    break label267;
                                }

                                readerThread.join(5000L);
                                if (readerThread.isAlive()) {
                                    readerThread.interrupt();

                                    this.reportFailure(
                                            scriptName,
                                            "Compiler diagnostics did not finish within the bounded shutdown period.");
                                    abortedResult = false;
                                    pendingCleanup = false;
                                    break label268;
                                }

                                if (readFailure.get() != null) {

                                    this.reportFailureWithCause(
                                            scriptName,
                                            "Could not read compiler diagnostics.",
                                            (Throwable) readFailure.get());
                                    abortedResult = false;
                                    pendingCleanup = false;
                                    break label269;
                                }

                                String output = outputBuffer.toString(StandardCharsets.UTF_8);
                                if (truncated.get()) {
                                    String lineSeparator = System.lineSeparator();

                                    output =
                                            output
                                                    + lineSeparator
                                                    + "Compiler diagnostics were truncated at 1 MiB.";
                                }

                                if (!output.isBlank()) {
                                    String diagnostics = output.strip();
                                    if (process.exitValue() == 0) {

                                        Astralis.LOGGER.warn(
                                                "[Script:{}] {}", scriptName, diagnostics);
                                    } else {

                                        Astralis.LOGGER.error(
                                                "[Script:{}] {}", scriptName, diagnostics);
                                    }

                                    this.reportCompilerOutput(
                                            scriptName, diagnostics, process.exitValue() == 0);
                                } else if (process.exitValue() != 0) {

                                    this.reportFailure(
                                            scriptName,
                                            "Compilation failed; the compiler did not provide diagnostics.");
                                }

                                succeeded = process.exitValue() == 0;
                                pendingCleanup = false;
                            } catch (InterruptedException ignored) {
                                Thread.currentThread().interrupt();
                                process.destroyForcibly();

                                Astralis.LOGGER.debug(
                                        "[Script:{}] Compilation cancelled", scriptName);
                                errorResult = false;
                                pendingCleanup = false;
                                break label266;
                            } catch (IOException | RuntimeException e) {
                                if (process != null) {
                                    process.destroyForcibly();
                                }

                                this.reportFailureWithCause(scriptName, "Compilation failed.", e);
                                errorResult = false;
                                pendingCleanup = false;
                                break label265;
                            } finally {
                                if (pendingCleanup) {
                                    try {
                                        if (path3 != null) {
                                            Files.deleteIfExists(path3);
                                        }
                                    } catch (IOException e) {

                                        Astralis.LOGGER.warn(
                                                "[Script:{}] Could not remove compiler temporary files",
                                                scriptName,
                                                e);
                                    }
                                }
                            }

                            try {
                                if (path3 != null) {
                                    Files.deleteIfExists(path3);
                                }
                            } catch (IOException e) {

                                Astralis.LOGGER.warn(
                                        "[Script:{}] Could not remove compiler temporary files",
                                        scriptName,
                                        e);
                            }

                            return succeeded;
                        }

                        try {
                            if (path3 != null) {
                                Files.deleteIfExists(path3);
                            }
                        } catch (IOException e) {

                            Astralis.LOGGER.warn(
                                    "[Script:{}] Could not remove compiler temporary files",
                                    scriptName,
                                    e);
                        }

                        return abortedResult;
                    }

                    try {
                        if (path3 != null) {
                            Files.deleteIfExists(path3);
                        }
                    } catch (IOException e) {

                        Astralis.LOGGER.warn(
                                "[Script:{}] Could not remove compiler temporary files",
                                scriptName,
                                e);
                    }

                    return abortedResult;
                }

                try {
                    if (path3 != null) {
                        Files.deleteIfExists(path3);
                    }
                } catch (IOException e) {

                    Astralis.LOGGER.warn(
                            "[Script:{}] Could not remove compiler temporary files", scriptName, e);
                }

                return abortedResult;
            }

            try {
                if (path3 != null) {
                    Files.deleteIfExists(path3);
                }
            } catch (IOException e) {

                Astralis.LOGGER.warn(
                        "[Script:{}] Could not remove compiler temporary files", scriptName, e);
            }

            return errorResult;
        }

        try {
            if (path3 != null) {
                Files.deleteIfExists(path3);
            }
        } catch (IOException e) {

            Astralis.LOGGER.warn(
                    "[Script:{}] Could not remove compiler temporary files", scriptName, e);
        }

        return errorResult;
    }

    private static MutableComponent createScriptMessageComponent(PendingScriptMessage pending) {
        String scriptName = pending.name();

        MutableComponent mutableComponent =
                Component.literal("[Script:" + scriptName + "] ")
                        .withStyle(ChatFormatting.DARK_RED);
        mutableComponent.append(Component.literal(pending.message()).withStyle(pending.color()));
        mutableComponent.append(Component.literal(" "));

        mutableComponent.append(
                Component.literal("[Copy error]")
                        .withStyle(
                                style -> {
                                    Style style2 =
                                            style.withColor(ChatFormatting.YELLOW)
                                                    .withUnderlined(true)
                                                    .withClickEvent(
                                                            new CopyToClipboard(
                                                                    pending.copyText()));

                                    return style2.withHoverEvent(
                                            new ShowText(
                                                    Component.literal(
                                                            "Copy the full script error")));
                                }));
        return mutableComponent;
    }

    public synchronized Map<String, Set<String>> getApprovedHttpDomains() throws IOException {
        return ScriptScanner.scanScripts(this.resolveScriptsDirectory()).stream()
                .filter(candidate -> !this.approvedDomainsFor(candidate).isEmpty())
                .collect(
                        Collectors.toUnmodifiableMap(
                                ScriptCandidate::getId,
                                candidate -> Set.copyOf(this.approvedDomainsFor(candidate))));
    }

    private static String truncateWithSuffix(String text, int maxLength, String suffix) {
        if (text.length() <= maxLength) {
            return text;
        }

        int cutoff = Math.max(0, maxLength - suffix.length());
        return text.substring(0, cutoff) + suffix;
    }

    public static AnnotationVisitor createAnnotationValidator(Set<String> scriptClasses) {

        return new ScriptAnnotationValidator(589824, scriptClasses);
    }

    public synchronized void unloadAll() {
        this.requireClientThread();
        this.unloadingAll = true;

        try {
            for (Script script : this.loadedScripts) {
                if (ScriptInternals.getScriptModule(script) != null
                        && ((ScriptModule) ScriptInternals.getScriptModule(script)).isEnabled()) {
                    try {
                        ((ScriptModule) ScriptInternals.getScriptModule(script)).setEnabled(false);
                    } catch (Throwable e) {

                        script.reportError("onDisable", e);
                    }
                }

                try {
                    ScriptInternals.invokeOnUnload(script);
                } catch (Throwable e) {

                    script.reportError("onUnload", e);
                }

                ScriptInternals.setScriptModule(script, null);
                ScriptInternals.destroyRuntime(script);
            }
        } finally {
            Astralis.getInstance()
                    .getModuleManager()
                    .removeMatching(module2 -> module2 instanceof ScriptModule);
            this.loadedScripts.clear();
            this.scriptIds.clear();
            this.loadedCandidates.clear();
            this.unloadingAll = false;
        }
    }

    void flushPendingMessages() {
        if (mc.isSameThread() && mc.player != null && !this.pendingMessages.isEmpty()) {
            for (PendingScriptMessage pending : this.pendingMessages) {
                if (this.pendingMessages.remove(pending)) {
                    ChatUtil.print(createScriptMessageComponent(pending));
                }
            }
        }
    }

    public List<Script> getLoadedScripts() {
        return List.copyOf(this.loadedScripts);
    }

    private StagingOutcome stageScripts(
            StagingKind kind, List<ScriptCandidate> rollbackCandidates) {
        if (mc.isSameThread()) {

            throw new IllegalStateException("Script staging cannot run on the client thread.");
        }

        PackageSnapshot incomingSources = null;
        PackageSnapshot rollbackSources = null;
        CompilationSnapshot incomingCompilation = null;
        CompilationSnapshot rollbackCompilation = null;
        ReloadResult failure = null;

        try {
            List<ScriptCandidate> discoveredCandidates =
                    ScriptScanner.scanScripts(this.resolveScriptsDirectory());
            String directoryPrefix;
            if (kind == StagingKind.STARTUP) {

                directoryPrefix = "startup-sources-";
            } else {

                directoryPrefix = "reload-sources-";
            }

            incomingSources = this.stageSourcePackages(discoveredCandidates, directoryPrefix);
            incomingCompilation = this.compileScripts(incomingSources.scripts());
            if (incomingCompilation == null) {
                failure = ReloadResult.PREFLIGHT_FAILED;
            } else if (kind == StagingKind.RELOAD && !rollbackCandidates.isEmpty()) {

                rollbackSources = this.stageSourcePackages(rollbackCandidates, "rollback-sources-");
                rollbackCompilation = this.compileScripts(rollbackSources.scripts());
                if (rollbackCompilation == null) {
                    failure = ReloadResult.PREFLIGHT_FAILED;
                }
            }
        } catch (IOException e) {

            Astralis.LOGGER.error("[ScriptManager] Could not stage script sources", e);
            failure = ReloadResult.SCAN_FAILED;
        } catch (Throwable e) {

            Astralis.LOGGER.error("[ScriptManager] Could not preflight script loading", e);
            failure = ReloadResult.PREFLIGHT_FAILED;
        }

        return new StagingOutcome(
                incomingSources,
                incomingCompilation,
                rollbackSources,
                rollbackCompilation,
                failure);
    }

    public synchronized void revokeHttpDomain(String scriptId, String domain) throws IOException {
        HttpDomainApproval approval = this.httpApprovals.get(scriptId);
        if (approval != null) {
            LinkedHashSet remainingDomains = new LinkedHashSet<>(approval.domains());
            remainingDomains.remove(ScriptScanner.normalizeHttpDomain(domain));
            if (remainingDomains.isEmpty()) {
                this.httpApprovals.remove(scriptId);
            } else {
                this.httpApprovals.put(
                        scriptId, new HttpDomainApproval(approval.revision(), remainingDomains));
            }

            try {
                this.savePermissions();
            } catch (IOException e) {
                this.httpApprovals.put(scriptId, approval);
                throw e;
            }
        }
    }

    private static void validateConstantPoolClass(String internalName, Set<String> scriptClasses) {

        if (!Set.of(
                        "java/lang/invoke/LambdaMetafactory",
                        "java/lang/invoke/MethodHandles",
                        "java/lang/invoke/MethodHandles$Lookup",
                        "java/lang/invoke/StringConcatFactory",
                        "java/lang/runtime/ObjectMethods")
                .contains(internalName)) {
            validateClassReference(internalName, scriptClasses);
        }
    }

    private String compilationFingerprint() {
        long fallbackNanos;
        if (this.cachedFingerprint != null) {
            return this.cachedFingerprint;
        }

        synchronized (this) {
            if (this.cachedFingerprint != null) {
                return this.cachedFingerprint;
            }

            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                String javaVersion = String.valueOf(Runtime.version());

                digest.update(
                        ("astralis-script-cache-4\n" + javaVersion + "\n")
                                .getBytes(StandardCharsets.UTF_8));
                Path apiJar = this.extractScriptingApiJar();
                List<Path> codeSources =
                        apiJar == null ? this.clientCodeSources() : List.of(apiJar);
                if (codeSources.isEmpty()) {

                    throw new IOException("The client code source is unavailable.");
                }

                for (Path codeSource : codeSources) {
                    if (Files.isRegularFile(codeSource, LinkOption.NOFOLLOW_LINKS)) {
                        try (InputStream jarInput = Files.newInputStream(codeSource)) {
                            byte[] jarBuffer = new byte[65536];

                            int jarBytesRead;
                            while ((jarBytesRead = jarInput.read(jarBuffer)) >= 0) {
                                digest.update(jarBuffer, 0, jarBytesRead);
                            }
                        }
                    } else {
                        if (!Files.isDirectory(codeSource, LinkOption.NOFOLLOW_LINKS)) {

                            throw new IOException("The client code source is unavailable.");
                        }

                        List<Path> files;
                        try (Stream<Path> walk = Files.walk(codeSource)) {
                            files =
                                    walk.filter(
                                                    file ->
                                                            Files.isRegularFile(
                                                                    file,
                                                                    LinkOption.NOFOLLOW_LINKS))
                                            .sorted(
                                                    Comparator.comparing(
                                                            sourceFile ->
                                                                    codeSource
                                                                            .relativize(sourceFile)
                                                                            .toString()))
                                            .toList();
                        }

                        for (Path classFile : files) {
                            String relativePath = codeSource.relativize(classFile).toString();

                            digest.update(
                                    relativePath
                                            .replace('\\', '/')
                                            .getBytes(StandardCharsets.UTF_8));
                            digest.update((byte) 0);

                            try (InputStream fileInput = Files.newInputStream(classFile)) {
                                byte[] fileBuffer = new byte[65536];

                                int fileBytesRead;
                                while ((fileBytesRead = fileInput.read(fileBuffer)) >= 0) {
                                    digest.update(fileBuffer, 0, fileBytesRead);
                                }
                            }
                        }
                    }

                    digest.update((byte) 0);
                }

                this.cachedFingerprint = HexFormat.of().formatHex(digest.digest());
            } catch (Exception e) {

                Astralis.LOGGER.warn(
                        "[ScriptManager] Persistent compilation caching is unavailable for this client run",
                        e);
                fallbackNanos = System.nanoTime();

                this.cachedFingerprint = "unavailable-" + fallbackNanos;
            }

            return this.cachedFingerprint;
        }
    }

    private String buildCompileClasspath() {

        LinkedHashSet classpathEntries = new LinkedHashSet();

        String javaClassPath = System.getProperty("java.class.path");
        if (javaClassPath != null && !javaClassPath.isBlank()) {
            classpathEntries.addAll(
                    List.of(javaClassPath.split(Pattern.quote(File.pathSeparator))));
        }

        this.collectClassLoaderPaths(classpathEntries, Script.class.getClassLoader());
        this.collectClassLoaderPaths(
                classpathEntries, Thread.currentThread().getContextClassLoader());
        this.addClassLocation(classpathEntries, Script.class);

        try {
            Path path = this.extractScriptingApiJar();
            if (path != null) {
                classpathEntries.add(path.toString());
            } else {

                Astralis.LOGGER.warn(
                        "[ScriptManager] Embedded scripting compiler API is unavailable");
            }
        } catch (IOException e) {

            Astralis.LOGGER.warn("[ScriptManager] Could not prepare the scripting compiler API", e);
        }

        for (Path path2 : this.clientCodeSources()) {
            classpathEntries.add(path2.toString());
        }

        return String.join(File.pathSeparator, classpathEntries);
    }

    private void addClassLocation(Set<String> paths, Class<?> clazz) {
        String className = clazz.getName();

        String internalName = className.replace('.', '/');

        String resourcePath = "/" + internalName + ".class";
        URL resourceUrl = clazz.getResource(resourcePath);
        if (resourceUrl != null) {
            try {
                if (resourceUrl.openConnection() instanceof JarURLConnection jarConnection) {
                    paths.add(Path.of(jarConnection.getJarFileURL().toURI()).toString());
                    return;
                }

                String protocol = resourceUrl.getProtocol();

                if (!protocol.equals("file")) {
                    return;
                }

                Path path = Path.of(resourceUrl.toURI());

                for (String segment : resourcePath.substring(1).split("/")) {
                    path = path.getParent();
                }

                if (path != null) {
                    paths.add(path.toString());
                }
            } catch (Exception ignored) {

                Astralis.LOGGER.warn(
                        "[ScriptManager] Could not add the scripting class resource to the compiler classpath");
            }
        }
    }

    private static String computeRevision(byte[] bytes) {

        try {
            HexFormat hex = HexFormat.of();

            return hex.formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException e) {

            throw new IllegalStateException("SHA-256 is unavailable.", e);
        }
    }

    public static void validateSignature(
            String signature, boolean typeSignature, Set<String> scriptClasses) {
        if (signature != null) {

            ScriptSignatureValidator validator =
                    new ScriptSignatureValidator(589824, scriptClasses);
            if (typeSignature) {
                new SignatureReader(signature).acceptType(validator);
            } else {
                new SignatureReader(signature).accept(validator);
            }
        }
    }

    public synchronized Map<String, Set<String>> getRequestedHttpDomains() throws IOException {
        return ScriptScanner.scanScripts(this.resolveScriptsDirectory()).stream()
                .filter(candidate -> !candidate.getScriptPackage().requestedHttpDomains().isEmpty())
                .collect(
                        Collectors.toUnmodifiableMap(
                                ScriptCandidate::getId,
                                candidate ->
                                        Set.copyOf(
                                                candidate
                                                        .getScriptPackage()
                                                        .requestedHttpDomains())));
    }

    private static String stackTraceToString(Throwable throwable) {
        if (throwable == null) {
            return "";
        }

        StringWriter stringWriter = new StringWriter();

        try (PrintWriter printWriter = new PrintWriter(stringWriter)) {
            throwable.printStackTrace(printWriter);
        }

        return stringWriter.toString().strip();
    }

    private void reportFailureWithCause(String scriptName, String message, Throwable cause) {

        Astralis.LOGGER.error("[Script:{}] {}", new Object[] {scriptName, message, cause});
        String diagnosticMessage = composeFailureMessage(message, cause);
        String chatMessage = truncateForChat(diagnosticMessage);
        String stackTrace = stackTraceToString(cause);
        String clipboardMessage = diagnosticMessage;
        String clipboardName = scriptName;

        this.queueScriptMessage(
                scriptName,
                chatMessage,
                truncateForClipboard(
                        "[Script:" + clipboardName + "] " + clipboardMessage + "\n\n" + stackTrace),
                ChatFormatting.RED);
    }

    private List<Path> clientCodeSources() {

        LinkedHashSet sources = new LinkedHashSet();
        ProtectionDomain protectionDomain = Script.class.getProtectionDomain();
        if (protectionDomain != null && protectionDomain.getCodeSource() != null) {
            URL location = protectionDomain.getCodeSource().getLocation();
            if (location != null) {

                if ("file".equals(location.getProtocol())) {
                    try {
                        Path locationPath = Path.of(location.toURI());
                        if (Files.isRegularFile(locationPath, LinkOption.NOFOLLOW_LINKS)
                                || Files.isDirectory(locationPath, LinkOption.NOFOLLOW_LINKS)) {
                            sources.add(locationPath);
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
        }

        FabricLoader fabricLoader = FabricLoader.getInstance();

        fabricLoader
                .getModContainer("astralis-recode")
                .ifPresent(
                        container -> {
                            ModOrigin origin = container.getOrigin();
                            if (origin.getKind() == Kind.PATH) {
                                for (Path originPath : origin.getPaths()) {
                                    if (Files.isRegularFile(originPath, LinkOption.NOFOLLOW_LINKS)
                                            || Files.isDirectory(
                                                    originPath, LinkOption.NOFOLLOW_LINKS)) {
                                        sources.add(originPath);
                                    }
                                }
                            }
                        });
        return List.copyOf(sources);
    }

    private PackageSnapshot stageSourcePackages(List<ScriptCandidate> candidates, String prefix)
            throws IOException {
        Path path = Files.createTempDirectory(this.cacheDirectory, prefix);
        boolean succeeded = false;
        boolean pendingCleanup = false /* VF: Semaphore variable */;

        PackageSnapshot snapshot;
        try {
            pendingCleanup = true;
            StagedScript[] stagedScripts = new StagedScript[candidates.size()];
            int scriptIndex = 0;

            for (ScriptCandidate candidate :
                    candidates.stream()
                            .sorted(Comparator.comparing(ScriptCandidate::getId))
                            .toList()) {
                Path path2 = path.resolve(candidate.getId()).normalize();
                if (path2.equals(path) || !path2.getParent().equals(path)) {

                    throw new IOException("A staged script package escaped its directory.");
                }

                Files.createDirectory(path2);
                File[] sourceFiles = new File[candidate.getSources().size()];
                int fileIndex = 0;

                for (ScriptSourceFile sourceFile : candidate.getSources()) {
                    Path path3 = path2.resolve(sourceFile.getRelativePath()).normalize();
                    if (!path3.startsWith(path2) || path3.equals(path2)) {

                        throw new IOException("A staged script source escaped its package.");
                    }

                    Files.createDirectories(path3.getParent());
                    Files.write(
                            path3,
                            rewriteLegacyImports(sourceFile.getContentCopy()),
                            StandardOpenOption.CREATE_NEW,
                            StandardOpenOption.WRITE);
                    sourceFiles[fileIndex++] = path3.toFile();
                }

                stagedScripts[scriptIndex++] = new StagedScript(candidate, sourceFiles);
            }

            succeeded = true;
            snapshot = new PackageSnapshot(path, stagedScripts);
            pendingCleanup = false;
        } finally {
            if (pendingCleanup) {
                if (!succeeded) {
                    this.deleteRecursively(path);
                }
            }
        }

        return snapshot;
    }

    private Set<String> approvedDomainsFor(ScriptCandidate candidate) {
        HttpDomainApproval approval = this.httpApprovals.get(candidate.getId());
        return approval != null && approval.revision().equals(candidate.getRevision())
                ? approval.domains()
                : Set.of();
    }

    private void ensureNameAvailable(Path path, String scriptName, boolean allowExisting)
            throws IOException {

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
            for (Path path2 : stream) {
                String fileName = path2.getFileName().toString();
                if (fileName.length() > 5) {
                    int suffixStart = fileName.length() - 5;

                    if (fileName.regionMatches(true, suffixStart, ".java", 0, 5)) {
                        String existingName = fileName.substring(0, fileName.length() - 5);
                        if (existingName.equals(scriptName)) {
                            if (!allowExisting) {
                                throw new FileAlreadyExistsException(path2.toString());
                            }
                        } else if (Normalizer.normalize(existingName, Form.NFC)
                                .equalsIgnoreCase(scriptName)) {
                            String existingPath = path2.toString();

                            throw new FileAlreadyExistsException(
                                    existingPath,
                                    null,
                                    "A script with a case-insensitive or Unicode-equivalent name already exists.");
                        }
                    }
                }
            }
        }
    }

    private static String truncateForClipboard(String text) {

        return truncateWithSuffix(text, 65536, "\n... diagnostic truncated ...");
    }

    private static void writeFileAtomically(Path path, Path path2, byte[] content)
            throws IOException {
        Path path3 = writeTempFile(path, content);

        try {
            moveReplacing(path3, path2);
        } finally {
            Files.deleteIfExists(path3);
        }
    }

    private void savePermissions() throws IOException {
        Path path = this.resolveScriptsDirectory();
        JsonObject jsonObject = new JsonObject();
        this.httpApprovals.entrySet().stream()
                .sorted(Entry.comparingByKey())
                .forEach(
                        entry -> {
                            JsonArray jsonArray = new JsonArray();
                            entry.getValue().domains().stream().sorted().forEach(jsonArray::add);
                            JsonObject jsonObject2 = new JsonObject();

                            jsonObject2.addProperty("revision", entry.getValue().revision());

                            jsonObject2.add("domains", jsonArray);
                            jsonObject.add(entry.getKey(), jsonObject2);
                        });

        Path path2 = Files.createTempFile(path, "permissions-", ".tmp");

        try {
            Files.writeString(
                    path2,
                    new GsonBuilder().setPrettyPrinting().create().toJson(jsonObject),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.TRUNCATE_EXISTING);
            moveReplacing(path2, this.permissionsFile.toPath());
        } finally {
            Files.deleteIfExists(path2);
        }
    }

    private CompilationSnapshot compileScripts(StagedScript[] stagedScripts) {
        if (!ScriptApiCompatibility.isScriptApiCompatible()) {

            this.reportApiMismatch(
                    "runtime",
                    new NoSuchFieldError(
                            "The running Script API is missing draggables or screens."));
            return null;
        }

        if (!this.cacheDirectorySafe) {
            return null;
        }

        Path path = null;
        boolean succeeded = false;
        boolean pendingCleanup = false /* VF: Semaphore variable */;

        Object errorResult;
        label270:
        {
            CompilationSnapshot result;
            try {
                ExecutorService executor;
                label285:
                {
                    pendingCleanup = true;

                    path = Files.createTempDirectory(this.cacheDirectory, "preflight-");
                    List<StagedScript> sortedScripts =
                            Arrays.stream(stagedScripts)
                                    .sorted(
                                            Comparator.comparing(
                                                    script -> script.candidate().getId()))
                                    .toList();
                    CompiledScript[] compiledScripts = new CompiledScript[sortedScripts.size()];
                    long startNanos = System.nanoTime();
                    int threadCount =
                            Math.max(
                                    1,
                                    Math.min(
                                            sortedScripts.size(),
                                            Math.max(
                                                    1,
                                                    Math.min(
                                                            4,
                                                            Runtime.getRuntime()
                                                                            .availableProcessors()
                                                                    / 2))));
                    OfVirtual threadBuilder = Thread.ofVirtual();

                    executor =
                            Executors.newFixedThreadPool(
                                    threadCount,
                                    threadBuilder.name("Astralis script compiler-", 0L).factory());
                    boolean executorCleanup = false /* VF: Semaphore variable */;

                    try {
                        executorCleanup = true;
                        ArrayList futures = new ArrayList();

                        for (StagedScript stagedScript : sortedScripts) {
                            Path path2 = path.resolve(stagedScript.candidate().getId());
                            futures.add(
                                    executor.submit(
                                            () ->
                                                    this.compileAndPreflightScript(
                                                            stagedScript, path2)));
                        }

                        for (int index = 0; index < futures.size(); index++) {
                            compiledScripts[index] =
                                    (CompiledScript) ((Future) futures.get(index)).get();
                            if (compiledScripts[index] == null) {
                                result = null;
                                executorCleanup = false;
                                break label285;
                            }
                        }

                        executorCleanup = false;
                    } finally {
                        if (executorCleanup) {
                            executor.shutdownNow();

                            try {

                                if (!executor.awaitTermination(10L, TimeUnit.SECONDS)) {

                                    Astralis.LOGGER.warn(
                                            "[ScriptManager] Script compiler tasks did not stop within ten seconds");
                                }
                            } catch (InterruptedException ignored) {
                                Thread.currentThread().interrupt();
                            }
                        }
                    }

                    executor.shutdownNow();

                    try {

                        if (!executor.awaitTermination(10L, TimeUnit.SECONDS)) {

                            Astralis.LOGGER.warn(
                                    "[ScriptManager] Script compiler tasks did not stop within ten seconds");
                        }
                    } catch (InterruptedException ignored) {
                        Thread.currentThread().interrupt();
                    }

                    long cachedCount =
                            Arrays.stream(compiledScripts).filter(CompiledScript::cached).count();

                    Astralis.LOGGER.info(
                            "[ScriptManager] Prepared {} Java script(s) in {} ms ({} cached, {} compiled)",
                            new Object[] {
                                compiledScripts.length,
                                TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos),
                                cachedCount,
                                compiledScripts.length - cachedCount
                            });
                    succeeded = true;
                    result = new CompilationSnapshot(path, compiledScripts);
                    pendingCleanup = false;
                    return result;
                }

                executor.shutdownNow();

                try {

                    if (!executor.awaitTermination(10L, TimeUnit.SECONDS)) {

                        Astralis.LOGGER.warn(
                                "[ScriptManager] Script compiler tasks did not stop within ten seconds");
                        pendingCleanup = false;
                    } else {
                        pendingCleanup = false;
                    }
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                    pendingCleanup = false;
                }
            } catch (Throwable e) {

                Astralis.LOGGER.error("[ScriptManager] Could not preflight script reload", e);
                errorResult = null;
                pendingCleanup = false;
                break label270;
            } finally {
                if (pendingCleanup) {
                    if (!succeeded && path != null) {
                        try {
                            this.deleteRecursively(path);
                        } catch (IOException e) {

                            Astralis.LOGGER.warn(
                                    "[ScriptManager] Could not remove preflight cache", e);
                        }
                    }
                }
            }

            if (path != null) {
                try {
                    this.deleteRecursively(path);
                } catch (IOException e) {

                    Astralis.LOGGER.warn("[ScriptManager] Could not remove preflight cache", e);
                }
            }

            return result;
        }

        if (!succeeded && path != null) {
            try {
                this.deleteRecursively(path);
            } catch (IOException e) {

                Astralis.LOGGER.warn("[ScriptManager] Could not remove preflight cache", e);
            }
        }

        return (CompilationSnapshot) errorResult;
    }

    public static String validateScriptName(String name) {
        if (name != null && !name.isBlank()) {
            String normalized = Normalizer.normalize(name, Form.NFC);
            if (!normalized.equals(name)) {

                throw new IllegalArgumentException(
                        "Script name must use normalized Unicode characters.");
            }

            int codePointCount = normalized.codePointCount(0, normalized.length());

            if (codePointCount > 64) {

                throw new IllegalArgumentException("Script name cannot exceed 64 characters.");
            }

            if (normalized.startsWith("_")) {

                throw new IllegalArgumentException("Script names cannot start with an underscore.");
            }

            if (SourceVersion.isIdentifier(normalized) && !SourceVersion.isKeyword(normalized)) {

                if (normalized.matches(
                        "(?i:CON|PRN|AUX|NUL|CLOCK\\$|CONIN\\$|CONOUT\\$|COM[1-9¹²³]|LPT[1-9¹²³])")) {

                    throw new IllegalArgumentException("Script name is reserved by Windows.");
                } else {
                    return normalized;
                }
            } else {

                throw new IllegalArgumentException(
                        "Script name must be a valid non-keyword Java class name.");
            }
        } else {

            throw new IllegalArgumentException("Script name is required.");
        }
    }

    public synchronized String createSource(String scriptName, String source) throws IOException {
        byte[] sourceBytes = encodeSource(source);
        Path path = this.resolveScriptsDirectory();
        this.ensureNameAvailable(path, validateScriptName(scriptName), false);
        Path path2 = this.resolveSourcePath(scriptName, path);
        boolean fileCreated = false;
        boolean fileWritten = false;
        boolean pendingCleanup = false /* VF: Semaphore variable */;

        try {
            pendingCleanup = true;

            try (SeekableByteChannel channel =
                    Files.newByteChannel(
                            path2,
                            StandardOpenOption.CREATE_NEW,
                            StandardOpenOption.WRITE,
                            LinkOption.NOFOLLOW_LINKS)) {
                fileCreated = true;
                ByteBuffer buffer = ByteBuffer.wrap(sourceBytes);

                while (buffer.hasRemaining()) {
                    if (channel.write(buffer) == 0) {

                        throw new IOException("Script source write did not make progress.");
                    }
                }

                fileWritten = true;
            }
        } finally {
            if (pendingCleanup) {
                if (fileCreated && !fileWritten) {
                    Files.deleteIfExists(path2);
                }
            }
        }

        return computeRevision(sourceBytes);
    }

    public static void validateHandle(Handle handle, Set<String> scriptClasses) {
        validateClassReference(handle.getOwner(), scriptClasses);
        if (handle.getTag() != 1
                && handle.getTag() != 2
                && handle.getTag() != 3
                && handle.getTag() != 4) {
            validateTypeReference(Type.getReturnType(handle.getDesc()), scriptClasses);

            for (Type argumentType : Type.getArgumentTypes(handle.getDesc())) {
                validateTypeReference(argumentType, scriptClasses);
            }
        } else {
            validateTypeReference(Type.getType(handle.getDesc()), scriptClasses);
        }
    }

    public synchronized Set<String> getApprovedHttpDomains(String scriptId) throws IOException {
        ScriptCandidate candidate =
                ScriptScanner.scanScripts(this.resolveScriptsDirectory()).stream()
                        .filter(scanned -> scanned.getId().equals(scriptId))
                        .findFirst()
                        .orElse(null);
        return candidate != null ? this.approvedDomainsFor(candidate) : Set.of();
    }

    private void discardFailedOutput(Path path, String scriptName) {

        try {
            this.deleteRecursively(path);
        } catch (IOException e) {

            Astralis.LOGGER.warn(
                    "[Script:{}] Could not remove failed compilation output", scriptName, e);
        }
    }

    private boolean restoreCachedOutput(ScriptCandidate candidate, Path path, String cacheKey) {
        long markerSize;
        Path path2 = this.cacheDirectory.resolve(candidate.getId()).normalize();
        if (!path2.equals(this.cacheDirectory)
                && path2.getParent().equals(this.cacheDirectory)
                && Files.isDirectory(path2, LinkOption.NOFOLLOW_LINKS)) {
            try {
                Path path3 = this.cacheDirectory.toRealPath();
                if (path2.toRealPath().equals(path2.toAbsolutePath().normalize())
                        && path2.toRealPath().getParent().equals(path3)) {

                    Path path4 = path2.resolve(".astralis-cache");
                    if (Files.isRegularFile(path4, LinkOption.NOFOLLOW_LINKS)) {
                        markerSize = Files.size(path4);

                        if (markerSize <= 1024L
                                && Files.readString(path4, StandardCharsets.UTF_8)
                                        .equals(cacheKey)) {
                            List<Path> cachedFiles;
                            try (Stream<Path> walk = Files.walk(path2)) {
                                cachedFiles = walk.sorted().toList();
                            }

                            Path path5 = path2.toRealPath();

                            for (Path path6 : cachedFiles) {
                                if (Files.isSymbolicLink(path6)) {
                                    return false;
                                }

                                if (Files.isDirectory(path6, LinkOption.NOFOLLOW_LINKS)) {
                                    if (!path6.toRealPath()
                                                    .equals(path6.toAbsolutePath().normalize())
                                            || !path6.toRealPath().startsWith(path5)) {
                                        return false;
                                    }
                                } else {
                                    if (Files.isRegularFile(path6, LinkOption.NOFOLLOW_LINKS)) {
                                        String fileName = path6.getFileName().toString();

                                        if (fileName.endsWith(".class")) {
                                            continue;
                                        }

                                        String markerFileName = path6.getFileName().toString();

                                        if (markerFileName.equals(".astralis-cache")) {
                                            continue;
                                        }
                                    }

                                    return false;
                                }
                            }

                            for (Path path7 : cachedFiles) {
                                Path path8 = path.resolve(path2.relativize(path7)).normalize();
                                if (!path8.startsWith(path)) {
                                    return false;
                                }

                                if (Files.isDirectory(path7, LinkOption.NOFOLLOW_LINKS)) {
                                    Files.createDirectories(path8);
                                } else {
                                    Files.copy(path7, path8, StandardCopyOption.REPLACE_EXISTING);
                                }
                            }

                            return true;
                        }
                    }

                    return false;
                } else {
                    return false;
                }
            } catch (IOException | RuntimeException e) {
                try {
                    this.deleteRecursively(path);
                    Files.createDirectories(path);
                } catch (IOException resetFailure) {
                    e.addSuppressed(resetFailure);
                }

                Astralis.LOGGER.warn(
                        "[Script:{}] Could not reuse the compilation cache",
                        candidate.getName(),
                        e);
                return false;
            }
        } else {
            return false;
        }
    }

    private static void moveReplacing(Path path, Path path2) throws IOException {
        try {
            Files.move(
                    path,
                    path2,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(path, path2, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private TargetedStagingOutcome stageTargetedScript(
            String scriptName, List<ScriptCandidate> knownCandidates) {
        PackageSnapshot incomingSources = null;
        PackageSnapshot rollbackSources = null;
        CompilationSnapshot incomingCompilation = null;
        CompilationSnapshot rollbackCompilation = null;
        TargetedStagingFailure failure = null;

        try {
            List<ScriptCandidate> discoveredCandidates =
                    ScriptScanner.scanScripts(this.resolveScriptsDirectory());
            ScriptCandidate target =
                    discoveredCandidates.stream()
                            .filter(scanned -> scanned.getId().equalsIgnoreCase(scriptName))
                            .findFirst()
                            .orElse(null);
            if (target == null) {
                target =
                        discoveredCandidates.stream()
                                .filter(scanned -> scanned.getName().equalsIgnoreCase(scriptName))
                                .findFirst()
                                .orElse(null);
            }

            if (target == null) {
                return new TargetedStagingOutcome(
                        null, null, null, null, TargetedStagingFailure.NOT_FOUND);
            }

            List targetList = List.of(target);

            incomingSources = this.stageSourcePackages(targetList, "target-sources-");
            incomingCompilation = this.compileScripts(incomingSources.scripts());
            if (incomingCompilation == null) {
                failure = TargetedStagingFailure.PREFLIGHT_FAILED;
            } else {
                ScriptCandidate finalTarget = target;
                ScriptCandidate rollbackCandidate =
                        knownCandidates.stream()
                                .filter(
                                        knownCandidate ->
                                                knownCandidate
                                                                .getId()
                                                                .equalsIgnoreCase(
                                                                        finalTarget.getId())
                                                        || knownCandidate
                                                                .getName()
                                                                .equalsIgnoreCase(
                                                                        finalTarget.getName()))
                                .findFirst()
                                .orElse(null);
                if (rollbackCandidate != null) {
                    List rollbackList = List.of(rollbackCandidate);

                    rollbackSources =
                            this.stageSourcePackages(rollbackList, "target-rollback-sources-");
                    rollbackCompilation = this.compileScripts(rollbackSources.scripts());
                    if (rollbackCompilation == null) {
                        failure = TargetedStagingFailure.PREFLIGHT_FAILED;
                    }
                }
            }
        } catch (IOException e) {

            Astralis.LOGGER.error("[ScriptManager] Could not stage targeted script loading", e);
            failure = TargetedStagingFailure.SCAN_FAILED;
        } catch (Throwable e) {

            Astralis.LOGGER.error("[ScriptManager] Could not preflight targeted script loading", e);
            failure = TargetedStagingFailure.PREFLIGHT_FAILED;
        }

        return new TargetedStagingOutcome(
                incomingSources,
                incomingCompilation,
                rollbackSources,
                rollbackCompilation,
                failure);
    }

    private static String messageOrDefault(String message) {

        String result;
        if (message != null && !message.isBlank()) {
            result = message.strip();
        } else {

            result = "The script could not be loaded.";
        }

        return result;
    }

    private synchronized Path extractScriptingApiJar() throws IOException {
        if (this.scriptingApiJar != null
                && Files.isRegularFile(this.scriptingApiJar, LinkOption.NOFOLLOW_LINKS)) {
            return this.scriptingApiJar;
        }

        InputStream jarStream =
                ScriptManager.class.getResourceAsStream("/META-INF/astralis/scripting-api.jar");
        if (jarStream == null) {
            return null;
        }

        Path path = this.cacheDirectory.resolve("scripting-api.jar");

        Path path2 = Files.createTempFile(this.cacheDirectory, "scripting-api-", ".jar");

        try (jarStream) {
            Files.copy(jarStream, path2, StandardCopyOption.REPLACE_EXISTING);
            if (Files.size(path2) == 0L) {

                throw new IOException("The embedded scripting compiler API is empty.");
            }

            moveReplacing(path2, path);
        } finally {
            Files.deleteIfExists(path2);
        }

        this.scriptingApiJar = path;
        return path;
    }

    private synchronized void finishLoading(long generation) {
        if (this.initialized && generation == this.lifecycleGeneration && this.loadInProgress) {
            this.loadInProgress = false;
            if (this.reloadRequested) {
                this.reloadRequested = false;
                this.scheduleLoading(StagingKind.RELOAD);
            }
        }
    }

    private void reportFailure(String scriptName, String message) {

        Astralis.LOGGER.error("[Script:{}] {}", scriptName, message);
        String chatMessage = truncateForChat(messageOrDefault(message));
        String fullMessage = messageOrDefault(message);

        this.queueScriptMessage(
                scriptName,
                chatMessage,
                truncateForClipboard("[Script:" + scriptName + "] " + fullMessage),
                ChatFormatting.RED);
    }

    private void reportCompilerOutput(String scriptName, String output, boolean warningOnly) {

        String headerValue;
        if (warningOnly) {

            headerValue = "Compiler warning:";
        } else {

            headerValue = "Compilation failed:";
        }

        String header = headerValue;
        String fullText = header + "\n" + output;
        String chatText = truncateForChat(fullText);

        this.queueScriptMessage(
                scriptName,
                chatText,
                truncateForClipboard("[Script:" + scriptName + "] " + fullText),
                warningOnly ? ChatFormatting.YELLOW : ChatFormatting.RED);
    }

    private static String truncateForChat(String text) {

        return truncateWithSuffix(
                text, 4096, "\n... truncated; click Copy error for the full diagnostic ...");
    }

    private void reportApiMismatch(String scriptName, Throwable error) {

        if (!this.apiMismatchReported) {
            this.apiMismatchReported = true;

            Astralis.LOGGER.error(
                    "[Script:{}] Compiled scripting API does not match the running client",
                    scriptName,
                    error);

            String stackTrace = stackTraceToString(error);

            this.queueScriptMessage(
                    scriptName,
                    "This script was compiled against a changed client API. Rebuild Astralis and fully restart Minecraft before loading it again.",
                    truncateForClipboard(
                            "[Script:"
                                    + scriptName
                                    + "] This script was compiled against a changed client API. Rebuild Astralis and fully restart Minecraft before loading it again.\n\n"
                                    + stackTrace),
                    ChatFormatting.RED);
        }
    }

    private void applyStagingOutcome(StagingKind kind, StagingOutcome outcome) {

        this.requireClientThread();
        if (outcome.failure() != null) {
            if (kind == StagingKind.RELOAD) {
                String failureMessage;
                if (outcome.failure() == ReloadResult.SCAN_FAILED) {

                    failureMessage =
                            "§c[Scripts] §fReload failed because the scripts folder could not be scanned.";
                } else {

                    failureMessage =
                            "§c[Scripts] §fReload rejected; current scripts remain active.";
                }

                ChatUtil.print(failureMessage);
            } else {

                Astralis.LOGGER.error(
                        "[ScriptManager] Startup script loading failed during preflight");
            }
        } else if (kind == StagingKind.STARTUP) {
            for (CompiledScript compiledScript : outcome.incomingCompilation().scripts()) {
                this.loadCompiledScript(
                        compiledScript,
                        this.enabledStates.getOrDefault(
                                compiledScript.script().candidate().getId(), false));
            }

            if (this.loadedScripts.size() != outcome.incomingSources().scripts().length) {

                Astralis.LOGGER.error(
                        "[ScriptManager] Startup was partial; unloading the staged script set");
                this.unloadAll();
            }

            Astralis.LOGGER.info(
                    "[ScriptManager] Startup loaded {} Java script(s)", this.loadedScripts.size());
        } else {
            HashMap<String, Boolean> previousEnabledStates = new HashMap<>();

            for (Script script : this.loadedScripts) {
                previousEnabledStates.put(
                        this.scriptIds.getOrDefault(script, script.getName()),
                        ScriptInternals.getScriptModule(script) != null
                                && ((ScriptModule) ScriptInternals.getScriptModule(script))
                                        .isEnabled());
            }

            this.enabledStates.putAll(previousEnabledStates);
            ConfigProfile activeConfig =
                    Astralis.getInstance().getConfigManager().getActiveConfig();
            JsonObject jsonObject = activeConfig != null ? activeConfig.getCurrentConfig() : null;
            this.unloadAll();

            for (CompiledScript incomingScript : outcome.incomingCompilation().scripts()) {
                this.loadCompiledScript(
                        incomingScript,
                        previousEnabledStates.getOrDefault(
                                incomingScript.script().candidate().getId(),
                                this.enabledStates.getOrDefault(
                                        incomingScript.script().candidate().getId(), false)));
            }

            boolean fullyLoaded =
                    this.loadedScripts.size() == outcome.incomingSources().scripts().length;
            if (!fullyLoaded) {

                Astralis.LOGGER.warn(
                        "[ScriptManager] Reload was partial; restoring the previous script snapshot");
                this.unloadAll();
                if (outcome.rollbackCompilation() != null) {
                    for (CompiledScript rollbackScript : outcome.rollbackCompilation().scripts()) {
                        this.loadCompiledScript(
                                rollbackScript,
                                previousEnabledStates.getOrDefault(
                                        rollbackScript.script().candidate().getId(), false));
                    }

                    if (this.loadedScripts.size() != outcome.rollbackSources().scripts().length) {

                        Astralis.LOGGER.error(
                                "[ScriptManager] Previous script snapshot restoration was partial");
                    }
                }
            }

            if (activeConfig != null) {
                activeConfig.loadConfig(jsonObject);
            }

            this.saveStates();
            if (mc.gui.screen() instanceof DropdownClickGuiScreen dropdownScreen) {
                dropdownScreen.refreshModules();
            } else if (mc.gui.screen() instanceof PanelClickGuiScreen panelScreen) {
                panelScreen.refreshModuleCards();
            }

            String message;
            if (fullyLoaded) {
                int loadedCount = this.loadedScripts.size();

                message = "§a[Scripts] §fReloaded " + loadedCount + " Java script(s).";
            } else {

                message = "§c[Scripts] §fReload failed; the previous script set was restored.";
            }

            ChatUtil.print(message);

            Astralis.LOGGER.info(
                    "[ScriptManager] Reload completed with {} active Java script(s)",
                    this.loadedScripts.size());
        }
    }

    private Path resolveSourcePath(String scriptName, Path path) throws IOException {
        String safeName = validateScriptName(scriptName);

        Path path2 = path.resolve(safeName + ".java").normalize();
        if (!path2.getParent().equals(path)) {

            throw new IOException("Script source escaped the scripts directory.");
        } else if (Files.exists(path2, LinkOption.NOFOLLOW_LINKS) && Files.isSymbolicLink(path2)) {

            throw new IOException("Symbolic-link script sources cannot be edited in game.");
        } else {
            return path2;
        }
    }
}
