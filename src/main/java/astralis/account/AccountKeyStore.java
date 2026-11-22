package astralis.account;

import astralis.util.CustomBase64;

import com.google.gson.Gson;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

public final class AccountKeyStore {
    private final Map<String, byte[]> keys;
    private final File storeFile;
    private final Gson gson;
    private boolean loadFailed;

    AccountKeyStore(File file2) {

        this.keys = new HashMap<>();
        this.gson = new Gson();

        this.storeFile = new File(file2, "astralis/alts.serenity");
    }

    void reset() {
        this.keys.clear();
        this.loadFailed = false;

        try {
            Files.deleteIfExists(this.storeFile.toPath());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    byte[] getKey(String keyId) {
        return this.keys.get(keyId);
    }

    void putKey(String keyId, byte[] key) {
        this.keys.put(keyId, key);
        this.save();
    }

    boolean hasLoadFailed() {
        return this.loadFailed;
    }

    void save() {

        try {
            HashMap encodedKeys = new HashMap();
            this.keys.forEach((keyId, key) -> encodedKeys.put(keyId, CustomBase64.encode(key)));
            byte[] jsonBytes = this.gson.toJson(encodedKeys).getBytes(StandardCharsets.UTF_8);
            byte[] encrypted = StorageCipher.encrypt(jsonBytes, StorageCipher.storeContext);
            this.storeFile.getParentFile().mkdirs();
            Path path = this.storeFile.toPath();
            String fileName = String.valueOf(path.getFileName());

            Path path2 = path.resolveSibling(fileName + ".tmp");
            Files.write(path2, encrypted);

            try {
                Files.move(
                        path2,
                        path,
                        StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(path2, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void load() {
        this.loadFailed = false;
        if (this.storeFile.exists()) {
            try {
                byte[] encrypted = Files.readAllBytes(this.storeFile.toPath());
                DecryptResult result = StorageCipher.decrypt(encrypted, StorageCipher.storeContext);
                byte[] jsonBytes = result.getData();
                Map<String, String> encodedKeys =
                        this.gson.fromJson(
                                new String(jsonBytes, StandardCharsets.UTF_8),
                                new StringMapType(this).getType());
                if (encodedKeys != null) {
                    encodedKeys.forEach(
                            (keyId, encodedKey) ->
                                    this.keys.put(keyId, CustomBase64.decode(encodedKey)));
                }

                if (result.isResaveRequired()) {
                    this.save();
                }
            } catch (Exception e) {
                this.loadFailed = true;
                this.keys.clear();
                e.printStackTrace();
            }
        }
    }

    boolean isEmpty() {
        return this.keys.isEmpty();
    }

    void removeKey(String keyId) {
        this.keys.remove(keyId);
        this.save();
    }
}
