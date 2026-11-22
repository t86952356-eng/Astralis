package astralis.util.io;

import astralis.network.crypto.ChaCha20Poly1305;
import astralis.util.CustomBase64;
import astralis.util.crypto.Sha256;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class CryptoUtil implements EmbeddedKeyConstants {
    private static byte[] kdfSalt;

    public static byte[] decrypt(byte[] ciphertext, byte[] key, long contextId) {
        return decrypt(ciphertext, key, longToBytes(contextId));
    }

    public static byte[] encrypt(byte[] plaintext, byte[] key, byte[] associatedData) {

        try {
            return ChaCha20Poly1305.encrypt(plaintext, key, associatedData);
        } catch (Exception e) {

            throw new RuntimeException("Encryption failed", e);
        }
    }

    public static byte[] hmacSha256(byte[] key, byte[] message) {

        byte[] blockKey = key;
        int keyLength = blockKey.length;

        if (keyLength > 64) {
            blockKey = Sha256.digest(blockKey);
        }

        byte[] paddedKey = new byte[64];
        System.arraycopy(blockKey, 0, paddedKey, 0, blockKey.length);

        byte[] innerData = new byte[64 + message.length];

        byte[] outerKeyPad = new byte[64];
        int i = 0;

        while (true) {

            if (i >= 64) {

                System.arraycopy(message, 0, innerData, 64, message.length);
                byte[] innerHash = Sha256.digest(innerData);

                byte[] outerData = new byte[64 + innerHash.length];

                System.arraycopy(outerKeyPad, 0, outerData, 0, 64);

                System.arraycopy(innerHash, 0, outerData, 64, innerHash.length);
                return Sha256.digest(outerData);
            }

            byte innerKeyByte = paddedKey[i];

            innerData[i] = (byte) (innerKeyByte ^ 54);
            byte outerKeyByte = paddedKey[i];

            outerKeyPad[i] = (byte) (outerKeyByte ^ 92);
            i++;
        }
    }

    public static String decryptString(String encoded, byte[] key, long contextId) {
        return decryptString(encoded, key, longToBytes(contextId));
    }

    public static String encryptString(String text, byte[] key, long contextId) {
        return encryptString(text, key, longToBytes(contextId));
    }

    public static byte[] encrypt(byte[] plaintext, byte[] key, long contextId) {
        return encrypt(plaintext, key, longToBytes(contextId));
    }

    public static byte[] hkdfExtract(byte[] salt, byte[] inputKeyMaterial) {

        if (salt == null || salt.length == 0) {

            salt = new byte[32];
        }

        return hmacSha256(salt, inputKeyMaterial);
    }

    public static byte[] decrypt(byte[] ciphertext, byte[] key, byte[] associatedData) {

        try {
            return ChaCha20Poly1305.decrypt(ciphertext, key, associatedData);
        } catch (Exception e) {

            throw new RuntimeException("Decryption failed", e);
        }
    }

    public static byte[] deriveKey(long primaryId, byte[] secret, long secondaryId) {

        byte[] pseudoRandomKey = hkdfExtract(kdfSalt, secret);
        byte[] primaryIdBytes = longToBytes(primaryId);
        byte[] secondaryIdBytes = longToBytes(secondaryId);

        byte[] info = new byte[16];

        System.arraycopy(primaryIdBytes, 0, info, 0, 8);

        System.arraycopy(secondaryIdBytes, 0, info, 8, 8);
        return hkdfExpand(pseudoRandomKey, info, 32);
    }

    public static byte[] hkdfExpand(byte[] pseudoRandomKey, byte[] info, int length) {
        byte[] infoWithCounter = new byte[info.length + 1];
        System.arraycopy(info, 0, infoWithCounter, 0, info.length);
        infoWithCounter[info.length] = 1;
        byte[] expanded = hmacSha256(pseudoRandomKey, infoWithCounter);
        return Arrays.copyOf(expanded, length);
    }

    public static byte[] longToBytes(long value) {

        return SimpleByteBuffer.allocate(8).writeLong(value).getArray();
    }

    public static String decryptString(String encoded, byte[] key, byte[] associatedData) {

        try {
            byte[] cipherBytes = CustomBase64.decode(encoded);
            byte[] plainBytes = ChaCha20Poly1305.decrypt(cipherBytes, key, associatedData);
            return new String(plainBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {

            throw new RuntimeException("Decryption failed", e);
        }
    }

    public static String encryptString(String text, byte[] key, byte[] associatedData) {

        try {
            byte[] cipherBytes =
                    ChaCha20Poly1305.encrypt(
                            text.getBytes(StandardCharsets.UTF_8), key, associatedData);
            return CustomBase64.encode(cipherBytes);
        } catch (Exception e) {

            throw new RuntimeException("Encryption failed", e);
        }
    }

    static {
        kdfSalt = "serenity-pkt-v2".getBytes(StandardCharsets.UTF_8);
    }
}
