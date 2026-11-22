package astralis.scripting.service;

import astralis.scripting.HttpFailureException;

import com.astralis.scripting.InstanceAccess;
import com.astralis.scripting.api.http.HttpCall;
import com.astralis.scripting.api.http.HttpFailure;
import com.astralis.scripting.api.http.HttpFailure.Code;
import com.astralis.scripting.api.http.HttpRequest.BodyType;
import com.astralis.scripting.api.http.HttpRequest.Method;
import com.astralis.scripting.api.http.HttpResponse;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import javax.net.ssl.SNIHostName;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

public final class HttpCallTask implements HttpCall, Runnable {
    private final Consumer<HttpFailure> onFailure;
    private final Consumer<HttpResponse> onSuccess;
    private final PreparedHttpRequest request;
    private volatile Socket socket;
    private volatile boolean cancelled;
    private volatile boolean done;
    private volatile boolean timedOut;
    final ScriptHttpService this$0;

    public HttpCallTask(
            ScriptHttpService service,
            PreparedHttpRequest request,
            Consumer<HttpResponse> onSuccess,
            Consumer<HttpFailure> onFailure) {
        Objects.requireNonNull(service);
        this.this$0 = service;
        super();
        this.request = request;
        this.onSuccess = onSuccess;
        this.onFailure = onFailure;
    }

    @Override
    public void run() {
        ScheduledExecutorService scheduler = ScriptHttpService.timeoutScheduler;
        Runnable timeoutTask =
                () -> {
                    this.timedOut = true;
                    this.cancelAndClose();
                };

        ScheduledFuture timeoutFuture = scheduler.schedule(timeoutTask, 12L, TimeUnit.SECONDS);

        try {
            if (this.cancelled) {
                HttpFailureException failure;
                Code failureCode = this.timedOut ? Code.TIMEOUT : Code.CANCELLED;
                String failureMessage;
                if (this.timedOut) {

                    failureMessage = "The HTTP request timed out.";
                } else {

                    failureMessage = "HTTP request cancelled.";
                }

                failure = new HttpFailureException(failureCode, failureMessage);
                throw failure;
            } else {
                InetAddress[] addresses = InetAddress.getAllByName(this.request.getHost());
                if (addresses.length != 0) {
                    int addressCount = addresses.length;
                    if (addressCount <= 16) {
                        for (InetAddress address : addresses) {
                            ScriptHttpService.checkPublicAddress(address);
                        }

                        HttpFailureException lastFailure =
                                new HttpFailureException(
                                        Code.CONNECTION_FAILED,
                                        "Could not connect to the HTTP server.");

                        for (InetAddress address : addresses) {
                            if (this.cancelled) {
                                HttpFailureException cancelFailure;
                                Code cancelCode = this.timedOut ? Code.TIMEOUT : Code.CANCELLED;
                                String cancelMessage;
                                if (this.timedOut) {

                                    cancelMessage = "The HTTP request timed out.";
                                } else {

                                    cancelMessage = "HTTP request cancelled.";
                                }

                                cancelFailure = new HttpFailureException(cancelCode, cancelMessage);
                                throw cancelFailure;
                            }

                            try {
                                this.deliver(this.execute(address));
                                return;
                            } catch (HttpFailureException httpFailure) {
                                lastFailure = httpFailure;
                                if (httpFailure.failureCode != Code.CONNECTION_FAILED) {
                                    throw httpFailure;
                                }
                            } catch (IOException ioException) {

                                lastFailure =
                                        new HttpFailureException(
                                                Code.CONNECTION_FAILED,
                                                "Could not connect to the HTTP server.",
                                                ioException);
                            }
                        }

                        throw lastFailure;
                    }
                }

                throw new HttpFailureException(
                        Code.CONNECTION_FAILED, "HTTP domain could not be resolved.");
            }
        } catch (HttpFailureException failure) {
            Code code = this.timedOut ? Code.TIMEOUT : failure.failureCode;
            String message;
            if (this.timedOut) {
                message = "The HTTP request timed out.";
            } else {
                message = failure.getMessage();
            }

            this.fail(code, message);
        } catch (IOException | RuntimeException e) {
            Code code = this.timedOut ? Code.TIMEOUT : Code.CONNECTION_FAILED;
            String message;
            if (this.timedOut) {
                message = "The HTTP request timed out.";
            } else {
                message = "The HTTP request failed.";
            }

            this.fail(code, message);
        } finally {
            this.done = true;
            timeoutFuture.cancel(false);
            this.closeSocket();
            synchronized (this.this$0) {
                this.this$0.activeCalls.remove(this);
            }
        }
    }

