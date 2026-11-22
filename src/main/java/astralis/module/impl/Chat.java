package astralis.module.impl;

import astralis.module.Module;
import astralis.setting.BooleanSetting;

import pub.astralis.client.module.core.Category;

public class Chat extends Module {
    private final BooleanSetting moreChatHistory;
    private final BooleanSetting showChatOnKey;
    private final BooleanSetting chatStacking;

    public Chat() {

        super(MODULE_ID, "", "", Category.Misc);

        this.moreChatHistory = new BooleanSetting(MORE_CHAT_HISTORY_SETTING_ID, true);

        this.showChatOnKey = new BooleanSetting(SHOW_CHAT_ON_KEY_SETTING_ID, false);

        this.chatStacking = new BooleanSetting(CHAT_STACKING_SETTING_ID, false);
        this.addSettings(this.moreChatHistory, this.showChatOnKey, this.chatStacking);
    }

    public BooleanSetting getShowChatOnKeySetting() {
        return this.showChatOnKey;
    }

    public BooleanSetting getMoreChatHistorySetting() {
        return this.moreChatHistory;
    }

    public BooleanSetting getChatStackingSetting() {
        return this.chatStacking;
    }

    private static final String MODULE_ID = "4c6db6964290fe12dcdf79836d963c5f";
    private static final String MORE_CHAT_HISTORY_SETTING_ID =
            "2cc51d6149c5bd492e44433f4d72c5fd0f6bb9f0bda1da1bb7ddf348675cb34c";
    private static final String SHOW_CHAT_ON_KEY_SETTING_ID = "be81a0344a73f56dc4a507bc81882537";
    private static final String CHAT_STACKING_SETTING_ID = "54ff9c7287b6e318750a74d5a5b340be";
}
