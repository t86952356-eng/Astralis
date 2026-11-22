package com.astralis.scripting.api;

import astralis.Astralis;
import astralis.mixin.ClientInputAccessor;
import astralis.social.friends.FriendManager;
import astralis.util.MoveUtil;
import astralis.util.ServerUtil;

import com.astralis.scripting.InstanceAccess;
import com.astralis.scripting.Script;
import com.astralis.scripting.api.entity.ScriptEntity;
import com.astralis.scripting.api.entity.ScriptPlayer;
import com.astralis.scripting.api.math.ScriptRotation;
import com.astralis.scripting.api.math.ScriptVector3;
import com.astralis.scripting.api.render.ScriptColor;
import com.astralis.scripting.api.screen.ScriptScreens;
import com.astralis.scripting.api.world.BlockFace;
import com.astralis.scripting.api.world.ScriptBlockHit;
import com.astralis.scripting.api.world.ScriptEntityHit;
import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

import org.lwjgl.glfw.GLFW;

import java.lang.StackWalker.Option;
import java.lang.StackWalker.StackFrame;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RejectedExecutionException;

public class Client implements InstanceAccess {
    private final Set<FutureTask<Void>> pendingTasks = new HashSet<>();
    private Script owner;
    private ExecutorService asyncExecutor;
    private long generation;
    private int pendingClientCallbacks;
    private boolean bound;
    private boolean destroyed;

    private static void checkScriptRuntimeCaller() {
        Class callerClass =
                StackWalker.getInstance(Option.RETAIN_CLASS_REFERENCE)
                        .walk(
                                frames ->
                                        frames.map(StackFrame::getDeclaringClass)
                                                .filter(frameClass -> frameClass != Client.class)
                                                .findFirst()
                                                .orElseThrow(
                                                        () ->
                                                                new SecurityException(
                                                                        "Missing script runtime caller.")));
        if (callerClass != Script.class
                && (!Script.class.isAssignableFrom(callerClass)
                        || callerClass.getClassLoader() != Script.class.getClassLoader())) {
            throw new SecurityException("Only the script runtime can change client ownership.");
        }
    }

    public void stop() {
        this.ensureActive();
        if (mc.player != null) {
            MoveUtil.stopHorizontalMotion();
        }
    }

    public void print(String message) {
        this.ensureActive();
        if (mc.player == null) {
            Astralis.LOGGER.info("[Script] {}", message);
        } else {
            mc.gui
                    .hud
                    .getChat()
                    .addClientSystemMessage(Component.literal("§7[Script] §f" + message));
        }
    }

