package astralis.ui.hud;

import astralis.Astralis;
import astralis.event.EventTarget;
import astralis.event.impl.Render2DEvent;
import astralis.util.render.Render2DContext;
import astralis.util.render.RenderUtil;
import astralis.util.render.RoundedRectShape;

import com.astralis.scripting.InstanceAccess;

import net.minecraft.client.Minecraft;

import org.lwjgl.glfw.GLFW;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/** Manages draggable HUD elements, retained layouts, resizing, and grid alignment. */
public class HudManager {
    private final CopyOnWriteArrayList<Draggable> draggableElements = new CopyOnWriteArrayList<>();
    private Draggable draggedElement;
    private Draggable resizedElement;
    private final Map<String, DraggableLayout> savedLayoutsById = new LinkedHashMap<>();
    private float dragOffsetX;
    private float dragOffsetY;
    private float resizeStartMouseY;
    private float resizeStartHeight;
    private static final int GRID_SNAP_DISTANCE = 8;
    private static final int GRID_SPACING = 20;
    private static final float RESIZE_HANDLE_SIZE = 10.0F;

    public HudManager() {
        Astralis.getInstance().getEventBus().register(this);
    }

    /** Adds a draggable without checking for an existing element with the same ID. */
    public void addDraggable(Draggable draggable) {
        this.draggableElements.add(draggable);
    }

    /** Removes an element and retains its normalized position and scale for reuse. */
    public synchronized void removeDraggable(Draggable draggable) {
        if (this.draggableElements.remove(draggable)) {
            if (this.draggedElement == draggable) {
                this.draggedElement = null;
            }

            if (this.resizedElement == draggable) {
                this.resizedElement = null;
            }

            this.savedLayoutsById.put(
                    draggable.getId(),
                    new DraggableLayout(
                            draggable.getNormalizedX(),
                            draggable.getNormalizedY(),
                            draggable.getScale()));

            while (true) {
                int cachedLayoutCount = this.savedLayoutsById.size();

                if (cachedLayoutCount <= 1024) {
                    return;
                }

                this.savedLayoutsById.remove(this.savedLayoutsById.keySet().iterator().next());
            }
        }
    }

    /** Draws the editing grid at 20px intervals across the scaled GUI dimensions. */
    private void drawAlignmentGrid(Render2DContext context, int screenWidth, int screenHeight) {
        int gridX = 0;

        while (gridX <= screenWidth) {
            context.drawRect(
                    RoundedRectShape.obtain()
                            .setBounds(gridX, 0.0F, 1.0F, screenHeight)
                            .setColor(520093695));
            gridX += 20;
        }

        for (int gridY = 0; gridY <= screenHeight; gridY += 20) {
            context.drawRect(
                    RoundedRectShape.obtain()
                            .setBounds(0.0F, gridY, screenWidth, 1.0F)
                            .setColor(520093695));
        }
    }

    /** Selects the topmost resize handle first, then the topmost draggable body. */
    public void beginInteraction(double mouseX, double mouseY) {
        int resizeIndex = this.draggableElements.size() - 1;

        while (resizeIndex >= 0) {
            Draggable resizeCandidate = this.draggableElements.get(resizeIndex);
            if (resizeCandidate.isVisible()) {
                float right = resizeCandidate.getX() + resizeCandidate.getScaledWidth();
                float bottom = resizeCandidate.getY() + resizeCandidate.getScaledHeight();
                if (mouseX >= right - 10.0F
                        && mouseX <= right
                        && mouseY >= bottom - 10.0F
                        && mouseY <= bottom) {
                    this.resizedElement = resizeCandidate;
                    this.resizeStartMouseY = (float) mouseY;
                    this.resizeStartHeight = resizeCandidate.getScaledHeight();
                    return;
                }
            }

            resizeIndex--;
        }

        for (int dragIndex = this.draggableElements.size() - 1; dragIndex >= 0; dragIndex--) {
            Draggable dragCandidate = this.draggableElements.get(dragIndex);
            if (dragCandidate.isVisible()
                    && RenderUtil.isInsideRect(
                            mouseX,
                            mouseY,
                            dragCandidate.getX(),
                            dragCandidate.getY(),
                            dragCandidate.getScaledWidth(),
                            dragCandidate.getScaledHeight())) {
                this.draggedElement = dragCandidate;
                this.dragOffsetX = (float) mouseX - dragCandidate.getX();
                this.dragOffsetY = (float) mouseY - dragCandidate.getY();
                return;
            }
        }
    }

