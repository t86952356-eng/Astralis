package astralis.ui.notification;

public final class NotificationEntry {
    public final Notification notification;
    public float animatedOffset = Float.NaN;

    public NotificationEntry(Notification notification) {
        this.notification = notification;
    }
}
