package astralis.resourcepack;

@FunctionalInterface
public interface DownloadProgressListener {
    void onProgress(long bytesDownloaded, long totalBytes);
}
