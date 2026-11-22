package astralis.social.chat;

import astralis.Astralis;
import astralis.module.impl.IRC;
import astralis.ui.chat.ChatTabManager;

import com.astralis.scripting.InstanceAccess;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.client.multiplayer.chat.GuiMessageTag;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent.ShowText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Queue;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IrcChatManager implements InstanceAccess {
    private static Pattern mentionPattern;
    private static DateTimeFormatter timestampFormatter;
    private static DateTimeFormatter todayFormatter;
    private static DateTimeFormatter olderDateFormatter;
    private static GuiMessageTag historyTag;
    private static GuiMessageTag liveTag;
    private final Set<String> displayedMessages;
    private final Set<String> pendingMessageSet;
    private final Queue<String> pendingMessageQueue;
    private final Object historyLock;
    private final Map<String, NavigableSet<Long>> messageTimestampIndex;
    private final List<String> historyLines;
    private final Path historyFile;
    private final Queue<String> outgoingMessages;
    private boolean historyQueued;

    public IrcChatManager() {

        this.displayedMessages = ConcurrentHashMap.newKeySet();
        this.pendingMessageSet = ConcurrentHashMap.newKeySet();
        this.pendingMessageQueue = new ConcurrentLinkedQueue<>();
        this.historyLock = new Object();
        this.messageTimestampIndex = new HashMap<>();
        this.historyLines = new CopyOnWriteArrayList<>();
        Path path = InstanceAccess.mc.gameDirectory.toPath();

        this.historyFile = path.resolve("astralis/irc-history.txt");
        this.outgoingMessages = new ConcurrentLinkedQueue<>();
        this.loadHistory();
    }

    static {
        mentionPattern = Pattern.compile("(?<![A-Za-z0-9_])@([A-Za-z0-9_.-]+)");
        timestampFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        todayFormatter = DateTimeFormatter.ofPattern("HH:mm");
        olderDateFormatter = DateTimeFormatter.ofPattern("EEE, MMM d, HH:mm", Locale.ENGLISH);
        MutableComponent historyTagText = Component.literal("Astralis IRC");
        historyTag = new GuiMessageTag(5941734, null, historyTagText, "IRC");
        MutableComponent liveTagText = Component.literal("Astralis IRC");
        liveTag = new GuiMessageTag(5941734, null, liveTagText, "IRC_LIVE");
    }

    public void tick() {
        this.discardOutgoingMessages();
        ChatTabManager chatTabManager =
                Astralis.getInstance().getManagerRegistry().getManager(ChatTabManager.class);
        if (InstanceAccess.mc.player != null
                && InstanceAccess.mc.gui.screen() instanceof ChatScreen
                && chatTabManager != null
                && chatTabManager.isIrcTabSelected()) {
            int processed = 0;

            while (true) {

                if (processed >= 32) {
                    break;
                }

                String pendingMessage = this.pendingMessageQueue.poll();
                if (pendingMessage == null) {
                    break;
                }

                this.pendingMessageSet.remove(pendingMessage);
                this.display(pendingMessage, historyTag);
                processed++;
            }
        }
    }

    private void playMentionSound() {
        if (IRC.isPingSoundEnabled()) {
            InstanceAccess.mc
                    .getSoundManager()
                    .play(
                            SimpleSoundInstance.forUI(
                                    (SoundEvent) SoundEvents.NOTE_BLOCK_PLING.value(), 1.8F, 1.0F));
        }
    }

    public Queue<String> getPendingMessageQueue() {
        return this.pendingMessageQueue;
    }

    public Queue<String> getOutgoingMessages() {
        return this.outgoingMessages;
    }

    public void receiveLiveMessage(String message) {
        String timestampedMessage = this.ensureTimestampPrefix(message);
        this.recordLiveMessage(timestampedMessage);
        if (this.mentionsLocalPlayer(timestampedMessage)) {
            InstanceAccess.mc.execute(this::playMentionSound);
        }

        this.display(timestampedMessage, liveTag);
    }

    private List<String> sortByTimestamp(List<String> messages) {
        ArrayList sorted = new ArrayList(messages);
        sorted.sort(Comparator.comparingLong(this::parseEpochSecond));
        return sorted;
    }

    private void loadHistory() {
        long epochSecond;

        try {
            if (!Files.exists(this.historyFile)) {
                return;
            }

            List<String> fileLines = Files.readAllLines(this.historyFile, StandardCharsets.UTF_8);
            ArrayList validLines = new ArrayList(fileLines.size());
            HashMap messageIndex = new HashMap();

            for (String line : fileLines) {
                epochSecond = this.parseEpochSecond(line);

                if (epochSecond != Long.MAX_VALUE && this.addMessageToIndex(messageIndex, line)) {
                    validLines.add(line);
                }
            }

            synchronized (this.historyLock) {
                this.historyLines.clear();
                this.historyLines.addAll(validLines);
                this.messageTimestampIndex.clear();
                this.messageTimestampIndex.putAll(messageIndex);
            }

            if (validLines.size() != fileLines.size()) {
                Files.write(
                        this.historyFile,
                        validLines,
                        StandardCharsets.UTF_8,
                        StandardOpenOption.TRUNCATE_EXISTING);
            }
        } catch (IOException ignored) {
        }
    }

    public void sendMessage(String message) {
        this.outgoingMessages.add(message);
        this.discardOutgoingMessages();
    }

    private String normalizeMessage(String message) {

        String withoutStar = message.replaceFirst("^★\\s*", "");

        String compacted =
                withoutStar.replaceFirst("^(.+?)\\s*\\(\\s*(\\d+)\\s*\\)\\s*:\\s*", "$1($2): ");

        return compacted.replaceAll("\\s+", " ").trim();
    }

    public List<String> getHistoryLines() {
        return this.historyLines;
    }

    private Component formatMessage(String line) {

        int separatorIndex = line.indexOf(": ");
        if (separatorIndex < 0) {
            return Component.literal(line);
        }

        String header = line.substring(0, separatorIndex);
        String body = line.substring(separatorIndex + 2);
        int openParen = header.lastIndexOf(40);
        int closeParen = header.endsWith(")") ? header.length() - 1 : -1;
        if (openParen > 0 && closeParen > openParen) {
            String senderLabel = header.substring(0, openParen);
            String rawName = senderLabel.replaceFirst("^\\[[^]]*\\] ?", "").trim();
            String userName = rawName.replaceFirst("^★\\s*", "");

            int userId;
            try {
                userId = Integer.parseInt(header.substring(openParen + 1, closeParen));
            } catch (NumberFormatException ignored) {
                return Component.literal(line);
            }

            int nameColor =
                    Astralis.getInstance().getProfileManager().getNameColor(userName, userId);
            MutableComponent mutableComponent = Component.empty();
            if (rawName.startsWith("★")
                    || Astralis.getInstance().getProfileManager().isAdmin(userName, userId)) {
                mutableComponent.append(
                        Component.literal("★ ")
                                .withStyle(
                                        style -> {
                                            Style style2 =
                                                    style.withColor(ChatFormatting.RED)
                                                            .withItalic(true);

                                            return style2.withHoverEvent(
                                                    new ShowText(
                                                            Component.literal(
                                                                    "This user is an Administrator of Astralis")));
                                        }));
            }

            mutableComponent.append(
                    Component.literal(userName).withStyle(style3 -> style3.withColor(nameColor)));
            String timestampText;
            if (header.startsWith("[")) {

                timestampText =
                        this.formatTimestampLabel(header.substring(1, header.indexOf(93))) + " ";
            } else {
                timestampText = "";
            }

            String timestampPrefix = timestampText;
            MutableComponent mutableComponent2 =
                    Component.literal(timestampPrefix)
                            .withStyle(ChatFormatting.GRAY)
                            .append(mutableComponent);

            return mutableComponent2
                    .append(Component.literal(" (" + userId + "): ").withStyle(ChatFormatting.GRAY))
                    .append(this.highlightMentions(body));
        } else {
            return Component.literal(line);
        }
    }

    public void resetDisplayState() {
        this.historyQueued = false;
        this.displayedMessages.clear();
        this.pendingMessageSet.clear();
        this.pendingMessageQueue.clear();
    }

    private void recordLiveMessage(String message) {
        synchronized (this.historyLock) {
            this.addMessageToIndex(this.messageTimestampIndex, message);
            this.historyLines.add(message);
        }

        this.appendToHistoryFile(List.of(message));
    }

    public Path getHistoryFile() {
        return this.historyFile;
    }

    private void discardOutgoingMessages() {
        if (!this.outgoingMessages.isEmpty()) {
            this.outgoingMessages.clear();
            astralis.OfflineServices.unavailable("IRC");
        }
    }

    private String ensureTimestampPrefix(String message) {
        String result;
        if (message.startsWith("[")) {
            result = message;
        } else {
            String timestamp = timestampFormatter.format(LocalDateTime.now());

            result = "[" + timestamp + "] " + message;
        }

        return result;
    }

    private boolean mentionsLocalPlayer(String line) {
        if (IRC.isPingSoundEnabled() && InstanceAccess.mc.player != null) {

            int separatorIndex = line.indexOf(": ");
            if (separatorIndex < 0) {
                return false;
            }

            String header = line.substring(0, separatorIndex);

            int openParen = header.lastIndexOf(40);
            int closeParen = header.endsWith(")") ? header.length() - 1 : -1;
            if (openParen > 0 && closeParen > openParen) {
                int userId;
                try {
                    userId = Integer.parseInt(header.substring(openParen + 1, closeParen));
                } catch (NumberFormatException ignored) {
                    return false;
                }

                String senderLabel = header.substring(0, openParen);

                String withoutTimestamp = senderLabel.replaceFirst("^\\[[^]]*\\]\\s*", "");

                String userName = withoutTimestamp.replaceFirst("^★\\s*", "").trim();
                if (!Astralis.getInstance().getProfileManager().isAdmin(userName, userId)) {
                    return false;
                }

                String localName = InstanceAccess.mc.player.getGameProfile().name();
                String rawAstralisName =
                        Astralis.getInstance()
                                .getProfileManager()
                                .getAstralisNameByUuid(InstanceAccess.mc.player.getUUID());
                String strippedName;
                if (rawAstralisName == null) {
                    strippedName = null;
                } else {

                    strippedName = rawAstralisName.replaceFirst("^★\\s*", "");
                }

                String astralisName = strippedName;
                Matcher matcher = mentionPattern.matcher(line.substring(separatorIndex + 2));

                while (matcher.find()) {
                    String mention = matcher.group(1);
                    if (mention.equalsIgnoreCase(localName)
                            || astralisName != null && mention.equalsIgnoreCase(astralisName)) {
                        return true;
                    }
                }

                return userName.equalsIgnoreCase(localName)
                        || astralisName != null && userName.equalsIgnoreCase(astralisName);
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    private void appendToHistoryFile(List<String> lines) {
        if (!lines.isEmpty()) {
            try {
                Files.createDirectories(this.historyFile.getParent());
                Files.writeString(
                        this.historyFile,
                        String.join(System.lineSeparator(), lines) + System.lineSeparator(),
                        StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.APPEND);
            } catch (IOException ignored) {
            }
        }
    }

    public Object getHistoryLock() {
        return this.historyLock;
    }

    public static boolean isLiveMessage(GuiMessage guiMessage) {

        if (guiMessage.tag() != null) {

            if ("IRC_LIVE".equals(guiMessage.tag().logTag())) {
                return true;
            }
        }

        return false;
    }

    public Set<String> getPendingMessageSet() {
        return this.pendingMessageSet;
    }

    private long parseEpochSecond(String line) {

        if (!line.startsWith("[")) {

            return Long.MAX_VALUE;
        }

        int closeBracket = line.indexOf(93);
        if (closeBracket < 0) {

            return Long.MAX_VALUE;
        }

        try {
            return LocalDateTime.parse(line.substring(1, closeBracket), timestampFormatter)
                    .toEpochSecond(ZoneOffset.UTC);
        } catch (DateTimeParseException ignored) {

            return Long.MAX_VALUE;
        }
    }

    public void addHistoryEntries(List<IrcHistoryEntry> entries) {

        ArrayList<String> newLines = new ArrayList<>();
        synchronized (this.historyLock) {
            for (IrcHistoryEntry entry : entries) {
                String body =
                        entry.getSenderName()
                                + " ("
                                + entry.getSenderId()
                                + "): "
                                + entry.getMessage();
                String line = "[" + entry.getSentAt() + "] " + body;
                if (this.addMessageToIndex(this.messageTimestampIndex, line)) {
                    newLines.add(line);
                }
            }

            this.historyLines.addAll(newLines);
        }

        this.appendToHistoryFile(newLines);

        for (String pendingLine : this.sortByTimestamp(newLines)) {
            this.enqueuePending(pendingLine);
        }
    }

    public boolean isHistoryQueued() {
        return this.historyQueued;
    }

    public static boolean isIrcMessage(GuiMessage guiMessage) {

        if (guiMessage.tag() != null) {

            if ("IRC".equals(guiMessage.tag().logTag())) {
                return true;
            }

            if ("IRC_LIVE".equals(guiMessage.tag().logTag())) {
                return true;
            }
        }

        return false;
    }

    private String stripTimestampPrefix(String line) {

        int closeBracket = line.indexOf(93);
        return closeBracket < 0 ? line : line.substring(closeBracket + 1).trim();
    }

    private boolean addMessageToIndex(Map<String, NavigableSet<Long>> index, String line) {
        long ceilingValue;
        String normalizedMessage = this.normalizeMessage(this.stripTimestampPrefix(line));
        long epochSecond = this.parseEpochSecond(line);
        NavigableSet<Long> timestamps =
                index.computeIfAbsent(normalizedMessage, key -> new TreeSet());

        if (timestamps.contains(Long.MAX_VALUE)) {
            return false;
        }

        if (epochSecond == Long.MAX_VALUE) {
            if (!timestamps.isEmpty()) {
                return false;
            }

            timestamps.add(epochSecond);
            return true;
        } else {

            Long ceiling = timestamps.ceiling(epochSecond - 30L);
            if (ceiling != null) {
                ceilingValue = ceiling;

                if (ceilingValue <= epochSecond + 30L) {
                    return false;
                }
            }

            timestamps.add(epochSecond);
            return true;
        }
    }

    private void enqueuePending(String line) {
        if (!this.displayedMessages.contains(line) && this.pendingMessageSet.add(line)) {
            this.pendingMessageQueue.add(line);
        }
    }

    public void queueRecentHistory() {
        if (!this.historyQueued && InstanceAccess.mc.player != null) {
            this.historyQueued = true;
            List<String> sortedLines = this.sortByTimestamp(this.historyLines);
            int lineCount = sortedLines.size();

            for (String line :
                    sortedLines.subList(Math.max(0, lineCount - 100), sortedLines.size())) {
                this.enqueuePending(line);
            }
        }
    }

    private String formatTimestampLabel(String timestamp) {

        try {
            LocalDateTime dateTime = LocalDateTime.parse(timestamp, timestampFormatter);
            DateTimeFormatter formatter =
                    dateTime.toLocalDate().equals(LocalDate.now())
                            ? todayFormatter
                            : olderDateFormatter;
            return "[" + formatter.format(dateTime) + "]";
        } catch (DateTimeParseException ignored) {
            return "[" + timestamp + "]";
        }
    }

    private void displayHistoryMessage(String line) {
        this.display(line, historyTag);
    }

    private void display(String line, GuiMessageTag guiMessageTag) {
        if (InstanceAccess.mc.player != null) {
            if (guiMessageTag != historyTag || this.displayedMessages.add(line)) {
                Component component = this.formatMessage(line);
                InstanceAccess.mc.execute(
                        () ->
                                InstanceAccess.mc
                                        .gui
                                        .hud
                                        .getChat()
                                        .addPlayerMessage(component, null, guiMessageTag));
            }
        }
    }

    private Component highlightMentions(String text) {
        Matcher matcher = mentionPattern.matcher(text);
        MutableComponent mutableComponent = Component.empty();

        int lastEnd;
        for (lastEnd = 0; matcher.find(); lastEnd = matcher.end()) {
            mutableComponent.append(
                    Component.literal(text.substring(lastEnd, matcher.start()))
                            .withStyle(ChatFormatting.WHITE));
            mutableComponent.append(
                    Component.literal(matcher.group()).withStyle(ChatFormatting.GREEN));
        }

        return mutableComponent.append(
                Component.literal(text.substring(lastEnd)).withStyle(ChatFormatting.WHITE));
    }

    public Set<String> getDisplayedMessages() {
        return this.displayedMessages;
    }

    public Map<String, NavigableSet<Long>> getMessageTimestampIndex() {
        return this.messageTimestampIndex;
    }

    public void requestRemoteHistory() {
        // Remote IRC history requires the removed backend; local history remains available.
    }
}
