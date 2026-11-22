package astralis.scripting;

import astralis.event.EventStage;
import astralis.event.EventTarget;
import astralis.event.impl.AttackEvent;
import astralis.event.impl.BlockDestroyedEvent;
import astralis.event.impl.ChatInputEvent;
import astralis.event.impl.ChatReceiveEvent;
import astralis.event.impl.ContainerScreenRenderEvent;
import astralis.event.impl.ContinueAttackEvent;
import astralis.event.impl.GameJoinEvent;
import astralis.event.impl.HandleKeybindsEvent;
import astralis.event.impl.InGameKeyPressEvent;
import astralis.event.impl.InputPollEvent;
import astralis.event.impl.ItemUseSlowdownEvent;
import astralis.event.impl.JumpYawEvent;
import astralis.event.impl.KeyInputEvent;
import astralis.event.impl.MotionEvent;
import astralis.event.impl.MouseButtonInputEvent;
import astralis.event.impl.MouseCursorMoveEvent;
import astralis.event.impl.MouseScrollEvent;
import astralis.event.impl.MouseUpdateEvent;
import astralis.event.impl.MoveInputEvent;
import astralis.event.impl.PacketEvent;
import astralis.event.impl.PlaySoundEvent;
import astralis.event.impl.PlayerMoveEvent;
import astralis.event.impl.PlayerUpdateEvent;
import astralis.event.impl.PostMotionEvent;
import astralis.event.impl.Render2DEvent;
import astralis.event.impl.Render3DEvent;
import astralis.event.impl.RotationFrameEvent;
import astralis.event.impl.ScreenOpenEvent;
import astralis.event.impl.StartAttackEvent;
import astralis.event.impl.StartUseItemEvent;
import astralis.event.impl.TickEvent;
import astralis.event.impl.TravelEvent;
import astralis.event.impl.VelocityEvent;
import astralis.event.impl.WorldChangeEvent;
import astralis.util.PacketUtil;
import astralis.util.input.KeyNames;
import astralis.util.render.ScreenProjector;

