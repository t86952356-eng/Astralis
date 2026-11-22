package com.astralis.scripting.api;

import astralis.mixin.MultiPlayerGameModeAccessor;

import com.astralis.scripting.InstanceAccess;
import com.astralis.scripting.Script;
import com.astralis.scripting.api.entity.ScriptItemStack;

import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.ContainerInput;

import java.lang.StackWalker.Option;
import java.lang.StackWalker.StackFrame;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Inventory implements InstanceAccess {
    private Script owner;
    private boolean destroyed;

    public final synchronized void destroy() {
        checkScriptRuntimeCaller();
        if (!this.destroyed) {
            this.destroyed = true;
            this.owner = null;
        }
    }

    public boolean open() {
        this.ensureActive();
        if (mc.player != null && mc.gui.screen() == null) {
            mc.gui.setScreen(new InventoryScreen(mc.player));
            return true;
        } else {
            return false;
        }
    }

    public int getSize() {
        this.ensureActive();
        return mc.player != null ? mc.player.getInventory().getContainerSize() : 0;
    }

    public boolean swap(int slot, int button) {
        return this.click(slot, button, "swap");
    }

    public final synchronized void bind(Script script) {
        checkScriptRuntimeCaller();
        if (script == null) {
            throw new IllegalArgumentException("Script owner cannot be null.");
        }

        if (this.destroyed) {
            throw new IllegalStateException("This inventory service has already been destroyed.");
        }

        if (this.owner != null) {
            if (this.owner != script) {
                throw new IllegalStateException("This inventory service is already bound.");
            }
        } else {
            this.owner = script;
        }
    }

    public int getSelectedSlot() {
        this.ensureActive();
        return mc.player != null ? mc.player.getInventory().getSelectedSlot() : -1;
    }

    public boolean setSelectedSlot(int slot) {
        this.ensureActive();
        if (mc.player != null && mc.gameMode != null && slot >= 0 && slot <= 8) {
            mc.player.getInventory().setSelectedSlot(slot);
            MultiPlayerGameModeAccessor accessor = (MultiPlayerGameModeAccessor) mc.gameMode;
            accessor.syncSlot();
            return true;
        } else {
            return false;
        }
    }

    public int getContainerSize() {
        this.ensureActive();
        return mc.player != null ? mc.player.containerMenu.slots.size() : 0;
    }

    public boolean click(int slotId, int button, String actionName) {
        this.ensureActive();
        if (mc.player != null
                && mc.gameMode != null
                && slotId >= -999
                && slotId < mc.player.containerMenu.slots.size()) {
            ContainerInput containerInput = parseContainerInput(actionName);
            if (containerInput != null && isValidClickButton(containerInput, button)) {
                mc.gameMode.handleContainerInput(
                        mc.player.containerMenu.containerId,
                        slotId,
                        button,
                        containerInput,
                        mc.player);
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    private synchronized void ensureActive() {
        if (this.owner == null || this.destroyed || !this.owner.isRuntimeActive()) {
            throw new IllegalStateException("The script inventory service is not active.");
        }
    }

    public boolean selectSlot(int slot) {
        return this.setSelectedSlot(slot);
    }

    public ScriptItemStack getItem(int slot) {
        this.ensureActive();
        return mc.player != null && slot >= 0 && slot < mc.player.getInventory().getContainerSize()
                ? new ScriptItemStack(mc.player.getInventory().getItem(slot))
                : null;
    }

    public List<ScriptItemStack> getItems() {
        this.ensureActive();
        if (mc.player == null) {
            return List.of();
        }

        ArrayList items = new ArrayList(mc.player.getInventory().getContainerSize());

        for (int slot = 0; slot < mc.player.getInventory().getContainerSize(); slot++) {
            items.add(new ScriptItemStack(mc.player.getInventory().getItem(slot)));
        }

        return List.copyOf(items);
    }

    public int countItem(String itemId) {
        this.ensureActive();
        if (mc.player != null && itemId != null) {
            int total = 0;

            for (int slot = 0; slot < mc.player.getInventory().getContainerSize(); slot++) {
                ScriptItemStack stack = new ScriptItemStack(mc.player.getInventory().getItem(slot));
                if (stack.getItemId().equalsIgnoreCase(itemId)) {
                    total += stack.getCount();
                }
            }

            return total;
        } else {
            return 0;
        }
    }

    private static boolean isValidClickButton(ContainerInput containerInput, int button) {
        return switch (containerInput) {
            case PICKUP, QUICK_MOVE, THROW -> button == 0 || button == 1;
            case SWAP -> button >= 0 && button <= 8;
            default -> false;
        };
    }

    private static ContainerInput parseContainerInput(String name) {
        if (name == null) {
            return null;
        }

        return switch (name.trim().toLowerCase(Locale.ROOT)) {
            case "pickup" -> ContainerInput.PICKUP;
            case "quick_move", "quickmove", "shift", "shift_click", "shiftclick" ->
                    ContainerInput.QUICK_MOVE;
            case "swap", "hotbar_swap", "hotbarswap" -> ContainerInput.SWAP;
            case "throw", "drop" -> ContainerInput.THROW;
            default -> null;
        };
    }

    private static void checkScriptRuntimeCaller() {
        Class callerClass =
                StackWalker.getInstance(Option.RETAIN_CLASS_REFERENCE)
                        .walk(
                                frames ->
                                        frames.map(StackFrame::getDeclaringClass)
                                                .filter(frameClass -> frameClass != Inventory.class)
                                                .findFirst()
                                                .orElseThrow(
                                                        () ->
                                                                new SecurityException(
                                                                        "Missing script runtime caller.")));
        if (callerClass != Script.class
                && (!Script.class.isAssignableFrom(callerClass)
                        || callerClass.getClassLoader() != Script.class.getClassLoader())) {
            throw new SecurityException("Script inventory ownership is managed by the runtime.");
        }
    }

    public String getContainerTitle() {
        this.ensureActive();
        return mc.gui.screen() != null ? mc.gui.screen().getTitle().getString() : "";
    }

    public ScriptItemStack getHotbarItem(int slot) {
        this.ensureActive();
        return mc.player != null && slot >= 0 && slot <= 8
                ? new ScriptItemStack(mc.player.getInventory().getItem(slot))
                : null;
    }

    public int findItemSlot(String itemId) {
        this.ensureActive();
        if (mc.player != null && itemId != null) {
            for (int slot = 0; slot < mc.player.getInventory().getContainerSize(); slot++) {
                if (new ScriptItemStack(mc.player.getInventory().getItem(slot))
                        .getItemId()
                        .equalsIgnoreCase(itemId)) {
                    return slot;
                }
            }

            return -1;
        } else {
            return -1;
        }
    }

    public int findHotbarSlot(String itemId) {
        this.ensureActive();
        if (mc.player != null && itemId != null) {
            for (int slot = 0; slot < 9; slot++) {
                if (new ScriptItemStack(mc.player.getInventory().getItem(slot))
                        .getItemId()
                        .equalsIgnoreCase(itemId)) {
                    return slot;
                }
            }

            return -1;
        } else {
            return -1;
        }
    }

    public String getContainerType() {
        this.ensureActive();
        if (mc.player == null) {
            return "";
        }

        try {
            Identifier identifier =
                    BuiltInRegistries.MENU.getKey(mc.player.containerMenu.getType());
            return identifier != null
                    ? identifier.toString()
                    : mc.player.containerMenu.getClass().getSimpleName();
        } catch (UnsupportedOperationException ignored) {
            return mc.player.containerMenu.getClass().getSimpleName();
        }
    }

    public List<ScriptItemStack> getHotbarItems() {
        this.ensureActive();
        if (mc.player == null) {
            return List.of();
        }

        ArrayList items = new ArrayList(9);

        for (int slot = 0; slot < 9; slot++) {
            items.add(new ScriptItemStack(mc.player.getInventory().getItem(slot)));
        }

        return List.copyOf(items);
    }

    public ScriptItemStack getStackInSlot(int slot) {
        return this.getItem(slot);
    }

    public ScriptItemStack getContainerItem(int slot) {
        this.ensureActive();
        return mc.player != null && slot >= 0 && slot < mc.player.containerMenu.slots.size()
                ? new ScriptItemStack(mc.player.containerMenu.getSlot(slot).getItem())
                : null;
    }
}
