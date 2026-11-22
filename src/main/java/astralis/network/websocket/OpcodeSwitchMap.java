package astralis.network.websocket;

public class OpcodeSwitchMap {
    static int[] opcodeCases;

    static {
        opcodeCases = new int[Opcode.values().length];

        try {
            opcodeCases[Opcode.PING.ordinal()] = 1;
        } catch (NoSuchFieldError ignored) {
        }

        try {
            opcodeCases[Opcode.PONG.ordinal()] = 2;
        } catch (NoSuchFieldError ignored) {
        }

        try {
            opcodeCases[Opcode.TEXT.ordinal()] = 3;
        } catch (NoSuchFieldError ignored) {
        }

        try {
            opcodeCases[Opcode.BINARY.ordinal()] = 4;
        } catch (NoSuchFieldError ignored) {
        }

        try {
            opcodeCases[Opcode.CLOSING.ordinal()] = 5;
        } catch (NoSuchFieldError ignored) {
        }

        try {
            opcodeCases[Opcode.CONTINUOUS.ordinal()] = 6;
        } catch (NoSuchFieldError ignored) {
        }
    }
}
