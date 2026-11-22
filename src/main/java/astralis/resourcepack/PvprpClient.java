package astralis.resourcepack;

import astralis.ui.component.PackSortOrder;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

/** Resource-pack search and download links require the removed Astralis backend. */
public final class PvprpClient {
    public PvprpClient() {}

    public CompletableFuture<PvprpPage> fetchPacks(
            String query, int page, String version, String sort, PackSortOrder category) {
        return CompletableFuture.failedFuture(
                new IOException(
                        "Resource-pack search is unavailable without the Astralis backend."));
    }

    public CompletableFuture<Path> downloadPack(
            PvprpPack pack, Path destination, DownloadProgressListener progress) {
        return CompletableFuture.failedFuture(
                new IOException(
                        "Resource-pack downloads are unavailable without the Astralis backend."));
    }
}
