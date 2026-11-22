package astralis.media.music;

import astralis.Astralis;
import astralis.util.Timer;

import com.astralis.scripting.InstanceAccess;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.textures.GpuTexture;

import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Builder;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

public class MusicTracker implements InstanceAccess {
    private static final String WINDOWS_MEDIA_SCRIPT =
            "[Console]::OutputEncoding = [System.Text.Encoding]::UTF8\nAdd-Type -AssemblyName System.Runtime.WindowsRuntime\n$null = [Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager, Windows.Media.Control, ContentType = WindowsRuntime]\n$null = [Windows.Storage.Streams.IRandomAccessStreamWithContentType, Windows.Storage.Streams, ContentType = WindowsRuntime]\n$asTaskMethod = ([System.WindowsRuntimeSystemExtensions].GetMethods() | Where-Object { $_.Name -eq 'AsTask' -and $_.GetParameters().Count -eq 1 -and $_.GetParameters()[0].ParameterType.Name -eq 'IAsyncOperation`1' })[0]\n$asStreamMethod = ([System.IO.WindowsRuntimeStreamExtensions].GetMethods() | Where-Object { $_.Name -eq 'AsStreamForRead' -and $_.GetParameters().Count -eq 1 })[0]\nfunction Await($op, $t) { $task = $asTaskMethod.MakeGenericMethod($t).Invoke($null, @($op)); $task.Wait(-1) | Out-Null; $task.Result }\nfunction Poll {\n    $mgr = Await ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager]::RequestAsync()) ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager])\n    $sessions = $mgr.GetSessions()\n    $s = $sessions | Where-Object { $_.GetPlaybackInfo().PlaybackStatus -eq 4 } | Select-Object -First 1\n    if (-not $s) { $s = $sessions | Select-Object -First 1 }\n    if ($s) {\n        $p = Await ($s.TryGetMediaPropertiesAsync()) ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionMediaProperties])\n        $tl = $s.GetTimelineProperties()\n        'TITLE=' + $p.Title\n        'ARTIST=' + $p.Artist\n        'ALBUM=' + $p.AlbumTitle\n        'POSITION=' + [long]$tl.Position.TotalMilliseconds\n        'UPDATED=' + [long]$tl.LastUpdatedTime.ToUnixTimeMilliseconds()\n        'LENGTH=' + [long]($tl.EndTime.TotalMilliseconds - $tl.StartTime.TotalMilliseconds)\n        'STATUS=' + $s.GetPlaybackInfo().PlaybackStatus\n        'APP=' + $s.SourceAppUserModelId\n        $key = '' + $p.Title + '|' + $p.Artist + '|' + $p.AlbumTitle\n        if ($key -ne $script:thumbnailKey) {\n            $script:thumbnailKey = $key\n            try {\n                if ($p.Thumbnail) {\n                    $stream = Await ($p.Thumbnail.OpenReadAsync()) ([Windows.Storage.Streams.IRandomAccessStreamWithContentType])\n                    if ($stream) {\n                        $memory = New-Object System.IO.MemoryStream\n                        $asStreamMethod.Invoke($null, @($stream)).CopyTo($memory)\n                        $bytes = $memory.ToArray()\n                        if ($bytes.Length -gt 0) {\n                            $encoded = [Convert]::ToBase64String($bytes)\n                            for ($offset = 0; $offset -lt $encoded.Length; $offset += 512) {\n                                'THUMB=' + $encoded.Substring($offset, [Math]::Min(512, $encoded.Length - $offset))\n                            }\n                        }\n                    }\n                }\n            } catch { }\n        }\n    }\n    'END'\n}\n'READY'\n";
    private static final String POLL_FUNCTION = "Poll";
    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36";
    private static final String[] YOUTUBE_THUMBNAIL_SIZES =
            new String[] {"maxresdefault", "hqdefault", "mqdefault"};
    private final HttpClient httpClient;
    private final ExecutorService pollExecutor;
    private final Timer pollTimer;
    private final AtomicBoolean polling;
    private Process powerShellProcess;
    private BufferedWriter powerShellWriter;
    private BufferedReader powerShellReader;
    private volatile TrackInfo currentTrack;
    private volatile Identifier coverTextureId;
    private String coverKey;

