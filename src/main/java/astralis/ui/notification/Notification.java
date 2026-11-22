package astralis.ui.notification;

public final class Notification {
    private final String title;
    private final String message;
    private final NotificationType type;
    private final long createdAtNanos;
    private final long durationNanos;

    public Notification(
            String title,
            String message,
            NotificationType type,
            long createdAtNanos,
            long durationNanos) {
        this.title = title;
        this.message = message;
        this.type = type;
        this.createdAtNanos = createdAtNanos;
        this.durationNanos = durationNanos;
    }

    public String getMessage() {
        return this.message;
    }

    public String getTitle() {
        return this.title;
    }

    public long getDurationNanos() {
        return this.durationNanos;
    }

    public NotificationType getType() {
        return this.type;
    }

    public long getCreatedAtNanos() {
        return this.createdAtNanos;
    }

    public static NotificationBuilder builder() {
        return new NotificationBuilder();
    }
}
