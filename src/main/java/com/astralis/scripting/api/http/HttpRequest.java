package com.astralis.scripting.api.http;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public record HttpRequest(
        String url,
        HttpRequest.Method method,
        Map<String, String> headers,
        String body,
        HttpRequest.BodyType bodyType) {
    public HttpRequest(
            String url,
            HttpRequest.Method method,
            Map<String, String> headers,
            String body,
            HttpRequest.BodyType bodyType) {
        url = Objects.requireNonNull(url, "url");
        method = Objects.requireNonNull(method, "method");
        headers = Map.copyOf(Objects.requireNonNull(headers, "headers"));
        body = Objects.requireNonNullElse(body, "");
        bodyType = Objects.requireNonNull(bodyType, "bodyType");
        this.url = url;
        this.method = method;
        this.headers = headers;
        this.body = body;
        this.bodyType = bodyType;
    }

    public static HttpRequest.Builder builder(String url) {
        return new HttpRequest.Builder(url);
    }

    public enum BodyType {
        NONE,
        TEXT,
        JSON;
    }

    public static final class Builder {
        private final Map<String, String> headers = new LinkedHashMap<>();
        private final String url;
        private HttpRequest.Method method = HttpRequest.Method.GET;
        private String body = "";
        private HttpRequest.BodyType bodyType = HttpRequest.BodyType.NONE;

        private Builder(String url) {
            this.url = Objects.requireNonNull(url, "url");
        }

        public HttpRequest.Builder header(String name, String value) {
            this.headers.put(
                    Objects.requireNonNull(name, "name"), Objects.requireNonNull(value, "value"));
            return this;
        }

        public HttpRequest.Builder get() {
            this.method = HttpRequest.Method.GET;
            this.body = "";
            this.bodyType = HttpRequest.BodyType.NONE;
            return this;
        }

        public HttpRequest.Builder text(String body) {
            this.method = HttpRequest.Method.POST;
            this.body = Objects.requireNonNull(body, "body");
            this.bodyType = HttpRequest.BodyType.TEXT;
            return this;
        }

        public HttpRequest.Builder json(String body) {
            this.method = HttpRequest.Method.POST;
            this.body = Objects.requireNonNull(body, "body");
            this.bodyType = HttpRequest.BodyType.JSON;
            return this;
        }

        public HttpRequest build() {
            return new HttpRequest(this.url, this.method, this.headers, this.body, this.bodyType);
        }
    }

    public enum Method {
        GET,
        POST;
    }
}