import com.astralis.scripting.InstanceAccess;
import com.astralis.scripting.ScriptInternals;
import com.astralis.scripting.api.entity.ScriptEntity;
import com.astralis.scripting.api.event.ScriptEvents.Attack;
import com.astralis.scripting.api.event.ScriptEvents.BlockBreak;
import com.astralis.scripting.api.event.ScriptEvents.Cancellable;
import com.astralis.scripting.api.event.ScriptEvents.Chat;
import com.astralis.scripting.api.event.ScriptEvents.Input;
import com.astralis.scripting.api.event.ScriptEvents.Key;
import com.astralis.scripting.api.event.ScriptEvents.Motion;
import com.astralis.scripting.api.event.ScriptEvents.MouseButton;
import com.astralis.scripting.api.event.ScriptEvents.MouseCursor;
import com.astralis.scripting.api.event.ScriptEvents.MouseMove;
import com.astralis.scripting.api.event.ScriptEvents.Move;
import com.astralis.scripting.api.event.ScriptEvents.Phase;
import com.astralis.scripting.api.event.ScriptEvents.Rotation;
import com.astralis.scripting.api.event.ScriptEvents.RotationModifier;
import com.astralis.scripting.api.event.ScriptEvents.Screen;
import com.astralis.scripting.api.event.ScriptEvents.ScreenInput;
import com.astralis.scripting.api.event.ScriptEvents.Scroll;
import com.astralis.scripting.api.event.ScriptEvents.SlowDown;
import com.astralis.scripting.api.event.ScriptEvents.Sound;
import com.astralis.scripting.api.event.ScriptEvents.Tick;
import com.astralis.scripting.api.event.ScriptEvents.Velocity;
import com.astralis.scripting.api.event.ScriptPacketHandle;
import com.astralis.scripting.api.math.ScriptVector3;
import com.astralis.scripting.api.render.RenderContext;
import com.astralis.scripting.api.render.WorldRenderContext;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ServerboundAttackPacket;
import net.minecraft.network.protocol.game.ServerboundChatCommandPacket;
import net.minecraft.network.protocol.game.ServerboundChatPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import org.joml.Matrix4f;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public class ScriptEventListener implements InstanceAccess {
    private static final int EVENT_UPDATE = 6777;
    private static final int EVENT_TICK = 11117;
    private static final int EVENT_ROTATION_REQUEST = 13761;
    private static final int EVENT_MOTION = 20007;
    private static final int EVENT_POST_MOTION = 22451;
    private static final int EVENT_MOVE = 27673;
    private static final int EVENT_RENDER = 29071;
    private static final int EVENT_WORLD_RENDER = 33493;
    private static final int EVENT_PACKET_SEND = 37707;
    private static final int EVENT_PACKET_RECEIVE = 42721;
    private static final int EVENT_CHAT_RECEIVE = 46925;
    private static final int EVENT_KEY_PRESS = 51283;
    private static final int EVENT_KEY = 55663;
    private static final int EVENT_CHAT_SEND = 60085;
    private static final int EVENT_ATTACK = 64449;
    private static final int EVENT_ATTACK_ENTITY = 68919;
    private static final int EVENT_INPUT = 73289;
    private static final int EVENT_MOUSE_MOVE = 77659;
    private static final int EVENT_MOUSE_BUTTON = 77933;
    private static final int EVENT_SCREEN_OPEN = 82303;
    private static final int EVENT_SLOW_DOWN = 86673;
    private static final int EVENT_JUMP_ROTATION = 91043;
    private static final int EVENT_ROTATION = 95413;
    private static final int EVENT_SCREEN_RENDER = 99783;
    private static final int EVENT_PLAYER_JOIN = 104153;
    private static final int EVENT_WORLD_CHANGE = 108523;
    private static final int EVENT_SOUND = 112893;
    private static final int EVENT_VELOCITY = 117263;
    private static final int EVENT_MOUSE_SCROLL = 121633;
    private static final int EVENT_ITEM_USE = 126003;
    private static final int EVENT_LIVING_UPDATE = 130373;
    private static final int EVENT_BLOCK_BREAKING = 134743;
    private static final int EVENT_BLOCK_BREAK = 139113;
    private static final int EVENT_CLICK = 143483;
    private static final int EVENT_SCREEN_INPUT = 147853;
    private final ScriptManager scriptManager;

    public ScriptEventListener(ScriptManager scriptManager) {
        this.scriptManager = scriptManager;
    }

    @EventTarget
    public void onPacket(PacketEvent packetEvent) {

        String action = "";
        String message = "";
        ScriptVector3 position = null;

        int direction = 255;
        int heldSlot = -1;
        float yaw = 0.0F;
        float pitch = 0.0F;
        boolean moving = false;
        if (packetEvent.getPacket()
                instanceof ServerboundMovePlayerPacket serverboundMovePlayerPacket) {
            moving = serverboundMovePlayerPacket.hasPosition();
            yaw =
                    serverboundMovePlayerPacket.getYRot(
                            mc.player != null ? mc.player.getYRot() : 0.0F);
            pitch =
                    serverboundMovePlayerPacket.getXRot(
                            mc.player != null ? mc.player.getXRot() : 0.0F);
            if (serverboundMovePlayerPacket.hasPosition()) {
                position =
                        new ScriptVector3(
                                serverboundMovePlayerPacket.getX(
                                        mc.player != null ? mc.player.getX() : 0.0),
                                serverboundMovePlayerPacket.getY(
                                        mc.player != null ? mc.player.getY() : 0.0),
                                serverboundMovePlayerPacket.getZ(
                                        mc.player != null ? mc.player.getZ() : 0.0));
            }
        } else if (packetEvent.getPacket()
                instanceof ServerboundPlayerActionPacket serverboundPlayerActionPacket) {
            action = serverboundPlayerActionPacket.getAction().name();
            position =
                    new ScriptVector3(
                            serverboundPlayerActionPacket.getPos().getX(),
                            serverboundPlayerActionPacket.getPos().getY(),
                            serverboundPlayerActionPacket.getPos().getZ());
            direction = serverboundPlayerActionPacket.getDirection().ordinal();
        } else if (packetEvent.getPacket()
                instanceof ServerboundPlayerCommandPacket serverboundPlayerCommandPacket) {
            action = serverboundPlayerCommandPacket.getAction().name();
        } else if (packetEvent.getPacket()
                instanceof ServerboundSetCarriedItemPacket serverboundSetCarriedItemPacket) {

            action = "SET_CARRIED_ITEM";
            heldSlot = serverboundSetCarriedItemPacket.getSlot();
        } else if (packetEvent.getPacket()
                instanceof ServerboundUseItemOnPacket serverboundUseItemOnPacket) {

            action = "USE_ITEM_ON";
            position =
                    new ScriptVector3(
                            serverboundUseItemOnPacket.getHitResult().getBlockPos().getX(),
                            serverboundUseItemOnPacket.getHitResult().getBlockPos().getY(),
                            serverboundUseItemOnPacket.getHitResult().getBlockPos().getZ());
            direction = serverboundUseItemOnPacket.getHitResult().getDirection().ordinal();
        } else if (packetEvent.getPacket() instanceof ServerboundChatPacket serverboundChatPacket) {
            message = serverboundChatPacket.message();
        } else if (packetEvent.getPacket()
                instanceof ServerboundChatCommandPacket(String command)) {
            message = "/" + command;
        } else if (packetEvent.getPacket() instanceof ServerboundAttackPacket) {

            action = "ATTACK";
        } else if (packetEvent.getPacket()
                instanceof ClientboundBlockUpdatePacket clientboundBlockUpdatePacket) {

            action = "BLOCK_UPDATE";
            position =
                    new ScriptVector3(
                            clientboundBlockUpdatePacket.getPos().getX(),
                            clientboundBlockUpdatePacket.getPos().getY(),
                            clientboundBlockUpdatePacket.getPos().getZ());
        }

        String packetType =
                packetEvent.getPacket() != null
                        ? packetEvent.getPacket().getClass().getSimpleName()
                        : "";
        String eventAction = action;
        String eventMessage = message;
        ScriptVector3 eventPosition = position;
        int eventDirection = direction;
        int eventHeldSlot = heldSlot;
        float eventYaw = yaw;
        float eventPitch = pitch;
        boolean eventMoving = moving;
        boolean[] cancelled = new boolean[] {packetEvent.isCancelled()};
        Packet packet = packetEvent.getPacket();
        ClientPacketListener clientPacketListener = mc.getConnection();
        AtomicBoolean receiveRequested = new AtomicBoolean();
        this.scriptManager.forEachActiveScript(
                script -> {
                    ScriptPacketHandle packetHandle =
                            packetEvent.getStage() == EventStage.RECEIVE && packet != null
                                    ? new ScriptPacketHandle(
                                            script::isRuntimeActive,
                                            () -> {
                                                if (receiveRequested.compareAndSet(false, true)) {
                                                    Runnable receiveTask =
                                                            () -> {
                                                                if (mc.player != null
                                                                        && mc.getConnection()
                                                                                == clientPacketListener) {
                                                                    PacketUtil.receiveNoEvent(
                                                                            packet);
                                                                }
                                                            };
                                                    if (mc.isSameThread()) {
                                                        receiveTask.run();
                                                    } else {
                                                        mc.execute(receiveTask);
                                                    }
                                                }
                                            })
                                    : null;
                    com.astralis.scripting.api.event.ScriptEvents.Packet scriptPacket =
                            new com.astralis.scripting.api.event.ScriptEvents.Packet(
                                    packetType,
                                    eventAction,
                                    eventMessage,
                                    eventPosition,
                                    eventDirection,
                                    eventHeldSlot,
                                    eventYaw,
                                    eventPitch,
                                    eventMoving,
                                    packetHandle,
                                    cancelled[0]);
                    if (packetEvent.getStage() == EventStage.SEND) {

                        ScriptInternals.dispatchEvent(script, 37707, scriptPacket);
                    } else {

                        ScriptInternals.dispatchEvent(script, 42721, scriptPacket);
                    }

                    cancelled[0] |= scriptPacket.isCancelled();
                });
        packetEvent.setCancelled(cancelled[0]);
    }

    @EventTarget
    public void onMouseButton(MouseButtonInputEvent mouseButtonEvent) {
        double mouseX =
                mouseButtonEvent.getX()
                        * mc.getWindow().getGuiScaledWidth()
                        / mc.getWindow().getScreenWidth();
        double mouseY =
                mouseButtonEvent.getY()
                        * mc.getWindow().getGuiScaledHeight()
                        / mc.getWindow().getScreenHeight();
        MouseButton scriptMouseButton =
                new MouseButton(
                        mouseButtonEvent.getButton(),
                        mouseButtonEvent.isPressed(),
                        mouseX,
                        mouseY,
                        mouseButtonEvent.isCancelled());
        this.scriptManager.forEachActiveScript(
                script -> {
                    ScriptInternals.dispatchEvent(script, 77933, scriptMouseButton);
                });
        mouseButtonEvent.setCancelled(scriptMouseButton.isCancelled());
    }

    @EventTarget
    public void onUpdate(PlayerUpdateEvent playerUpdateEvent) {
        Cancellable cancellable = new Cancellable(playerUpdateEvent.isCancelled());
        this.scriptManager.forEachActiveScript(
                script -> {
                    ScriptInternals.dispatchEvent(script, 6777, cancellable);
                });
        playerUpdateEvent.setCancelled(cancellable.isCancelled());
    }

    @EventTarget
    public void onLivingUpdate(TravelEvent travelEvent) {
        Cancellable cancellable = new Cancellable(travelEvent.isCancelled());
        this.scriptManager.forEachActiveScript(
                script -> {
                    ScriptInternals.dispatchEvent(script, 130373, cancellable);
                });
        travelEvent.setCancelled(cancellable.isCancelled());
    }

    @EventTarget
    public void onScriptKey(KeyInputEvent keyInputEvent) {
        Key scriptKey =
                new Key(
                        KeyNames.getKeyName(keyInputEvent.getKey()),
                        keyInputEvent.getKey(),
                        keyInputEvent.isPressed(),
                        keyInputEvent.isScreenOpen(),
                        keyInputEvent.isCancelled());
        this.scriptManager.forEachActiveScript(
                script -> {
                    ScriptInternals.dispatchEvent(script, 55663, scriptKey);
                });
        keyInputEvent.setCancelled(scriptKey.isCancelled());
    }

    @EventTarget
    public void onVelocity(VelocityEvent velocityEvent) {
        double initialX = velocityEvent.getX();
        double initialY = velocityEvent.getY();
        double initialZ = velocityEvent.getZ();
        Velocity scriptVelocity =
                new Velocity(
                        velocityEvent.getX(),
                        velocityEvent.getY(),
                        velocityEvent.getZ(),
                        velocityEvent.isExplosion(),
                        velocityEvent.isCancelled());
        this.scriptManager.forEachActiveScript(
                script -> {
                    ScriptInternals.dispatchEvent(script, 117263, scriptVelocity);
                });
        velocityEvent.setX(
                Double.isFinite(scriptVelocity.getX()) ? scriptVelocity.getX() : initialX);
        velocityEvent.setY(
                Double.isFinite(scriptVelocity.getY()) ? scriptVelocity.getY() : initialY);
        velocityEvent.setZ(
                Double.isFinite(scriptVelocity.getZ()) ? scriptVelocity.getZ() : initialZ);
        velocityEvent.setCancelled(scriptVelocity.isCancelled());
    }

    @EventTarget
    public void onBlockBreaking(ContinueAttackEvent continueAttackEvent) {
        Cancellable cancellable = new Cancellable(continueAttackEvent.isCancelled());
        this.scriptManager.forEachActiveScript(
                script -> {
                    ScriptInternals.dispatchEvent(script, 134743, cancellable);
                });
        continueAttackEvent.setCancelled(cancellable.isCancelled());
    }

    @EventTarget
    public void onSound(PlaySoundEvent playSoundEvent) {
        float volume;
        float pitch;
        try {
            volume = playSoundEvent.getSound().getVolume();
            pitch = playSoundEvent.getSound().getPitch();
        } catch (NullPointerException ignored) {
            return;
        }

        Cancellable cancellable = new Cancellable(playSoundEvent.isCancelled());
        this.scriptManager.forEachActiveScript(
                script -> {
                    ScriptInternals.dispatchEvent(
                            script,
                            112893,
                            new Sound(
                                    playSoundEvent.getSound().getIdentifier().toString(),
                                    volume,
                                    pitch,
                                    cancellable));
                });
        playSoundEvent.setCancelled(cancellable.isCancelled());
    }

    @EventTarget
    public void onInput(MoveInputEvent moveInputEvent) {
        Input scriptInput =
                new Input(
                        moveInputEvent.isForward(),
                        moveInputEvent.isBackward(),
                        moveInputEvent.isLeft(),
                        moveInputEvent.isRight(),
                        moveInputEvent.isJump(),
                        moveInputEvent.isSneak(),
                        moveInputEvent.isSprint());
        this.scriptManager.forEachActiveScript(
                script -> {
                    ScriptInternals.dispatchEvent(script, 73289, scriptInput);
                });
        moveInputEvent.setForwardImpulse(scriptInput.getForward());
        moveInputEvent.setStrafeImpulse(scriptInput.getStrafe());
        moveInputEvent.setJump(scriptInput.isJump());
        moveInputEvent.setSneak(scriptInput.isSneak());
        moveInputEvent.setSprint(scriptInput.isSprint());
    }

    @EventTarget
    public void onKey(InGameKeyPressEvent inGameKeyPressEvent) {
        this.scriptManager.forEachActiveScript(
                script -> {
                    ScriptInternals.dispatchEvent(script, 51283, inGameKeyPressEvent.getKey());
                });
    }

    @EventTarget
    public void onMove(PlayerMoveEvent playerMoveEvent) {
        double initialX = playerMoveEvent.getX();
        double initialY = playerMoveEvent.getY();
        double initialZ = playerMoveEvent.getZ();
        Move scriptMove =
                new Move(
                        playerMoveEvent.getX(),
                        playerMoveEvent.getY(),
                        playerMoveEvent.getZ(),
                        playerMoveEvent.isCancelled());
        this.scriptManager.forEachActiveScript(
                script -> {
                    ScriptInternals.dispatchEvent(script, 27673, scriptMove);
                });
        playerMoveEvent.setX(Double.isFinite(scriptMove.getX()) ? scriptMove.getX() : initialX);
        playerMoveEvent.setY(Double.isFinite(scriptMove.getY()) ? scriptMove.getY() : initialY);
        playerMoveEvent.setZ(Double.isFinite(scriptMove.getZ()) ? scriptMove.getZ() : initialZ);
        playerMoveEvent.setCancelled(scriptMove.isCancelled());
    }

    @EventTarget
    public void onChatInput(ChatInputEvent chatInputEvent) {
        Chat scriptChat =
                new Chat(chatInputEvent.getMessage(), false, chatInputEvent.isCancelled());
        this.scriptManager.forEachActiveScript(
                script -> {
                    ScriptInternals.dispatchEvent(script, 60085, scriptChat);
                });
        if (scriptChat.getMessage() == null) {
            scriptChat.cancel();
        } else {
            chatInputEvent.setMessage(scriptChat.getMessage());
        }

        chatInputEvent.setCancelled(scriptChat.isCancelled());
    }

    @EventTarget
    public void onWorldChange(WorldChangeEvent worldChangeEvent) {
        this.scriptManager.forEachActiveScript(
                script -> {
                    ScriptInternals.dispatchEvent(script, 108523);
                });
    }

    @EventTarget
    public void onTick(TickEvent tickEvent) {
        Phase phase = tickEvent.getStage() == EventStage.PRE ? Phase.PRE : Phase.POST;
        if (phase == Phase.PRE) {
            this.scriptManager.flushPendingMessages();
        }

        this.scriptManager.forEachActiveScript(
                script -> {
                    if (phase == Phase.PRE) {
                        ScriptInternals.tickTasks(script);
                    }

                    ScriptInternals.dispatchEvent(script, 11117, new Tick(phase));
                });
    }

    @EventTarget
    public void onMotion(MotionEvent motionEvent) {
        double initialX = motionEvent.getX();
        double initialY = motionEvent.getY();
        double initialZ = motionEvent.getZ();
        float initialYaw = motionEvent.getYaw();
        float initialPitch = motionEvent.getPitch();
        Motion scriptMotion =
                new Motion(
                        motionEvent.getX(),
                        motionEvent.getY(),
                        motionEvent.getZ(),
                        motionEvent.getYaw(),
                        motionEvent.getPitch(),
                        motionEvent.isOnGround(),
                        motionEvent.isCancelled());
        this.scriptManager.forEachActiveScript(
                script -> {
                    ScriptInternals.dispatchEvent(script, 13761, scriptMotion);

                    ScriptInternals.dispatchEvent(script, 20007, scriptMotion);
                });
        motionEvent.setX(Double.isFinite(scriptMotion.getX()) ? scriptMotion.getX() : initialX);
        motionEvent.setY(Double.isFinite(scriptMotion.getY()) ? scriptMotion.getY() : initialY);
        motionEvent.setZ(Double.isFinite(scriptMotion.getZ()) ? scriptMotion.getZ() : initialZ);
        motionEvent.setYaw(
                Float.isFinite(scriptMotion.getYaw()) ? scriptMotion.getYaw() : initialYaw);
        motionEvent.setPitch(
                Float.isFinite(scriptMotion.getPitch())
                        ? Math.clamp(scriptMotion.getPitch(), -90.0F, 90.0F)
                        : initialPitch);
        motionEvent.setOnGround(scriptMotion.isOnGround());
        motionEvent.setCancelled(scriptMotion.isCancelled());
    }

    @EventTarget
    public void onHandleKeybinds(HandleKeybindsEvent handleKeybindsEvent) {
        this.scriptManager.forEachActiveScript(
                script -> {
                    ScriptInternals.dispatchEvent(script, 143483);
                });
    }

    @EventTarget
    public void onMouseCursor(MouseCursorMoveEvent mouseCursorMoveEvent) {
        this.scriptManager.forEachActiveScript(
                script -> {
                    ScriptInternals.dispatchEvent(
                            script,
                            79535,
                            new MouseCursor(
                                    mouseCursorMoveEvent.getX(), mouseCursorMoveEvent.getY()));
                });
    }

    @EventTarget
    public void onRotation(RotationFrameEvent rotationFrameEvent) {
        float[] adjustedRotation =
                new float[] {
                    Float.isFinite(rotationFrameEvent.getTargetYaw())
                            ? rotationFrameEvent.getTargetYaw()
                            : rotationFrameEvent.getYaw(),
                    Float.isFinite(rotationFrameEvent.getTargetPitch())
                            ? Math.clamp(rotationFrameEvent.getTargetPitch(), -90.0F, 90.0F)
                            : rotationFrameEvent.getPitch()
                };
        this.scriptManager.forEachActiveScript(
                script -> {
                    RotationModifier rotationModifier =
                            new RotationModifier(
                                    rotationFrameEvent.getPartialTick(),
                                    rotationFrameEvent.getYaw(),
                                    rotationFrameEvent.getPitch(),
                                    adjustedRotation[0],
                                    adjustedRotation[1],
                                    rotationFrameEvent.getTag(),
                                    rotationFrameEvent.getTargetEntity() != null
                                            ? new ScriptEntity(
                                                    rotationFrameEvent.getTargetEntity(), script)
                                            : null);
                    ScriptInternals.applyRotationStrategies(script, rotationModifier);

                    ScriptInternals.dispatchEvent(script, 95413, rotationModifier);
                    if (Float.isFinite(rotationModifier.getYaw())) {
                        adjustedRotation[0] = rotationModifier.getYaw();
                    }

                    if (Float.isFinite(rotationModifier.getPitch())) {
                        adjustedRotation[1] =
                                Math.clamp(rotationModifier.getPitch(), -90.0F, 90.0F);
                    }
                });
        rotationFrameEvent.setAdjustedRotation(adjustedRotation[0], adjustedRotation[1]);
    }

    @EventTarget
    public void onPostMotion(PostMotionEvent postMotionEvent) {
        this.scriptManager.forEachActiveScript(
                script -> {
                    ScriptInternals.dispatchEvent(script, 22451);
                });
    }

    @EventTarget
    public void onScreenOpen(ScreenOpenEvent screenOpenEvent) {
        Screen scriptScreen =
                new Screen(
                        screenOpenEvent.getScreen() == null
                                ? ""
                                : screenOpenEvent.getScreen().getClass().getSimpleName(),
                        screenOpenEvent.isKeepingKeyStates());
        this.scriptManager.forEachActiveScript(
                script -> {
                    ScriptInternals.dispatchEvent(script, 82303, scriptScreen);
                });
        if (scriptScreen.isPreserveInput()) {
            screenOpenEvent.keepKeyStates();
        }
    }

    @EventTarget
    public void onScreenRender(ContainerScreenRenderEvent containerScreenRenderEvent) {
        this.scriptManager.forEachActiveScript(
                script -> {
                    try (RenderContext renderContext =
                            new RenderContext(
                                    containerScreenRenderEvent.getContext(), null, 0.0, 0.0, 0.0)) {

                        ScriptInternals.dispatchEvent(script, 99783, renderContext);
                    }
                });
    }

    @EventTarget
    public void onWorldRender(Render3DEvent render3DEvent) {
        this.scriptManager.forEachActiveScript(
                script -> {
                    try (WorldRenderContext worldRenderContext =
                            new WorldRenderContext(render3DEvent.getPoseStack())) {

                        ScriptInternals.dispatchEvent(script, 33493, worldRenderContext);
                    }
                });
    }

    @EventTarget
    public void onMouseMove(MouseUpdateEvent mouseUpdateEvent) {
        double initialDeltaX = mouseUpdateEvent.getDeltaX();
        double initialDeltaY = mouseUpdateEvent.getDeltaY();
        double initialSensitivityFactor = mouseUpdateEvent.getSensitivityFactor();
        MouseMove scriptMouseMove =
                new MouseMove(
                        mouseUpdateEvent.getDeltaX(),
                        mouseUpdateEvent.getDeltaY(),
                        mouseUpdateEvent.getSensitivityFactor(),
                        mouseUpdateEvent.getOriginalDeltaX(),
                        mouseUpdateEvent.getOriginalDeltaY(),
                        mouseUpdateEvent.isSmoothCameraDisabled(),
                        mouseUpdateEvent.isCancelled());
        this.scriptManager.forEachActiveScript(
                script -> {
                    ScriptInternals.dispatchEvent(script, 77659, scriptMouseMove);
                });
        mouseUpdateEvent.setDeltaX(
                Double.isFinite(scriptMouseMove.getDeltaX())
                        ? scriptMouseMove.getDeltaX()
                        : initialDeltaX);
        mouseUpdateEvent.setDeltaY(
                Double.isFinite(scriptMouseMove.getDeltaY())
                        ? scriptMouseMove.getDeltaY()
                        : initialDeltaY);
        mouseUpdateEvent.setSensitivityFactor(
                Double.isFinite(scriptMouseMove.getSensitivityMultiplier())
                                && scriptMouseMove.getSensitivityMultiplier() >= 0.0
                        ? scriptMouseMove.getSensitivityMultiplier()
                        : initialSensitivityFactor);
        mouseUpdateEvent.setSmoothCameraDisabled(scriptMouseMove.isHandled());
        mouseUpdateEvent.setCancelled(scriptMouseMove.isCancelled());
    }

    @EventTarget
    public void onScreenInput(InputPollEvent inputPollEvent) {
        ScreenInput scriptScreenInput = new ScreenInput(inputPollEvent.isHandled());
        this.scriptManager.forEachActiveScript(
                script -> {
                    ScriptInternals.dispatchEvent(script, 147853, scriptScreenInput);
                });
        inputPollEvent.setHandled(scriptScreenInput.isHandled());
    }

    @EventTarget
    public void onPlayerJoin(GameJoinEvent gameJoinEvent) {
        this.scriptManager.forEachActiveScript(
                script -> {
                    ScriptInternals.dispatchEvent(
                            script, 104153, gameJoinEvent.getJoinTimeMillis());
                });
    }

    @EventTarget
    public void onScroll(MouseScrollEvent mouseScrollEvent) {
        double initialHorizontal = mouseScrollEvent.getHorizontal();
        double initialVertical = mouseScrollEvent.getVertical();
        Scroll scriptScroll =
                new Scroll(
                        mouseScrollEvent.getHorizontal(),
                        mouseScrollEvent.getVertical(),
                        mouseScrollEvent.isCancelled());
        this.scriptManager.forEachActiveScript(
                script -> {
                    ScriptInternals.dispatchEvent(script, 121633, scriptScroll);
                });
        mouseScrollEvent.setHorizontal(
                Double.isFinite(scriptScroll.getX()) ? scriptScroll.getX() : initialHorizontal);
        mouseScrollEvent.setVertical(
                Double.isFinite(scriptScroll.getY()) ? scriptScroll.getY() : initialVertical);
        mouseScrollEvent.setCancelled(scriptScroll.isCancelled());
    }

    @EventTarget
    public void onItemUse(StartUseItemEvent startUseItemEvent) {
        Cancellable cancellable = new Cancellable(startUseItemEvent.isCancelled());
        this.scriptManager.forEachActiveScript(
                script -> {
                    ScriptInternals.dispatchEvent(script, 126003, cancellable);
                });
        startUseItemEvent.setCancelled(cancellable.isCancelled());
    }

    @EventTarget
    public void onChatReceive(ChatReceiveEvent chatReceiveEvent) {
        String originalMessage = chatReceiveEvent.getMessage().getString();
        Chat scriptChat =
                new Chat(
                        originalMessage,
                        chatReceiveEvent.isOverlay(),
                        chatReceiveEvent.isCancelled());
        this.scriptManager.forEachActiveScript(
                script -> {
                    ScriptInternals.dispatchEvent(script, 46925, scriptChat);
                });
        if (scriptChat.getMessage() == null) {
            scriptChat.cancel();
        } else if (!Objects.equals(originalMessage, scriptChat.getMessage())) {
            chatReceiveEvent.setMessage(Component.literal(scriptChat.getMessage()));
        }

        chatReceiveEvent.setOverlay(scriptChat.isOverlay());
        chatReceiveEvent.setCancelled(scriptChat.isCancelled());
    }

    @EventTarget
    public void onAttackEntity(AttackEvent attackEvent) {
        Cancellable cancellable = new Cancellable(attackEvent.isCancelled());
        this.scriptManager.forEachActiveScript(
                script -> {
                    ScriptInternals.dispatchEvent(
                            script,
                            68919,
                            new Attack(
                                    new ScriptEntity(attackEvent.getTarget(), script),
                                    attackEvent.getStage() == EventStage.PRE
                                            ? Phase.PRE
                                            : Phase.POST,
                                    cancellable));
                });
        if (attackEvent.getStage() == EventStage.PRE) {
            attackEvent.setCancelled(cancellable.isCancelled());
        }
    }

    @EventTarget
    public void onAttack(StartAttackEvent startAttackEvent) {
        Cancellable cancellable = new Cancellable(startAttackEvent.isCancelled());
        this.scriptManager.forEachActiveScript(
                script -> {
                    ScriptInternals.dispatchEvent(script, 64449, cancellable);
                });
        startAttackEvent.setCancelled(cancellable.isCancelled());
    }

    @EventTarget
    public void onSapphireRender(Render2DEvent render2DEvent) {
        Matrix4f matrix4f = null;
        double cameraX = 0.0;
        double cameraY = 0.0;
        double cameraZ = 0.0;
        if (ScreenProjector.getModelViewMatrix() != null
                && ScreenProjector.getProjectionMatrix() != null) {
            Vec3 vector3 = mc.gameRenderer.mainCamera().position();
            cameraX = vector3.x;
            cameraY = vector3.y;
            cameraZ = vector3.z;
            matrix4f =
                    new Matrix4f(ScreenProjector.getProjectionMatrix())
                            .mul(ScreenProjector.getModelViewMatrix());
        }

        Matrix4f matrix4f2 = matrix4f;
        double contextCameraX = cameraX;
        double contextCameraY = cameraY;
        double contextCameraZ = cameraZ;
        this.scriptManager.forEachActiveScript(
                script -> {
                    try (RenderContext renderContext =
                            new RenderContext(
                                    render2DEvent.getContext(),
                                    matrix4f2,
                                    contextCameraX,
                                    contextCameraY,
                                    contextCameraZ)) {

                        ScriptInternals.dispatchEvent(script, 29071, renderContext);
                    }
                });
    }

    @EventTarget
    public void onBlockDestroyed(BlockDestroyedEvent blockDestroyedEvent) {
        this.scriptManager.forEachActiveScript(
                script -> {
                    ScriptInternals.dispatchEvent(
                            script,
                            139113,
                            new BlockBreak(
                                    new ScriptVector3(
                                            blockDestroyedEvent.getPosition().getX(),
                                            blockDestroyedEvent.getPosition().getY(),
                                            blockDestroyedEvent.getPosition().getZ()),
                                    BuiltInRegistries.BLOCK
                                            .getKey(blockDestroyedEvent.getState().getBlock())
                                            .toString()));
                });
    }

    @EventTarget
    public void onSlowDown(ItemUseSlowdownEvent itemUseSlowdownEvent) {
        float initialMultiplier = itemUseSlowdownEvent.getMultiplier();
        SlowDown scriptSlowDown =
                new SlowDown(
                        itemUseSlowdownEvent.getMultiplier(), itemUseSlowdownEvent.isCancelled());
        this.scriptManager.forEachActiveScript(
                script -> {
                    ScriptInternals.dispatchEvent(script, 86673, scriptSlowDown);
                });
        itemUseSlowdownEvent.setMultiplier(
                Float.isFinite(scriptSlowDown.getMultiplier())
                        ? scriptSlowDown.getMultiplier()
                        : initialMultiplier);
        itemUseSlowdownEvent.setCancelled(scriptSlowDown.isCancelled());
    }

    @EventTarget
    public void onJumpRotation(JumpYawEvent jumpYawEvent) {
        float initialYaw = jumpYawEvent.getYaw();
        Rotation scriptRotation = new Rotation(jumpYawEvent.getYaw());
        this.scriptManager.forEachActiveScript(
                script -> {
                    ScriptInternals.dispatchEvent(script, 91043, scriptRotation);
                });
        jumpYawEvent.setYaw(
                Float.isFinite(scriptRotation.getYaw())
                        ? Mth.wrapDegrees(scriptRotation.getYaw())
                        : initialYaw);
    }
}
