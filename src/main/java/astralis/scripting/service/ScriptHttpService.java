package astralis.scripting.service;

import astralis.scripting.HttpFailureException;

import com.astralis.scripting.InstanceAccess;
import com.astralis.scripting.Script;
import com.astralis.scripting.api.http.HttpCall;
import com.astralis.scripting.api.http.HttpFailure;
import com.astralis.scripting.api.http.HttpFailure.Code;
import com.astralis.scripting.api.http.HttpRequest;
import com.astralis.scripting.api.http.HttpRequest.BodyType;
import com.astralis.scripting.api.http.HttpRequest.Method;
import com.astralis.scripting.api.http.HttpResponse;
import com.astralis.scripting.api.http.ScriptHttp;

import java.net.IDN;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;

public final class ScriptHttpService implements ScriptHttp, InstanceAccess {
    private static Set<String> forbiddenHeaders;
    public static ScheduledExecutorService timeoutScheduler;
    private final ArrayDeque<Long> requestTimestamps = new ArrayDeque<>();
    public final Set<HttpCallTask> activeCalls = new HashSet<>();
    public final Script script;
    private ExecutorService executor;
    private Set<String> approvedDomains = Set.of();
    private boolean bound;
    public boolean destroyed;
    private static long rateLimitWindowMillis;

    static {
        rateLimitWindowMillis = 60000L;
        forbiddenHeaders =
                Set.of(
                        "connection",
                        "content-length",
                        "content-type",
                        "cookie",
                        "expect",
                        "forwarded",
                        "host",
                        "proxy-authorization",
                        "proxy-connection",
                        "te",
                        "trailer",
                        "transfer-encoding",
                        "upgrade",
                        "via",
                        "x-forwarded-for",
                        "x-forwarded-host",
                        "x-forwarded-proto");
        timeoutScheduler =
                Executors.newSingleThreadScheduledExecutor(
                        runnable -> {
                            Thread thread = new Thread(runnable, "Astralis-Script-HTTP-Timeout");
                            thread.setDaemon(true);
                            return thread;
                        });
    }

    public ScriptHttpService(Script script) {
        this.script = script;
    }

    public synchronized void destroy() {
        if (!this.destroyed) {
            this.destroyed = true;
            this.bound = false;
            this.activeCalls.forEach(HttpCallTask::cancel);
            this.activeCalls.clear();
            this.requestTimestamps.clear();
            this.approvedDomains = Set.of();
            if (this.executor != null) {
                this.executor.shutdownNow();
            }

            this.executor = null;
        }
    }

    public synchronized void bind(Set<String> domains) {
        if (!this.bound && !this.destroyed) {
            HashSet normalizedDomains = new HashSet();

            Iterator iterator = Objects.requireNonNull(domains, "domains").iterator();

            String domain;
            while (true) {
                if (!iterator.hasNext()) {
                    this.approvedDomains = Set.copyOf(normalizedDomains);
                    this.executor =
                            Executors.newFixedThreadPool(
                                    2,
                                    runnable -> {
                                        String scriptName = this.script.getName();

                                        Thread thread =
                                                new Thread(
                                                        runnable,
                                                        "Astralis-Script-" + scriptName + "-HTTP");
                                        thread.setDaemon(true);
                                        return thread;
                                    });
                    this.bound = true;
                    return;
                }

                domain = (String) iterator.next();

                boolean wildcard = domain.startsWith("*.");
                String baseDomain = normalizeDomain(wildcard ? domain.substring(2) : domain);
                if (baseDomain.isEmpty()) {
                    break;
                }

                if (baseDomain.equals("localhost")) {
                    break;
                }

                if (baseDomain.indexOf(46) < 0) {
                    break;
                }

                String entry;
                if (wildcard) {

                    entry = "*." + baseDomain;
                } else {
                    entry = baseDomain;
                }

                normalizedDomains.add(entry);
            }

            throw new IllegalArgumentException("Invalid allowed HTTP domain: " + domain);
        } else {

            throw new IllegalStateException("The script HTTP service cannot be rebound.");
        }
    }

