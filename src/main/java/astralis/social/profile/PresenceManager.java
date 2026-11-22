package astralis.social.profile;

import astralis.social.presence.AstralisUser;
import astralis.social.presence.PresenceSnapshotEntry;

import com.astralis.scripting.InstanceAccess;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PresenceManager implements InstanceAccess {
    private static String adminPrefix;
    private static Pattern mentionPattern;
    static long statusResendIntervalMillis;
    private final Map<Integer, AstralisUser> usersByUid;
    private volatile String locationType;
    private volatile String serverAddress;
    private volatile String serverFingerprint;
    private volatile String dimension;
    private volatile String locationDetail;
    private volatile String locationExtra;
    private volatile String statusKey;
    private volatile long lastSentTime;
    private volatile boolean statusDirty;
    private int revision;

    static {
        adminPrefix = "★ ";
        statusResendIntervalMillis = 30000L;
        mentionPattern = Pattern.compile("(?<![A-Za-z0-9_])@([A-Za-z0-9_.-]+)");
    }

    public PresenceManager() {

        this.usersByUid = new ConcurrentHashMap<>();

        this.locationType = "MENU";

        this.serverAddress = "Menu";
        this.serverFingerprint = "";
        this.dimension = "";
        this.locationDetail = "";
        this.locationExtra = "";
        this.statusKey = "";
    }

    public String getAstralisNameByUuid(UUID uuid) {
        AstralisUser user = this.findUserByUuid(uuid);
        return user == null ? null : this.stripAdminPrefix(user.backendName);
    }

    public void tick() {
        // Client presence transmission was removed with the authenticated backend.
    }

    public String getLocationType() {
        return this.locationType;
    }

    public Map<Integer, AstralisUser> getUsersByUid() {
        return this.usersByUid;
    }

    private AstralisUser findUserByUuid(UUID uuid) {
        if (uuid == null) {
            return null;
        }

        for (AstralisUser user : this.usersByUid.values()) {
            if (uuid.equals(user.minecraftUuid)) {
                return user;
            }
        }

        return null;
    }

    synchronized boolean applyStatusIfRevisionMatches(
            int expectedRevision,
            String locationType,
            String serverAddress,
            String dimension,
            String locationDetail,
            String locationExtra,
            String serverFingerprint,
            long sentTime) {
        if (expectedRevision != this.revision) {
            return false;
        }

        this.updateStatus(
                locationType,
                serverAddress,
                dimension,
                locationDetail,
                locationExtra,
                serverFingerprint,
                sentTime);
        return true;
    }

    public Component decoratePlayerName(UUID uuid, Component component) {
        AstralisUser user = this.findUserByUuid(uuid);
        if (user == null) {
            return component;
        }

        int nameColor = this.getNameColor(user.backendName, user.uid);
        MutableComponent mutableComponent = Component.empty();

        if (user.backendName.startsWith("★ ")) {

            mutableComponent.append(
                    Component.literal("★ ")
                            .withStyle(
                                    style -> style.withColor(ChatFormatting.RED).withItalic(true)));
        }

        mutableComponent.append(
                Component.literal(this.stripAdminPrefix(user.backendName))
                        .withStyle(style2 -> style2.withColor(nameColor)));
        MutableComponent mutableComponent2 = component.copy();

        return mutableComponent2
                .append(Component.literal(" (").withStyle(style3 -> style3.withColor(nameColor)))
                .append(mutableComponent)
                .append(Component.literal(")").withStyle(style4 -> style4.withColor(nameColor)));
    }

    public synchronized void invalidateStatus() {
        this.statusKey = "";
        this.lastSentTime = 0L;
        this.statusDirty = true;
        this.revision++;
    }

    public List<AstralisUser> getUserList() {
        return new ArrayList<>(this.usersByUid.values());
    }

    public int getNameColorByUuid(UUID uuid) {

        AstralisUser user = this.findUserByUuid(uuid);
        int color;
        if (user == null) {

            color = 11766015;
        } else {
            color = this.getNameColor(user.backendName, user.uid);
        }

        return color;
    }

    public synchronized void resetPresence() {
        this.usersByUid.clear();

        this.locationType = "MENU";

        this.serverAddress = "Menu";
        this.serverFingerprint = "";
        this.dimension = "";
        this.locationDetail = "";
        this.locationExtra = "";
        this.statusKey = "";
        this.lastSentTime = 0L;
        this.statusDirty = true;
        this.revision++;
    }

    public int getRevision() {
        return this.revision;
    }

    public void update(List<PresenceSnapshotEntry> entries) {
        long now = System.currentTimeMillis();
        HashSet seenUids = new HashSet();

        for (PresenceSnapshotEntry snapshotEntry : entries) {
            UUID uuid = null;
            if (snapshotEntry.getMinecraftUuid() != null
                    && !snapshotEntry.getMinecraftUuid().isBlank()) {
                try {
                    uuid = UUID.fromString(snapshotEntry.getMinecraftUuid());
                } catch (IllegalArgumentException ignored) {
                }
            }

            seenUids.add(snapshotEntry.getUid());
            this.usersByUid.put(
                    snapshotEntry.getUid(),
                    new AstralisUser(
                            snapshotEntry.getUid(),
                            snapshotEntry.getBackendName(),
                            snapshotEntry.getMinecraftName(),
                            uuid,
                            now));
        }

        this.usersByUid
                .entrySet()
                .removeIf(
                        userEntry -> {
                            long age;
                            if (!seenUids.contains(userEntry.getKey())) {
                                age = now - userEntry.getValue().lastSeenAt;

                                if (age > 10000L) {
                                    return true;
                                }
                            }

                            return false;
                        });
    }

    synchronized int getRevisionSynchronized() {
        return this.revision;
    }

    public int getNameColor(String backendName, int uid) {
        AstralisUser user = this.findUser(backendName, uid);
        int userUid = user == null ? uid : user.uid;
        String strippedName =
                user == null
                        ? this.stripAdminPrefix(backendName)
                        : this.stripAdminPrefix(user.backendName);
        int nameHash = strippedName.hashCode();

        int combinedHash = nameHash * 31 + userUid;

        return Color.HSBtoRGB(Math.floorMod(combinedHash, 360) / 360.0F, 0.4F, 0.95F);
    }

    public boolean isAdmin(String backendName, int uid) {
        AstralisUser user = this.findUser(backendName, uid);
        if (user != null) {

            if (user.backendName.startsWith("★ ")) {
                return true;
            }
        }

        return false;
    }

    private String stripAdminPrefix(String name) {
        if (name != null) {

            if (name.startsWith("★ ")) {

                return name.substring("★ ".length());
            }
        }

        return name;
    }

    public String getMinecraftNameForAstralisName(String astralisName) {

        AstralisUser user = this.findUser(astralisName, Integer.MIN_VALUE);
        return user == null ? null : user.minecraftName;
    }

    synchronized void updateStatus(
            String locationType,
            String serverAddress,
            String dimension,
            String locationDetail,
            String locationExtra,
            String serverFingerprint,
            long sentTime) {
        if (!locationType.equals(this.locationType)
                || !serverAddress.equalsIgnoreCase(this.serverAddress)
                || !dimension.equals(this.dimension)) {
            this.usersByUid.clear();
        }

        this.locationType = locationType;
        this.serverAddress = serverAddress;
        this.serverFingerprint = serverFingerprint;
        this.dimension = dimension;
        this.locationDetail = locationDetail;
        this.locationExtra = locationExtra;
        this.statusKey =
                locationType
                        + "\n"
                        + serverAddress
                        + "\n"
                        + dimension
                        + "\n"
                        + locationDetail
                        + "\n"
                        + locationExtra
                        + "\n"
                        + serverFingerprint;
        this.lastSentTime = sentTime;
        this.statusDirty = false;
    }

    public String getLocationExtra() {
        return this.locationExtra;
    }

    public AstralisUser findUser(String backendName, int uid) {
        Iterator iterator = this.usersByUid.values().iterator();

        while (iterator.hasNext()) {
            AstralisUser user = (AstralisUser) iterator.next();
            if (user.uid == uid
                    || this.stripAdminPrefix(user.backendName)
                            .equalsIgnoreCase(this.stripAdminPrefix(backendName))) {
                return user;
            }
        }

        return null;
    }

    public String getServerFingerprint() {
        return this.serverFingerprint;
    }

    public String replaceMentions(String message) {
        Matcher matcher = mentionPattern.matcher(message);
        StringBuffer buffer = new StringBuffer();

        while (matcher.find()) {
            String minecraftName = this.getMinecraftNameForAstralisName(matcher.group(1));
            matcher.appendReplacement(
                    buffer,
                    Matcher.quoteReplacement(
                            minecraftName == null ? matcher.group() : minecraftName));
        }

        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private synchronized int beginStatusUpdateIfNeeded(
            String locationType,
            String serverAddress,
            String dimension,
            String locationDetail,
            String locationExtra,
            long currentTime) {
        return !this.shouldSendStatus(
                        locationType,
                        serverAddress,
                        dimension,
                        locationDetail,
                        locationExtra,
                        currentTime)
                ? -1
                : this.revision;
    }

    public List<String> getMentionCompletions(String text, int cursorIndex) {
        int clampedCursor = Math.max(0, Math.min(cursorIndex, text.length()));

        int atIndex = text.lastIndexOf(64, clampedCursor - 1);
        if (atIndex >= 0 && atIndex < clampedCursor) {
            String prefix = text.substring(atIndex + 1, clampedCursor);
            ArrayList completions = new ArrayList();

            for (AstralisUser user : this.usersByUid.values()) {
                String name = this.stripAdminPrefix(user.backendName);
                if (name.regionMatches(true, 0, prefix, 0, prefix.length())) {
                    completions.add("@" + name);
                }
            }

            return completions;
        } else {
            return List.of();
        }
    }

    public String getDimension() {
        return this.dimension;
    }

    synchronized boolean shouldSendStatus(
            String locationType,
            String serverAddress,
            String dimension,
            String locationDetail,
            String locationExtra,
            long currentTime) {
        long elapsed;
        if (!this.statusDirty) {
            elapsed = currentTime - this.lastSentTime;

            if (elapsed < 30000L) {
                return !locationType.equals(this.locationType)
                        || !serverAddress.equalsIgnoreCase(this.serverAddress)
                        || !dimension.equals(this.dimension)
                        || !locationDetail.equals(this.locationDetail)
                        || !locationExtra.equals(this.locationExtra);
            }
        }

        return true;
    }

    public String getServerAddress() {
        return this.serverAddress;
    }

    public String getStatusKey() {
        return this.statusKey;
    }

    public long getLastSentTime() {
        return this.lastSentTime;
    }

    public String getLocationDetail() {
        return this.locationDetail;
    }

    public boolean isStatusDirty() {
        return this.statusDirty;
    }
}
