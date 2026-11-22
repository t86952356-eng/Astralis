package com.astralis.scripting.api.event;

import com.astralis.scripting.api.entity.ScriptEntity;
import com.astralis.scripting.api.math.ScriptVector3;

import lombok.Generated;

public final class ScriptEvents {
    private ScriptEvents() {}

    public record Attack(
            ScriptEntity target, ScriptEvents.Phase phase, ScriptEvents.Cancellable cancellation) {
        public void cancel() {
            this.cancellation.cancel();
        }

        public boolean isCancelled() {
            return this.cancellation.isCancelled();
        }
    }

    public record BlockBreak(ScriptVector3 position, String blockId) {}

    public static class Cancellable {
        private boolean cancelled;

        public Cancellable() {}

        public Cancellable(boolean cancelled) {
            this.cancelled = cancelled;
        }

        public void cancel() {
            this.cancelled = true;
        }

        public boolean isCancelled() {
            return this.cancelled;
        }

        public void setCancelled(boolean cancelled) {
            this.cancelled |= cancelled;
        }
    }

    public static class Chat extends ScriptEvents.Cancellable {
        private String message;
        private boolean overlay;

        public Chat(String message, boolean overlay, boolean cancelled) {
            super(cancelled);
            this.message = message;
            this.overlay = overlay;
        }

        public String getMessage() {
            return this.message;
        }

        public void setOverlay(boolean overlay) {
            this.overlay = overlay;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public boolean isOverlay() {
            return this.overlay;
        }
    }

    public static class Input {
        private boolean forward;
        private boolean backward;
        private boolean left;
        private boolean right;
        private boolean jump;
        private boolean sneak;
        private boolean sprint;
        private float forwardInput;
        private float strafeInput;

        public Input(
                boolean forward,
                boolean backward,
                boolean left,
                boolean right,
                boolean jump,
                boolean sneak,
                boolean sprint) {
            this.forward = forward;
            this.backward = backward;
            this.left = left;
            this.right = right;
            this.jump = jump;
            this.sneak = sneak;
            this.sprint = sprint;
            this.forwardInput = forward == backward ? 0.0F : (forward ? 1.0F : -1.0F);
            this.strafeInput = left == right ? 0.0F : (left ? 1.0F : -1.0F);
        }

        public void setBackward(boolean backward) {
            this.backward = backward;
            this.forwardInput = this.forward == backward ? 0.0F : (this.forward ? 1.0F : -1.0F);
        }

        public float getForwardInput() {
            return this.forwardInput;
        }

        public boolean isBackward() {
            return this.backward;
        }

        public void setRight(boolean right) {
            this.right = right;
            this.strafeInput = this.left == right ? 0.0F : (this.left ? 1.0F : -1.0F);
        }

        public void setSneak(boolean sneak) {
            this.sneak = sneak;
        }

        public void setSprint(boolean sprint) {
            this.sprint = sprint;
        }

        public void setJump(boolean jump) {
            this.jump = jump;
        }

        public void setLeft(boolean left) {
            this.left = left;
            this.strafeInput = left == this.right ? 0.0F : (left ? 1.0F : -1.0F);
        }

        public void setStrafe(float strafe) {
            this.strafeInput = Float.isFinite(strafe) ? Math.clamp(strafe, -1.0F, 1.0F) : 0.0F;
            this.left = this.strafeInput > 0.0F;
            this.right = this.strafeInput < 0.0F;
        }

        public void setForward(float forward) {
            this.forwardInput = Float.isFinite(forward) ? Math.clamp(forward, -1.0F, 1.0F) : 0.0F;
            this.forward = this.forwardInput > 0.0F;
            this.backward = this.forwardInput < 0.0F;
        }

        public void setForward(boolean forward) {
            this.forward = forward;
            this.forwardInput = forward == this.backward ? 0.0F : (forward ? 1.0F : -1.0F);
        }

        public float getForward() {
            return this.forwardInput;
        }

        public boolean isLeft() {
            return this.left;
        }

        public boolean isRight() {
            return this.right;
        }

        public boolean isJump() {
            return this.jump;
        }

        public boolean isSneak() {
            return this.sneak;
        }

