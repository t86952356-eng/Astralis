package com.astralis.scripting.api;

import com.astralis.scripting.InstanceAccess;
import com.astralis.scripting.Script;
import com.astralis.scripting.api.entity.ScriptEntity;
import com.astralis.scripting.api.entity.ScriptNetworkPlayer;
import com.astralis.scripting.api.entity.ScriptPlayer;
import com.astralis.scripting.api.math.ScriptVector3;
import com.astralis.scripting.api.world.ScriptBlockState;
import com.astralis.scripting.api.world.ScriptScoreboardEntry;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;

import java.lang.StackWalker.Option;
import java.lang.StackWalker.StackFrame;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;

public class World implements InstanceAccess {
    private volatile Script owner;
    private boolean destroyed;

    public final synchronized void destroy() {
        checkScriptRuntimeCaller();
        if (!this.destroyed) {
            this.destroyed = true;
            this.owner = null;
        }
    }

    public boolean exists() {
        return mc.level != null;
    }

    public final synchronized void bind(Script script) {
        checkScriptRuntimeCaller();
        if (script == null) {
            throw new IllegalArgumentException("Script owner cannot be null.");
        }

        if (this.destroyed) {
            throw new IllegalStateException("This world service has already been destroyed.");
        }

        if (this.owner != null) {
            if (this.owner != script) {
                throw new IllegalStateException("This world service is already bound.");
            }
        } else {
            this.owner = script;
        }
    }

    public long getTime() {
        return mc.level != null ? mc.level.getGameTime() : 0L;
    }

    public long getDayTime() {
        return mc.level != null ? mc.level.getOverworldClockTime() % 24000L : 0L;
    }

    public ScriptPlayer getPlayerByName(String name) {
        if (mc.level == null) {
            return null;
        }

        for (Player player2 : mc.level.players()) {
            if (player2.getName().getString().equalsIgnoreCase(name)) {
                return new ScriptPlayer(player2, this.owner);
            }
        }

        return null;
    }

    public ScriptBlockState getBlockState(int x, int y, int z) {
        if (mc.level == null) {
            return null;
        }

        BlockPos blockPos = new BlockPos(x, y, z);
        BlockState blockState = mc.level.getBlockState(blockPos);
        LinkedHashMap properties = new LinkedHashMap();
        blockState
                .getValues()
                .forEach(
                        propertyValue ->
                                properties.put(
                                        propertyValue.property().getName(),
                                        propertyValue.valueName()));
        return new ScriptBlockState(
                BuiltInRegistries.BLOCK.getKey(blockState.getBlock()).toString(),
                blockState.isAir(),
                blockState.canBeReplaced(),
                !blockState.getCollisionShape(mc.level, blockPos).isEmpty(),
                isInteractable(blockState, blockPos),
                !blockState.getFluidState().isEmpty(),
                properties);
    }

    public ScriptBlockState getBlockState(ScriptVector3 position) {
        return position == null
                ? null
                : this.getBlockState(
                        (int) Math.floor(position.x()),
                        (int) Math.floor(position.y()),
                        (int) Math.floor(position.z()));
    }

    public String getBiome(int x, int y, int z) {
        return mc.level == null
                ? "unknown"
                : mc.level
                        .getBiome(new BlockPos(x, y, z))
                        .unwrapKey()
                        .map(biomeKey -> biomeKey.identifier().toString())
                        .orElse("unknown");
    }

    public List<ScriptPlayer> getPlayers() {
        return mc.level == null
                ? List.of()
                : mc.level.players().stream()
                        .filter(abstractClientPlayer -> abstractClientPlayer != mc.player)
                        .map(
                                abstractClientPlayer2 ->
                                        new ScriptPlayer(abstractClientPlayer2, this.owner))
                        .toList();
    }

    public boolean isRaining() {
        return mc.level != null && mc.level.isRaining();
    }

    public int getEntityCount() {
        return mc.level != null ? mc.level.getEntityCount() : 0;
    }

    public ScriptBlockState getBlockAt(int x, int y, int z) {
        return this.getBlockState(x, y, z);
    }

    public ScriptBlockState getBlockAt(ScriptVector3 position) {
        return this.getBlockState(position);
    }

    private static void checkScriptRuntimeCaller() {
        Class callerClass =
                StackWalker.getInstance(Option.RETAIN_CLASS_REFERENCE)
                        .walk(
                                frames ->
                                        frames.map(StackFrame::getDeclaringClass)
                                                .filter(frameClass -> frameClass != World.class)
                                                .findFirst()
                                                .orElseThrow(
                                                        () ->
                                                                new SecurityException(
                                                                        "Missing script runtime caller.")));
        if (callerClass != Script.class
                && (!Script.class.isAssignableFrom(callerClass)
                        || callerClass.getClassLoader() != Script.class.getClassLoader())) {
            throw new SecurityException("Script world ownership is managed by the runtime.");
        }
    }