    public final void destroy() {
        checkScriptRuntimeCaller();
        Set<FutureTask<Void>> tasksToCancel;
        ExecutorService executor;
        synchronized (this) {
            if (this.destroyed) {
                return;
            }

            this.destroyed = true;
            this.bound = false;
            this.generation++;
            tasksToCancel = Set.copyOf(this.pendingTasks);
            this.pendingTasks.clear();
            executor = this.asyncExecutor;
            this.asyncExecutor = null;
        }

        tasksToCancel.forEach(task -> task.cancel(true));
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    public final synchronized void bind(Script script) {
        checkScriptRuntimeCaller();
        Objects.requireNonNull(script, "owner");
        if (this.destroyed) {
            throw new IllegalStateException("This client service has already been destroyed.");
        }

        if (this.owner != null) {
            if (this.owner != script || !this.bound) {
                throw new IllegalStateException("This client service is already bound.");
            }
        } else {
            this.owner = script;
            this.generation++;
            this.asyncExecutor =
                    Executors.newSingleThreadExecutor(
                            runnable -> {
                                Thread thread =
                                        new Thread(
                                                runnable,
                                                "Astralis-Script-" + script.getName() + "-Async");
                                thread.setDaemon(true);
                                return thread;
                            });
            this.bound = true;
        }
    }

    public void jump() {
        this.ensureActive();
        if (mc.player != null) {
            mc.player.jumpFromGround();
        }
    }

    public void async(Runnable task) {
        if (task != null) {
            synchronized (this) {
                this.ensureActive();
                if (this.pendingTasks.size() >= 32) {
                    throw new RejectedExecutionException(
                            "A script cannot have more than 32 outstanding async tasks.");
                }

                long currentGeneration = this.generation;
                FutureTask futureTask =
                        new FutureTask<Void>(
                                () -> {
                                    if (this.isGenerationCurrent(currentGeneration)) {
                                        try {
                                            task.run();
                                        } catch (Throwable throwable) {
                                            if (this.isGenerationCurrent(currentGeneration)) {
                                                this.owner.reportError("client.async", throwable);
                                            }
                                        }
                                    }
                                },
                                null) {
                            @Override
                            protected void done() {
                                synchronized (Client.this) {
                                    Client.this.pendingTasks.remove(this);
                                }
                            }
                        };
                this.pendingTasks.add(futureTask);

                try {
                    this.asyncExecutor.execute(futureTask);
                } catch (RuntimeException e) {
                    this.pendingTasks.remove(futureTask);
                    throw e;
                }
            }
        }
    }

    public String getUsername() {
        return mc.getUser().getName();
    }

    public int getWindowWidth() {
        return mc.getWindow().getGuiScaledWidth();
    }

    public int getWindowHeight() {
        return mc.getWindow().getGuiScaledHeight();
    }

    public float getForwardInput() {
        return mc.player != null ? mc.player.input.getMoveVector().y : 0.0F;
    }

    public ScriptRotation getRotation(ScriptVector3 position) {
        if (position == null) {
            return new ScriptRotation(0.0F, 0.0F);
        }

        float[] rotations = this.getRotations(position.x(), position.y(), position.z());
        return new ScriptRotation(rotations[0], rotations[1]);
    }

    public void setSprinting(boolean sprinting) {
        this.setMovementInput(
                this.getForwardInput(),
                this.getStrafeInput(),
                this.isJumping(),
                this.isSneaking(),
                sprinting);
        if (mc.player != null) {
            mc.player.setSprinting(sprinting);
        }
    }

    public void setSneak(boolean sneaking) {
        this.setMovementInput(
                this.getForwardInput(),
                this.getStrafeInput(),
                this.isJumping(),
                sneaking,
                this.isSprinting());
    }

    public void setJump(boolean jumping) {
        this.setMovementInput(
                this.getForwardInput(),
                this.getStrafeInput(),
                jumping,
                this.isSneaking(),
                this.isSprinting());
    }

    public void setStrafe(float strafe) {
        this.setMovementInput(
                this.getForwardInput(),
                strafe,
                this.isJumping(),
                this.isSneaking(),
                this.isSprinting());
    }

    public void setForward(float forward) {
        this.setMovementInput(
                forward,
                this.getStrafeInput(),
                this.isJumping(),
                this.isSneaking(),
                this.isSprinting());
    }

    public void setFlying(boolean flying) {
        this.ensureActive();
        if (mc.player != null && mc.player.getAbilities().mayfly) {
            mc.player.getAbilities().flying = flying;
            mc.player.onUpdateAbilities();
        }
    }

    public void setMotion(ScriptVector3 motion) {
        if (motion != null) {
            this.setMotion(motion.x(), motion.y(), motion.z());
        }
    }

    public void setMotion(double x, double y, double z) {
        this.ensureActive();
        if (mc.player != null && Double.isFinite(x) && Double.isFinite(y) && Double.isFinite(z)) {
            mc.player.setDeltaMovement(x, y, z);
        }
    }

    public boolean isInGame() {
        return mc.player != null && mc.level != null;
    }

    public void reconnect() {
        this.ensureActive();
        if (ServerUtil.canReconnect()) {
            Screen screen2 = mc.gui.screen();
            ServerUtil.reconnect(screen2);
        }
    }

    public boolean isMoving() {
        return mc.player != null && MoveUtil.isMoving();
    }

    public void setClipboard(String text) {
        this.ensureActive();
        if (text != null && text.length() <= 16384) {
            mc.keyboardHandler.setClipboard(text);
        } else {
            throw new IllegalArgumentException(
                    "Clipboard text must contain at most 16384 characters.");
        }
    }

    public boolean useItem(boolean offHand, float yaw, float pitch) {
        this.ensureActive();
        if (mc.player != null
                && mc.gameMode != null
                && Float.isFinite(yaw)
                && Float.isFinite(pitch)) {
            float originalYaw = mc.player.getYRot();
            float originalPitch = mc.player.getXRot();

            try {
                mc.player.setYRot(yaw);
                mc.player.setXRot(Math.clamp(pitch, -90.0F, 90.0F));
                return mc.gameMode
                        .useItem(
                                mc.player,
                                offHand ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND)
                        .consumesAction();
            } finally {
                mc.player.setYRot(originalYaw);
                mc.player.setXRot(originalPitch);
            }
        } else {
            return false;
        }
    }

    public boolean useItem(boolean offHand) {
        this.ensureActive();
        return mc.player != null
                && mc.gameMode != null
                && mc.gameMode
                        .useItem(
                                mc.player,
                                offHand ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND)
                        .consumesAction();
    }

    public void swing(boolean offHand) {
        this.ensureActive();
        if (mc.player != null) {
            mc.player.swing(offHand ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        }
    }

    public ScriptPlayer getPlayer() {
        this.ensureActive();
        return mc.player != null ? new ScriptPlayer(mc.player, this.owner) : null;
    }

    public boolean isKeyDown(int keyCode) {
        return keyCode >= 32 && keyCode <= 348 && InputConstants.isKeyDown(mc.getWindow(), keyCode);
    }

    public int getFps() {
        return mc.getFps();
    }

    public boolean isFriend(String name) {
        if (name != null) {
            Astralis astralisInstance = Astralis.getInstance();
            FriendManager friendManager = astralisInstance.getFriendManager();
            if (friendManager.getFriends().stream()
                    .anyMatch(friend -> friend.equalsIgnoreCase(name))) {
                return true;
            }
        }

        return false;
    }

    public boolean isPaused() {
        return mc.isPaused();
    }

    public void sendChat(String message) {
        this.ensureActive();
        if (mc.player != null && message != null) {
            if (message.startsWith("/")) {
                mc.player.connection.sendCommand(message.substring(1));
            } else {
                mc.player.connection.sendChat(message);
            }
        }
    }

    public List<String> getFriends() {
        Astralis astralisInstance = Astralis.getInstance();
        FriendManager friendManager = astralisInstance.getFriendManager();
        return List.copyOf(friendManager.getFriends());
    }

    public double[] getMotion() {
        return mc.player == null
                ? new double[] {0.0, 0.0, 0.0}
                : new double[] {
                    mc.player.getDeltaMovement().x,
                    mc.player.getDeltaMovement().y,
                    mc.player.getDeltaMovement().z
                };
    }

    public String getUuid() {
        return mc.getUser().getProfileId().toString();
    }

    public void setSpeed(double speed) {
        this.ensureActive();
        if (mc.player != null && Double.isFinite(speed)) {
            double moveSpeed = speed;
            MoveUtil.setSpeed(moveSpeed);
        }
    }

    public boolean isCreative() {
        return mc.player != null && mc.player.isCreative();
    }

    public double getSpeed() {
        return mc.player != null ? MoveUtil.getSpeed() : 0.0;
    }

    private synchronized boolean isGenerationCurrent(long expectedGeneration) {
        return this.bound
                && !this.destroyed
                && this.owner != null
                && this.owner.isRuntimeActive()
                && this.generation == expectedGeneration;
    }

    public boolean isJumping() {
        return mc.player != null && mc.player.input.keyPresses.jump();
    }

    private static Direction toDirection(BlockFace face) {
        return switch (face) {
            case DOWN -> Direction.DOWN;
            case UP -> Direction.UP;
            case NORTH -> Direction.NORTH;
            case SOUTH -> Direction.SOUTH;
            case WEST -> Direction.WEST;
            case EAST -> Direction.EAST;
        };
    }

    public boolean isContainerOpen() {
        return mc.gui.screen() instanceof AbstractContainerScreen;
    }

    public float getMovementDirection() {
        return mc.player != null ? MoveUtil.getMoveYaw() : 0.0F;
    }

    public void playSound(String soundName, float volume, float pitch) {
        this.ensureActive();
        if (mc.player != null
                && soundName != null
                && Float.isFinite(volume)
                && Float.isFinite(pitch)) {
            SoundEvent soundEvent =
                    switch (soundName.toLowerCase()) {
                        case "random.orb" -> SoundEvents.EXPERIENCE_ORB_PICKUP;
                        case "random.levelup" -> SoundEvents.PLAYER_LEVELUP;
                        case "random.bow" -> SoundEvents.ARROW_SHOOT;
                        case "random.fizz" -> SoundEvents.FIRE_EXTINGUISH;
                        case "random.pop" -> SoundEvents.ITEM_PICKUP;
                        default -> null;
                    };
            if (soundEvent != null) {
                mc.player.playSound(
                        soundEvent, Math.clamp(volume, 0.0F, 4.0F), Math.clamp(pitch, 0.01F, 4.0F));
            }
        }
    }

    public double getClientVersion() {
        return 2.0;
    }

    public boolean canFly() {
        return mc.player != null && mc.player.getAbilities().mayfly;
    }

    public int getPing() {
        return ServerUtil.getPing();
    }

    public boolean isSneaking() {
        return mc.player != null && mc.player.isShiftKeyDown();
    }

    public boolean isWindowFocused() {
        return mc.isWindowActive();
    }

    public int getGuiScale() {
        return mc.getWindow().getGuiScale();
    }

    private synchronized void ensureActive() {
        if (!this.bound || this.destroyed || this.owner == null || !this.owner.isRuntimeActive()) {
            throw new IllegalStateException("The script client is not active.");
        }
    }

    public int getAvailableProcessors() {
        return Runtime.getRuntime().availableProcessors();
    }

    public boolean placeBlock(ScriptBlockHit hit) {
        this.ensureActive();
        if (mc.player != null
                && mc.level != null
                && mc.gameMode != null
                && hit != null
                && mc.player.getMainHandItem().getItem() instanceof BlockItem) {
            if (!(hit.hitX() < hit.supportX() - 0.001)
                    && !(hit.hitX() > hit.supportX() + 1.001)
                    && !(hit.hitY() < hit.supportY() - 0.001)
                    && !(hit.hitY() > hit.supportY() + 1.001)
                    && !(hit.hitZ() < hit.supportZ() - 0.001)
                    && !(hit.hitZ() > hit.supportZ() + 1.001)) {
                BlockPos blockPos = new BlockPos(hit.supportX(), hit.supportY(), hit.supportZ());
                BlockPos blockPos2 = new BlockPos(hit.targetX(), hit.targetY(), hit.targetZ());
                if (mc.level.isLoaded(blockPos)
                        && mc.level.isLoaded(blockPos2)
                        && !mc.level.getBlockState(blockPos).canBeReplaced()
                        && mc.level.getBlockState(blockPos2).canBeReplaced()
                        && (!World.isInteractable(mc.level.getBlockState(blockPos), blockPos)
                                || mc.player.isShiftKeyDown()
                                || mc.options.keyShift.isDown())
                        && !(mc.player
                                        .getEyePosition()
                                        .distanceToSqr(hit.hitX(), hit.hitY(), hit.hitZ())
                                > 36.0)) {
                    BlockHitResult blockHitResult =
                            new BlockHitResult(
                                    new Vec3(hit.hitX(), hit.hitY(), hit.hitZ()),
                                    toDirection(hit.face()),
                                    blockPos,
                                    false);
                    if (!mc.gameMode
                            .useItemOn(mc.player, InteractionHand.MAIN_HAND, blockHitResult)
                            .consumesAction()) {
                        return false;
                    }

                    mc.player.swing(InteractionHand.MAIN_HAND);
                    return true;
                } else {
                    return false;
                }
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    public long getNanoTime() {
        return System.nanoTime();
    }

    public String getGameDirectory() {
        return mc.gameDirectory.getAbsolutePath();
    }

    public void dropItem(boolean fullStack) {
        this.ensureActive();
        if (mc.player != null) {
            mc.player.drop(fullStack);
        }
    }

    public boolean isFlying() {
        return mc.player != null && mc.player.getAbilities().flying;
    }

    public boolean isRiding() {
        return mc.player != null && mc.player.isPassenger();
    }

    public boolean isSingleplayer() {
        return mc.hasSingleplayerServer();
    }

    public boolean isFullscreen() {
        return mc.getWindow().isFullscreen();
    }

    public boolean isSprinting() {
        return mc.player != null && mc.player.isSprinting();
    }

    public boolean isSpectator() {
        return mc.player != null && mc.player.isSpectator();
    }

    private static BlockFace toBlockFace(Direction direction) {
        return switch (direction) {
            case DOWN -> BlockFace.DOWN;
            case UP -> BlockFace.UP;
            case NORTH -> BlockFace.NORTH;
            case SOUTH -> BlockFace.SOUTH;
            case WEST -> BlockFace.WEST;
            case EAST -> BlockFace.EAST;
        };
    }

    public String getServerAddress() {
        return ServerUtil.getServerAddress();
    }

    public boolean isFriendOnline(String name) {
        if (name != null) {
            Astralis astralisInstance = Astralis.getInstance();
            FriendManager friendManager = astralisInstance.getFriendManager();
            if (friendManager.getOnlineFriends().stream()
                    .anyMatch(friend -> friend.equalsIgnoreCase(name))) {
                return true;
            }
        }

        return false;
    }

    public String getDisplayServerAddress() {
        return ServerUtil.getServerDisplayName();
    }

    public String getMinecraftVersion() {
        return mc.getLaunchedVersion();
    }

    public String getScreenTitle() {
        return mc.gui.screen() != null ? mc.gui.screen().getTitle().getString() : "";
    }

    public boolean canReconnect() {
        return ServerUtil.canReconnect();
    }

    public long getSystemTime() {
        return System.currentTimeMillis();
    }

    public ScriptColor getSecondColor() {
        Astralis astralisInstance = Astralis.getInstance();
        return new ScriptColor(astralisInstance.getSecondaryColor().getRGB());
    }

    public List<String> getOnlineFriends() {
        Astralis astralisInstance = Astralis.getInstance();
        FriendManager friendManager = astralisInstance.getFriendManager();
        return List.copyOf(friendManager.getOnlineFriends());
    }

    public String getCurrentScreen() {
        return mc.gui.screen() != null ? mc.gui.screen().getClass().getSimpleName() : "";
    }

    public int getFramebufferWidth() {
        return mc.getWindow().getScreenWidth();
    }

    public int getFramebufferHeight() {
        return mc.getWindow().getScreenHeight();
    }

    public String getClientName() {
        return "Astralis";
    }

    public ScriptColor getFirstColor() {
        Astralis astralisInstance = Astralis.getInstance();
        return new ScriptColor(astralisInstance.getPrimaryColor().getRGB());
    }

    public boolean isMouseButtonDown(int button) {
        return button >= 0
                && button <= 7
                && GLFW.glfwGetMouseButton(mc.getWindow().handle(), button) == 1;
    }

    public long getUsedMemory() {
        return Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
    }

    public long getFreeMemory() {
        return Runtime.getRuntime().freeMemory();
    }

    public float[] getRotations(double x, double y, double z) {
        if (mc.player != null && Double.isFinite(x) && Double.isFinite(y) && Double.isFinite(z)) {
            double deltaX = x - mc.player.getX();
            double deltaY = y - mc.player.getEyeY();
            double deltaZ = z - mc.player.getZ();
            double horizontalDistance = Math.hypot(deltaX, deltaZ);
            return new float[] {
                Mth.wrapDegrees((float) Math.toDegrees(Math.atan2(deltaZ, deltaX)) - 90.0F),
                Mth.wrapDegrees((float) (-Math.toDegrees(Math.atan2(deltaY, horizontalDistance))))
            };
        } else {
            return new float[] {0.0F, 0.0F};
        }
    }

    public long getMaxMemory() {
        return Runtime.getRuntime().maxMemory();
    }

    public void releaseMovementInput() {
        this.ensureActive();
        if (mc.player != null) {
            mc.player.input.keyPresses = Input.EMPTY;
            ClientInputAccessor clientInput = (ClientInputAccessor) mc.player.input;
            Vec2 vec2 = Vec2.ZERO;
            ClientInputAccessor inputAccessor = clientInput;
            inputAccessor.setMoveVector(vec2);
        }
    }

    public void runOnClient(Runnable callback) {
        if (callback != null) {
            long currentGeneration;
            synchronized (this) {
                this.ensureActive();
                if (this.pendingClientCallbacks >= 64) {
                    throw new RejectedExecutionException(
                            "A script cannot queue more than 64 client callbacks.");
                }

                this.pendingClientCallbacks++;
                currentGeneration = this.generation;
            }

            try {
                mc.execute(
                        () -> {
                            try {
                                if (this.isGenerationCurrent(currentGeneration)) {
                                    callback.run();
                                }
                            } catch (Throwable throwable) {
                                if (this.isGenerationCurrent(currentGeneration)) {
                                    this.owner.reportError("client.runOnClient", throwable);
                                }
                            } finally {
                                synchronized (this) {
                                    this.pendingClientCallbacks--;
                                }
                            }
                        });
            } catch (RuntimeException e) {
                synchronized (this) {
                    this.pendingClientCallbacks--;
                }

                throw e;
            }
        }
    }

    public ScriptEntityHit raycastEntity(double maxDistance, float yaw, float pitch) {
        if (mc.player != null
                && mc.level != null
                && Double.isFinite(maxDistance)
                && Float.isFinite(yaw)
                && Float.isFinite(pitch)) {
            double clampedDistance = Math.clamp(maxDistance, 0.0, 6.0);
            Vec3 vector3 = mc.player.getEyePosition();
            Vec3 vector32 =
                    vector3.add(
                            Vec3.directionFromRotation(Math.clamp(pitch, -90.0F, 90.0F), yaw)
                                    .scale(clampedDistance));
            BlockHitResult blockHitResult =
                    mc.level.clip(
                            new ClipContext(
                                    vector3, vector32, Block.OUTLINE, Fluid.NONE, mc.player));
            double maxHitDistanceSq =
                    blockHitResult.getType() == Type.MISS
                            ? clampedDistance * clampedDistance
                            : vector3.distanceToSqr(blockHitResult.getLocation());
            EntityHitResult entityHitResult =
                    ProjectileUtil.getEntityHitResult(
                            mc.player,
                            vector3,
                            vector32,
                            mc.player
                                    .getBoundingBox()
                                    .expandTowards(vector32.subtract(vector3))
                                    .inflate(1.0),
                            entity2 -> !entity2.isSpectator() && entity2.isPickable(),
                            maxHitDistanceSq);
            return entityHitResult == null
                    ? null
                    : new ScriptEntityHit(
                            new ScriptEntity(entityHitResult.getEntity(), this.owner),
                            new ScriptVector3(
                                    entityHitResult.getLocation().x,
                                    entityHitResult.getLocation().y,
                                    entityHitResult.getLocation().z),
                            vector3.distanceToSqr(entityHitResult.getLocation()));
        } else {
            return null;
        }
    }

    public ScriptEntityHit raycastEntity(double maxDistance) {
        return mc.player == null
                ? null
                : this.raycastEntity(maxDistance, mc.player.getYRot(), mc.player.getXRot());
    }

    public float getStrafeInput() {
        return mc.player != null ? mc.player.input.getMoveVector().x : 0.0F;
    }

    public ScriptBlockHit raycastBlock(double maxDistance) {
        return mc.player == null
                ? null
                : this.raycastBlock(maxDistance, mc.player.getYRot(), mc.player.getXRot());
    }

    public ScriptBlockHit raycastBlock(double maxDistance, float yaw, float pitch) {
        if (mc.player != null
                && mc.level != null
                && Double.isFinite(maxDistance)
                && Float.isFinite(yaw)
                && Float.isFinite(pitch)) {
            double clampedDistance = Math.clamp(maxDistance, 0.0, 6.0);
            Vec3 vector3 = mc.player.getEyePosition();
            BlockHitResult blockHitResult =
                    mc.level.clip(
                            new ClipContext(
                                    vector3,
                                    vector3.add(
                                            Vec3.directionFromRotation(
                                                            Math.clamp(pitch, -90.0F, 90.0F), yaw)
                                                    .scale(clampedDistance)),
                                    Block.OUTLINE,
                                    Fluid.NONE,
                                    mc.player));
            return blockHitResult.getType() != Type.BLOCK
                    ? null
                    : new ScriptBlockHit(
                            blockHitResult.getBlockPos().getX(),
                            blockHitResult.getBlockPos().getY(),
                            blockHitResult.getBlockPos().getZ(),
                            toBlockFace(blockHitResult.getDirection()),
                            blockHitResult.getLocation().x,
                            blockHitResult.getLocation().y,
                            blockHitResult.getLocation().z);
        } else {
            return null;
        }
    }

    public void setMovementInput(
            float forward, float strafe, boolean jump, boolean sneak, boolean sprint) {
        this.ensureActive();
        if (mc.player != null && Float.isFinite(forward) && Float.isFinite(strafe)) {
            float clampedForward = Math.clamp(forward, -1.0F, 1.0F);
            float clampedStrafe = Math.clamp(strafe, -1.0F, 1.0F);
            mc.player.input.keyPresses =
                    new Input(
                            clampedForward > 0.0F,
                            clampedForward < 0.0F,
                            clampedStrafe > 0.0F,
                            clampedStrafe < 0.0F,
                            jump,
                            sneak,
                            sprint);
            ClientInputAccessor clientInput = (ClientInputAccessor) mc.player.input;
            Vec2 vec2 = new Vec2(clampedStrafe, clampedForward);
            ClientInputAccessor inputAccessor = clientInput;
            inputAccessor.setMoveVector(vec2);
        }
    }

    public void releaseItemUse() {
        this.ensureActive();
        if (mc.player != null && mc.gameMode != null) {
            mc.gameMode.releaseUsingItem(mc.player);
        }
    }

    public long getAllocatedMemory() {
        return Runtime.getRuntime().totalMemory();
    }

    public double getBlocksPerSecond() {
        return mc.player != null ? MoveUtil.getBps() : 0.0;
    }

    public boolean canEditWorld() {
        return mc.player != null && !mc.player.isSpectator();
    }

    public boolean isMovingDiagonally() {
        return mc.player != null && MoveUtil.isMovingDiagonally();
    }

    public void closeScreen() {
        this.ensureActive();
        if (!(mc.gui.screen() instanceof ScriptScreens.Managed)) {
            mc.gui.setScreen(null);
        }
    }

    public ScriptVector3 getMotionVector() {
        return mc.player == null
                ? new ScriptVector3(0.0, 0.0, 0.0)
                : new ScriptVector3(
                        mc.player.getDeltaMovement().x,
                        mc.player.getDeltaMovement().y,
                        mc.player.getDeltaMovement().z);
    }
}
