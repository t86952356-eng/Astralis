package astralis.util;

public class Base64Decoder {

    public static byte[] decode(String encoded) {

        if (encoded != null && !encoded.isEmpty()) {
            int charCount = 0;

            for (int i = 0; i < encoded.length(); i++) {
                char character = encoded.charAt(i);
                if (!Character.isWhitespace(character)) {
                    charCount++;
                }
            }

            if (charCount % 4 != 0) {

                throw new IllegalArgumentException("Invalid Base64!");
            }

            int[] decodeTable = new int[256];
            int alphabetIndex = 0;

            while (true) {

                if (alphabetIndex
                        >= "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
                                .length()) {

                    decodeTable[61] = 0;
                    int paddingCount = 0;
                    int lastIndex = encoded.length() - 1;

                    while (lastIndex >= 0 && Character.isWhitespace(encoded.charAt(lastIndex))) {
                        lastIndex--;
                    }

                    if (lastIndex >= 0) {
                        char lastChar = encoded.charAt(lastIndex);

                        if (lastChar == '=') {
                            paddingCount++;
                            if (--lastIndex >= 0) {
                                char secondLastChar = encoded.charAt(lastIndex);

                                if (secondLastChar == '=') {
                                    paddingCount++;
                                }
                            }
                        }
                    }

                    int totalBits = charCount * 6;

                    int outputLength = totalBits / 8 - paddingCount;
                    byte[] output = new byte[outputLength];
                    int[] group = new int[4];
                    int outputIndex = 0;
                    int groupSize = 0;

                    for (int inputIndex = 0; inputIndex < encoded.length(); inputIndex++) {
                        char inputChar = encoded.charAt(inputIndex);
                        if (!Character.isWhitespace(inputChar)) {

                            if (inputChar >= 256) {

                                throw new IllegalArgumentException("Invalid Base64 character");
                            }

                            int sextet = decodeTable[inputChar];
                            group[groupSize++] = sextet;
                            if (groupSize == 4) {
                                int firstValue = group[0];

                                int firstShifted = firstValue << 18;
                                int secondValue = group[1];

                                int partialBits = firstShifted | secondValue << 12;
                                int thirdValue = group[2];

                                int combined = partialBits | thirdValue << 6 | group[3];
                                if (outputIndex < outputLength) {
                                    int firstWriteIndex = outputIndex++;

                                    int firstByteBits = combined >>> 16;

                                    output[firstWriteIndex] = (byte) (firstByteBits & 0xFF);
                                }

                                if (outputIndex < outputLength) {
                                    int secondWriteIndex = outputIndex++;

                                    int secondByteBits = combined >>> 8;

                                    output[secondWriteIndex] = (byte) (secondByteBits & 0xFF);
                                }

                                if (outputIndex < outputLength) {
                                    int thirdWriteIndex = outputIndex++;

                                    output[thirdWriteIndex] = (byte) (combined & 0xFF);
                                }

                                groupSize = 0;
                            }
                        }
                    }

                    return output;
                }

                decodeTable[
                                "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
                                        .charAt(alphabetIndex)] =
                        alphabetIndex++;
            }
        } else {
            return new byte[0];
        }
    }
}