    @Override
    public synchronized HttpCall send(
            HttpRequest request,
            Consumer<HttpResponse> onSuccess,
            Consumer<HttpFailure> onFailure) {
        if (this.bound && !this.destroyed && this.script.isRuntimeActive()) {

            Objects.requireNonNull(request, "request");

            Objects.requireNonNull(onSuccess, "onSuccess");

            Objects.requireNonNull(onFailure, "onFailure");
            if (this.activeCalls.size() >= 4) {

                throw new RejectedExecutionException(
                        "A script cannot have more than 4 HTTP requests in progress.");
            }

            long now = System.currentTimeMillis();

            while (!this.requestTimestamps.isEmpty()
                    && this.requestTimestamps.peekFirst() <= now - 60000L) {
                this.requestTimestamps.removeFirst();
            }

            int requestCount = this.requestTimestamps.size();

            if (requestCount >= 20) {

                throw new RejectedExecutionException(
                        "A script cannot send more than 20 HTTP requests per minute.");
            }

            PreparedHttpRequest prepared = this.validateRequest(request);
            HttpCallTask call = new HttpCallTask(this, prepared, onSuccess, onFailure);
            this.activeCalls.add(call);
            this.requestTimestamps.addLast(now);

            try {
                this.executor.execute(call);
                return call;
            } catch (RuntimeException e) {
                this.activeCalls.remove(call);
                this.requestTimestamps.removeLastOccurrence(now);
                throw e;
            }
        } else {

            throw new IllegalStateException("The script HTTP service is not active.");
        }
    }