    /** Ends the current drag or resize interaction. */
    public void endInteraction() {
        this.draggedElement = null;
        this.resizedElement = null;
    }

    /** Updates the selected element from GUI-scaled mouse coordinates while a screen is open. */
    @EventTarget
    public void onRender2D(Render2DEvent event) {
        if (InstanceAccess.mc.gui.screen() == null) {
            this.draggedElement = null;
            this.resizedElement = null;
        } else {
            int screenWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
            int screenHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight();
            double mouseX =
                    Minecraft.getInstance().mouseHandler.xpos()
                            * screenWidth
                            / Minecraft.getInstance().getWindow().getScreenWidth();
            double mouseY =
                    Minecraft.getInstance().mouseHandler.ypos()
                            * screenHeight
                            / Minecraft.getInstance().getWindow().getScreenHeight();
            if (this.resizedElement != null) {
                float unscaledHeight = this.resizedElement.getHeight();
                if (unscaledHeight > 0.0F) {
                    float scaledHeight =
                            this.resizeStartHeight + ((float) mouseY - this.resizeStartMouseY);
                    float scale = scaledHeight / unscaledHeight;
                    scale = Math.max(0.5F, Math.min(2.5F, scale));
                    this.resizedElement.setScale(scale);
                }
            } else {
                if (this.draggedElement != null) {
                    boolean snapEnabled = this.isGridSnapModifierHeld();
                    if (snapEnabled) {
                        this.drawAlignmentGrid(event.getContext(), screenWidth, screenHeight);
                    }

                    float elementX = (float) mouseX - this.dragOffsetX;
                    float elementY = (float) mouseY - this.dragOffsetY;
                    if (snapEnabled) {
                        elementX = this.snapToGrid(elementX);
                        elementY = this.snapToGrid(elementY);
                    }

                    this.draggedElement.setPositionClamped(
                            elementX, elementY, screenWidth, screenHeight);
                }
            }
        }
    }

    /** Registers a unique ID and restores its retained layout, if present. */
    public synchronized boolean addUniqueDraggable(Draggable draggable) {
        if (this.draggableElements.stream()
                .anyMatch(
                        registeredElement -> registeredElement.getId().equals(draggable.getId()))) {
            return false;
        }

        DraggableLayout savedLayout = this.savedLayoutsById.remove(draggable.getId());
        if (savedLayout != null) {
            draggable.setNormalizedPosition(
                    savedLayout.getNormalizedX(), savedLayout.getNormalizedY());
            draggable.setScale(savedLayout.getScale());
        }

        this.draggableElements.add(draggable);
        return true;
    }

    /** Returns the live list of registered draggable elements. */
    public CopyOnWriteArrayList<Draggable> getDraggables() {
        return this.draggableElements;
    }

    /** Enables alignment while either Shift or Control key is held. */
    private boolean isGridSnapModifierHeld() {
        long windowHandle = Minecraft.getInstance().getWindow().handle();

        if (GLFW.glfwGetKey(windowHandle, 340) != 1) {

            if (GLFW.glfwGetKey(windowHandle, 344) != 1) {

                if (GLFW.glfwGetKey(windowHandle, 341) != 1) {

                    if (GLFW.glfwGetKey(windowHandle, 345) != 1) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    /** Snaps to a nearby 20px grid line when its distance is less than 8px. */
    private float snapToGrid(float coordinate) {
        int gridIndex = Math.round(coordinate / 20.0F);

        float gridCoordinate = gridIndex * 20;
        return Math.abs(gridCoordinate - coordinate) < 8.0F ? gridCoordinate : coordinate;
    }
}
