package astralis.scripting.service;

import com.astralis.scripting.api.http.HttpRequest.BodyType;
import com.astralis.scripting.api.http.HttpRequest.Method;

import java.util.Map;

public record PreparedHttpRequest(
        String host,
        String target,
        Method method,
        Map<String, String> headers,
        byte[] body,
        BodyType bodyType) {
    public PreparedHttpRequest(
            String host,
            String target,
            Method method,
            Map<String, String> headers,
            byte[] body,
            BodyType bodyType) {
        headers = Map.copyOf(headers);
        body = (byte[]) body.clone();
        this.host = host;
        this.target = target;
        this.method = method;
        this.headers = headers;
        this.body = body;
        this.bodyType = bodyType;
    }

    public byte[] getBodyCopy() {
        return (byte[]) this.body.clone();
    }

    public String getHost() {
        return this.host;
    }

    public String getTarget() {
        return this.target;
    }

    public Method getMethod() {
        return this.method;
    }

    public Map<String, String> getHeaders() {
        return this.headers;
    }

    public BodyType getBodyType() {
        return this.bodyType;
    }
}
