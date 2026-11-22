package astralis.network.websocket;

public class InvalidDataException extends Exception {
    private static long serialVersionUID;
    private final int closeCode;

    public InvalidDataException(int closeCode, String message, Throwable cause) {
        super(message, cause);
        this.closeCode = closeCode;
    }

    public InvalidDataException(int closeCode, Throwable cause) {
        super(cause);
        this.closeCode = closeCode;
    }

    public InvalidDataException(int closeCode, String message) {
        super(message);
        this.closeCode = closeCode;
    }

    public InvalidDataException(int closeCode) {
        this.closeCode = closeCode;
    }

    public int getCloseCode() {
        return this.closeCode;
    }

    static {
        serialVersionUID = 3731842424390998726L;
    }
}
