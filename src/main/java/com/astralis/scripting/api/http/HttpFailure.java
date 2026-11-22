package com.astralis.scripting.api.http;

import java.util.Objects;

public record HttpFailure(HttpFailure.Code code, String message) {
    public HttpFailure(HttpFailure.Code code, String message) {
        Objects.requireNonNull(code, "code");
        message = Objects.requireNonNullElse(message, "HTTP request failed.");
        this.code = code;
        this.message = message;
    }

    public enum Code {
        CANCELLED,
        DENIED,
        INVALID_REQUEST,
        RATE_LIMITED,
        TIMEOUT,
        CONNECTION_FAILED,
        INVALID_RESPONSE,
        RESPONSE_TOO_LARGE;
    }
}