    private PreparedHttpRequest validateRequest(HttpRequest request) {

        int urlLength = request.url().length();

        if (urlLength > 2048) {

            throw new IllegalArgumentException("HTTP URLs cannot exceed 2048 characters.");
        }

        URI uri;
        try {
            uri = new URI(request.url()).normalize();
        } catch (URISyntaxException e) {

            throw new IllegalArgumentException("Invalid HTTP URL.", e);
        }

        label158:
        if ("https".equalsIgnoreCase(uri.getScheme())
                && uri.getHost() != null
                && uri.getUserInfo() == null
                && uri.getFragment() == null) {
            if (uri.getPort() != -1) {
                int port = uri.getPort();

                if (port != 443) {
                    break label158;
                }
            }

            String host = normalizeDomain(uri.getHost());
            if (!isIpLiteral(uri.getHost()) && this.isDomainApproved(host)) {
                if (request.method() != Method.GET
                        || request.bodyType() == BodyType.NONE && request.body().isEmpty()) {
                    if (request.method() == Method.POST && request.bodyType() == BodyType.NONE) {

                        throw new IllegalArgumentException(
                                "POST requests must contain a text or JSON body.");
                    }

                    byte[] bodyBytes = request.body().getBytes(StandardCharsets.UTF_8);
                    int bodyLength = bodyBytes.length;

                    if (bodyLength > 65536) {

                        throw new IllegalArgumentException(
                                "HTTP request bodies cannot exceed 64 KiB.");
                    }

                    int headerCount = request.headers().size();

                    if (headerCount > 16) {

                        throw new IllegalArgumentException(
                                "HTTP requests cannot contain more than 16 custom headers.");
                    }

                    int headerBytes = 0;
                    LinkedHashMap headers = new LinkedHashMap();

                    for (Entry headerEntry : request.headers().entrySet()) {
                        String headerName = (String) headerEntry.getKey();
                        String headerValue = (String) headerEntry.getValue();
                        if (!headerName.isEmpty()) {
                            int headerNameLength = headerName.length();

                            if (headerNameLength <= 64) {

                                if (headerName.matches("[A-Za-z0-9!#$%&'*+.^_`|~-]+")) {
                                    int headerValueLength = headerValue.length();

                                    if (headerValueLength <= 1024
                                            && !headerValue
                                                    .chars()
                                                    .anyMatch(
                                                            ch -> {
                                                                if (ch >= 32) {

                                                                    if (ch != 127) {
                                                                        return false;
                                                                    }
                                                                }

                                                                return true;
                                                            })) {
                                        if (!forbiddenHeaders.contains(
                                                headerName.toLowerCase(Locale.ROOT))) {

                                            if (!headerName.regionMatches(true, 0, "Sec-", 0, 4)) {
                                                headerBytes +=
                                                        headerName.length()
                                                                + headerValue.length()
                                                                + 4;

                                                if (headerBytes > 8192) {
                                                    throw new IllegalArgumentException(
                                                            "Custom HTTP headers cannot exceed 8 KiB.");
                                                }

                                                headers.put(headerName, headerValue);
                                                continue;
                                            }
                                        }

                                        throw new IllegalArgumentException(
                                                "Scripts cannot set the HTTP header: "
                                                        + headerName);
                                    }

                                    throw new IllegalArgumentException(
                                            "Invalid HTTP header value.");
                                }
                            }
                        }

                        throw new IllegalArgumentException("Invalid HTTP header name.");
                    }

                    String asciiUrl = uri.toASCIIString();
                    URI parsedUri = URI.create(asciiUrl);
                    String target =
                            (parsedUri.getRawPath() != null && !parsedUri.getRawPath().isEmpty()
                                            ? parsedUri.getRawPath()
                                            : "/")
                                    + (parsedUri.getRawQuery() != null
                                            ? "?" + parsedUri.getRawQuery()
                                            : "");
                    int targetLength = target.length();

                    if (targetLength <= 2048
                            && !target.chars()
                                    .anyMatch(
                                            ch -> {
                                                if (ch >= 32) {

                                                    if (ch != 127) {
                                                        return false;
                                                    }
                                                }

                                                return true;
                                            })) {
                        return new PreparedHttpRequest(
                                host,
                                target,
                                request.method(),
                                Map.copyOf(headers),
                                bodyBytes,
                                request.bodyType());
                    }

                    throw new IllegalArgumentException("Invalid HTTP request target.");
                }

                throw new IllegalArgumentException("GET requests cannot contain a body.");
            }

            throw new SecurityException("The HTTP domain is not approved for this script.");
        }

        throw new IllegalArgumentException(
                "Only HTTPS URLs on port 443 without credentials or fragments are allowed.");
    }

    private static boolean isIpLiteral(String host) {

        if (host.indexOf(58) < 0) {

            if (!host.matches("[0-9.]+") && !host.startsWith("[") && !host.endsWith("]")) {
                return false;
            }
        }

        return true;
    }

