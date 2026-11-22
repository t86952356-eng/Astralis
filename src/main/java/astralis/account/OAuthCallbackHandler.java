package astralis.account;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;

public record OAuthCallbackHandler(String clientId) implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws java.io.IOException {

        try {
            String method = exchange.getRequestMethod();

            if (method.equalsIgnoreCase("GET")) {
                List<NameValuePair> params =
                        URLEncodedUtils.parse(
                                exchange.getRequestURI(), StandardCharsets.UTF_8.name());
                String code = null;

                for (NameValuePair param : params) {

                    if ("code".equals(param.getName())) {
                        code = param.getValue();
                        break;
                    }
                }

                if (code != null && !code.isEmpty()) {
                    String page;
                    if (this.exchangeCode(code, this.clientId)) {
                        page = this.buildSuccessPage();
                    } else {

                        page =
                                this.buildErrorPage(
                                        "Microsoft did not return a usable refresh token.");
                    }

                    this.sendHtml(exchange, page);
                    return;
                }

                this.sendHtml(exchange, this.buildErrorPage("No authorization code was provided."));
                return;
            }

            this.sendHtml(exchange, "<html><body><h3>Invalid request method.</h3></body></html>");
        } catch (Exception e) {
            e.printStackTrace();

            this.sendHtml(
                    exchange,
                    this.buildErrorPage("An unexpected error occurred while completing login."));
            return;
        } finally {
            MicrosoftAuth.stopCallbackServer();
        }
    }

    private boolean exchangeCode(String code, String clientId) {

        LinkedHashMap form = new LinkedHashMap();

        form.put("client_id", clientId);

        form.put("code", code);

        form.put("grant_type", "authorization_code");

        form.put("redirect_uri", "http://localhost:8247");

        String responseBody =
                HttpUtil.post(
                        "https://login.live.com/oauth20_token.srf",
                        MicrosoftAuth.encodeForm(form),
                        false);
        MicrosoftTokenResponse tokenResponse =
                MicrosoftAuth.gson.fromJson(responseBody, MicrosoftTokenResponse.class);
        if (tokenResponse != null
                && tokenResponse.refreshToken != null
                && !tokenResponse.refreshToken.isBlank()) {
            MicrosoftAuth.tokenCallback.accept(tokenResponse.refreshToken);
            return true;
        } else {
            MicrosoftAuth.tokenCallback.accept(null);
            return false;
        }
    }

    private String buildSuccessPage() {

        return "<!doctype html>\n<html lang=\"en\">\n  <head>\n    <meta charset=\"utf-8\">\n    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n    <title>Astralis</title>\n    <style>\n      :root {\n        color-scheme: dark;\n        --background: #0b0c11;\n        --text: #f3f3f7;\n        --muted: #8c8f9d;\n      }\n      * { box-sizing: border-box; }\n      body {\n        min-height: 100vh;\n        margin: 0;\n        display: grid;\n        place-items: center;\n        background: var(--background);\n        color: var(--text);\n        font: 15px/1.5 Inter, ui-sans-serif, system-ui, sans-serif;\n      }\n      main {\n        width: min(88vw, 430px);\n        padding: 38px 34px;\n        text-align: center;\n      }\n      img { width: 112px; height: 112px; display: block; margin: 0 auto 22px; object-fit: contain; }\n      h1 { margin: 0; font-size: 25px; font-weight: 600; letter-spacing: -.03em; }\n      p { margin: 10px 0 0; color: var(--muted); }\n    </style>\n  </head>\n  <body>\n    <main>\n      <img src=\"%s\" alt=\"Astralis\">\n      <h1>Continue in Astralis</h1>\n      <p>You may close this page now. Your account was successfully added.</p>\n    </main>\n  </body>\n</html>\n"
                .formatted(this.getIconDataUri());
    }

    private String buildErrorPage(String message) {

        return "<!doctype html>\n<html lang=\"en\">\n  <head><meta charset=\"utf-8\"><meta name=\"viewport\" content=\"width=device-width, initial-scale=1\"><title>Astralis | Login failed</title></head>\n  <body style=\"margin:0;min-height:100vh;display:grid;place-items:center;background:#08090d;color:#f5f7ff;font:15px/1.5 system-ui,sans-serif;text-align:center\">\n    <main style=\"width:min(88vw,420px);padding:38px 32px;border:1px solid #343642;border-radius:22px;background:#171923;box-shadow:0 24px 70px #0008\">\n      <div style=\"font-size:12px;letter-spacing:.2em;text-transform:uppercase;color:#9b7cff;font-weight:700\">Astralis</div>\n      <h1 style=\"margin:22px 0 8px;font-size:26px\">Login failed</h1>\n      <p style=\"margin:0;color:#969aaa\">%s</p>\n    </main>\n  </body>\n</html>\n"
                .formatted(message);
    }

    private String getIconDataUri() {

        try {

            try (InputStream stream =
                    MicrosoftAuth.class.getResourceAsStream("/assets/astralis-recode/icon.png")) {
                if (stream == null) {
                    return "";
                }

                String encoded = Base64.getEncoder().encodeToString(stream.readAllBytes());

                return "data:image/png;base64," + encoded;
            }
        } catch (IOException ignored) {
            return "";
        }
    }

    private void sendHtml(HttpExchange exchange, String html) throws java.io.IOException {

        OutputStream outputStream = exchange.getResponseBody();
        byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
        Headers headers = exchange.getResponseHeaders();

        headers.add("Content-Type", "text/html; charset=utf-8");

        exchange.sendResponseHeaders(200, bytes.length);
        outputStream.write(bytes);
        outputStream.flush();
        outputStream.close();
    }

    public String getClientId() {
        return this.clientId;
    }
}
