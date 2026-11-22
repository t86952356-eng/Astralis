package astralis.network.crypto;

import java.security.SecureRandom;
import java.util.Arrays;

public final class ChaCha20Poly1305 {
    private static final int NONCE_SIZE = 12;
    private static final int TAG_SIZE = 16;
    private static final int KEY_SIZE = 32;
    private static final int[] SIGMA = new int[] {1634760805, 857760878, 2036477234, 1797285236};

    private ChaCha20Poly1305() {}

    public static void writeIntLittleEndian(byte[] buffer, int offset, int value) {
        buffer[offset] = (byte) value;
        int offset1 = offset + 1;

        buffer[offset1] = (byte) (value >>> 8);
        int offset2 = offset + 2;

        buffer[offset2] = (byte) (value >>> 16);
        int offset3 = offset + 3;

        buffer[offset3] = (byte) (value >>> 24);
    }

    private static void quarterRound(int[] state, int a, int b, int c, int d) {
        state[a] += state[b];
        int xored = state[d] ^ state[a];

        state[d] = Integer.rotateLeft(xored, 16);
        state[c] += state[d];
        xored = state[b] ^ state[c];

        state[b] = Integer.rotateLeft(xored, 12);
        state[a] += state[b];
        xored = state[d] ^ state[a];

        state[d] = Integer.rotateLeft(xored, 8);
        state[c] += state[d];
        xored = state[b] ^ state[c];

        state[b] = Integer.rotateLeft(xored, 7);
    }

    public static long readUnsignedIntLittleEndian(byte[] data, int offset) {
        long result;
        long nextByte;
        result = data[offset];

        result &= 255L;
        nextByte = data[offset + 1];

        nextByte &= 255L;

        result |= nextByte << 8;
        nextByte = data[offset + 2];

        nextByte &= 255L;

        result |= nextByte << 16;
        nextByte = data[offset + 3];

        nextByte &= 255L;

        return result | nextByte << 24;
    }

    static byte[] computePoly1305Mac(byte[] data, byte[] key) {

        Poly1305 poly = new Poly1305(key);
        int dataLength = data.length;

        int fullBlockBytes = dataLength & -16;

        for (int offset = 0; offset < fullBlockBytes; offset += 16) {

            poly.processBlock(data, offset, 16777216L);
        }

        int remaining = data.length - fullBlockBytes;
        if (remaining > 0) {

            byte[] lastBlock = new byte[16];
            System.arraycopy(data, fullBlockBytes, lastBlock, 0, remaining);
            lastBlock[remaining] = 1;
            poly.processBlock(lastBlock, 0, 0L);
        }

        return poly.finishTag();
    }

    private static byte[] computeAeadTag(
            byte[] polyKey,
            byte[] aad,
            byte[] ciphertext,
            int ciphertextOffset,
            int ciphertextLength) {

        Poly1305 poly = new Poly1305(polyKey);
        poly.processData(aad, 0, aad.length);
        poly.processData(ciphertext, ciphertextOffset, ciphertextLength);

        byte[] lengthBlock = new byte[16];
        writeLongLittleEndian(lengthBlock, 0, aad.length);

        writeLongLittleEndian(lengthBlock, 8, ciphertextLength);

        poly.processBlock(lengthBlock, 0, 16777216L);
        return poly.finishTag();
    }

    private static int[] createChaChaState(byte[] key, byte[] nonce, int counter) {

        int[] state = new int[16];
        state[0] = SIGMA[0];
        state[1] = SIGMA[1];
        state[2] = SIGMA[2];
        state[3] = SIGMA[3];
        int keyWordIndex = 0;

        while (true) {

            if (keyWordIndex >= 8) {

                state[12] = counter;

                for (int nonceWordIndex = 0; nonceWordIndex < 3; nonceWordIndex++) {

                    state[13 + nonceWordIndex] =
                            (int) readUnsignedIntLittleEndian(nonce, nonceWordIndex * 4);
                }

                return state;
            }

            state[4 + keyWordIndex] = (int) readUnsignedIntLittleEndian(key, keyWordIndex * 4);
            keyWordIndex++;
        }
    }

    private static void writeLongLittleEndian(byte[] buffer, int offset, long value) {
        int i = 0;

        while (true) {

            if (i >= 8) {
                return;
            }

            int index = offset + i;

            buffer[index] = (byte) (value >>> 8 * i);
            i++;
        }
    }

