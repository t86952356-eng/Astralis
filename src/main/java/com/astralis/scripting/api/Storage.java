package com.astralis.scripting.api;

import astralis.Astralis;

import com.astralis.scripting.InstanceAccess;
import com.astralis.scripting.Script;

import org.json.JSONObject;

import java.io.IOException;
import java.lang.StackWalker.Option;
import java.lang.StackWalker.StackFrame;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class Storage implements InstanceAccess {
    private final Map<String, Object> values = new ConcurrentHashMap<>();
    private Path storagePath;
    private Script owner;
    private boolean destroyed;

    public synchronized Object remove(String key) {
        this.ensureActive();
        Object removed = this.values.remove(this.validateKey(key));
        this.flush();
        return removed != null ? this.sanitizeValue(removed, 0, new int[] {0}) : null;
    }

    public int size() {
        this.ensureActive();
        return this.values.size();
    }

    public Object get(String key) {
        this.ensureActive();
        Object value = this.values.get(this.validateKey(key));
        return value != null ? this.sanitizeValue(value, 0, new int[] {0}) : null;
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        this.ensureActive();
        return this.values.get(this.validateKey(key)) instanceof Boolean booleanValue
                ? booleanValue
                : defaultValue;
    }

    public int getInt(String key, int defaultValue) {
        this.ensureActive();
        return this.values.get(this.validateKey(key)) instanceof Number number
                ? number.intValue()
                : defaultValue;
    }

    public long getLong(String key, long defaultValue) {
        this.ensureActive();
        return this.values.get(this.validateKey(key)) instanceof Number number
                ? number.longValue()
                : defaultValue;
    }

    public double getDouble(String key, double defaultValue) {
        this.ensureActive();
        return this.values.get(this.validateKey(key)) instanceof Number number
                ? number.doubleValue()
                : defaultValue;
    }

    public synchronized void clear() {
        this.ensureActive();
        this.values.clear();
        this.flush();
    }

    public synchronized void flush() {
        this.ensureActive();
        this.writeToDisk();
    }

    public boolean isEmpty() {
        this.ensureActive();
        return this.values.isEmpty();
    }

    public synchronized void set(String key, Object value) {
        this.ensureActive();
        String storageKey = this.validateKey(key);
        Object previous = this.values.get(storageKey);
        if (value == null) {
            this.values.remove(storageKey);
        } else {
            this.values.put(storageKey, this.sanitizeValue(value, 0, new int[] {0}));
        }

        try {
            if (this.values.size() > 512) {
                throw new IllegalStateException(
                        "Script storage cannot contain more than 512 keys.");
            }

            if (new JSONObject(this.values).toString().getBytes(StandardCharsets.UTF_8).length
                    > 1048576) {
                throw new IllegalStateException("Script storage exceeds 1 MiB.");
            }
        } catch (RuntimeException e) {
            if (previous == null) {
                this.values.remove(storageKey);
            } else {
                this.values.put(storageKey, previous);
            }

            throw e;
        }

        this.flush();
    }

    public synchronized Map<String, Object> snapshot() {
        this.ensureActive();
        LinkedHashMap result = new LinkedHashMap();
        this.values.forEach(
                (entryKey, entryValue) ->
                        result.put(entryKey, this.sanitizeValue(entryValue, 0, new int[] {0})));
        return Map.copyOf(result);
    }

    public synchronized void destroy() {
        checkScriptRuntimeCaller();
        if (!this.destroyed) {
            this.destroyed = true;
            this.writeToDisk();
            this.owner = null;
            this.storagePath = null;
            this.values.clear();
        }
    }

    public Set<String> keys() {
        this.ensureActive();
        return Set.copyOf(this.values.keySet());
    }

    public Object getOrDefault(String key, Object defaultValue) {
        this.ensureActive();
        Object value = this.values.get(this.validateKey(key));
        return value != null ? this.sanitizeValue(value, 0, new int[] {0}) : defaultValue;
    }

    public synchronized void bind(Script script) {
        checkScriptRuntimeCaller();
        if (script == null || script.getName() == null) {
            throw new IllegalStateException("Script storage requires an initialized owner.");
        }

        if (this.destroyed) {
            throw new IllegalStateException("This storage service has already been destroyed.");
        }

        Path path =
                mc.gameDirectory
                        .toPath()
                        .resolve("astralis/scripts/data")
                        .resolve(script.getName() + ".json")
                        .toAbsolutePath()
                        .normalize();
        if (this.owner == null && this.storagePath == null) {
            try {
                Files.createDirectories(mc.gameDirectory.toPath().resolve("astralis/scripts/data"));
                this.validateStoragePath(path);
            } catch (IOException e) {
                throw new IllegalStateException("Could not secure the script storage path.", e);
            }

            this.owner = script;
            this.storagePath = path;
            this.values.clear();
            if (Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
                try {
                    if (Files.size(path) > 1048576L) {
                        throw new IOException("Storage file exceeds 1 MiB.");
                    }

                    Map<String, Object> loadedValues =
                            new JSONObject(Files.readString(path, StandardCharsets.UTF_8)).toMap();
                    if (loadedValues.size() > 512) {
                        throw new IllegalArgumentException(
                                "Storage file contains more than 512 keys.");
                    }

                    int[] itemCount = new int[] {0};
                    loadedValues.forEach(
                            (entryKey, entryValue) -> {
                                if (entryValue != null) {
                                    this.values.put(
                                            this.validateKey(entryKey),
                                            this.sanitizeValue(entryValue, 0, itemCount));
                                }
                            });
                } catch (IOException | RuntimeException e) {
                    this.values.clear();
                    Astralis.LOGGER.error("[ScriptStorage] Failed to load {}", path, e);

                    try {
                        Files.move(
                                path,
                                path.resolveSibling(
                                        path.getFileName()
                                                + ".corrupt-"
                                                + System.currentTimeMillis()));
                    } catch (IOException moveException) {
                        e.addSuppressed(moveException);
                    }
                }
            }
        } else if (this.owner != script || !path.equals(this.storagePath)) {
            throw new IllegalStateException("This storage service is already bound.");
        }
    }

    public boolean has(String key) {
        this.ensureActive();
        return this.values.containsKey(this.validateKey(key));
    }

    public String getString(String key, String defaultValue) {
        this.ensureActive();
        return this.values.get(this.validateKey(key)) instanceof String stringValue
                ? stringValue
                : defaultValue;
    }

    private void validateStoragePath(Path path) throws IOException {
        Path path2 =
                mc.gameDirectory
                        .toPath()
                        .resolve("astralis/scripts/data")
                        .toAbsolutePath()
                        .normalize();
        if (path.getParent().equals(path2)
                && path.getFileName().toString().matches("[A-Za-z_$][A-Za-z0-9_$]{0,63}\\.json")) {
            BasicFileAttributes directoryAttributes =
                    Files.readAttributes(
                            path2, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
            if (!directoryAttributes.isDirectory()
                    || directoryAttributes.isSymbolicLink()
                    || directoryAttributes.isOther()) {
                throw new SecurityException(
                        "The script data directory cannot be a link or reparse point.");
            }

            if (!path2.toRealPath(LinkOption.NOFOLLOW_LINKS).equals(path2)) {
                throw new SecurityException("The script data directory does not resolve safely.");
            }

            if (Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
                BasicFileAttributes fileAttributes =
                        Files.readAttributes(
                                path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
                if (!fileAttributes.isRegularFile()
                        || fileAttributes.isSymbolicLink()
                        || fileAttributes.isOther()) {
                    throw new SecurityException(
                            "Script storage files cannot be links or reparse points.");
                }
            }
        } else {
            throw new SecurityException(
                    "Script storage must stay inside the managed data directory.");
        }
    }

    private String validateKey(String key) {
        if (key != null && !key.isBlank() && key.length() <= 128) {
            return key;
        } else {
            throw new IllegalArgumentException(
                    "Storage keys must contain between 1 and 128 characters.");
        }
    }

    private synchronized void ensureActive() {
        if (this.owner == null || this.destroyed || !this.owner.isRuntimeActive()) {
            throw new IllegalStateException("The script storage service is not active.");
        }
    }

    private Object sanitizeValue(Object value, int depth, int[] itemCount) {
        if (depth > 16 || ++itemCount[0] > 4096) {
            throw new IllegalArgumentException("Storage values exceed the nesting or item limit.");
        }

        if (value instanceof String string) {
            if (string.length() > 65536) {
                throw new IllegalArgumentException(
                        "Storage strings cannot exceed 65,536 characters.");
            } else {
                return string;
            }
        } else {
            if (value instanceof Boolean) {
                return value;
            }

            if (value instanceof Number number) {
                if (!Double.isFinite(number.doubleValue())) {
                    throw new IllegalArgumentException("Storage numbers must be finite.");
                } else {
                    return value;
                }
            } else if (!(value instanceof List list)) {
                if (value instanceof Map<?, ?> map) {
                    LinkedHashMap sanitizedMap = new LinkedHashMap();

                    for (Entry entry : map.entrySet()) {
                        if (!(entry.getKey() instanceof String mapKey)) {
                            throw new IllegalArgumentException("Storage map keys must be strings.");
                        }

                        sanitizedMap.put(
                                this.validateKey(mapKey),
                                this.sanitizeValue(entry.getValue(), depth + 1, itemCount));
                    }

                    return Map.copyOf(sanitizedMap);
                } else {
                    throw new IllegalArgumentException("Storage values must be JSON-compatible.");
                }
            } else {
                ArrayList sanitizedList = new ArrayList(list.size());

                for (Object element : list) {
                    sanitizedList.add(this.sanitizeValue(element, depth + 1, itemCount));
                }

                return List.copyOf(sanitizedList);
            }
        }
    }

    private void writeToDisk() {
        if (this.storagePath != null) {
            try {
                this.validateStoragePath(this.storagePath);
                Path path =
                        Files.createTempFile(
                                this.storagePath.getParent(),
                                this.storagePath.getFileName().toString(),
                                ".tmp");

                try {
                    String json = new JSONObject(this.values).toString(2);
                    if (json.getBytes(StandardCharsets.UTF_8).length > 1048576) {
                        throw new IllegalStateException("Script storage exceeds 1 MiB.");
                    }

                    Files.writeString(path, json, StandardCharsets.UTF_8);

                    try {
                        Files.move(
                                path,
                                this.storagePath,
                                StandardCopyOption.REPLACE_EXISTING,
                                StandardCopyOption.ATOMIC_MOVE);
                    } catch (AtomicMoveNotSupportedException ignored) {
                        Files.move(path, this.storagePath, StandardCopyOption.REPLACE_EXISTING);
                    }
                } finally {
                    Files.deleteIfExists(path);
                }
            } catch (IOException | RuntimeException e) {
                Astralis.LOGGER.error("[ScriptStorage] Failed to save {}", this.storagePath, e);
            }
        }
    }

    private static void checkScriptRuntimeCaller() {
        Class callerClass =
                StackWalker.getInstance(Option.RETAIN_CLASS_REFERENCE)
                        .walk(
                                frames ->
                                        frames.map(StackFrame::getDeclaringClass)
                                                .filter(frameClass -> frameClass != Storage.class)
                                                .findFirst()
                                                .orElseThrow(
                                                        () ->
                                                                new SecurityException(
                                                                        "Missing script runtime caller.")));
        if (callerClass != Script.class
                && (!Script.class.isAssignableFrom(callerClass)
                        || callerClass.getClassLoader() != Script.class.getClassLoader())) {
            throw new SecurityException("Script storage ownership is managed by the runtime.");
        }
    }
}
