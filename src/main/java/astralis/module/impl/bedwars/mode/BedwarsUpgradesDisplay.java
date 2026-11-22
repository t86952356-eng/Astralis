package astralis.module.impl.bedwars.mode;

import astralis.Astralis;
import astralis.event.EventStage;
import astralis.event.EventTarget;
import astralis.event.impl.ChatReceiveEvent;
import astralis.event.impl.Render2DEvent;
import astralis.event.impl.TickEvent;
import astralis.event.impl.WorldChangeEvent;
import astralis.module.impl.Bedwars;
import astralis.network.hypixel.HypixelLocation;
import astralis.setting.BooleanSetting;
import astralis.setting.ColorSetting;
import astralis.setting.Mode;
import astralis.setting.NumberSetting;
import astralis.setting.SettingGroup;
import astralis.ui.hud.Draggable;
import astralis.util.render.FontManager;
import astralis.util.render.font.SizedFont;
import astralis.util.render.font.TextDrawRequest;

import net.hypixel.data.type.GameType;

import java.awt.Color;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Queue;

public class BedwarsUpgradesDisplay extends Mode<Bedwars> {
    private static final String MISSING_TEXT = "§cX";
    private final BooleanSetting shortNames;
    private final BooleanSetting onlyPurchased;
    private final NumberSetting scale;
    private final ColorSetting textColor;
    private final BooleanSetting sharpness;
    private final BooleanSetting protection;
    private final BooleanSetting traps;
    private final BooleanSetting featherFalling;
    private final BooleanSetting healPool;
    private final BooleanSetting forge;
    private final SettingGroup settingsGroup;
    private final SettingGroup showGroup;
    private final Draggable hudElement;
    private final Queue<String> pendingTraps;
    private int sharpnessLevel;
    private int savedSharpnessLevel;
    private int protectionLevel;
    private int savedProtectionLevel;
    private int featherFallingLevel;
    private int savedFeatherFallingLevel;
    private String activeTrap;
    private String savedActiveTrap;
    private boolean healPoolPurchased;
    private boolean savedHealPoolPurchased;
    private String forgeTier;
    private String savedForgeTier;
    private BooleanSetting toggle;

    public BedwarsUpgradesDisplay(Bedwars bedwars) {

        super("Upgrades Display", bedwars);

        this.shortNames = new BooleanSetting("677dcac92c928b2505b74a8f3982e89f", false);

        this.onlyPurchased =
                new BooleanSetting(
                        "d995e9a98b567e0e5d624dadc8d998dd44128b37a79f925fc5de37abc848976e", true);

        this.scale = new NumberSetting("137a749a1d2ba133748fc087474416fc", 1.0F, 0.5F, 1.5F, 0.05F);

        this.textColor = new ColorSetting("7cb4a92d19da4d5aa92692100e774020", Color.WHITE);

        this.sharpness = new BooleanSetting("61e19b45aaf42572566fd08ac9db5c77", true);

        this.protection = new BooleanSetting("2d1cb349cfd36ae6ef5097688c9384f6", true);

        this.traps = new BooleanSetting("1d4a7944b54a0dd8a1baad9d62b1b3cb", true);

        this.featherFalling = new BooleanSetting("7df15a205b4ba8087f257b9dd527bf2a", true);

        this.healPool = new BooleanSetting("5ab79c6ce95f91a700c424478d50c779", true);

        this.forge = new BooleanSetting("15ea40396a70dc8dd52b2d10c81d55e6", true);

        this.settingsGroup =
                new SettingGroup("e1b6ffd7ce011c799b9f90fcf88693e3")
                        .addValues(this.onlyPurchased, this.shortNames, this.scale, this.textColor);

        SettingGroup settingGroup =
                new SettingGroup("889ff915c520838500f733caf91a980c").collapsed();

        this.showGroup =
                settingGroup.addValues(
                        this.sharpness,
                        this.protection,
                        this.traps,
                        this.featherFalling,
                        this.healPool,
                        this.forge);
        this.pendingTraps = new ArrayDeque<>();
        this.activeTrap = "";
        this.savedActiveTrap = "";
        this.forgeTier = "";
        this.savedForgeTier = "";

        this.hudElement = new Draggable("Upgrades Display", 120.0F, 40.0F, 8.0F, 40.0F);
        this.hudElement.setNormalizedPosition(0.01F, 0.14F);
        this.hudElement.setVisibleWhen(this::isHudVisible);
        Astralis.getInstance().getHudManager().addDraggable(this.hudElement);
    }

