package astralis.scripting;

import com.astralis.scripting.api.http.HttpFailure.Code;

public final class HttpFailureException extends Exception {
    public final Code failureCode;

    public HttpFailureException(Code code, String message) {
        super(message);
        this.failureCode = code;
    }

    public HttpFailureException(Code code, String message, Throwable cause) {
        super(message, cause);
        this.failureCode = code;
    }
}
