package astralis.module.impl;

import astralis.event.EventTarget;
import astralis.event.impl.ScreenOpenEvent;
import astralis.event.impl.TickEvent;
import astralis.module.Module;
import astralis.module.impl.guimove.mode.GuiMoveSpoof;
import astralis.module.impl.guimove.mode.GuiMoveStop;
import astralis.module.impl.guimove.mode.GuiMoveVanilla;
import astralis.module.impl.guimove.mode.GuiMoveWatchdog;
import astralis.module.impl.guimove.mode.GuiMoveWatchdogBlinkless;
import astralis.setting.BooleanSetting;
import astralis.setting.ModeSetting;
import astralis.setting.SettingGroup;
import astralis.ui.screen.DropdownClickGuiScreen;
import astralis.ui.screen.ImGuiClickGuiScreen;
import astralis.ui.screen.PanelClickGuiScreen;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;

import pub.astralis.client.module.core.Category;

import java.util.Objects;

public class GuiMove extends Module {
    private final BooleanSetting screenCheck;
    private final BooleanSetting allowChest;
    private final BooleanSetting allowInventory;
    private final BooleanSetting allowClickGui;
    private final BooleanSetting overrideSneak;
    private final ModeSetting mode;

    public GuiMove() {

        super(MODULE_ID, "", "", Category.Misc);

        this.screenCheck = new BooleanSetting(SCREEN_CHECK_SETTING_ID, true);

        this.allowChest = new BooleanSetting(ALLOW_CHEST_SETTING_ID, true);

        this.allowInventory = new BooleanSetting(ALLOW_INVENTORY_SETTING_ID, true);

        this.allowClickGui = new BooleanSetting(ALLOW_CLICK_GUI_SETTING_ID, true);

        this.overrideSneak = new BooleanSetting(OVERRIDE_SNEAK_SETTING_ID, true);

        ModeSetting modeSetting =
                new ModeSetting(MODE_SETTING_ID, this)
                        .addValues(new GuiMoveVanilla(this))
                        .addValues(new GuiMoveWatchdogBlinkless(this))
                        .addValues(new GuiMoveWatchdog(this))
                        .addValues(new GuiMoveStop(this))
                        .addValues(new GuiMoveSpoof(this));
        this.mode = modeSetting.setDefault("Vanilla");
        SettingGroup settingGroup =
                new SettingGroup(SCREENS_GROUP_ID)
                        .addValues(this.allowChest, this.allowInventory, this.allowClickGui);
        settingGroup.hideIf(() -> !this.screenCheck.getValue());
        this.addSettings(this.mode, this.overrideSneak, this.screenCheck, settingGroup);
    }

    public void restoreKeyStates() {
        KeyMapping.setAll();
        if (this.overrideSneak.getValue()) {
            mc.options.keyShift.setDown(false);
        }
    }

    @Override
    public String getSuffix() {
        return this.mode.getValue().getName();
    }

    public BooleanSetting getAllowChest() {
        return this.allowChest;
    }

    public boolean isMovementAllowedIn(Screen screen2) {
        if (screen2 == null || this.isClickGuiScreen(screen2)) {
            return false;
        }

        if (!this.screenCheck.getValue()) {
            return true;
        }

        Screen screen3 = screen2;
        byte restartIndex = 0;

        return switch (typeSwitch0(screen3, restartIndex)) {
            case 0 -> {
                InventoryScreen inventoryScreen = (InventoryScreen) screen3;
                yield this.allowInventory.getValue();
            }
            case 1 -> {
                AbstractContainerScreen abstractContainerScreen = (AbstractContainerScreen) screen3;
                yield this.allowChest.getValue();
            }
            default -> false;
        };
    }

    @EventTarget
    public void onScreenOpen(ScreenOpenEvent event) {
        Screen screen2 = event.getScreen();
        if (this.mode.getValue() instanceof GuiMoveWatchdogBlinkless) {
            if (this.allowInventory.getValue() && screen2 instanceof InventoryScreen) {
                event.keepKeyStates();
            }
        } else if (this.isMovementAllowedIn(screen2)) {
            if (!(this.mode.getValue() instanceof GuiMoveSpoof)
                    || screen2 instanceof InventoryScreen) {
                event.keepKeyStates();
            }
        }
    }

    public boolean isBlinklessModeActive() {
        return this.isEnabled() && this.mode.getValue() instanceof GuiMoveWatchdogBlinkless;
    }

    public BooleanSetting getAllowInventory() {
        return this.allowInventory;
    }

    public boolean isMovementAllowedInCurrentScreen() {
        return this.isMovementAllowedIn(mc.gui.screen());
    }

    public BooleanSetting getAllowClickGui() {
        return this.allowClickGui;
    }

    private boolean isClickGuiOpen() {
        return this.isClickGuiScreen(mc.gui.screen());
    }

    @EventTarget
    public void onTick(TickEvent tickEvent) {
        if (this.allowClickGui.getValue() && this.isClickGuiOpen()) {
            this.restoreKeyStates();
        }
    }

    private boolean isClickGuiScreen(Screen screen2) {
        return screen2 instanceof ImGuiClickGuiScreen
                || screen2 instanceof DropdownClickGuiScreen
                || screen2 instanceof PanelClickGuiScreen;
    }

    private static int typeSwitch0(Screen screen2, int restartIndex) {
        Objects.checkIndex(restartIndex, 3);
        if (screen2 == null) {
            return -1;
        } else if (restartIndex <= 0 && screen2 instanceof InventoryScreen) {
            return 0;
        } else {
            return restartIndex <= 1 && screen2 instanceof AbstractContainerScreen ? 1 : 2;
        }
    }

    private static final String MODULE_ID = "8a2473cad88e4736a6ebb213f8ff26dd";
    private static final String SCREEN_CHECK_SETTING_ID = "c3c3a5d08cf38292bd452f066d1a935e";
    private static final String ALLOW_CHEST_SETTING_ID = "274c583647ca738ddb00dbd7a05f70bd";
    private static final String ALLOW_INVENTORY_SETTING_ID = "a1d1516b965d74b77a8aea3809fd7691";
    private static final String ALLOW_CLICK_GUI_SETTING_ID = "67fb89a9ccadf9e6a758778a297da886";
    private static final String OVERRIDE_SNEAK_SETTING_ID = "f3bb8af35a6914c34a41c9c437292cea";
    private static final String MODE_SETTING_ID = "5e7a9015ea584af4a04a409297fa5600";
    private static final String SCREENS_GROUP_ID = "9b30df64aa789abb3c192dde144beb96";
}
