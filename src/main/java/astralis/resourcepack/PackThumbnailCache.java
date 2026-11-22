package astralis.resourcepack;

import astralis.Astralis;

import com.astralis.scripting.InstanceAccess;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.textures.GpuTexture;

import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Builder;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Map;
import java.util.OptionalLong;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

import javax.imageio.ImageIO;

public final class PackThumbnailCache implements AutoCloseable, InstanceAccess {
    private final HttpClient httpClient;
    private final ExecutorService loadExecutor;
    private final Map<String, Identifier> loadedTextures;
    private final Set<String> failedUrls;
    private final Set<String> pendingUrls;
    private final Set<NativeImage> pendingImages;
    private final Object closeLock;
    private volatile boolean closed;

    public PackThumbnailCache() {
        super();
        Builder builder = HttpClient.newBuilder();

        this.httpClient =
                builder.connectTimeout(Duration.ofSeconds(10L))
                        .followRedirects(Redirect.NEVER)
                        .build();
        this.loadExecutor = Executors.newVirtualThreadPerTaskExecutor();
        this.loadedTextures = new ConcurrentHashMap<>();
        this.failedUrls = ConcurrentHashMap.newKeySet();
        this.pendingUrls = ConcurrentHashMap.newKeySet();
        this.pendingImages = ConcurrentHashMap.newKeySet();
        this.closeLock = new Object();
    }

    public GpuTexture get(String url) {
        if (!this.closed && url != null && !url.isBlank()) {
            Identifier identifier = this.loadedTextures.get(url);
            if (identifier != null) {
                return mc.getTextureManager().getTexture(identifier).getTexture();
            }

            if (this.failedUrls.contains(url)) {
                return null;
            }

            URI uri;
            try {
                uri = URI.create(url);
            } catch (IllegalArgumentException ignored) {
                this.failedUrls.add(url);
                return null;
            }

            if (!isAllowedUri(uri)) {
                this.failedUrls.add(url);
                return null;
            }

            if (!this.pendingUrls.add(url)) {
                return null;
            }

            if (!this.closed && !this.failedUrls.contains(url)) {
                try {
                    this.loadExecutor.execute(() -> this.load(url, uri));
                } catch (RejectedExecutionException ignored) {
                    if (!this.closed) {
                        this.failedUrls.add(url);
                    }

                    this.pendingUrls.remove(url);
                }

                return null;
            } else {
                this.pendingUrls.remove(url);
                return null;
            }
        } else {
            return null;
        }
    }

    public boolean isLoading(String url) {
        return !this.closed && url != null && this.pendingUrls.contains(url);
    }

    @Override
    public void close() {

        java.util.ArrayList<Identifier> textures;
        synchronized (this.closeLock) {
            if (this.closed) return;
            this.closed = true;
            textures = new java.util.ArrayList<>(this.loadedTextures.values());
            this.loadedTextures.clear();
        }
        this.loadExecutor.shutdownNow();
        this.pendingImages.forEach(NativeImage::close);
        this.pendingImages.clear();
        Runnable release = () -> textures.forEach(mc.getTextureManager()::release);
        if (mc.isSameThread()) release.run();
        else mc.execute(release);
    }

    private void load(String url, URI uri) {

        NativeImage image;
        try {
            image = this.download(uri);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            this.markFailed(url, e);
            return;
        } catch (Exception e) {
            this.markFailed(url, e);
            return;
        }

        this.pendingImages.add(image);
        if (this.closed) {
            this.pendingImages.remove(image);
            image.close();
            this.pendingUrls.remove(url);
        } else {
            Identifier textureId;
            try {

                HexFormat hexFormat = HexFormat.of();

                String hash =
                        hexFormat.formatHex(
                                MessageDigest.getInstance("SHA-256")
                                        .digest(url.getBytes(StandardCharsets.UTF_8)));

                textureId = Identifier.fromNamespaceAndPath("astralis", "pack/thumbnail/" + hash);
            } catch (NoSuchAlgorithmException e) {
                this.pendingImages.remove(image);
                image.close();
                this.markFailed(url, e);
                return;
            }

            try {
                mc.execute(() -> this.register(url, textureId, image));
            } catch (RejectedExecutionException e) {
                this.pendingImages.remove(image);
                image.close();
                this.markFailed(url, e);
            }
        }
    }