        public boolean isSprint() {
            return this.sprint;
        }

        public float getStrafe() {
            return this.strafeInput;
        }
    }

    public static class Key extends ScriptEvents.Cancellable {
        private final String name;
        private final int keyCode;
        private final boolean pressed;
        private final boolean inGui;

        public String getName() {
            return this.name;
        }

        public Key(String name, int keyCode, boolean pressed, boolean inGui, boolean cancelled) {
            super(cancelled);
            this.name = name != null ? name : "UNKNOWN";
            this.keyCode = keyCode;
            this.pressed = pressed;
            this.inGui = inGui;
        }

        public boolean isInGui() {
            return this.inGui;
        }

        public boolean isPressed() {
            return this.pressed;
        }

        public int getKeyCode() {
            return this.keyCode;
        }
    }

    public static class Motion extends ScriptEvents.Cancellable {
        private double x;
        private double y;
        private double z;
        private float yaw;
        private float pitch;
        private boolean onGround;

        public Motion(
                double x,
                double y,
                double z,
                float yaw,
                float pitch,
                boolean onGround,
                boolean cancelled) {
            super(cancelled);
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.pitch = pitch;
            this.onGround = onGround;
        }

        public void setYaw(float yaw) {
            this.yaw = yaw;
        }

        public void setZ(double z) {
            this.z = z;
        }

        public void setY(double y) {
            this.y = y;
        }

        public void setX(double x) {
            this.x = x;
        }

        public float getPitch() {
            return this.pitch;
        }

        public void setPitch(float pitch) {
            this.pitch = pitch;
        }

        public double getZ() {
            return this.z;
        }

        public double getX() {
            return this.x;
        }

        public boolean isOnGround() {
            return this.onGround;
        }

        public double getY() {
            return this.y;
        }

        public void setOnGround(boolean onGround) {
            this.onGround = onGround;
        }

        public float getYaw() {
            return this.yaw;
        }
    }

    public static class MouseButton extends ScriptEvents.Cancellable {
        private final int button;
        private final boolean pressed;
        private final double x;
        private final double y;

        public MouseButton(int button, boolean pressed, double x, double y, boolean cancelled) {
            super(cancelled);
            this.button = button;
            this.pressed = pressed;
            this.x = x;
            this.y = y;
        }

        public boolean isPressed() {
            return this.pressed;
        }

        public double getX() {
            return this.x;
        }

        public double getY() {
            return this.y;
        }

        public int getButton() {
            return this.button;
        }
    }

    public record MouseCursor(double x, double y) {}

    public static class MouseMove extends ScriptEvents.Cancellable {
        private double deltaX;
        private double deltaY;
        private double sensitivityMultiplier;
        private final double originalDeltaX;
        private final double originalDeltaY;
        private boolean handled;

        public MouseMove(
                double deltaX,
                double deltaY,
                double sensitivityMultiplier,
                double originalDeltaX,
                double originalDeltaY,
                boolean handled,
                boolean cancelled) {
            super(cancelled);
            this.deltaX = deltaX;
            this.deltaY = deltaY;
            this.sensitivityMultiplier = sensitivityMultiplier;
            this.originalDeltaX = originalDeltaX;
            this.originalDeltaY = originalDeltaY;
            this.handled = handled;
        }

        public double getOriginalDeltaY() {
            return this.originalDeltaY;
        }

        public void setSensitivityMultiplier(double sensitivityMultiplier) {
            this.sensitivityMultiplier = sensitivityMultiplier;
        }

        public double getOriginalDeltaX() {
            return this.originalDeltaX;
        }

        public void setHandled(boolean handled) {
            this.handled = handled;
        }

        public void setDeltaX(double deltaX) {
            this.deltaX = deltaX;
        }

        public double getDeltaY() {
            return this.deltaY;
        }

        public void setDeltaY(double deltaY) {
            this.deltaY = deltaY;
        }

        public double getDeltaX() {
            return this.deltaX;
        }

        public double getSensitivityMultiplier() {
            return this.sensitivityMultiplier;
        }

        public boolean isHandled() {
            return this.handled;
        }
    }