    public static byte[] encrypt(byte[] plaintext, byte[] key, byte[] aad) {

        int keyLength = key.length;

        if (keyLength != 32) {

            throw new IllegalArgumentException("Key must be 32 bytes");
        } else {

            byte[] nonce = new byte[12];
            new SecureRandom().nextBytes(nonce);
            byte[] polyKeyBlock = chaCha20Block(key, nonce, 0);

            byte[] polyKey = Arrays.copyOf(polyKeyBlock, 32);

            int payloadLength = 12 + plaintext.length;

            byte[] output = new byte[payloadLength + 16];

            System.arraycopy(nonce, 0, output, 0, 12);

            chaCha20Xor(key, nonce, 1, plaintext, 0, output, 12, plaintext.length);
            int tagOffset;
            byte[] tag = computeAeadTag(polyKey, aad, output, 12, plaintext.length);
            tagOffset = 12 + plaintext.length;

            System.arraycopy(tag, 0, output, tagOffset, 16);
            Arrays.fill(polyKey, (byte) 0);
            return output;
        }
    }

    private static boolean constantTimeEquals(byte[] computedTag, byte[] data, int tagOffset) {
        int diff = 0;

        for (int i = 0; i < computedTag.length; i++) {
            diff |= computedTag[i] ^ data[tagOffset + i];
        }

        return diff == 0;
    }

    static void chaCha20Xor(
            byte[] key,
            byte[] nonce,
            int counter,
            byte[] input,
            int inputOffset,
            byte[] output,
            int outputOffset,
            int length) {

        int[] state = createChaChaState(key, nonce, counter);

        byte[] keystream = new byte[64];
        int processed = 0;

        while (processed < length) {
            chaChaRounds(state, keystream);

            state[12]++;

            int chunkLength = Math.min(64, length - processed);

            for (int i = 0; i < chunkLength; i++) {
                output[outputOffset + processed + i] =
                        (byte) (input[inputOffset + processed + i] ^ keystream[i]);
            }

            processed += chunkLength;
        }
    }

    public static byte[] decrypt(byte[] data, byte[] key, byte[] aad)
            throws AuthenticationFailedException {
        int keyLength = key.length;

        if (keyLength != 32) {

            throw new IllegalArgumentException("Key must be 32 bytes");
        } else {
            int dataLength = data.length;

            if (dataLength < 28) {

                throw new IllegalArgumentException("Data too short");
            } else {

                byte[] nonce = Arrays.copyOfRange(data, 0, 12);
                int totalLength = data.length;

                int bodyLength = totalLength - 12;

                int ciphertextLength = bodyLength - 16;
                byte[] polyKeyBlock = chaCha20Block(key, nonce, 0);

                byte[] polyKey = Arrays.copyOf(polyKeyBlock, 32);

                byte[] expectedTag = computeAeadTag(polyKey, aad, data, 12, ciphertextLength);
                Arrays.fill(polyKey, (byte) 0);

                if (!constantTimeEquals(expectedTag, data, 12 + ciphertextLength)) {

                    throw new AuthenticationFailedException(
                            "Failed to authenticate message, either tampered or corrupted!");
                } else {
                    byte[] plaintext = new byte[ciphertextLength];

                    chaCha20Xor(key, nonce, 1, data, 12, plaintext, 0, ciphertextLength);
                    return plaintext;
                }
            }
        }
    }

    static byte[] chaCha20Block(byte[] key, byte[] nonce, int counter) {

        int[] state = createChaChaState(key, nonce, counter);

        byte[] block = new byte[64];
        chaChaRounds(state, block);
        return block;
    }

    private static void chaChaRounds(int[] state, byte[] output) {
        int[] working = (int[]) state.clone();
        int round = 0;

        while (true) {

            if (round >= 10) {
                int i = 0;

                while (true) {

                    if (i >= 16) {
                        return;
                    }

                    writeIntLittleEndian(output, i * 4, working[i] + state[i]);
                    i++;
                }
            }

            quarterRound(working, 0, 4, 8, 12);

            quarterRound(working, 1, 5, 9, 13);

            quarterRound(working, 2, 6, 10, 14);

            quarterRound(working, 3, 7, 11, 15);

            quarterRound(working, 0, 5, 10, 15);

            quarterRound(working, 1, 6, 11, 12);

            quarterRound(working, 2, 7, 8, 13);

            quarterRound(working, 3, 4, 9, 14);
            round++;
        }
    }
}