    public MusicTracker() {

        Builder builder = HttpClient.newBuilder();

        this.httpClient =
                builder.connectTimeout(Duration.ofSeconds(10L))
                        .followRedirects(Redirect.NORMAL)
                        .build();
        this.pollExecutor =
                Executors.newSingleThreadExecutor(
                        runnable -> {
                            Thread thread = new Thread(runnable, "Astralis Music Tracker");
                            thread.setDaemon(true);
                            return thread;
                        });
        this.pollTimer = new Timer();
        this.polling = new AtomicBoolean();
        this.coverKey = "";
    }

    private void updateTrack(
            String title,
            String artist,
            String album,
            long durationMs,
            long positionMs,
            boolean playing,
            String coverUrl,
            byte[] coverBytes,
            long positionUpdatedAtMs) {
        TrackInfo previousTrack;
        long position;
        long timestamp;
        label46:
        {
            previousTrack = this.currentTrack;
            long now = System.currentTimeMillis();
            position = positionMs;
            if (positionUpdatedAtMs > 0L && now - positionUpdatedAtMs >= 0L) {
                timestamp = now - positionUpdatedAtMs;

                if (timestamp < 30000L) {
                    timestamp = positionUpdatedAtMs;
                    break label46;
                }
            }

            timestamp = now;
        }

        long updatedAtMs = timestamp;
        if (previousTrack != null && previousTrack.getTitle().equals(title)) {
            if (playing && position == previousTrack.getPositionMs()) {
                updatedAtMs = previousTrack.getPositionUpdatedAtMs();
            } else if (playing) {
                long estimatedPosition = previousTrack.getEstimatedPositionMs();

                if (position < estimatedPosition - 3000L) {
                    position = previousTrack.getEstimatedPositionMs();
                    updatedAtMs = System.currentTimeMillis();
                }
            }
        }

        this.currentTrack =
                new TrackInfo(title, artist, album, durationMs, position, updatedAtMs, playing);
        this.updateCover(coverUrl, title, artist, coverBytes);
    }

