package astralis.network.websocket;

public class InvalidFrameException extends InvalidDataException {

    public InvalidFrameException(String message, Throwable cause) {
        super(1002, message, cause);
    }

    public InvalidFrameException(Throwable cause) {
        super(1002, cause);
    }

    public InvalidFrameException(String message) {
        super(1002, message);
    }

    public InvalidFrameException() {
        super(1002);
    }
}
