package com.astralis.scripting.api.http;

import java.util.function.Consumer;

public interface ScriptHttp {
    HttpCall send(
            HttpRequest request,
            Consumer<HttpResponse> onResponse,
            Consumer<HttpFailure> onFailure);
}