    private ScriptEntity toScriptEntity(Entity entity2) {
        return entity2 instanceof Player player2
                ? new ScriptPlayer(player2, this.owner)
                : new ScriptEntity(entity2, this.owner);
    }

    public List<ScriptEntity> getEntities() {
        if (mc.level == null) {
            return List.of();
        }

        ArrayList entities = new ArrayList();

        for (Entity entity2 : mc.level.entitiesForRendering()) {
            if (entity2 != mc.player) {
                entities.add(this.toScriptEntity(entity2));
            }
        }

        return List.copyOf(entities);
    }

    public String getDimension() {
        return mc.level != null ? mc.level.dimension().identifier().toString() : "unknown";
    }

    public float getRainLevel() {
        return mc.level != null ? mc.level.getRainLevel(1.0F) : 0.0F;
    }

    public boolean isAir(int x, int y, int z) {
        return mc.level != null && mc.level.isEmptyBlock(new BlockPos(x, y, z));
    }

    static boolean isInteractable(BlockState blockState, BlockPos blockPos) {
        if (mc.level != null && blockState.getMenuProvider(mc.level, blockPos) != null) {
            return true;
        }

        String blockId = BuiltInRegistries.BLOCK.getKey(blockState.getBlock()).toString();
        return blockId.contains("button")
                || blockId.contains("lever")
                || blockId.contains("door")
                || blockId.contains("trapdoor")
                || blockId.contains("fence_gate")
                || blockId.contains("bed")
                || blockId.contains("cake")
                || blockId.contains("bell")
                || blockId.contains("note_block")
                || blockId.contains("respawn_anchor")
                || blockId.contains("crafting_table");
    }

    public List<ScriptEntity> getPlayerEntities() {
        return List.copyOf(this.getPlayers());
    }

    public String getSidebarTitle() {
        if (mc.level == null) {
            return "";
        }

        Objective objective = mc.level.getScoreboard().getDisplayObjective(DisplaySlot.SIDEBAR);
        return objective != null ? objective.getDisplayName().getString() : "";
    }

    public boolean isBlockReplaceable(int x, int y, int z) {
        return mc.level != null && mc.level.getBlockState(new BlockPos(x, y, z)).canBeReplaced();
    }

    public boolean isBlockInteractable(int x, int y, int z) {
        if (mc.level == null) {
            return false;
        }

        BlockPos blockPos = new BlockPos(x, y, z);
        return isInteractable(mc.level.getBlockState(blockPos), blockPos);
    }

    public ScriptEntity getEntityById(int id) {
        if (mc.level == null) {
            return null;
        }

        Entity entity2 = mc.level.getEntity(id);
        return entity2 != null ? this.toScriptEntity(entity2) : null;
    }

    public ScriptEntity getClosestEntity(double radius) {
        if (mc.level != null && mc.player != null && Double.isFinite(radius)) {
            double clampedRadius = Math.clamp(radius, 0.0, 128.0);
            return mc
                    .level
                    .getEntitiesOfClass(
                            Entity.class,
                            mc.player.getBoundingBox().inflate(clampedRadius),
                            entity2 -> entity2 != mc.player)
                    .stream()
                    .filter(
                            entity3 ->
                                    mc.player.distanceToSqr(entity3)
                                            <= clampedRadius * clampedRadius)
                    .min(Comparator.comparingDouble(mc.player::distanceToSqr))
                    .map(this::toScriptEntity)
                    .orElse(null);
        } else {
            return null;
        }
    }

    public List<ScriptEntity> getEntitiesInRadius(ScriptVector3 center, double radius) {
        if (mc.level != null && center != null && Double.isFinite(radius)) {
            double clampedRadius = Math.clamp(radius, 0.0, 128.0);
            return this.getEntities().stream()
                    .filter(
                            scriptEntity ->
                                    scriptEntity.getPosition().distanceToSquared(center)
                                            <= clampedRadius * clampedRadius)
                    .toList();
        } else {
            return List.of();
        }
    }