    private HttpResponse execute(InetAddress address) throws IOException, HttpFailureException {
        Socket rawSocket = new Socket();
        this.socket = rawSocket;

        InetSocketAddress endpoint = new InetSocketAddress(address, 443);

        rawSocket.connect(endpoint, 5000);
        rawSocket.setSoTimeout(8000);
        SSLSocketFactory socketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
        String host = this.request.getHost();
        SSLSocket sslSocket = (SSLSocket) socketFactory.createSocket(rawSocket, host, 443, true);
        this.socket = sslSocket;
        SSLParameters sslParameters = sslSocket.getSSLParameters();

        sslParameters.setEndpointIdentificationAlgorithm("HTTPS");
        sslParameters.setServerNames(List.of(new SNIHostName(this.request.getHost())));
        String[] protocols = new String[2];

        protocols[0] = "TLSv1.3";

        protocols[1] = "TLSv1.2";
        sslParameters.setProtocols(protocols);
        sslSocket.setSSLParameters(sslParameters);
        sslSocket.startHandshake();
        OutputStream out = sslSocket.getOutputStream();
        StringBuilder methodPart = new StringBuilder().append(this.request.getMethod().name());
        StringBuilder targetPart = methodPart.append(' ').append(this.request.getTarget());
        StringBuilder versionPart = targetPart.append(" HTTP/1.1\r\n");
        StringBuilder hostPart = versionPart.append("Host: ").append(this.request.getHost());
        StringBuilder hostLineEnd = hostPart.append("\r\n");
        StringBuilder userAgentPart = hostLineEnd.append("User-Agent: Astralis-Scripting/1\r\n");
        StringBuilder acceptPart =
                userAgentPart.append("Accept: application/json, text/plain, text/*\r\n");
        StringBuilder encodingPart = acceptPart.append("Accept-Encoding: identity\r\n");
        StringBuilder requestBuilder = encodingPart.append("Connection: close\r\n");
        this.request
                .getHeaders()
                .forEach(
                        (headerName, headerValue) -> {
                            StringBuilder namePart = requestBuilder.append(headerName);

                            StringBuilder valuePart = namePart.append(": ").append(headerValue);

                            valuePart.append("\r\n");
                        });
        if (this.request.getMethod() == Method.POST) {
            StringBuilder contentTypeLabel = requestBuilder.append("Content-Type: ");
            String contentType;
            if (this.request.getBodyType() == BodyType.JSON) {
                contentType = "application/json";
            } else {
                contentType = "text/plain";
            }

            StringBuilder contentTypeEntry = contentTypeLabel.append(contentType);
            StringBuilder contentLengthEntry =
                    contentTypeEntry
                            .append("; charset=utf-8\r\nContent-Length: ")
                            .append(this.request.getBodyCopy().length);
            contentLengthEntry.append("\r\n");
        }

        requestBuilder.append("\r\n");
        out.write(requestBuilder.toString().getBytes(StandardCharsets.ISO_8859_1));
        if (this.request.getBodyCopy().length > 0) {
            out.write(this.request.getBodyCopy());
        }

        out.flush();
        return this.readResponse(new BufferedInputStream(sslSocket.getInputStream()));
    }