    public void setToggleSetting(BooleanSetting booleanSetting) {
        this.toggle = booleanSetting;
    }

    @EventTarget
    public void onTick(TickEvent tickEvent) {
        if (tickEvent.getStage() == EventStage.POST) {
            if (!this.isBedwarsGame()) {
                this.resetUpgrades();
            }
        }
    }

    private String shortenLabel(String label) {

        if (!this.shortNames.getValue()) {
            return label;
        }

        return switch (label) {
            case "Sharpness: " -> {
                yield "Sharp: ";
            }
            case "Protection: " -> {
                yield "Prot: ";
            }
            case "Feather Falling: " -> {
                yield "Feather: ";
            }
            case "Heal Pool: " -> {
                yield "Heal: ";
            }
            default -> label;
        };
    }

    private void restoreSavedUpgrades() {
        this.sharpnessLevel = this.savedSharpnessLevel;
        this.protectionLevel = this.savedProtectionLevel;
        this.activeTrap = this.savedActiveTrap;
        this.featherFallingLevel = this.savedFeatherFallingLevel;
        this.healPoolPurchased = this.savedHealPoolPurchased;
        this.forgeTier = this.savedForgeTier;
    }

    private static String colorForgeTier(String tier) {
        return switch (tier) {
            case "Iron" -> "§7" + tier;
            case "Golden" -> "§6" + tier;
            case "Emerald" -> "§2" + tier;
            case "Molten" -> "§4" + tier;
            default -> tier;
        };
    }

    @EventTarget
    public void onChatReceive(ChatReceiveEvent event) {
        if (this.isBedwarsGame()) {
            String message = ChatTextUtil.normalizeText(event.getMessage().getString());
            if (!message.contains("»")) {

                if (message.equals("You will respawn because you still have a bed!")) {
                    this.restoreSavedUpgrades();
                }

                if (message.contains("purchased")) {

                    if (message.contains("Sharpened Swords")) {

                        this.sharpnessLevel =
                                this.savedSharpnessLevel =
                                        message.contains("Sharpened Swords II") ? 2 : 1;
                    }

                    if (message.contains("Reinforced Armor")) {

                        if (message.contains("Reinforced Armor IV")) {
                            this.protectionLevel = this.savedProtectionLevel = 4;
                        } else {

                            if (message.contains("Reinforced Armor III")) {
                                this.protectionLevel = this.savedProtectionLevel = 3;
                            } else {

                                if (message.contains("Reinforced Armor II")) {
                                    this.protectionLevel = this.savedProtectionLevel = 2;
                                } else {

                                    if (message.contains("Reinforced Armor I")) {
                                        this.protectionLevel = this.savedProtectionLevel = 1;
                                    }
                                }
                            }
                        }
                    }

                    if (message.contains("Trap")) {

                        if (message.contains("Miner Fatigue")) {

                            this.addTrap("Miner Fatigue");
                        } else {

                            if (message.contains("Blindness")) {

                                this.addTrap("Blindness");
                            } else {

                                if (message.contains("Reveal")) {

                                    this.addTrap("Reveal");
                                } else {

                                    if (message.contains("Counter-Offensive")) {

                                        this.addTrap("Counter-Offensive");
                                    }
                                }
                            }
                        }
                    }

                    if (message.contains("Cushioned Boots")) {

                        if (message.contains("Cushioned Boots II")) {
                            this.featherFallingLevel = this.savedFeatherFallingLevel = 2;
                        } else {

                            if (message.contains("Cushioned Boots I")) {
                                this.featherFallingLevel = this.savedFeatherFallingLevel = 1;
                            }
                        }
                    }

                    if (message.contains("Heal Pool")) {
                        this.healPoolPurchased = this.savedHealPoolPurchased = true;
                    }

                    if (message.contains("Forge")) {

                        if (message.contains("Molten")) {

                            this.forgeTier = this.savedForgeTier = "Molten";
                        } else {

                            if (message.contains("Emerald")) {

                                this.forgeTier = this.savedForgeTier = "Emerald";
                            } else {

                                if (message.contains("Golden")) {

                                    this.forgeTier = this.savedForgeTier = "Golden";
                                } else {

                                    if (message.contains("Iron")) {

                                        this.forgeTier = this.savedForgeTier = "Iron";
                                    }
                                }
                            }
                        }
                    }
                }

                if (!message.contains("Trap was set off!")) {

                    if (!message.contains("Your Bed was destroyed")) {
                        return;
                    }
                }

                this.activeTrap = this.pendingTraps.poll();
                if (this.activeTrap == null) {
                    this.activeTrap = "";
                }

                this.savedActiveTrap = this.activeTrap;
            }
        }
    }

