package astralis.event.impl;

import astralis.event.Event;

public class MoveInputEvent implements Event {
    private boolean forward;
    private boolean backward;
    private boolean left;
    private boolean right;
    private boolean jump;
    private boolean sneak;
    private boolean sprint;
    private float forwardImpulse;
    private float strafeImpulse;

    public MoveInputEvent(
            boolean forward,
            boolean backward,
            boolean left,
            boolean right,
            boolean jump,
            boolean sneak,
            boolean sprint) {

        float impulse;
        this.forward = forward;
        this.backward = backward;
        this.left = left;
        this.right = right;
        this.jump = jump;
        this.sneak = sneak;
        this.sprint = sprint;
        if (forward == backward) {
            impulse = 0.0F;
        } else {
            impulse = forward ? 1.0F : -1.0F;
        }

        this.forwardImpulse = impulse;
        if (left == right) {
            impulse = 0.0F;
        } else {
            impulse = left ? 1.0F : -1.0F;
        }

        this.strafeImpulse = impulse;
    }

    public void setSneak(boolean sneak) {
        this.sneak = sneak;
    }

    public void setForward(boolean forward) {
        float impulse;
        this.forward = forward;
        if (forward == this.backward) {
            impulse = 0.0F;
        } else {
            impulse = forward ? 1.0F : -1.0F;
        }

        this.forwardImpulse = impulse;
    }

    public float getStrafeImpulse() {
        return this.strafeImpulse;
    }

    public void setStrafeImpulse(float strafeImpulse) {
        float clampedImpulse =
                Float.isFinite(strafeImpulse) ? Math.clamp(strafeImpulse, -1.0F, 1.0F) : 0.0F;
        this.strafeImpulse = clampedImpulse;
        float leftCompare;
        int leftFlag =
                (leftCompare = this.strafeImpulse - 0.0F) == 0.0F
                        ? 0
                        : (leftCompare < 0.0F ? -1 : 1);
        leftFlag = leftFlag > 0 ? 1 : 0;
        this.left = leftFlag != 0;
        float rightCompare;
        int rightFlag =
                (rightCompare = this.strafeImpulse - 0.0F) == 0.0F
                        ? 0
                        : (rightCompare < 0.0F ? -1 : 1);
        rightFlag = rightFlag < 0 ? 1 : 0;
        this.right = rightFlag != 0;
    }

    public void setLeft(boolean left) {
        float impulse;
        this.left = left;
        if (left == this.right) {
            impulse = 0.0F;
        } else {
            impulse = left ? 1.0F : -1.0F;
        }

        this.strafeImpulse = impulse;
    }

    public void setJump(boolean jump) {
        this.jump = jump;
    }

    public boolean isBackward() {
        return this.backward;
    }

    public void setBackward(boolean backward) {
        float impulse;
        this.backward = backward;
        if (this.forward == backward) {
            impulse = 0.0F;
        } else {
            impulse = this.forward ? 1.0F : -1.0F;
        }

        this.forwardImpulse = impulse;
    }

    public void setRight(boolean right) {
        float impulse;
        this.right = right;
        if (this.left == right) {
            impulse = 0.0F;
        } else {
            impulse = this.left ? 1.0F : -1.0F;
        }

        this.strafeImpulse = impulse;
    }

    public boolean isRight() {
        return this.right;
    }

    public boolean isSprint() {
        return this.sprint;
    }

    public float getForwardImpulse() {
        return this.forwardImpulse;
    }

    public boolean isLeft() {
        return this.left;
    }

    public void setForwardImpulse(float forwardImpulse) {
        float clampedImpulse =
                Float.isFinite(forwardImpulse) ? Math.clamp(forwardImpulse, -1.0F, 1.0F) : 0.0F;
        this.forwardImpulse = clampedImpulse;
        float forwardCompare;
        int forwardFlag =
                (forwardCompare = this.forwardImpulse - 0.0F) == 0.0F
                        ? 0
                        : (forwardCompare < 0.0F ? -1 : 1);
        forwardFlag = forwardFlag > 0 ? 1 : 0;
        this.forward = forwardFlag != 0;
        float backwardCompare;
        int backwardFlag =
                (backwardCompare = this.forwardImpulse - 0.0F) == 0.0F
                        ? 0
                        : (backwardCompare < 0.0F ? -1 : 1);
        backwardFlag = backwardFlag < 0 ? 1 : 0;
        this.backward = backwardFlag != 0;
    }

    public boolean isJump() {
        return this.jump;
    }

    public void setSprint(boolean sprint) {
        this.sprint = sprint;
    }

    public boolean isForward() {
        return this.forward;
    }

    public boolean isSneak() {
        return this.sneak;
    }
}
