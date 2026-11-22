package astralis.account;

import astralis.util.OperatingSystem;
import astralis.util.SystemUtil;
import astralis.util.io.CryptoUtil;

import java.nio.charset.StandardCharsets;

public final class StorageCipher {
    private static OperatingSystem platform;
    public static byte[] storeContext;

    public static byte[] decryptBytes(byte[] data, byte[] context) {
        return decrypt(data, context).getData();
    }

    private static DecryptResult decryptWithLocalKey(byte[] data, byte[] context) {
        try {
            return new DecryptResult(
                    CryptoUtil.decrypt(data, LocalAccountKey.loadOrCreateKey(), context), false);
        } catch (RuntimeException primaryError) {
            try {
                return new DecryptResult(
                        CryptoUtil.decrypt(data, LocalAccountKey.getLegacyKey1(), context), true);
            } catch (RuntimeException legacyError1) {
                try {
                    return new DecryptResult(
                            CryptoUtil.decrypt(data, LocalAccountKey.getLegacyKey2(), context),
                            true);
                } catch (RuntimeException legacyError2) {
                    try {
                        return new DecryptResult(
                                CryptoUtil.decrypt(data, LocalAccountKey.getLegacyKey3(), context),
                                true);
                    } catch (RuntimeException legacyError3) {
                        throw primaryError;
                    }
                }
            }
        }
    }

    public static DecryptResult decrypt(byte[] data, byte[] context) {

        return switch (PlatformSwitchMap.switchMapPlatform[platform.ordinal()]) {
            case 1 -> new DecryptResult(WindowsDpapi.unprotect(data), false);
            case 2, 3 -> decryptWithLocalKey(data, context);
            default -> {
                String platformName = String.valueOf(platform);

                throw new UnsupportedOperationException("Unsupported platform: " + platformName);
            }
        };
    }

    public static byte[] encrypt(byte[] data, byte[] context) {

        byte[] encrypted =
                switch (PlatformSwitchMap.switchMapPlatform[platform.ordinal()]) {
                    case 1 -> WindowsDpapi.protect(data);
                    case 2, 3 ->
                            CryptoUtil.encrypt(data, LocalAccountKey.loadOrCreateKey(), context);
                    default -> {
                        String platformName = String.valueOf(platform);

                        throw new UnsupportedOperationException(
                                "Unsupported platform: " + platformName);
                    }
                };
        return encrypted;
    }

    static {
        platform = SystemUtil.detectOperatingSystem();
        storeContext = "astralis-alt-store-v1".getBytes(StandardCharsets.UTF_8);
    }
}