    private boolean isBedwarsGame() {
        if (Bedwars.isInBedwarsGame()) {
            return true;
        }

        HypixelLocation location = Astralis.getInstance().getHypixelApi().getLocation();
        if (location == null || location.getServerType() != GameType.DUELS) {
            return false;
        }

        if (location.getLobbyName() != null && !location.getLobbyName().isBlank()) {
            return false;
        }

        String mode = location.getMode();
        if (mode != null) {
            String upperMode = mode.toUpperCase(Locale.ROOT);

            if (upperMode.contains("BEDWARS")) {
                return true;
            }
        }

        return false;
    }

    public SettingGroup getShowGroup() {
        return this.showGroup;
    }

    private void addTrap(String trapName) {
        if (this.activeTrap.isEmpty()) {
            this.activeTrap = this.savedActiveTrap = trapName;
        } else {
            this.pendingTraps.offer(trapName);
        }
    }

    @EventTarget
    public void onWorldChange(WorldChangeEvent worldChangeEvent) {
        this.resetUpgrades();
    }

    @EventTarget
    public void onRender2D(Render2DEvent render2DEvent) {

        if (mc.level != null && this.isHudVisible()) {
            ArrayList<String> lines = new ArrayList<>();
            if (this.shouldShow(this.sharpness, this.sharpnessLevel > 0)) {

                String sharpnessLabel = this.shortenLabel("Sharpness: ");
                String sharpnessValue;
                if (this.sharpnessLevel > 0) {

                    sharpnessValue = "§a" + this.sharpnessLevel;
                } else {

                    sharpnessValue = "§cX";
                }

                lines.add(sharpnessLabel + sharpnessValue);
            }

            if (this.shouldShow(this.protection, this.protectionLevel > 0)) {

                String protectionLabel = this.shortenLabel("Protection: ");
                String protectionValue;
                if (this.protectionLevel > 0) {

                    protectionValue = "§a" + this.protectionLevel;
                } else {

                    protectionValue = "§cX";
                }

                lines.add(protectionLabel + protectionValue);
            }

            if (this.shouldShow(this.traps, !this.activeTrap.isEmpty())) {

                String trapLabel = this.shortenLabel("Trap: ");
                String trapValue;
                if (this.activeTrap.isEmpty()) {

                    trapValue = "§cX";
                } else {

                    trapValue = "§a" + this.activeTrap;
                }

                lines.add(trapLabel + trapValue);
            }

            if (this.shouldShow(this.featherFalling, this.featherFallingLevel > 0)) {

                String featherLabel = this.shortenLabel("Feather Falling: ");
                String featherValue;
                if (this.featherFallingLevel > 0) {

                    featherValue = "§a" + this.featherFallingLevel;
                } else {

                    featherValue = "§cX";
                }

                lines.add(featherLabel + featherValue);
            }

            if (this.shouldShow(this.healPool, this.healPoolPurchased)) {

                String healLabel = this.shortenLabel("Heal Pool: ");
                String healValue;
                if (this.healPoolPurchased) {

                    healValue = "§a✓";
                } else {

                    healValue = "§cX";
                }

                lines.add(healLabel + healValue);
            }

            if (this.shouldShow(this.forge, !this.forgeTier.isEmpty())) {

                String forgeLabel = this.shortenLabel("Forge: ");
                String forgeValue;
                if (this.forgeTier.isEmpty()) {

                    forgeValue = "§cX";
                } else {
                    forgeValue = colorForgeTier(this.forgeTier);
                }

                lines.add(forgeLabel + forgeValue);
            }

            if (!lines.isEmpty()) {

                SizedFont font =
                        FontManager.getThemedFont(
                                "Product Sans Regular", 8.0F * this.scale.getValue());
                float textX = this.hudElement.getX();
                float textY = this.hudElement.getY();
                float lineHeight = font.getLineHeight();
                float maxWidth = 0.0F;

                for (String measuredLine : lines) {
                    maxWidth = Math.max(maxWidth, font.getStringWidth(measuredLine));
                }

                this.hudElement.setWidth(maxWidth);
                this.hudElement.setHeight(lineHeight * lines.size());
                int textColorRgb = this.textColor.getValue().getRGB();

                for (String drawnLine : lines) {
                    render2DEvent
                            .getContext()
                            .drawText(
                                    TextDrawRequest.obtain()
                                            .setText(drawnLine)
                                            .setPosition(textX, textY)
                                            .setFont(font.getMsdfFont())
                                            .setFontSize(font.getSize())
                                            .enableShadow()
                                            .setColor(textColorRgb));
                    textY += lineHeight;
                }
            }
        }
    }