    public static void checkPublicAddress(InetAddress address) throws HttpFailureException {
        if (!address.isAnyLocalAddress()
                && !address.isLoopbackAddress()
                && !address.isLinkLocalAddress()
                && !address.isSiteLocalAddress()
                && !address.isMulticastAddress()) {
            byte[] octets = address.getAddress();
            if (address instanceof Inet4Address) {
                byte rawFirst = octets[0];

                int first = rawFirst & 255;
                byte rawSecond = octets[1];

                int second = rawSecond & 255;
                byte rawThird = octets[2];

                int third = rawThird & 255;
                if (first != 0) {

                    if (first != 10) {

                        if (first != 127) {

                            label200:
                            if (first < 224) {

                                if (first == 100) {

                                    if (second >= 64) {

                                        if (second <= 127) {
                                            break label200;
                                        }
                                    }
                                }

                                if (first == 169) {

                                    if (second == 254) {
                                        break label200;
                                    }
                                }

                                if (first == 172) {

                                    if (second >= 16) {

                                        if (second <= 31) {
                                            break label200;
                                        }
                                    }
                                }

                                if (first != 192 || second != 0 || third != 0) {

                                    label183:
                                    if (first != 192 || second != 0 || third != 2) {

                                        if (first == 192) {

                                            if (second == 168) {
                                                break label183;
                                            }
                                        }

                                        if (first == 198) {

                                            if (second == 18) {
                                                break label183;
                                            }

                                            if (second == 19) {
                                                break label183;
                                            }
                                        }

                                        if (first == 198) {

                                            if (second == 51) {

                                                if (third == 100) {
                                                    break label183;
                                                }
                                            }
                                        }

                                        if (first == 203 && second == 0) {

                                            if (third == 113) {
                                                break label183;
                                            }
                                        }

                                        return;
                                    }
                                }
                            }
                        }
                    }
                }

                throw new HttpFailureException(
                        Code.DENIED, "The HTTP domain resolves to a reserved address.");
            } else {
                byte firstByte = octets[0];

                int addressClass = firstByte & 224;

                if (addressClass == 32) {
                    byte teredoFirstByte = octets[0];

                    if (teredoFirstByte != 32
                            || octets[1] != 1
                            || octets[2] != 0
                            || octets[3] != 0) {
                        byte benchmarkFirstByte = octets[0];

                        label230:
                        if (benchmarkFirstByte != 32
                                || octets[1] != 1
                                || octets[2] != 0
                                || octets[3] != 2) {
                            byte orchidFirstByte = octets[0];

                            if (orchidFirstByte == 32 && octets[1] == 1) {
                                byte orchidThirdByte = octets[2];

                                int orchidThirdMasked = orchidThirdByte & 240;

                                if (orchidThirdMasked == 32) {
                                    break label230;
                                }
                            }

                            byte documentationFirstByte = octets[0];

                            if (documentationFirstByte == 32 && octets[1] == 1) {
                                byte documentationThirdByte = octets[2];

                                if (documentationThirdByte == 13) {
                                    byte documentationFourthByte = octets[3];

                                    int documentationFourthMasked = documentationFourthByte & 255;

                                    if (documentationFourthMasked == 184) {
                                        break label230;
                                    }
                                }
                            }

                            byte sixToFourFirstByte = octets[0];

                            label216:
                            if (sixToFourFirstByte != 32 || octets[1] != 2) {
                                byte sixBoneFirstByte = octets[0];

                                if (sixBoneFirstByte == 63) {
                                    byte sixBoneSecondByte = octets[1];

                                    int sixBoneSecondMasked = sixBoneSecondByte & 240;

                                    if (sixBoneSecondMasked == 240) {
                                        break label216;
                                    }
                                }

                                return;
                            }
                        }
                    }
                }

                throw new HttpFailureException(
                        Code.DENIED, "The HTTP domain resolves to a reserved address.");
            }
        } else {

            throw new HttpFailureException(
                    Code.DENIED, "The HTTP domain resolves to a non-public address.");
        }
    }

    public static String getFirstHeader(Map<String, List<String>> headers, String name) {
        List values = (List) headers.get(name);
        return values != null && !values.isEmpty() ? (String) values.getFirst() : "";
    }

    private boolean isDomainApproved(String domain) {
        Iterator iterator = this.approvedDomains.iterator();

        while (iterator.hasNext()) {
            String approved = (String) iterator.next();

            if (approved.startsWith("*.")
                            && domain.endsWith(approved.substring(1))
                            && domain.length() > approved.length() - 1
                    || domain.equals(approved)) {
                return true;
            }
        }

        return false;
    }

    private static String normalizeDomain(String domain) {
        if (domain == null) {
            return "";
        }

        String trimmed = domain.trim();

        while (trimmed.endsWith(".")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }

        try {
            return IDN.toASCII(trimmed, 2).toLowerCase(Locale.ROOT);
        } catch (IllegalArgumentException ignored) {
            return "";
        }
    }
}
