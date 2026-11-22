package astralis.util.render;

public enum DrawCommandType {
    RECTANGLE,
    TEXT,
    TEXTURE,
    PUSH,
    POP,
    TRANSLATE,
    SCALE,
    ROTATE,
    SCISSOR_START,
    SCISSOR_END,
    DEPTH_WRITE,
    DEPTH_READ,
    DEPTH_END,
    CUSTOM,
    FLUSH;
}
