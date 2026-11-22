package astralis.network.websocket;

public enum Opcode {
    CONTINUOUS,
    TEXT,
    BINARY,
    PING,
    PONG,
    CLOSING;
}
