package astralis.account;

import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.nio.file.*;
import java.security.SecureRandom;

/** Loads or creates the local account-encryption key. */
public final class LocalAccountKey {
    private static byte[] key;

    private LocalAccountKey() {}

    public static synchronized byte[] loadOrCreateKey() {
        if (key == null) {
            Path path =
                    Minecraft.getInstance()
                            .gameDirectory
                            .toPath()
                            .resolve("astralis/local-account.key");
            try {
                if (Files.exists(path)) key = Files.readAllBytes(path);
                else {
                    byte[] created = new byte[32];
                    new SecureRandom().nextBytes(created);
                    Files.createDirectories(path.getParent());
                    try {
                        Files.write(path, created, StandardOpenOption.CREATE_NEW);
                        key = created;
                    } catch (FileAlreadyExistsException raced) {
                        key = Files.readAllBytes(path);
                    }
                }
                if (key.length != 32) throw new IOException("Invalid local account encryption key");
            } catch (IOException error) {
                throw new IllegalStateException(
                        "Unable to read local account encryption key", error);
            }
        }
        return key.clone();
    }

    public static byte[] getLegacyKey1() {
        return loadOrCreateKey();
    }

    public static byte[] getLegacyKey2() {
        return loadOrCreateKey();
    }

    public static byte[] getLegacyKey3() {
        return loadOrCreateKey();
    }
}