    private String runCommand(String... command) {

        try {
            Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
            String output =
                    new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

            if (!process.waitFor(5L, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                return null;
            } else {
                return process.exitValue() == 0 ? output : null;
            }
        } catch (IOException ignored) {
            return null;
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    public GpuTexture getCoverTexture() {
        Identifier identifier = this.coverTextureId;
        return identifier == null
                ? null
                : mc.getTextureManager().getTexture(identifier).getTexture();
    }

    private BufferedImage cropToSquare(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        if (width > 0 && height > 0 && Math.abs(width - height) > 2) {
            int size = Math.min(width, height);
            return image.getSubimage((width - size) / 2, (height - size) / 2, size, size);
        } else {
            return image;
        }
    }

    public TrackInfo getCurrentTrack() {
        return this.currentTrack;
    }

    private void ensurePowerShellSession() throws IOException, InterruptedException {
        long now;

        if (this.powerShellProcess == null || !this.powerShellProcess.isAlive()) {

            String[] command = new String[7];

            command[0] = "powershell";

            command[1] = "-NoProfile";

            command[2] = "-ExecutionPolicy";

            command[3] = "Bypass";

            command[4] = "-NoExit";

            command[5] = "-Command";
            command[6] = "-";
            this.powerShellProcess = new ProcessBuilder(command).redirectErrorStream(true).start();
            this.powerShellWriter =
                    new BufferedWriter(
                            new OutputStreamWriter(
                                    this.powerShellProcess.getOutputStream(),
                                    StandardCharsets.UTF_8));
            this.powerShellReader =
                    new BufferedReader(
                            new InputStreamReader(
                                    this.powerShellProcess.getInputStream(),
                                    StandardCharsets.UTF_8));

            this.powerShellWriter.write(
                    "[Console]::OutputEncoding = [System.Text.Encoding]::UTF8\nAdd-Type -AssemblyName System.Runtime.WindowsRuntime\n$null = [Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager, Windows.Media.Control, ContentType = WindowsRuntime]\n$null = [Windows.Storage.Streams.IRandomAccessStreamWithContentType, Windows.Storage.Streams, ContentType = WindowsRuntime]\n$asTaskMethod = ([System.WindowsRuntimeSystemExtensions].GetMethods() | Where-Object { $_.Name -eq 'AsTask' -and $_.GetParameters().Count -eq 1 -and $_.GetParameters()[0].ParameterType.Name -eq 'IAsyncOperation`1' })[0]\n$asStreamMethod = ([System.IO.WindowsRuntimeStreamExtensions].GetMethods() | Where-Object { $_.Name -eq 'AsStreamForRead' -and $_.GetParameters().Count -eq 1 })[0]\nfunction Await($op, $t) { $task = $asTaskMethod.MakeGenericMethod($t).Invoke($null, @($op)); $task.Wait(-1) | Out-Null; $task.Result }\nfunction Poll {\n    $mgr = Await ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager]::RequestAsync()) ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager])\n    $sessions = $mgr.GetSessions()\n    $s = $sessions | Where-Object { $_.GetPlaybackInfo().PlaybackStatus -eq 4 } | Select-Object -First 1\n    if (-not $s) { $s = $sessions | Select-Object -First 1 }\n    if ($s) {\n        $p = Await ($s.TryGetMediaPropertiesAsync()) ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionMediaProperties])\n        $tl = $s.GetTimelineProperties()\n        'TITLE=' + $p.Title\n        'ARTIST=' + $p.Artist\n        'ALBUM=' + $p.AlbumTitle\n        'POSITION=' + [long]$tl.Position.TotalMilliseconds\n        'UPDATED=' + [long]$tl.LastUpdatedTime.ToUnixTimeMilliseconds()\n        'LENGTH=' + [long]($tl.EndTime.TotalMilliseconds - $tl.StartTime.TotalMilliseconds)\n        'STATUS=' + $s.GetPlaybackInfo().PlaybackStatus\n        'APP=' + $s.SourceAppUserModelId\n        $key = '' + $p.Title + '|' + $p.Artist + '|' + $p.AlbumTitle\n        if ($key -ne $script:thumbnailKey) {\n            $script:thumbnailKey = $key\n            try {\n                if ($p.Thumbnail) {\n                    $stream = Await ($p.Thumbnail.OpenReadAsync()) ([Windows.Storage.Streams.IRandomAccessStreamWithContentType])\n                    if ($stream) {\n                        $memory = New-Object System.IO.MemoryStream\n                        $asStreamMethod.Invoke($null, @($stream)).CopyTo($memory)\n                        $bytes = $memory.ToArray()\n                        if ($bytes.Length -gt 0) {\n                            $encoded = [Convert]::ToBase64String($bytes)\n                            for ($offset = 0; $offset -lt $encoded.Length; $offset += 512) {\n                                'THUMB=' + $encoded.Substring($offset, [Math]::Min(512, $encoded.Length - $offset))\n                            }\n                        }\n                    }\n                }\n            } catch { }\n        }\n    }\n    'END'\n}\n'READY'\n");
            this.powerShellWriter.newLine();
            this.powerShellWriter.flush();
            now = System.currentTimeMillis();

            long deadline = now + 10000L;

            while (System.currentTimeMillis() < deadline) {
                if (this.powerShellReader.ready()) {
                    String line = this.powerShellReader.readLine();
                    if (line != null) {
                        String trimmedLine = line.trim();

                        if (trimmedLine.equals("READY")) {
                            break;
                        }
                    }
                } else {

                    Thread.sleep(20L);
                }
            }
        }
    }

    private void pollWindows() throws IOException, InterruptedException {
        long now;

        this.ensurePowerShellSession();

        this.powerShellWriter.write("Poll");
        this.powerShellWriter.newLine();
        this.powerShellWriter.flush();
        StringBuilder output = new StringBuilder();
        StringBuilder thumbnailBase64 = new StringBuilder();
        now = System.currentTimeMillis();

        long deadline = now + 3000L;

        while (System.currentTimeMillis() < deadline) {
            if (this.powerShellReader.ready()) {
                String line = this.powerShellReader.readLine();
                if (line == null) {
                    break;
                }

                if (line.equals("END")) {
                    break;
                }

                if (line.startsWith("THUMB=")) {

                    thumbnailBase64.append(line, 6, line.length());
                } else {
                    output.append(line).append("\n");
                }
            } else {

                Thread.sleep(5L);
            }
        }

        byte[] thumbnailBytes = null;
        if (!thumbnailBase64.isEmpty()) {
            try {
                thumbnailBytes = Base64.getDecoder().decode(thumbnailBase64.toString());
            } catch (IllegalArgumentException ignored) {
                thumbnailBytes = null;
            }
        }

        String outputText = output.toString();
        if (outputText.isBlank()) {
            this.currentTrack = null;
        } else {

            String title = this.matchFirstGroup(outputText, "TITLE=(.*)");
            if (title != null && !title.isBlank()) {
                String artist;
                String album;
                String positionText;
                String lengthText;
                String updatedText;
                String app;
                boolean playingResult;
                label92:
                {
                    artist = this.matchFirstGroup(outputText, "ARTIST=(.*)");

                    album = this.matchFirstGroup(outputText, "ALBUM=(.*)");

                    positionText = this.matchFirstGroup(outputText, "POSITION=(\\d+)");

                    lengthText = this.matchFirstGroup(outputText, "LENGTH=(\\d+)");

                    updatedText = this.matchFirstGroup(outputText, "UPDATED=(\\d+)");

                    String status = this.matchFirstGroup(outputText, "STATUS=(.*)");

                    app = this.matchFirstGroup(outputText, "APP=(.*)");
                    if (status != null) {
                        String trimmedStatus = status.trim();

                        if (trimmedStatus.equals("Playing") || status.trim().equals("4")) {
                            playingResult = true;
                            break label92;
                        }
                    }

                    playingResult = false;
                }

                boolean playing = playingResult;
                this.updateTrack(
                        title.trim(),
                        artist == null ? "" : artist.trim(),
                        album == null ? "" : album.trim(),
                        lengthText == null ? 0L : Long.parseLong(lengthText),
                        positionText == null ? 0L : Long.parseLong(positionText),
                        playing,
                        "",
                        this.isBrowserApp(app) ? null : thumbnailBytes,
                        updatedText == null ? 0L : Long.parseLong(updatedText));
            } else {
                this.currentTrack = null;
            }
        }
    }

    public void poll() {

        if (this.pollTimer.hasPassed(500L) && this.polling.compareAndSet(false, true)) {
            this.pollTimer.reset();
            this.pollExecutor.execute(
                    () -> {
                        try {

                            String osName = System.getProperty("os.name").toLowerCase(Locale.ROOT);

                            if (osName.contains("win")) {
                                this.pollWindows();
                            } else {

                                if (osName.contains("mac")) {
                                    this.pollMacSpotify();
                                } else {
                                    this.pollLinuxSpotify();
                                }
                            }
                        } catch (Exception ignored) {
                            this.currentTrack = null;
                        } finally {
                            this.polling.set(false);
                        }
                    });
        }
    }

    public long getTrackPosition(TrackInfo track) {
        return track.getEstimatedPositionMs();
    }

    private BufferedImage fetchYoutubeThumbnail(String query) throws InterruptedException {

        String videoId;
        try {
            HttpClient client = this.httpClient;
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);

            java.net.http.HttpRequest.Builder requestBuilder =
                    HttpRequest.newBuilder(
                            URI.create(
                                    "https://www.youtube.com/results?sp=EgIQAQ%3D%3D&search_query="
                                            + encodedQuery));

            java.net.http.HttpRequest.Builder requestWithUserAgent =
                    requestBuilder.header(
                            "User-Agent",
                            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36");

            java.net.http.HttpRequest.Builder requestWithLanguage =
                    requestWithUserAgent.header("Accept-Language", "en-US,en;q=0.9");

            java.net.http.HttpRequest.Builder requestWithCookie =
                    requestWithLanguage.header("Cookie", "SOCS=CAI");

            String html =
                    client.send(
                                    requestWithCookie
                                            .timeout(Duration.ofSeconds(10L))
                                            .GET()
                                            .build(),
                                    BodyHandlers.ofString())
                            .body();
            videoId = this.matchFirstGroup(html, "\"videoId\":\"([\\w-]{11})\"");
        } catch (IOException | RuntimeException ignored) {
            return null;
        }

        if (videoId == null) {
            return null;
        }

        for (String size : YOUTUBE_THUMBNAIL_SIZES) {
            try {
                HttpClient client = this.httpClient;

                java.net.http.HttpRequest.Builder requestBuilder =
                        HttpRequest.newBuilder(
                                URI.create(
                                        "https://i.ytimg.com/vi/" + videoId + "/" + size + ".jpg"));
                HttpResponse response =
                        client.send(
                                requestBuilder.timeout(Duration.ofSeconds(10L)).GET().build(),
                                BodyHandlers.ofByteArray());
                int statusCode = response.statusCode();
                if (statusCode == 200) {
                    BufferedImage image =
                            ImageIO.read(new ByteArrayInputStream((byte[]) response.body()));
                    if (image != null) {
                        return image;
                    }
                }
            } catch (IOException | RuntimeException ignored) {
                return null;
            }
        }

        return null;
    }

    private boolean isBrowserApp(String appId) {
        if (appId == null) {
            return false;
        }

        String normalizedId = appId.trim().toLowerCase(Locale.ROOT);

        if (!normalizedId.startsWith("brave")) {

            if (!normalizedId.startsWith("chrome")) {

                if (!normalizedId.startsWith("msedge")) {

                    if (!normalizedId.startsWith("firefox")) {

                        if (!normalizedId.startsWith("opera")) {

                            if (!normalizedId.startsWith("vivaldi")) {

                                if (!normalizedId.contains("chromium")) {
                                    return false;
                                }
                            }
                        }
                    }
                }
            }
        }

        return true;
    }

    private String matchFirstGroup(String text, String regex) {
        Matcher matcher = Pattern.compile(regex).matcher(text);
        return matcher.find() ? matcher.group(1) : null;
    }

    private void pollMacSpotify() {
        String[] command = new String[3];

        command[0] = "osascript";

        command[1] = "-e";

        command[2] =
                "if application \"Spotify\" is running then\n    tell application \"Spotify\"\n        if player state is not stopped then\n            return name of current track & \"\\n\" & artist of current track & \"\\n\" & album of current track & \"\\n\" & duration of current track & \"\\n\" & player position & \"\\n\" & (player state as text) & \"\\n\" & artwork url of current track\n        end if\n    end tell\nend if";
        String output = this.runCommand(command);
        if (output != null && !output.isBlank()) {
            String[] lines = output.split("\n");
            int lineCount = lines.length;
            if (lineCount >= 7 && !lines[0].isBlank()) {
                String title = lines[0].trim();
                String artist = lines[1].trim();
                String album = lines[2].trim();
                String durationText = lines[3].trim();

                long durationSeconds =
                        Math.round(Double.parseDouble(durationText.replace(',', '.')));
                String positionText = lines[4].trim();

                long positionMs =
                        Math.round(Double.parseDouble(positionText.replace(',', '.')) * 1000.0);
                String stateText = lines[5].trim();

                boolean playing = stateText.equalsIgnoreCase("playing");

                this.updateTrack(
                        title,
                        artist,
                        album,
                        durationSeconds,
                        positionMs,
                        playing,
                        lines[6].trim(),
                        null,
                        0L);
            } else {
                this.currentTrack = null;
            }
        } else {
            this.currentTrack = null;
        }
    }

    private String fetchItunesArtworkUrl(String query) throws InterruptedException {

        try {
            HttpClient client = this.httpClient;
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);

            java.net.http.HttpRequest.Builder requestBuilder =
                    HttpRequest.newBuilder(
                            URI.create(
                                    "https://itunes.apple.com/search?media=music&entity=song&limit=1&term="
                                            + encodedQuery));

            JsonObject jsonObject =
                    JsonParser.parseString(
                                    client.send(
                                                    requestBuilder
                                                            .timeout(Duration.ofSeconds(10L))
                                                            .GET()
                                                            .build(),
                                                    BodyHandlers.ofString())
                                            .body())
                            .getAsJsonObject();
            JsonArray jsonArray = jsonObject.getAsJsonArray("results");
            if (jsonArray != null && !jsonArray.isEmpty()) {
                JsonObject jsonObject2 = jsonArray.get(0).getAsJsonObject();
                String artworkUrl = jsonObject2.get("artworkUrl100").getAsString();

                return artworkUrl.replace("100x100", "600x600");
            } else {
                return null;
            }
        } catch (IOException | RuntimeException ignored) {
            return null;
        }
    }

    private void pollLinuxSpotify() {

        String[] command = new String[7];

        command[0] = "dbus-send";

        command[1] = "--print-reply";

        command[2] = "--session";

        command[3] = "--dest=org.mpris.MediaPlayer2.spotify";

        command[4] = "/org/mpris/MediaPlayer2";

        command[5] = "org.freedesktop.DBus.Properties.GetAll";

        command[6] = "string:org.mpris.MediaPlayer2.Player";
        String output = this.runCommand(command);
        if (output == null) {
            this.currentTrack = null;
        } else {

            String title =
                    this.matchFirstGroup(
                            output, "string \"xesam:title\"\\s+variant\\s+string \"([^\"]*)\"");
            if (title != null && !title.isBlank()) {

                String albumRaw =
                        this.matchFirstGroup(
                                output, "string \"xesam:album\"\\s+variant\\s+string \"([^\"]*)\"");

                String artistsRaw =
                        this.matchFirstGroup(
                                output,
                                "string \"xesam:artist\"\\s+variant\\s+array \\[([^\\]]*)]");

                String artUrl =
                        this.matchFirstGroup(
                                output,
                                "string \"mpris:artUrl\"\\s+variant\\s+string \"([^\"]*)\"");

                String lengthText =
                        this.matchFirstGroup(
                                output,
                                "string \"mpris:length\"\\s+variant\\s+(?:uint64|int64) (\\d+)");

                String positionText =
                        this.matchFirstGroup(
                                output, "string \"Position\"\\s+variant\\s+int64 (\\d+)");

                String status =
                        this.matchFirstGroup(
                                output,
                                "string \"PlaybackStatus\"\\s+variant\\s+string \"(\\w+)\"");
                StringBuilder artists = new StringBuilder();
                if (artistsRaw != null) {

                    for (Matcher artistMatcher =
                                    Pattern.compile("string \"([^\"]*)\"").matcher(artistsRaw);
                            artistMatcher.find();
                            artists.append(artistMatcher.group(1))) {
                        if (!artists.isEmpty()) {
                            artists.append(", ");
                        }
                    }
                }

                String artist = artists.toString();
                String album = albumRaw == null ? "" : albumRaw;
                long durationMs;
                if (lengthText == null) {
                    durationMs = 0L;
                } else {
                    long lengthMicros = Long.parseLong(lengthText);

                    durationMs = lengthMicros / 1000L;
                }

                long positionMs;
                if (positionText == null) {
                    positionMs = 0L;
                } else {
                    long positionMicros = Long.parseLong(positionText);

                    positionMs = positionMicros / 1000L;
                }

                boolean playing = "Playing".equals(status);
                String coverUrl;
                if (artUrl == null) {
                    coverUrl = "";
                } else {

                    coverUrl = artUrl.replace("open.spotify.com", "i.scdn.co");
                }

                this.updateTrack(
                        title, artist, album, durationMs, positionMs, playing, coverUrl, null, 0L);
            } else {
                this.currentTrack = null;
            }
        }
    }

    private void updateCover(String coverUrl, String title, String artist, byte[] coverBytes) {
        String key = coverUrl.isBlank() ? title + "\u0000" + artist : coverUrl;
        if (!key.equals(this.coverKey)) {
            this.coverKey = key;
            Identifier identifier = this.coverTextureId;
            this.coverTextureId = null;
            if (identifier != null) {
                mc.execute(() -> mc.getTextureManager().release(identifier));
            }

            try {
                String resolvedUrl = coverUrl;
                if (coverBytes != null) {
                    BufferedImage embeddedImage =
                            ImageIO.read(new ByteArrayInputStream(coverBytes));
                    if (embeddedImage != null) {
                        this.registerCover(this.cropToSquare(embeddedImage));
                        return;
                    }
                }

                if (resolvedUrl.isBlank()) {
                    String searchQuery = (artist + " " + title).trim();
                    String artworkUrl = this.fetchItunesArtworkUrl(searchQuery);
                    if (artworkUrl == null) {
                        BufferedImage thumbnail = this.fetchYoutubeThumbnail(searchQuery);
                        if (thumbnail == null) {
                            return;
                        }

                        this.registerCover(this.cropToSquare(thumbnail));
                        return;
                    }

                    resolvedUrl = artworkUrl;
                }

                HttpClient client = this.httpClient;
                java.net.http.HttpRequest.Builder requestBuilder =
                        HttpRequest.newBuilder(URI.create(resolvedUrl));

                BufferedImage downloadedImage =
                        ImageIO.read(
                                new ByteArrayInputStream(
                                        client.send(
                                                        requestBuilder
                                                                .timeout(Duration.ofSeconds(10L))
                                                                .GET()
                                                                .build(),
                                                        BodyHandlers.ofByteArray())
                                                .body()));
                if (downloadedImage == null) {
                    return;
                }

                this.registerCover(downloadedImage);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {

                Astralis.LOGGER.warn("Failed to load music cover for {}", title, e);
            }
        }
    }

    private void registerCover(BufferedImage image) {
        long timestamp;
        NativeImage nativeImage = new NativeImage(image.getWidth(), image.getHeight(), false);

        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                nativeImage.setPixel(x, y, image.getRGB(x, y));
            }
        }

        timestamp = System.nanoTime();

        Identifier identifier =
                Identifier.fromNamespaceAndPath("astralis", "music/cover_" + timestamp);
        mc.execute(
                () -> {
                    mc.getTextureManager()
                            .register(
                                    identifier,
                                    new DynamicTexture(() -> "Music cover", nativeImage));
                    this.coverTextureId = identifier;
                });
    }
}
