package astralis.util.crypto;

import astralis.util.io.SimpleByteBuffer;

import java.nio.IntBuffer;

public class Sha256 {
    private static final int[] ROUND_CONSTANTS =
            new int[] {
                1116352408,
                1899447441,
                -1245643825,
                -373957723,
                961987163,
                1508970993,
                -1841331548,
                -1424204075,
                -670586216,
                310598401,
                607225278,
                1426881987,
                1925078388,
                -2132889090,
                -1680079193,
                -1046744716,
                -459576895,
                -272742522,
                264347078,
                604807628,
                770255983,
                1249150122,
                1555081692,
                1996064986,
                -1740746414,
                -1473132947,
                -1341970488,
                -1084653625,
                -958395405,
                -710438585,
                113926993,
                338241895,
                666307205,
                773529912,
                1294757372,
                1396182291,
                1695183700,
                1986661051,
                -2117940946,
                -1838011259,
                -1564481375,
                -1474664885,
                -1035236496,
                -949202525,
                -778901479,
                -694614492,
                -200395387,
                275423344,
                430227734,
                506948616,
                659060556,
                883997877,
                958139571,
                1322822218,
                1537002063,
                1747873779,
                1955562222,
                2024104815,
                -2067236844,
                -1933114872,
                -1866530822,
                -1538233109,
                -1090935817,
                -965641998
            };
    private static final int[] INITIAL_HASH =
            new int[] {
                1779033703,
                -1150833019,
                1013904242,
                -1521486534,
                1359893119,
                -1694144372,
                528734635,
                1541459225
            };
    private static final int BLOCK_BITS = 512;
    private static final int BLOCK_BYTES = 64;

    public static byte[] digest(byte[] data) {

        int[] schedule = new int[64];

        int[] hash = new int[8];

        int[] working = new int[8];
        System.arraycopy(INITIAL_HASH, 0, hash, 0, INITIAL_HASH.length);
        int[] words = padToWords(data);
        int blockIndex = 0;
        int wordCount = words.length;

        for (int blockCount = wordCount / 16; blockIndex < blockCount; blockIndex++) {

            int blockOffset = blockIndex * 16;

            System.arraycopy(words, blockOffset, schedule, 0, 16);

            for (int t = 16; t < schedule.length; t++) {
                int sigma1Value = smallSigma1(schedule[t - 2]);

                int sumWithW7 = sigma1Value + schedule[t - 7];

                int sumWithSigma0 = sumWithW7 + smallSigma0(schedule[t - 15]);

                schedule[t] = sumWithSigma0 + schedule[t - 16];
            }

            System.arraycopy(hash, 0, working, 0, hash.length);

            for (int round = 0; round < schedule.length; round++) {

                int hPlusSigma1 = working[7] + bigSigma1(working[4]);
                int e = working[4];
                int f = working[5];

                int temp1 =
                        hPlusSigma1
                                + choose(e, f, working[6])
                                + ROUND_CONSTANTS[round]
                                + schedule[round];
                int temp2 = bigSigma0(working[0]) + majority(working[0], working[1], working[2]);
                System.arraycopy(working, 0, working, 1, working.length - 1);
                working[4] += temp1;
                working[0] = temp1 + temp2;
            }

            for (int i = 0; i < hash.length; i++) {
                hash[i] += working[i];
            }
        }

        return wordsToBytes(hash);
    }

    public static int[] padToWords(byte[] data) {
        int lengthForRemainder = data.length;

        int remainderBytes = lengthForRemainder % 64;
        int lengthForBlocks = data.length;

        int fullBlocks = lengthForBlocks / 64;
        int remainderWithMarker = remainderBytes + 1;

        int tailWithLength = remainderWithMarker + 8;

        int blockCount = fullBlocks + (tailWithLength > 64 ? 2 : 1);

        IntBuffer wordBuffer = IntBuffer.allocate(blockCount * 16);
        SimpleByteBuffer reader = SimpleByteBuffer.wrap(data);
        int wordIndex = 0;

        for (int wholeWordCount = data.length / 4; wordIndex < wholeWordCount; wordIndex++) {
            wordBuffer.put(reader.readInt());
        }

        SimpleByteBuffer tailBuffer = SimpleByteBuffer.allocate(4);
        SimpleByteBuffer tailWithData = tailBuffer.writeBuffer(reader);

        tailWithData.writeByte((byte) -128).rewind();
        wordBuffer.put(tailBuffer.readInt());
        wordBuffer.position(wordBuffer.capacity() - 2);
        long bitLength = data.length * 8L;

        wordBuffer.put((int) (bitLength >>> 32));
        wordBuffer.put((int) bitLength);
        return wordBuffer.array();
    }

    private static int smallSigma1(int x) {

        int rotr17 = Integer.rotateRight(x, 17);

        int rotr17Xor19 = rotr17 ^ Integer.rotateRight(x, 19);

        return rotr17Xor19 ^ x >>> 10;
    }

    private static int bigSigma1(int x) {

        int rotr6 = Integer.rotateRight(x, 6);

        int rotr6Xor11 = rotr6 ^ Integer.rotateRight(x, 11);

        return rotr6Xor11 ^ Integer.rotateRight(x, 25);
    }

    private static byte[] wordsToBytes(int[] words) {
        SimpleByteBuffer buffer = SimpleByteBuffer.allocate(words.length * 4);

        for (int word : words) {
            buffer.writeInt(word);
        }

        return buffer.getArray();
    }

    private static int majority(int a, int b, int c) {
        return a & b | a & c | b & c;
    }

    private static int bigSigma0(int x) {
        int rotr2 = Integer.rotateRight(x, 2);

        int rotr2Xor13 = rotr2 ^ Integer.rotateRight(x, 13);

        return rotr2Xor13 ^ Integer.rotateRight(x, 22);
    }

    private static int smallSigma0(int x) {

        int rotr7 = Integer.rotateRight(x, 7);

        return rotr7 ^ Integer.rotateRight(x, 18) ^ x >>> 3;
    }

    private static int choose(int x, int y, int z) {
        return x & y | ~x & z;
    }
}