    public static class Move extends ScriptEvents.Cancellable {
        private double x;
        private double y;
        private double z;

        public Move(double x, double y, double z, boolean cancelled) {
            super(cancelled);
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public void setZ(double z) {
            this.z = z;
        }

        public void setY(double y) {
            this.y = y;
        }

        public void setX(double x) {
            this.x = x;
        }

        public double getZ() {
            return this.z;
        }

        public double getX() {
            return this.x;
        }

        public double getY() {
            return this.y;
        }
    }

    public static class Packet extends ScriptEvents.Cancellable {
        private final String type;
        private final String action;
        private final String message;
        private final ScriptVector3 position;
        private final ScriptPacketHandle handle;
        private final int direction;
        private final int heldSlot;
        private final float yaw;
        private final float pitch;
        private final boolean moving;

        public Packet(String type, boolean cancelled) {
            this(type, "", "", null, 255, -1, 0.0F, 0.0F, false, null, cancelled);
        }

        public Packet(
                String type,
                String action,
                String message,
                ScriptVector3 position,
                int direction,
                float yaw,
                float pitch,
                boolean moving,
                boolean cancelled) {
            this(
                    type, action, message, position, direction, -1, yaw, pitch, moving, null,
                    cancelled);
        }

        public Packet(
                String type,
                String action,
                String message,
                ScriptVector3 position,
                int direction,
                int heldSlot,
                float yaw,
                float pitch,
                boolean moving,
                ScriptPacketHandle handle,
                boolean cancelled) {
            super(cancelled);
            this.type = type != null ? type : "";
            this.action = action != null ? action : "";
            this.message = message != null ? message : "";
            this.position = position;
            this.handle = handle;
            this.direction = direction;
            this.heldSlot = heldSlot;
            this.yaw = Float.isFinite(yaw) ? yaw : 0.0F;
            this.pitch = Float.isFinite(pitch) ? pitch : 0.0F;
            this.moving = moving;
        }

        public String getType() {
            return this.type;
        }

        public String getAction() {
            return this.action;
        }

        public String getMessage() {
            return this.message;
        }

        public ScriptVector3 getPosition() {
            return this.position;
        }

        public int getDirection() {
            return this.direction;
        }

        public int getHeldSlot() {
            return this.heldSlot;
        }

        public float getYaw() {
            return this.yaw;
        }

        public float getPitch() {
            return this.pitch;
        }

        public boolean isMoving() {
            return this.moving;
        }

        public ScriptPacketHandle hold() {
            if (this.handle != null && this.handle.tryHold()) {
                this.cancel();
                return this.handle;
            } else {
                return null;
            }
        }
    }

    public enum Phase {
        PRE,
        POST;
    }

    public static class Rotation {
        private float yaw;

        public Rotation(float yaw) {
            this.yaw = yaw;
        }

        public void setYaw(float yaw) {
            this.yaw = yaw;
        }

        public float getYaw() {
            return this.yaw;
        }
    }

    public static class RotationModifier {
        private final float tickDelta;
        private final float baseYaw;
        private final float basePitch;
        private final float targetYaw;
        private final float targetPitch;
        private final String source;
        private final ScriptEntity target;
        private float yaw;
        private float pitch;

        public RotationModifier(
                float tickDelta,
                float baseYaw,
                float basePitch,
                float targetYaw,
                float targetPitch,
                String source,
                ScriptEntity target) {
            this.tickDelta = tickDelta;
            this.baseYaw = baseYaw;
            this.basePitch = basePitch;
            this.targetYaw = targetYaw;
            this.targetPitch = targetPitch;
            this.source = source;
            this.target = target;
            this.yaw = baseYaw;
            this.pitch = basePitch;
        }

        public void add(float yawDelta, float pitchDelta) {
            this.addYaw(yawDelta);
            this.addPitch(pitchDelta);
        }

        public void addYaw(float yawDelta) {
            if (Float.isFinite(yawDelta) && Float.isFinite(this.yaw + yawDelta)) {
                this.yaw += yawDelta;
            }
        }

        public void addPitch(float pitchDelta) {
            if (Float.isFinite(pitchDelta) && Float.isFinite(this.pitch + pitchDelta)) {
                this.pitch = Math.clamp(this.pitch + pitchDelta, -90.0F, 90.0F);
            }
        }