    private HttpResponse readResponse(InputStream input) throws IOException, HttpFailureException {

        String statusLine = this.readLine(input, 8192);

        while (true) {

            if (!statusLine.startsWith("HTTP/1.1 100 ")) {

                if (!statusLine.matches("HTTP/1\\.[01] [0-9]{3}( .*)?")) {

                    throw new HttpFailureException(
                            Code.INVALID_RESPONSE, "Invalid HTTP status line.");
                }

                int statusCode = Integer.parseInt(statusLine.substring(9, 12));
                Map headers = this.readHeaders(input);

                if (statusCode >= 300) {

                    if (statusCode < 400) {

                        throw new HttpFailureException(
                                Code.DENIED, "HTTP redirects are not allowed.");
                    }
                }

                String contentEncoding =
                        ScriptHttpService.getFirstHeader(headers, "content-encoding");
                if (!contentEncoding.isEmpty()) {

                    if (!contentEncoding.equalsIgnoreCase("identity")) {

                        throw new HttpFailureException(
                                Code.INVALID_RESPONSE,
                                "Compressed HTTP responses are not supported.");
                    }
                }

                String contentType =
                        ScriptHttpService.getFirstHeader(headers, "content-type")
                                .toLowerCase(Locale.ROOT);
                if (!contentType.isEmpty()) {

                    if (!contentType.startsWith("text/")) {

                        if (!contentType.startsWith("application/json")) {

                            if (!contentType.contains("+json")) {

                                throw new HttpFailureException(
                                        Code.INVALID_RESPONSE,
                                        "Only text and JSON HTTP responses are allowed.");
                            }
                        }
                    }
                }

                label78:
                {
                    String transferEncoding =
                            ScriptHttpService.getFirstHeader(headers, "transfer-encoding");

                    byte[] body;
                    if (transferEncoding.equalsIgnoreCase("chunked")) {
                        body = this.readChunkedBody(input);
                    } else {

                        if (!ScriptHttpService.getFirstHeader(headers, "transfer-encoding")
                                .isEmpty()) {

                            throw new HttpFailureException(
                                    Code.INVALID_RESPONSE, "Unsupported HTTP transfer encoding.");
                        }

                        if (!ScriptHttpService.getFirstHeader(headers, "content-length")
                                .isEmpty()) {
                            long contentLength;
                            try {

                                contentLength =
                                        Long.parseLong(
                                                ScriptHttpService.getFirstHeader(
                                                        headers, "content-length"));
                            } catch (NumberFormatException e) {

                                throw new HttpFailureException(
                                        Code.INVALID_RESPONSE, "Invalid HTTP content length.", e);
                            }

                            if (contentLength < 0L) {
                                break label78;
                            }

                            if (contentLength > 1048576L) {
                                break label78;
                            }

                            body = this.readBodyBytes(input, (int) contentLength, true);
                        } else {
                            body = this.readBodyBytes(input, 1048576, false);
                        }
                    }

                    try {
                        return new HttpResponse(
                                statusCode,
                                headers,
                                StandardCharsets.UTF_8
                                        .newDecoder()
                                        .onMalformedInput(CodingErrorAction.REPORT)
                                        .onUnmappableCharacter(CodingErrorAction.REPORT)
                                        .decode(ByteBuffer.wrap(body))
                                        .toString());
                    } catch (CharacterCodingException e) {

                        throw new HttpFailureException(
                                Code.INVALID_RESPONSE, "HTTP response is not valid UTF-8 text.", e);
                    }
                }

                throw new HttpFailureException(
                        Code.RESPONSE_TOO_LARGE, "HTTP responses cannot exceed 1 MiB.");
            }

            this.readHeaders(input);
            statusLine = this.readLine(input, 8192);
        }
    }

    private Map<String, List<String>> readHeaders(InputStream input)
            throws IOException, HttpFailureException {
        LinkedHashMap<String, List<String>> headers = new LinkedHashMap<>();
        int totalHeaderBytes = 0;

        while (true) {

            String line = this.readLine(input, 8192);
            totalHeaderBytes += line.length() + 2;
            if (totalHeaderBytes <= 32768) {
                int headerCount = headers.size();
                if (headerCount <= 100) {
                    if (line.isEmpty()) {
                        return Map.copyOf(headers);
                    }

                    if (!Character.isWhitespace(line.charAt(0))) {
                        if (line.indexOf(58) >= 1) {

                            String name =
                                    line.substring(0, line.indexOf(58))
                                            .trim()
                                            .toLowerCase(Locale.ROOT);

                            String value = line.substring(line.indexOf(58) + 1).trim();
                            if (name.matches("[A-Za-z0-9!#$%&'*+.^_`|~-]+")
                                    && !value.chars()
                                            .anyMatch(
                                                    ch -> {
                                                        if (ch < 32) {

                                                            if (ch != 9) {
                                                                return true;
                                                            }
                                                        }

                                                        return ch == 127;
                                                    })) {
                                if (!name.equals("set-cookie")) {
                                    if (!name.equals("set-cookie2")) {
                                        headers.computeIfAbsent(name, key -> new ArrayList<>())
                                                .add(value);
                                    }
                                }
                                continue;
                            }

                            throw new HttpFailureException(
                                    Code.INVALID_RESPONSE, "Invalid HTTP response header.");
                        }
                    }

                    throw new HttpFailureException(
                            Code.INVALID_RESPONSE, "Invalid HTTP response header.");
                }
            }

            throw new HttpFailureException(
                    Code.INVALID_RESPONSE, "HTTP response headers are too large.");
        }
    }