    private NativeImage download(URI uri) throws java.io.IOException, InterruptedException {
        long contentLength;
        URI currentUri = uri;

        for (int redirectCount = 0; redirectCount <= 5; redirectCount++) {
            HttpClient client = this.httpClient;
            java.net.http.HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(currentUri);

            java.net.http.HttpRequest.Builder timedBuilder =
                    requestBuilder.timeout(Duration.ofSeconds(20L));

            java.net.http.HttpRequest.Builder agentBuilder =
                    timedBuilder.header("User-Agent", "Astralis-Pack-Browser/1.0");

            HttpResponse response =
                    client.send(
                            agentBuilder.header("Accept", "image/*").GET().build(),
                            BodyHandlers.ofInputStream());
            if (!isAllowedUri(response.uri())) {
                ((InputStream) response.body()).close();

                throw new IOException("PVPRP redirected to a disallowed host");
            }

            int statusCode = response.statusCode();

            try (InputStream body = (InputStream) response.body()) {
                if (statusCode != 301) {
                    if (statusCode != 302) {
                        if (statusCode != 303) {
                            if (statusCode != 307) {
                                if (statusCode != 308) {
                                    if (statusCode >= 200) {
                                        if (statusCode < 300) {
                                            HttpHeaders headers = response.headers();
                                            OptionalLong contentLengthHeader =
                                                    headers.firstValueAsLong("Content-Length");
                                            contentLength = contentLengthHeader.orElse(-1L);
                                            if (contentLength > 10485760L) {

                                                throw new IOException(
                                                        "PVPRP thumbnail exceeds 10 MiB");
                                            }

                                            byte[] data = body.readNBytes(10485761);
                                            int dataLength = data.length;
                                            if (dataLength > 10485760) {

                                                throw new IOException(
                                                        "PVPRP thumbnail exceeds 10 MiB");
                                            }

                                            return this.decodeImage(data);
                                        }
                                    }

                                    throw new IOException(
                                            "PVPRP thumbnail request returned HTTP " + statusCode);
                                }
                            }
                        }
                    }
                }

                if (redirectCount == 5) {

                    throw new IOException("PVPRP thumbnail exceeded the redirect limit");
                }

                HttpHeaders redirectHeaders = response.headers();

                currentUri =
                        currentUri.resolve(
                                redirectHeaders
                                        .firstValue("Location")
                                        .orElseThrow(
                                                () -> {
                                                    return new IOException(
                                                            "PVPRP redirect omitted its location");
                                                }));
                if (!isAllowedUri(currentUri)) {

                    throw new IOException("PVPRP redirected to a disallowed host");
                }
            }
        }

        throw new IOException("PVPRP thumbnail exceeded the redirect limit");
    }

    private void register(String url, Identifier identifier, NativeImage nativeImage) {
        this.pendingImages.remove(nativeImage);
        synchronized (this.closeLock) {
            if (this.closed) {
                nativeImage.close();
                this.pendingUrls.remove(url);
            } else {
                DynamicTexture dynamicTexture;
                try {
                    dynamicTexture =
                            new DynamicTexture(
                                    () -> {
                                        String idString = String.valueOf(identifier);

                                        return "PVPRP pack thumbnail " + idString;
                                    },
                                    nativeImage);
                } catch (Exception e) {
                    nativeImage.close();
                    this.markFailed(url, e);
                    return;
                }

                try {
                    mc.getTextureManager().register(identifier, dynamicTexture);
                } catch (Exception e) {
                    dynamicTexture.close();
                    this.markFailed(url, e);
                    return;
                }

                this.loadedTextures.put(url, identifier);
                this.pendingUrls.remove(url);
            }
        }
    }

    private NativeImage decodeImage(byte[] data) throws java.io.IOException {
        BufferedImage bufferedImage = ImageIO.read(new ByteArrayInputStream(data));
        if (bufferedImage == null) {

            throw new IOException("PVPRP thumbnail format is unsupported");
        }

        NativeImage nativeImage =
                new NativeImage(bufferedImage.getWidth(), bufferedImage.getHeight(), false);

        try {
            for (int y = 0; y < bufferedImage.getHeight(); y++) {
                for (int x = 0; x < bufferedImage.getWidth(); x++) {
                    nativeImage.setPixel(x, y, bufferedImage.getRGB(x, y));
                }
            }

            return nativeImage;
        } catch (RuntimeException e) {
            nativeImage.close();
            throw e;
        }
    }

    private void markFailed(String url, Exception exception) {
        if (!this.closed) {
            this.failedUrls.add(url);

            Astralis.LOGGER.warn("Failed to load PVPRP thumbnail {}", url, exception);
        }

        this.pendingUrls.remove(url);
    }

    private static boolean isAllowedUri(URI uri) {

        String host = uri.getHost();

        if ("https".equalsIgnoreCase(uri.getScheme()) && uri.getUserInfo() == null) {
            if (uri.getPort() != -1) {
                int port = uri.getPort();

                if (port != 443) {
                    return false;
                }
            }

            if (host != null) {

                if (host.equalsIgnoreCase("pvprp.com")) {
                    return true;
                }

                if (host.equalsIgnoreCase("www.pvprp.com")) {
                    return true;
                }
            }
        }

        return false;
    }
}