    public List<ScriptEntity> getEntitiesInRadius(double radius) {
        if (mc.level != null && mc.player != null && Double.isFinite(radius)) {
            double clampedRadius = Math.clamp(radius, 0.0, 128.0);
            return mc
                    .level
                    .getEntitiesOfClass(
                            Entity.class,
                            mc.player.getBoundingBox().inflate(clampedRadius),
                            entity2 -> entity2 != mc.player)
                    .stream()
                    .filter(
                            entity3 ->
                                    mc.player.distanceToSqr(entity3)
                                            <= clampedRadius * clampedRadius)
                    .map(this::toScriptEntity)
                    .toList();
        } else {
            return List.of();
        }
    }

    public ScriptEntity getEntityByName(String name) {
        if (mc.level == null) {
            return null;
        }

        for (Entity entity2 : mc.level.entitiesForRendering()) {
            if (entity2.getName().getString().equalsIgnoreCase(name)) {
                return this.toScriptEntity(entity2);
            }
        }

        return null;
    }

    public ScriptPlayer getClosestPlayer() {
        return mc.level != null && mc.player != null
                ? mc.level.players().stream()
                        .filter(abstractClientPlayer -> abstractClientPlayer != mc.player)
                        .min(Comparator.comparingDouble(mc.player::distanceToSqr))
                        .map(
                                abstractClientPlayer2 ->
                                        new ScriptPlayer(abstractClientPlayer2, this.owner))
                        .orElse(null)
                : null;
    }

    public String getBlockName(int x, int y, int z) {
        return mc.level == null
                ? "minecraft:air"
                : BuiltInRegistries.BLOCK
                        .getKey(mc.level.getBlockState(new BlockPos(x, y, z)).getBlock())
                        .toString();
    }

    public String getBlockName(ScriptVector3 position) {
        return position == null
                ? "minecraft:air"
                : this.getBlockName(
                        (int) Math.floor(position.x()),
                        (int) Math.floor(position.y()),
                        (int) Math.floor(position.z()));
    }

    public float getBlockHardness(int x, int y, int z) {
        if (mc.level == null) {
            return 0.0F;
        }

        BlockPos blockPos = new BlockPos(x, y, z);
        return mc.level.getBlockState(blockPos).getDestroySpeed(mc.level, blockPos);
    }

    public int getRedstonePower(int x, int y, int z) {
        return mc.level == null ? 0 : mc.level.getBestNeighborSignal(new BlockPos(x, y, z));
    }

    public List<ScriptScoreboardEntry> getSidebarScores() {
        if (mc.level == null) {
            return List.of();
        }

        Objective objective = mc.level.getScoreboard().getDisplayObjective(DisplaySlot.SIDEBAR);
        return objective == null
                ? List.of()
                : mc.level.getScoreboard().listPlayerScores(objective).stream()
                        .limit(128L)
                        .map(
                                playerScoreEntry ->
                                        new ScriptScoreboardEntry(
                                                playerScoreEntry.ownerName().getString(),
                                                playerScoreEntry.value()))
                        .toList();
    }

    public String getFluidName(int x, int y, int z) {
        return mc.level == null
                ? "minecraft:empty"
                : BuiltInRegistries.FLUID
                        .getKey(mc.level.getFluidState(new BlockPos(x, y, z)).getType())
                        .toString();
    }

    public List<ScriptNetworkPlayer> getNetworkPlayers() {
        return mc.player == null
                ? List.of()
                : mc.player.connection.getOnlinePlayers().stream()
                        .map(
                                playerInfo ->
                                        new ScriptNetworkPlayer(
                                                playerInfo.getProfile().id().toString(),
                                                playerInfo.getProfile().name(),
                                                playerInfo.getTabListDisplayName() != null
                                                        ? playerInfo
                                                                .getTabListDisplayName()
                                                                .getString()
                                                        : playerInfo.getProfile().name(),
                                                playerInfo.getLatency(),
                                                playerInfo.getGameMode().getName()))
                        .toList();
    }

    public boolean isBlockLoaded(int x, int y, int z) {
        return mc.level != null && mc.level.isLoaded(new BlockPos(x, y, z));
    }

    public int getLightLevel(int x, int y, int z) {
        return mc.level == null
                ? 0
                : mc.level.getLightEngine().getRawBrightness(new BlockPos(x, y, z), 0);
    }

    public boolean containsFluid(int x, int y, int z) {
        return mc.level != null && !mc.level.getFluidState(new BlockPos(x, y, z)).isEmpty();
    }

    public boolean isBlockSolid(int x, int y, int z) {
        if (mc.level == null) {
            return false;
        }

        BlockPos blockPos = new BlockPos(x, y, z);
        return !mc.level.getBlockState(blockPos).getCollisionShape(mc.level, blockPos).isEmpty();
    }
}
