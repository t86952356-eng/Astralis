package astralis.module.impl;

import astralis.Astralis;
import astralis.event.EventTarget;
import astralis.event.impl.ChatReceiveEvent;
import astralis.event.impl.GameJoinEvent;
import astralis.module.Module;
import astralis.setting.BooleanSetting;
import astralis.setting.NumberSetting;
import astralis.setting.TextSetting;
import astralis.util.ChatUtil;
import astralis.util.HypixelModApi;

import net.minecraft.client.gui.screens.inventory.BookViewScreen.BookAccess;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.ClickEvent.Action;
import net.minecraft.network.chat.ClickEvent.RunCommand;
import net.minecraft.network.chat.Component;

import pub.astralis.client.module.core.Category;

import java.util.List;
import java.util.regex.Pattern;

public class AutoHypixel extends Module {
    private final BooleanSetting autoSetLanguage;
    private final BooleanSetting autoGg;
    private final TextSetting autoGgMessage;
    private final BooleanSetting autoPlay;
    private final NumberSetting autoPlayDelay;
    public final BooleanSetting autoAcceptPolicy;
    private final List<Pattern> autoGgPatterns;
    private final List<Pattern> autoPlayPatterns;
    HypixelModApi hypixelModApi;

    public AutoHypixel() {

        super(MODULE_ID, "", "", Category.Misc);

        this.autoSetLanguage = new BooleanSetting(AUTO_SET_LANGUAGE_SETTING_ID, true);

        this.autoGg = new BooleanSetting(AUTO_GG_SETTING_ID, true);

        this.autoGgMessage = new TextSetting("Auto gg message", "3e6b8f5260d9ce0704728f13783b8fbc");

        this.autoPlay = new BooleanSetting(AUTO_PLAY_SETTING_ID, false);

        this.autoPlayDelay = new NumberSetting(AUTO_PLAY_DELAY_SETTING_ID, 2.0F, 0.0F, 5.0F, 0.1F);

        this.autoAcceptPolicy = new BooleanSetting(AUTO_ACCEPT_POLICY_SETTING_ID, true);

        Pattern gameOverPattern = Pattern.compile("^ +Game over!$");
        this.autoGgPatterns =
                List.of(
                        gameOverPattern,
                        Pattern.compile(
                                "^ +1st Killer - ?\\[?\\w*\\+*\\]? \\w+ - \\d+(?: Kills?)?$"));

        Pattern eliminatedPattern = Pattern.compile("^You have been eliminated!$");
        Pattern diedPattern = Pattern.compile("^You died! Want to play again\\? Click here! $");

        Pattern playGameOverPattern = Pattern.compile("^ +Game over!$");

        this.autoPlayPatterns =
                List.of(
                        eliminatedPattern,
                        diedPattern,
                        playGameOverPattern,
                        Pattern.compile(
                                "^ +1st Killer - ?\\[?\\w*\\+*\\]? \\w+ - \\d+(?: Kills?)?$"));
        this.hypixelModApi = Astralis.getInstance().getHypixelApi();
        this.autoPlayDelay.hideIf(
                () -> {
                    return !this.autoPlay.getValue();
                });
        this.autoGgMessage.hideIf(
                () -> {
                    return !this.autoGg.getValue();
                });

        this.addSettings(
                this.autoSetLanguage,
                this.autoGg,
                this.autoGgMessage,
                this.autoPlay,
                this.autoPlayDelay,
                this.autoAcceptPolicy);
    }

    @EventTarget
    public void onGameJoin(GameJoinEvent event) {

        if (this.hypixelModApi.getLocation() != null) {
            if (this.autoSetLanguage.getValue()) {

                ChatUtil.sendCommand("language English");
            }
        }
    }

    public boolean acceptPolicyInComponent(Component component) {
        if (mc.player == null) {
            return false;
        }

        ClickEvent clickEvent = component.getStyle().getClickEvent();
        if (clickEvent != null
                && clickEvent.action() == Action.RUN_COMMAND
                && component.getString() != null) {
            String messageText = component.getString();

            if (messageText.contains("I Agree")) {
                mc.player.connection.sendCommand(
                        ((RunCommand) clickEvent).command().replaceFirst("/", ""));
                return true;
            }
        }

        for (Component component2 : component.getSiblings()) {
            if (this.acceptPolicyInComponent(component2)) {
                return true;
            }
        }

        return false;
    }

    public boolean acceptPolicyInBook(BookAccess bookAccess) {

        for (Component component : bookAccess.pages()) {
            if (this.acceptPolicyInComponent(component)) {
                return true;
            }
        }

        return false;
    }

    @EventTarget
    public void onChatMessageReceived(ChatReceiveEvent event) {
        if (this.hypixelModApi.getLocation() != null) {
            Component component = event.getMessage();
            if (this.autoGg.getValue()
                    && this.autoGgPatterns.stream()
                            .anyMatch(pattern -> pattern.matcher(component.getString()).find())) {
                String autoGgText = this.autoGgMessage.getValue();

                ChatUtil.sendCommand("ac " + autoGgText);
            }

            if (this.autoPlay.getValue()
                    && this.autoPlayPatterns.stream()
                            .anyMatch(pattern -> pattern.matcher(component.getString()).find())) {
                this.isGonnaProbablyAutoPlay(this.hypixelModApi.getLocation().getMode());
            }
        }
    }

    private void isGonnaProbablyAutoPlay(String mode) {
        new Thread(
                        () -> {
                            try {
                                Thread.sleep(this.autoPlayDelay.getValue().longValue() * 1000L);
                            } catch (InterruptedException ignored) {
                            }

                            ChatUtil.sendCommand("play " + mode);
                        })
                .start();
    }

    private static final String MODULE_ID = "4974576e3fd8d00a0fe92c6f4705084c";
    private static final String AUTO_SET_LANGUAGE_SETTING_ID =
            "2ed283c95541c746ea81684089d316b430adfaee2a993919e993dbb4caef6f57";
    private static final String AUTO_GG_SETTING_ID = "92960c3e10513bb7531a271dcc9abb47";
    private static final String AUTO_PLAY_SETTING_ID = "72eace1e5a68084954d6bc32dda19f87";
    private static final String AUTO_PLAY_DELAY_SETTING_ID = "31d364b010d1a0c2f9fbca2b9e14d5b3";
    private static final String AUTO_ACCEPT_POLICY_SETTING_ID =
            "5a5163569581283392fab541eac0e6b68debb2014ca003fad57f8bc490422cbb";
}
