package astralis.util;

public class CustomBase64 {
    private static final String ALPHABET =
            "57jyo2YgC3lL9r1RmZkNGtPTzp4d6wxVnA+SuUD8MiBQJc/WvqKIEesXa0OfhbFH";
    private static final char PADDING = '#';

    public static String encode(byte[] data) {

        if (data != null && data.length != 0) {

            char[] alphabet =
                    "57jyo2YgC3lL9r1RmZkNGtPTzp4d6wxVnA+SuUD8MiBQJc/WvqKIEesXa0OfhbFH"
                            .toCharArray();
            int encodedLength = (data.length + 2) / 3 * 4;
            char[] output = new char[encodedLength];
            int inputIndex = 0;
            int outputIndex = 0;
            int fullGroups = data.length / 3;

            for (int groupIndex = 0; groupIndex < fullGroups; groupIndex++) {
                byte firstByte = data[inputIndex++];

                int firstUnsigned = firstByte & 255;

                int firstShifted = firstUnsigned << 16;
                byte secondByte = data[inputIndex++];

                int secondUnsigned = secondByte & 255;

                int partialBits = firstShifted | secondUnsigned << 8;
                byte thirdByte = data[inputIndex++];

                int combined = partialBits | thirdByte & 255;
                int firstCharIndex = outputIndex++;

                int firstSextet = combined >>> 18;

                output[firstCharIndex] = alphabet[firstSextet & 63];
                int secondCharIndex = outputIndex++;

                int secondSextet = combined >>> 12;

                output[secondCharIndex] = alphabet[secondSextet & 63];
                int thirdCharIndex = outputIndex++;

                int thirdSextet = combined >>> 6;

                output[thirdCharIndex] = alphabet[thirdSextet & 63];
                int fourthCharIndex = outputIndex++;

                output[fourthCharIndex] = alphabet[combined & 63];
            }

            int remainder = data.length % 3;
            if (remainder == 1) {
                byte singleByte = data[inputIndex];

                int singleUnsigned = singleByte & 255;

                int singleBits = singleUnsigned << 16;
                int singleCharIndex1 = outputIndex++;

                int singleSextet = singleBits >>> 18;

                output[singleCharIndex1] = alphabet[singleSextet & 63];
                int singleCharIndex2 = outputIndex++;

                singleSextet = singleBits >>> 12;

                output[singleCharIndex2] = alphabet[singleSextet & 63];
                int singleCharIndex3 = outputIndex++;
                output[singleCharIndex3] = '#';
                output[outputIndex] = '#';
            } else if (remainder == 2) {
                byte pairFirstByte = data[inputIndex++];

                int pairFirstUnsigned = pairFirstByte & 255;

                int pairFirstShifted = pairFirstUnsigned << 16;
                byte pairSecondByte = data[inputIndex];

                int pairSecondUnsigned = pairSecondByte & 255;

                int pairBits = pairFirstShifted | pairSecondUnsigned << 8;
                int pairCharIndex1 = outputIndex++;

                int pairSextet1 = pairBits >>> 18;

                output[pairCharIndex1] = alphabet[pairSextet1 & 63];
                int pairCharIndex2 = outputIndex++;

                int pairSextet2 = pairBits >>> 12;

                output[pairCharIndex2] = alphabet[pairSextet2 & 63];
                int pairCharIndex3 = outputIndex++;

                int pairSextet3 = pairBits >>> 6;

                output[pairCharIndex3] = alphabet[pairSextet3 & 63];

                output[outputIndex] = '#';
            }

            return new String(output);
        } else {
            return "";
        }
    }

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
                        >= "57jyo2YgC3lL9r1RmZkNGtPTzp4d6wxVnA+SuUD8MiBQJc/WvqKIEesXa0OfhbFH"
                                .length()) {

                    decodeTable[35] = 0;
                    int paddingCount = 0;
                    int lastIndex = encoded.length() - 1;

                    while (lastIndex >= 0 && Character.isWhitespace(encoded.charAt(lastIndex))) {
                        lastIndex--;
                    }

                    if (lastIndex >= 0) {
                        char lastChar = encoded.charAt(lastIndex);

                        if (lastChar == '#') {
                            paddingCount++;
                            if (--lastIndex >= 0) {
                                char secondLastChar = encoded.charAt(lastIndex);

                                if (secondLastChar == '#') {
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
                                "57jyo2YgC3lL9r1RmZkNGtPTzp4d6wxVnA+SuUD8MiBQJc/WvqKIEesXa0OfhbFH"
                                        .charAt(alphabetIndex)] =
                        alphabetIndex++;
            }
        } else {
            return new byte[0];
        }
    }
}
