package com.astralis.scripting.api.http;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record HttpResponse(int status, Map<String, List<String>> headers, String body) {
    public HttpResponse(int status, Map<String, List<String>> headers, String body) {
        LinkedHashMap copiedHeaders = new LinkedHashMap();
        Objects.requireNonNull(headers, "headers")
                .forEach((name, values) -> copiedHeaders.put(name, List.copyOf(values)));
        headers = Map.copyOf(copiedHeaders);
        body = Objects.requireNonNull(body, "body");
        this.status = status;
        this.headers = headers;
        this.body = body;
    }

    public String firstHeader(String name) {
        return name == null
                ? ""
                : this.headers.entrySet().stream()
                        .filter(entry -> entry.getKey().equalsIgnoreCase(name))
                        .findFirst()
                        .flatMap(matchedEntry -> matchedEntry.getValue().stream().findFirst())
                        .orElse("");
    }
}