    private boolean shouldShow(BooleanSetting booleanSetting, boolean purchased) {
        return !booleanSetting.getValue() ? false : !this.onlyPurchased.getValue() || purchased;
    }

    private boolean isHudVisible() {
        return this.parent.isEnabled()
                        && this.toggle != null
                        && this.toggle.getValue()
                        && this.isBedwarsGame()
                        && !mc.gui.hud.isHidden()
                ? !this.onlyPurchased.getValue() || this.hasVisibleUpgrade()
                : false;
    }

    private void resetUpgrades() {
        this.sharpnessLevel = this.savedSharpnessLevel = 0;
        this.protectionLevel = this.savedProtectionLevel = 0;
        this.featherFallingLevel = this.savedFeatherFallingLevel = 0;
        this.activeTrap = this.savedActiveTrap = "";
        this.healPoolPurchased = this.savedHealPoolPurchased = false;
        this.forgeTier = this.savedForgeTier = "";
        this.pendingTraps.clear();
    }

    public SettingGroup getSettingsGroup() {
        return this.settingsGroup;
    }

    @Override
    public void onDisable() {
        this.resetUpgrades();
    }

    private boolean hasVisibleUpgrade() {
        if (this.sharpness.getValue() && this.sharpnessLevel > 0) {
            return true;
        } else if (this.protection.getValue() && this.protectionLevel > 0) {
            return true;
        } else if (this.traps.getValue() && !this.activeTrap.isEmpty()) {
            return true;
        } else if (this.featherFalling.getValue() && this.featherFallingLevel > 0) {
            return true;
        } else {
            return this.healPool.getValue() && this.healPoolPurchased
                    ? true
                    : this.forge.getValue() && !this.forgeTier.isEmpty();
        }
    }
}
