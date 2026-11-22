package astralis.ui.notification;

import astralis.Astralis;

import java.util.concurrent.TimeUnit;

public final class NotificationBuilder {
    private String title;
    private String message;
    private NotificationType type;
    private long durationMillis;

    public NotificationBuilder() {

        this.type = NotificationType.INFO;
        this.durationMillis = 4000L;
    }

    public NotificationBuilder withTitle(String title) {
        this.title = title;
        return this;
    }

    public NotificationBuilder withMessage(String message) {
        this.message = message;
        return this;
    }

    public NotificationBuilder withType(NotificationType type) {
        this.type = type;
        return this;
    }

    public NotificationBuilder withDuration(long durationMillis) {
        this.durationMillis = durationMillis;
        return this;
    }

    public Notification build() {
        if (this.title == null || this.title.isBlank()) {

            throw new IllegalArgumentException("Notification title is required.");
        } else if (this.message == null || this.message.isBlank()) {

            throw new IllegalArgumentException("Notification message is required.");
        } else if (this.type == null) {

            throw new IllegalArgumentException("Notification type is required.");
        } else if (this.durationMillis <= 0L) {

            throw new IllegalArgumentException("Notification duration must be positive.");
        } else {
            return new Notification(
                    this.title,
                    this.message,
                    this.type,
                    System.nanoTime(),
                    TimeUnit.MILLISECONDS.toNanos(this.durationMillis));
        }
    }

    public void show() {
        Astralis.getInstance()
                .getManagerRegistry()
                .getManager(NotificationManager.class)
                .addNotification(this.build());
    }
}