        public void setRotation(float yaw, float pitch) {
            this.setYaw(yaw);
            this.setPitch(pitch);
        }

        public void setYaw(float yaw) {
            if (Float.isFinite(yaw)) {
                this.yaw = yaw;
            }
        }

        public void setPitch(float pitch) {
            if (Float.isFinite(pitch)) {
                this.pitch = Math.clamp(pitch, -90.0F, 90.0F);
            }
        }

        public void reset() {
            this.yaw = this.baseYaw;
            this.pitch = this.basePitch;
        }

        public float getYawOffset() {
            return this.yaw - this.baseYaw;
        }

        public float getPitch() {
            return Math.clamp(this.pitch, -90.0F, 90.0F);
        }

        public float getPitchOffset() {
            return this.getPitch() - this.basePitch;
        }

        public boolean isSource(String source) {
            return this.source.equalsIgnoreCase(source);
        }

        @Generated
        public float getTickDelta() {
            return this.tickDelta;
        }

        @Generated
        public float getBaseYaw() {
            return this.baseYaw;
        }

        @Generated
        public float getBasePitch() {
            return this.basePitch;
        }

        @Generated
        public float getTargetYaw() {
            return this.targetYaw;
        }

        @Generated
        public float getTargetPitch() {
            return this.targetPitch;
        }

        @Generated
        public String getSource() {
            return this.source;
        }

        @Generated
        public ScriptEntity getTarget() {
            return this.target;
        }

        @Generated
        public float getYaw() {
            return this.yaw;
        }
    }

    public static class Screen {
        private final String name;
        private boolean preserveInput;

        public String getName() {
            return this.name;
        }

        public Screen(String name, boolean preserveInput) {
            this.name = name;
            this.preserveInput = preserveInput;
        }

        public void preserveInput() {
            this.preserveInput = true;
        }

        public boolean isPreserveInput() {
            return this.preserveInput;
        }
    }

    public static class ScreenInput {
        private boolean handled;

        public ScreenInput(boolean handled) {
            this.handled = handled;
        }

        public void setHandled(boolean handled) {
            this.handled = handled;
        }

        public boolean isHandled() {
            return this.handled;
        }
    }

    public static class Scroll extends ScriptEvents.Cancellable {
        private double x;
        private double y;

        public Scroll(double x, double y, boolean cancelled) {
            super(cancelled);
            this.x = x;
            this.y = y;
        }

        public void setY(double y) {
            this.y = y;
        }

        public void setX(double x) {
            this.x = x;
        }

        public double getX() {
            return this.x;
        }

        public double getY() {
            return this.y;
        }
    }

    public static class SlowDown extends ScriptEvents.Cancellable {
        private float multiplier;

        public SlowDown(float multiplier, boolean cancelled) {
            super(cancelled);
            this.multiplier = multiplier;
        }

        public float getMultiplier() {
            return this.multiplier;
        }

        public void setMultiplier(float multiplier) {
            this.multiplier = multiplier;
        }
    }

    public record Sound(
            String name, float volume, float pitch, ScriptEvents.Cancellable cancellation) {
        public void cancel() {
            this.cancellation.cancel();
        }

        public boolean isCancelled() {
            return this.cancellation.isCancelled();
        }
    }

    public record Tick(ScriptEvents.Phase phase) {}

    public static class Velocity extends ScriptEvents.Cancellable {
        private double x;
        private double y;
        private double z;
        private final boolean explosion;

        public Velocity(double x, double y, double z, boolean explosion, boolean cancelled) {
            super(cancelled);
            this.x = x;
            this.y = y;
            this.z = z;
            this.explosion = explosion;
        }

        public boolean isExplosion() {
            return this.explosion;
        }

        public void setZ(double z) {
            this.z = z;
        }

        public void setY(double y) {
            this.y = y;
        }

        public void setX(double x) {
            this.x = x;
        }

        public double getZ() {
            return this.z;
        }

        public double getX() {
            return this.x;
        }

        public double getY() {
            return this.y;
        }
    }
}
