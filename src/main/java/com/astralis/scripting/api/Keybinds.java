package com.astralis.scripting.api;

import astralis.mixin.KeyMappingAccessor;

import com.astralis.scripting.InstanceAccess;
import com.astralis.scripting.Script;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.InputConstants.Key;
import com.mojang.blaze3d.platform.InputConstants.Type;

import net.minecraft.client.KeyMapping;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action;

import org.lwjgl.glfw.GLFW;

import java.lang.StackWalker.Option;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class Keybinds implements InstanceAccess {
    private final Set<KeyMapping> heldKeys = new HashSet<>();
    private Script owner;
    private boolean destroyed;

    public void destroy() {
        if (StackWalker.getInstance(Option.RETAIN_CLASS_REFERENCE).getCallerClass()
                != Script.class) {
            throw new SecurityException("Only the script runtime can destroy key ownership.");
        }

        if (!this.destroyed) {
            this.releaseAll();
            this.owner = null;
            this.destroyed = true;
        }
    }

    public void bind(Script script) {
        if (StackWalker.getInstance(Option.RETAIN_CLASS_REFERENCE).getCallerClass()
                != Script.class) {
            throw new SecurityException("Only the script runtime can bind key ownership.");
        }

        if (this.destroyed) {
            throw new IllegalStateException("This key service has already been destroyed.");
        }

        if (script == null) {
            throw new IllegalArgumentException("Script owner cannot be null.");
        }

        if (this.owner != null) {
            if (this.owner != script) {
                throw new IllegalStateException("This key service is already bound.");
            }
        } else {
            this.owner = script;
        }
    }

    public boolean tap(String keyName) {
        this.ensureActive();
        if (mc.player == null || keyName == null) {
            return false;
        }

        if (isSwapHandsAlias(keyName)) {
            mc.player.connection.send(
                    new ServerboundPlayerActionPacket(
                            Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ZERO, Direction.DOWN));
            return true;
        }

        KeyMapping keyMapping = this.findKeyMapping(keyName);
        if (keyMapping == null) {
            return false;
        }

        keyMapping.setDown(true);
        keyMapping.setDown(false);
        KeyMappingAccessor accessor = (KeyMappingAccessor) keyMapping;
        KeyMapping.click(accessor.getBoundKey());
        return true;
    }

    public boolean setPressed(String keyName, boolean pressed) {
        this.ensureActive();
        KeyMapping keyMapping = this.findKeyMapping(keyName);
        if (keyMapping == null) {
            return false;
        }

        keyMapping.setDown(pressed);
        if (pressed) {
            this.heldKeys.add(keyMapping);
        } else {
            this.heldKeys.remove(keyMapping);
        }

        return true;
    }

    public int getKeycode(String keyName) {
        return this.getKeyCode(keyName);
    }

    public boolean isPressed(String keyName) {
        KeyMapping keyMapping = this.findKeyMapping(keyName);
        return keyMapping != null && keyMapping.isDown();
    }

    public void releaseAll() {
        for (KeyMapping keyMapping : this.heldKeys) {
            keyMapping.setDown(false);
        }

        this.heldKeys.clear();
    }

    private void ensureActive() {
        if (this.destroyed) {
            throw new IllegalStateException("This key service has already been destroyed.");
        } else if (this.owner == null || !this.owner.isRuntimeActive()) {
            throw new IllegalStateException("Key mutations require the script-owned key service.");
        }
    }

    private static boolean isSwapHandsAlias(String keyName) {
        return switch (keyName.trim().toLowerCase(Locale.ROOT)) {
            case "swaphands", "swap_hands", "swap-hands" -> true;
            default -> false;
        };
    }

    public double[] getMousePosition() {
        return new double[] {
            mc.mouseHandler.getScaledXPos(mc.getWindow()),
            mc.mouseHandler.getScaledYPos(mc.getWindow())
        };
    }

    public int getKeyCode(String keyName) {
        KeyMapping keyMapping = this.findKeyMapping(keyName);
        int keyCode;
        if (keyMapping != null) {
            KeyMappingAccessor accessor = (KeyMappingAccessor) keyMapping;
            keyCode = accessor.getBoundKey().getValue();
        } else {
            keyCode = -1;
        }

        return keyCode;
    }

    public boolean isMouseDown(int button) {
        return button >= 0
                && button <= 7
                && GLFW.glfwGetMouseButton(mc.getWindow().handle(), button) == 1;
    }

    private KeyMapping findKeyMapping(String keyName) {
        if (keyName == null) {
            return null;
        }

        return switch (keyName.trim().toLowerCase(Locale.ROOT)) {
            case "forward" -> mc.options.keyUp;
            case "back", "backward" -> mc.options.keyDown;
            case "left" -> mc.options.keyLeft;
            case "right" -> mc.options.keyRight;
            case "jump" -> mc.options.keyJump;
            case "sneak", "shift" -> mc.options.keyShift;
            case "sprint" -> mc.options.keySprint;
            case "attack" -> mc.options.keyAttack;
            case "use" -> mc.options.keyUse;
            case "drop" -> mc.options.keyDrop;
            case "inventory" -> mc.options.keyInventory;
            case "swaphands", "swap_hands", "swap-hands" -> mc.options.keySwapOffhand;
            case "pickblock", "pick_block", "pick-block" -> mc.options.keyPickItem;
            default -> null;
        };
    }

    public boolean isPhysicallyDown(String keyName) {
        KeyMapping keyMapping = this.findKeyMapping(keyName);
        if (keyMapping == null) {
            return false;
        } else {
            KeyMappingAccessor accessor = (KeyMappingAccessor) keyMapping;
            Key boundKey = accessor.getBoundKey();
            if (boundKey.getType() == Type.MOUSE) {
                return this.isMouseDown(boundKey.getValue());
            } else {
                return boundKey.getType() != Type.KEYSYM
                        ? keyMapping.isDown()
                        : boundKey.getValue() >= 32
                                && boundKey.getValue() <= 348
                                && InputConstants.isKeyDown(mc.getWindow(), boundKey.getValue());
            }
        }
    }
}
