package astralis.social.friends;

import astralis.Astralis;
import astralis.util.ChatUtil;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.textures.GpuTexture;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class FriendManager {
    private final List<String> incomingRequests;
    private final List<String> sentRequests;
    private final List<String> friends;
    private final List<String> onlineFriends;
    private volatile String lastMessageSender;
    private boolean onlineListInitialized;
    private final Map<String, List<String>> chatHistory;
    private final Map<String, byte[]> profilePictureData;
    private final Map<String, byte[]> uploadedPictureData;
    private final Map<String, GpuTexture> profilePictureTextures;
    private final Set<String> failedProfilePictures;
    private boolean notificationsMuted;

    public FriendManager() {
        super();
        this.incomingRequests = new CopyOnWriteArrayList<>();
        this.sentRequests = new CopyOnWriteArrayList<>();
        this.friends = new CopyOnWriteArrayList<>();
        this.onlineFriends = new CopyOnWriteArrayList<>();
        this.chatHistory = new ConcurrentHashMap<>();
        this.profilePictureData = new ConcurrentHashMap<>();
        this.uploadedPictureData = new ConcurrentHashMap<>();
        this.profilePictureTextures = new ConcurrentHashMap<>();
        this.failedProfilePictures = ConcurrentHashMap.newKeySet();
    }

    public void printFriendSummary() {
        String friendsText = this.formatNameList(this.friends);

        ChatUtil.print("Friends: " + friendsText);
        String onlineFriendsText = this.formatNameList(this.onlineFriends);

        ChatUtil.print("Online friends: " + onlineFriendsText);
        String incomingText = this.formatNameList(this.incomingRequests);

        ChatUtil.print("Incoming requests: " + incomingText);
        String sentText = this.formatNameList(this.sentRequests);

        ChatUtil.print("Sent requests: " + sentText);
    }

    public void setNotificationsMuted(boolean muted) {
        this.notificationsMuted = muted;
    }

    public void setFriends(List<String> newFriends) {
        this.friends.clear();
        this.friends.addAll(newFriends);
        this.profilePictureData
                .keySet()
                .removeIf(
                        name -> {
                            return !newFriends.contains(name);
                        });
        this.uploadedPictureData
                .keySet()
                .removeIf(
                        name -> {
                            return !newFriends.contains(name);
                        });
        this.profilePictureTextures
                .keySet()
                .removeIf(
                        name -> {
                            return !newFriends.contains(name);
                        });
        this.failedProfilePictures.removeIf(
                name -> {
                    return !newFriends.contains(name);
                });
    }

    public boolean areNotificationsMuted() {
        return this.notificationsMuted;
    }

    public Set<String> getFailedProfilePictures() {
        return this.failedProfilePictures;
    }

    public List<String> getIncomingRequests() {
        return this.incomingRequests;
    }

    public boolean isOnlineListInitialized() {
        return this.onlineListInitialized;
    }

    public void setFriendRequests(List<String> incoming, List<String> sent) {
        this.incomingRequests.clear();
        this.incomingRequests.addAll(incoming);
        this.sentRequests.clear();
        this.sentRequests.addAll(sent);
    }

    private void printNotificationComponent(Component component) {
        if (!this.notificationsMuted) {
            ChatUtil.print(component);
        }
    }

    public Map<String, byte[]> getProfilePictureData() {
        return this.profilePictureData;
    }

    public void notifyNewFriendRequests(List<String> requesters) {
        if (!requesters.isEmpty()) {
            String namesText = this.formatNameList(requesters);

            this.printNotification("New friend request(s): " + namesText);
        }
    }

    public Map<String, GpuTexture> getProfilePictureTextures() {
        return this.profilePictureTextures;
    }

    public List<String> getFriends() {
        return this.friends;
    }

    public void updateFriendHistory(String friendName, List<FriendChatHistoryEntry> entries) {
        List history =
                this.chatHistory.computeIfAbsent(friendName, key -> new CopyOnWriteArrayList<>());
        history.clear();

        this.printNotification("Friend chat history with " + friendName + ":");
        if (entries.isEmpty()) {

            this.printNotification("No messages.");
        } else {
            for (FriendChatHistoryEntry entry : entries) {
                String sentAtValue = entry.getSentAt();
                String senderNameValue = entry.getSenderName();
                String message = entry.getMessage();
                String receiverName = entry.getReceiverName();
                String sender = senderNameValue;
                String timestamp = sentAtValue;

                String line =
                        "[" + timestamp + "] " + sender + " -> " + receiverName + ": " + message;
                history.add(line);
                this.printNotification(line);
            }
        }
    }

    public void updateOnlineFriends(List<String> currentOnline) {
        List<String> previousOnline = List.copyOf(this.onlineFriends);
        this.onlineFriends.clear();
        this.onlineFriends.addAll(currentOnline);
        if (!this.onlineListInitialized) {
            this.onlineListInitialized = true;
        } else {
            for (String joined : currentOnline) {
                if (!previousOnline.contains(joined)) {

                    this.printNotificationComponent(
                            Component.literal("+ " + joined).withStyle(ChatFormatting.GREEN));
                }
            }

            for (String left : previousOnline) {
                if (!currentOnline.contains(left)) {

                    this.printNotificationComponent(
                            Component.literal("- " + left).withStyle(ChatFormatting.RED));
                }
            }
        }
    }

    public List<String> getSentRequests() {
        return this.sentRequests;
    }

    public GpuTexture getFriendProfilePictureTexture(String friendName) {

        byte[] pictureData = this.profilePictureData.get(friendName);
        if (pictureData != null && !this.failedProfilePictures.contains(friendName)) {
            byte[] uploadedData = this.uploadedPictureData.get(friendName);
            if (uploadedData == pictureData) {
                return this.profilePictureTextures.get(friendName);
            }

            try {
                NativeImage nativeImage = NativeImage.read(new ByteArrayInputStream(pictureData));

                String lowerName = friendName.toLowerCase(Locale.ROOT);

                String safeName = lowerName.replaceAll("[^a-z0-9_./-]", "_");

                Identifier identifier =
                        Identifier.fromNamespaceAndPath("astralis", "friend_pfp_" + safeName);
                Minecraft.getInstance()
                        .getTextureManager()
                        .register(
                                identifier,
                                new DynamicTexture(() -> "friend_pfp_" + friendName, nativeImage));
                this.profilePictureTextures.put(
                        friendName,
                        Minecraft.getInstance()
                                .getTextureManager()
                                .getTexture(identifier)
                                .getTexture());
                this.uploadedPictureData.put(friendName, pictureData);
                return this.profilePictureTextures.get(friendName);
            } catch (Exception e) {
                this.failedProfilePictures.add(friendName);

                Astralis.LOGGER.error("Failed to load friend pfp for {}", friendName, e);
                return null;
            }
        } else {
            return null;
        }
    }

    public void updateProfilePictures(List<FriendProfilePictureEntry> entries) {

        for (FriendProfilePictureEntry entry : entries) {
            byte[] existingData = this.profilePictureData.get(entry.getFriendName());
            if (existingData == null || !Arrays.equals(existingData, entry.getProfilePicture())) {
                this.profilePictureData.put(entry.getFriendName(), entry.getProfilePicture());
                this.uploadedPictureData.remove(entry.getFriendName());
                this.profilePictureTextures.remove(entry.getFriendName());
                this.failedProfilePictures.remove(entry.getFriendName());
            }
        }
    }

    public void receiveFriendMessage(
            String sender, String receiver, String message, String sentAt) {
        String ownName = Minecraft.getInstance().getUser().getName();
        if (!sender.equalsIgnoreCase(ownName)) {
            this.lastMessageSender = sender;
        }

        String messageText = message;
        String receiverText = receiver;
        String senderText = sender;
        String sentAtText = sentAt;

        String historyLine =
                "[" + sentAtText + "] " + senderText + " -> " + receiverText + ": " + messageText;
        this.chatHistory
                .computeIfAbsent(
                        this.resolveChatPartner(sender, receiver),
                        key -> new CopyOnWriteArrayList<>())
                .add(historyLine);
        String dmMessage = message;
        String dmReceiver = receiver;
        String dmSender = sender;
        this.printNotification("[DM] " + dmSender + " -> " + dmReceiver + ": " + dmMessage);
    }

    public Map<String, byte[]> getUploadedPictureData() {
        return this.uploadedPictureData;
    }

    private String resolveChatPartner(String sender, String receiver) {
        return this.friends.contains(sender) ? sender : receiver;
    }

    private void printNotification(String message) {
        if (!this.notificationsMuted) {
            ChatUtil.print(message);
        }
    }

    public Map<String, List<String>> getChatHistory() {
        return this.chatHistory;
    }

    private String formatNameList(List<String> names) {

        String result;
        if (names.isEmpty()) {

            result = "none";
        } else {

            result = String.join(", ", names);
        }

        return result;
    }

    public String getLastMessageSender() {
        return this.lastMessageSender;
    }

    public List<String> getOnlineFriends() {
        return this.onlineFriends;
    }
}