    private byte[] readChunkedBody(InputStream input) throws IOException, HttpFailureException {
        ByteArrayOutputStream body = new ByteArrayOutputStream();

        while (true) {

            String rawLine = this.readLine(input, 128);
            String lineWithoutExtension;
            if (rawLine.contains(";")) {

                lineWithoutExtension = rawLine.substring(0, rawLine.indexOf(59));
            } else {
                lineWithoutExtension = rawLine;
            }

            String sizeField = lineWithoutExtension;

            int chunkSize;
            try {
                String trimmedSize = sizeField.trim();
                chunkSize = Integer.parseUnsignedInt(trimmedSize, 16);
            } catch (NumberFormatException e) {

                throw new HttpFailureException(
                        Code.INVALID_RESPONSE, "Invalid HTTP chunk size.", e);
            }

            if (chunkSize == 0) {
                this.readHeaders(input);
                return body.toByteArray();
            }

            if (chunkSize > 1048576 - body.size()) {

                throw new HttpFailureException(
                        Code.RESPONSE_TOO_LARGE, "HTTP responses cannot exceed 1 MiB.");
            }

            body.write(this.readBodyBytes(input, chunkSize, true));
            int carriageReturn = input.read();
            if (carriageReturn == 13) {
                int lineFeed = input.read();
                if (lineFeed == 10) {
                    continue;
                }
            }

            throw new HttpFailureException(Code.INVALID_RESPONSE, "Invalid HTTP chunk terminator.");
        }
    }

    private byte[] readBodyBytes(InputStream input, int length, boolean lengthDeclared)
            throws IOException, HttpFailureException {

        ByteArrayOutputStream output = new ByteArrayOutputStream(Math.min(length, 8192));

        byte[] buffer = new byte[8192];

        while (output.size() < length) {
            int bytesRead = input.read(buffer, 0, Math.min(buffer.length, length - output.size()));
            if (bytesRead < 0) {
                if (lengthDeclared) {
                    throw new HttpFailureException(
                            Code.INVALID_RESPONSE,
                            "HTTP response ended before its declared length.");
                }

                return output.toByteArray();
            }

            output.write(buffer, 0, bytesRead);
        }

        if (!lengthDeclared && input.read() >= 0) {
            throw new HttpFailureException(
                    Code.RESPONSE_TOO_LARGE, "HTTP responses cannot exceed 1 MiB.");
        } else {
            return output.toByteArray();
        }
    }

    private String readLine(InputStream input, int maxLength)
            throws IOException, HttpFailureException {
        ByteArrayOutputStream lineBuffer = new ByteArrayOutputStream();

        while (lineBuffer.size() <= maxLength) {
            int value = input.read();
            if (value < 0) {

                throw new EOFException("HTTP response ended unexpectedly.");
            }

            if (value == 10) {
                byte[] lineData = lineBuffer.toByteArray();
                if (lineData.length != 0) {
                    byte lastByte = lineData[lineData.length - 1];

                    if (lastByte == 13) {
                        return new String(
                                lineData, 0, lineData.length - 1, StandardCharsets.ISO_8859_1);
                    }
                }

                throw new HttpFailureException(Code.INVALID_RESPONSE, "Invalid HTTP line ending.");
            }

            lineBuffer.write(value);
        }

        throw new HttpFailureException(Code.INVALID_RESPONSE, "HTTP response line is too long.");
    }

    private void deliver(HttpResponse response) {
        if (!this.cancelled && !this.this$0.destroyed && this.this$0.script.isRuntimeActive()) {
            InstanceAccess.mc.execute(
                    () -> {
                        if (!this.cancelled
                                && !this.this$0.destroyed
                                && this.this$0.script.isRuntimeActive()) {
                            try {
                                this.onSuccess.accept(response);
                            } catch (Throwable e) {

                                this.this$0.script.reportError("http.success", e);
                            }
                        }
                    });
        }
    }

    private void fail(Code code, String message) {
        if ((!this.cancelled || code == Code.CANCELLED || code == Code.TIMEOUT)
                && !this.this$0.destroyed
                && this.this$0.script.isRuntimeActive()) {
            InstanceAccess.mc.execute(
                    () -> {
                        if (!this.this$0.destroyed && this.this$0.script.isRuntimeActive()) {
                            try {
                                this.onFailure.accept(new HttpFailure(code, message));
                            } catch (Throwable e) {

                                this.this$0.script.reportError("http.failure", e);
                            }
                        }
                    });
        }
    }

    @Override
    public void cancel() {
        this.cancelAndClose();
    }

    private void cancelAndClose() {
        this.cancelled = true;
        this.closeSocket();
    }

    private void closeSocket() {
        Socket currentSocket = this.socket;
        this.socket = null;
        if (currentSocket != null) {
            try {
                currentSocket.close();
            } catch (IOException ignored) {
            }
        }
    }

    @Override
    public boolean isCancelled() {
        return this.cancelled;
    }

    @Override
    public boolean isDone() {
        return this.done;
    }
}
