package astralis.module.impl.movement;

import astralis.Astralis;
import astralis.module.Module;
import astralis.module.impl.combat.KillAura;
import astralis.module.impl.movement.noslow.NoSlowGrim;
import astralis.module.impl.movement.noslow.NoSlowMiniBlox;
import astralis.module.impl.movement.noslow.NoSlowUniversal;
import astralis.module.impl.movement.noslow.NoSlowVanilla;
import astralis.module.impl.movement.noslow.NoSlowWatchdogA;
import astralis.setting.BooleanSetting;
import astralis.setting.ModeSetting;
import astralis.setting.NumberSetting;
import astralis.setting.SettingGroup;

import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.SplashPotionItem;

import pub.astralis.client.module.core.Category;

public class NoSlowdown extends Module {
    private final NoSlowWatchdogA watchdogAMode;
    private final NoSlowMiniBlox miniBloxMode;
    private final ModeSetting mode;
    private final BooleanSetting food;
    private final BooleanSetting swords;
    private final BooleanSetting bows;
    private final BooleanSetting shields;
    private final BooleanSetting potions;
    private final SettingGroup allowedItems;
    private final NumberSetting slowdownMultiplier;

    public NoSlowdown() {

        super(MODULE_ID, "", "", Category.Misc);
        this.watchdogAMode = new NoSlowWatchdogA(this);
        this.miniBloxMode = new NoSlowMiniBlox(this);

        ModeSetting modeSetting =
                new ModeSetting(MODE_SETTING_ID, this)
                        .addValues(new NoSlowVanilla(this))
                        .addValues(new NoSlowUniversal(this))
                        .addValues(new NoSlowGrim(this))
                        .addValues(this.watchdogAMode)
                        .addValues(this.miniBloxMode);
        this.mode = modeSetting.setDefault("Vanilla");

        this.food = new BooleanSetting(FOOD_SETTING_ID, true);

        this.swords = new BooleanSetting(SWORDS_SETTING_ID, true);

        this.bows = new BooleanSetting(BOWS_SETTING_ID, true);

        this.shields = new BooleanSetting(SHIELDS_SETTING_ID, true);

        this.potions = new BooleanSetting(POTIONS_SETTING_ID, true);

        this.allowedItems =
                new SettingGroup(ALLOWED_ITEMS_GROUP_ID, this)
                        .addValues(this.food)
                        .addValues(this.swords)
                        .addValues(this.bows)
                        .addValues(this.shields)
                        .addValues(this.potions);

        this.slowdownMultiplier =
                new NumberSetting(SLOWDOWN_MULTIPLIER_SETTING_ID, 0.0F, 0.0F, 1.0F, 0.1F);
        this.addSettings(this.mode, this.allowedItems, this.slowdownMultiplier);
    }

    public NumberSetting getSlowdownMultiplier() {
        return this.slowdownMultiplier;
    }

    public ModeSetting getMode() {
        return this.mode;
    }

    public NoSlowMiniBlox getMiniBloxMode() {
        return this.miniBloxMode;
    }

    public SettingGroup getAllowedItems() {
        return this.allowedItems;
    }

    @Override
    public String getSuffix() {
        return this.mode.getValue().getName();
    }

    public boolean shouldNoSlow() {
        boolean autoBlocking =
                Astralis.getInstance()
                        .getModuleManager()
                        .getModule(KillAura.class)
                        .isAutoBlocking();
        if (!mc.player.isUsingItem() && !autoBlocking) {
            return false;
        }

        ItemStack itemStack =
                autoBlocking ? mc.player.getInventory().getSelectedItem() : mc.player.getUseItem();
        if (itemStack.isEmpty()) {
            return false;
        }

        if (itemStack.has(DataComponents.FOOD)) {
            return this.food.getValue();
        }

        if (itemStack.is(ItemTags.SWORDS)) {
            return this.swords.getValue();
        }

        if (itemStack.getItem() instanceof ShieldItem) {
            return this.shields.getValue();
        }

        if (itemStack.getItem() instanceof PotionItem
                && !(itemStack.getItem() instanceof SplashPotionItem)) {
            return this.potions.getValue();
        }

        Item item2 = itemStack.getItem();
        return !(item2 instanceof BowItem) && !(item2 instanceof CrossbowItem)
                ? false
                : this.bows.getValue();
    }

    public NoSlowWatchdogA getWatchdogAMode() {
        return this.watchdogAMode;
    }

    public BooleanSetting getSwords() {
        return this.swords;
    }

    public BooleanSetting getFood() {
        return this.food;
    }

    private static final String MODULE_ID = "829ba5a9eb6424764edbd4b42581a00d";
    private static final String MODE_SETTING_ID = "5e7a9015ea584af4a04a409297fa5600";
    private static final String FOOD_SETTING_ID = "34e574856be27c7b9bc728882d178785";
    private static final String SWORDS_SETTING_ID = "7a4a714b95dc8bc2f392664665125a88";
    private static final String BOWS_SETTING_ID = "332c3350db3761e83f64e0fe80ee6452";
    private static final String SHIELDS_SETTING_ID = "dc9329955ee5abaee1f77091bdcf4403";
    private static final String POTIONS_SETTING_ID = "cb790f40459f95885fe3900ab7513274";
    private static final String ALLOWED_ITEMS_GROUP_ID = "951607bd184c4293eb7e2355f0acab2b";
    private static final String SLOWDOWN_MULTIPLIER_SETTING_ID =
            "a3b11569a628d65d5e6e2c6ba4ffc9b6096c61fba6a1559568bbb569edfc0753";
}
