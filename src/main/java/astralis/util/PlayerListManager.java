package astralis.util;

import astralis.util.json.JsonUtil;

import com.google.gson.JsonObject;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

import java.io.File;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Maintains local UUID lists for friends, enemies, and players marked as hackers. */
public class PlayerListManager extends Listener {
    private static final String FRIENDS_FILE_NAME = "Friends.astralis";
    private static final String ENEMIES_FILE_NAME = "Enemies.astralis";
    private static final String HACKERS_FILE_NAME = "Hackers.astralis";
    private final Set<UUID> friendIds;
    private final Set<UUID> enemyIds;
    private final Set<UUID> hackerIds;
    private final File friendsFile;
    private final File enemiesFile;
    private final File hackersFile;

    public PlayerListManager() {

        this.friendIds = ConcurrentHashMap.newKeySet();
        this.enemyIds = ConcurrentHashMap.newKeySet();
        this.hackerIds = ConcurrentHashMap.newKeySet();

        String directoryName = "Astralis".toLowerCase();
        File listDirectory = new File(Minecraft.getInstance().gameDirectory, directoryName);
        if (!listDirectory.exists()) {
            listDirectory.mkdirs();
        }

        this.friendsFile = new File(listDirectory, "Friends.astralis");

        this.enemiesFile = new File(listDirectory, "Enemies.astralis");

        this.hackersFile = new File(listDirectory, "Hackers.astralis");
        this.loadFriends();
        this.loadEnemies();
        this.loadHackers();
    }

    /** Adds the player to the enemy list when a player is supplied. */
    public void addEnemy(Player player) {
        if (player != null) {
            this.addEnemy(player.getUUID());
        }
    }

    private void loadEnemies() {
        if (this.enemiesFile.exists()) {
            try {
                JsonObject listJson = JsonUtil.readFromFile(this.enemiesFile.getAbsolutePath());
                if (listJson == null) {
                    return;
                }

                for (String storedPlayerId : listJson.keySet()) {
                    try {
                        this.enemyIds.add(UUID.fromString(storedPlayerId));
                    } catch (Exception invalidPlayerId) {
                    }
                }
            } catch (Exception loadFailure) {
            }
        }
    }

    /** Removes an enemy UUID and saves the enemy list when it changes. */
    public void removeEnemy(UUID playerId) {
        if (this.enemyIds.remove(playerId)) {
            this.saveEnemies();
        }
    }

    /** Adds the player to the friend list when a player is supplied. */
    public void addFriend(Player player) {
        if (player != null) {
            this.addFriend(player.getUUID());
        }
    }

    /** Checks whether the UUID is marked as a hacker. */
    public boolean isHacker(UUID playerId) {
        return this.hackerIds.contains(playerId);
    }

    /** Checks whether the supplied player is an enemy. */
    public boolean isEnemy(Player player) {
        return player != null && this.enemyIds.contains(player.getUUID());
    }

    /** Adds a friend UUID, removes it from the in-memory enemy set, and saves friends. */
    public void addFriend(UUID playerId) {
        if (this.friendIds.add(playerId)) {
            this.enemyIds.remove(playerId);
            this.saveFriends();
        }
    }

    /** Returns the live friend UUID set. */
    public Set<UUID> getFriendIds() {
        return this.friendIds;
    }

    /** Removes the supplied player from the friend list. */
    public void removeFriend(Player player) {
        if (player != null) {
            this.removeFriend(player.getUUID());
        }
    }

    private void saveEnemies() {
        JsonObject listJson = new JsonObject();

        for (UUID playerId : this.enemyIds) {
            listJson.addProperty(playerId.toString(), true);
        }

        JsonUtil.writeToFile(listJson, this.enemiesFile.getAbsolutePath());
    }

    public boolean isFriend(Player player) {
        return player != null && this.friendIds.contains(player.getUUID());
    }

    /** Checks whether the UUID is a friend. */
    public boolean isFriend(UUID playerId) {
        return this.friendIds.contains(playerId);
    }

    /** Removes the supplied player from the enemy list. */
    public void removeEnemy(Player player) {
        if (player != null) {
            this.removeEnemy(player.getUUID());
        }
    }

    /** Returns the live hacker UUID set. */
    public Set<UUID> getHackerIds() {
        return this.hackerIds;
    }

    private void saveHackers() {
        JsonObject listJson = new JsonObject();
        Iterator playerIds = this.hackerIds.iterator();

        while (playerIds.hasNext()) {
            UUID playerId = (UUID) playerIds.next();
            listJson.addProperty(playerId.toString(), true);
        }

        JsonUtil.writeToFile(listJson, this.hackersFile.getAbsolutePath());
    }

    private void loadHackers() {
        if (this.hackersFile.exists()) {
            try {
                JsonObject listJson = JsonUtil.readFromFile(this.hackersFile.getAbsolutePath());
                if (listJson == null) {
                    return;
                }

                for (String storedPlayerId : listJson.keySet()) {
                    try {
                        this.hackerIds.add(UUID.fromString(storedPlayerId));
                    } catch (Exception invalidPlayerId) {
                    }
                }
            } catch (Exception loadFailure) {
            }
        }
    }

    private void saveFriends() {
        JsonObject listJson = new JsonObject();
        Iterator playerIds = this.friendIds.iterator();

        while (playerIds.hasNext()) {
            UUID playerId = (UUID) playerIds.next();
            listJson.addProperty(playerId.toString(), true);
        }

        JsonUtil.writeToFile(listJson, this.friendsFile.getAbsolutePath());
    }

    /** Removes a friend UUID and saves the friend list when it changes. */
    public void removeFriend(UUID playerId) {
        if (this.friendIds.remove(playerId)) {
            this.saveFriends();
        }
    }

    /** Adds an enemy UUID, removes it from the in-memory friend set, and saves enemies. */
    public void addEnemy(UUID playerId) {
        if (this.enemyIds.add(playerId)) {
            this.friendIds.remove(playerId);
            this.saveEnemies();
        }
    }

    private void loadFriends() {
        if (this.friendsFile.exists()) {
            try {
                JsonObject listJson = JsonUtil.readFromFile(this.friendsFile.getAbsolutePath());
                if (listJson == null) {
                    return;
                }

                for (String storedPlayerId : listJson.keySet()) {
                    try {
                        this.friendIds.add(UUID.fromString(storedPlayerId));
                    } catch (Exception invalidPlayerId) {
                    }
                }
            } catch (Exception loadFailure) {
            }
        }
    }

    /** Returns the live enemy UUID set. */
    public Set<UUID> getEnemyIds() {
        return this.enemyIds;
    }

    /** Marks a non-null UUID as a hacker and saves the list when it changes. */
    public void addHacker(UUID playerId) {
        if (playerId != null && this.hackerIds.add(playerId)) {
            this.saveHackers();
        }
    }

    /** Checks whether the UUID is an enemy. */
    public boolean isEnemy(UUID playerId) {
        return this.enemyIds.contains(playerId);
    }
}
