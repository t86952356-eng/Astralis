package astralis.module.impl.bedwars.mode;

import astralis.event.EventStage;
import astralis.event.EventTarget;
import astralis.event.impl.PacketEvent;
import astralis.event.impl.TickEvent;
import astralis.event.impl.WorldChangeEvent;
import astralis.module.impl.Bedwars;
import astralis.setting.BooleanSetting;
import astralis.setting.Mode;
import astralis.setting.NumberSetting;
import astralis.setting.Setting;
import astralis.setting.SettingGroup;
import astralis.util.ChatUtil;
import astralis.util.inventory.ItemUtil;
import astralis.util.player.TeamUtil;

import com.mojang.datafixers.util.Pair;

import net.minecraft.ChatFormatting;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.TeamColor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class BedwarsAlerts extends Mode<Bedwars> {
    private final BooleanSetting ping;
    private final BooleanSetting showDistance;
    private final NumberSetting alertDelay;
    private final BooleanSetting chainmailArmor;
    private final BooleanSetting ironArmor;
    private final BooleanSetting diamondArmor;
    private final BooleanSetting ironSword;
    private final BooleanSetting diamondSword;
    private final BooleanSetting diamondPickaxe;
    private final BooleanSetting enderPearl;
    private final BooleanSetting bridgeEgg;
    private final BooleanSetting milkBucket;
    private final BooleanSetting fireball;
    private final BooleanSetting bow;
    private final BooleanSetting obsidian;
    private final BooleanSetting tnt;
    private final BooleanSetting rotationals;
    private final BooleanSetting sharpness;
    private final BooleanSetting protection;
    private final SettingGroup settingsGroup;
    private final SettingGroup itemsGroup;
    private final List<AlertItem> trackedItems;
    private final Map<Integer, String> announcedLeggings;
    private final Map<String, Long> heldItemCooldowns;
    private final Set<String> announcedTeamUpgrades;

    public BedwarsAlerts(Bedwars bedwars) {

        super("Alerts", bedwars);

        this.ping = createAlertSetting("Ping");

        this.showDistance = createAlertSetting("Show distance");

        this.alertDelay =
                new NumberSetting("a2d2226c3f2ffe753fd6fa897e2ca07d", 15.0F, 0.0F, 60.0F, 1.0F);

        this.chainmailArmor = createAlertSetting("Chainmail Armor");

        this.ironArmor = createAlertSetting("Iron Armor");

        this.diamondArmor = createAlertSetting("Diamond Armor");

        this.ironSword = createAlertSetting("Iron Sword");

        this.diamondSword = createAlertSetting("Diamond Sword");

        this.diamondPickaxe = createAlertSetting("Diamond Pickaxe");

        this.enderPearl = createAlertSetting("Ender Pearl");

        this.bridgeEgg = createAlertSetting("Bridge Egg");

        this.milkBucket = createAlertSetting("Milk Bucket");

        this.fireball = createAlertSetting("Fireball");

        this.bow = createAlertSetting("Bow");

        this.obsidian = createAlertSetting("Obsidian");

        this.tnt = createAlertSetting("TNT");

        this.rotationals = createAlertSetting("Rotationals");

        this.sharpness = createAlertSetting("Sharpness");

        this.protection = createAlertSetting("Protection");

        this.settingsGroup =
                new SettingGroup("e1b6ffd7ce011c799b9f90fcf88693e3")
                        .addValues(this.ping, this.showDistance, this.alertDelay);

        SettingGroup settingGroup =
                new SettingGroup("85bcb947d24612694894cee81e6b2ae8").collapsed();
        Setting[] itemSettings =
                new Setting[] {
                    this.chainmailArmor,
                    this.ironArmor,
                    this.diamondArmor,
                    this.ironSword,
                    this.diamondSword,
                    this.diamondPickaxe,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
                };

        itemSettings[6] = this.enderPearl;

        itemSettings[7] = this.bridgeEgg;

        itemSettings[8] = this.milkBucket;

        itemSettings[9] = this.fireball;

        itemSettings[10] = this.bow;

        itemSettings[11] = this.obsidian;

        itemSettings[12] = this.tnt;

        itemSettings[13] = this.rotationals;

        itemSettings[14] = this.sharpness;

        itemSettings[15] = this.protection;
        this.itemsGroup = settingGroup.addValues(itemSettings);

        AlertItem[] alertItems = new AlertItem[26];

        alertItems[0] =
                AlertItem.forArmor("chainmail_leggings", "§fChainmail Armor", this.chainmailArmor);

        alertItems[1] = AlertItem.forArmor("iron_leggings", "§fIron Armor", this.ironArmor);

        alertItems[2] =
                AlertItem.forArmor("diamond_leggings", "§bDiamond Armor", this.diamondArmor);

        alertItems[3] = AlertItem.forItem("iron_sword", "§fIron Sword", this.ironSword);

        alertItems[4] = AlertItem.forItem("diamond_sword", "§bDiamond Sword", this.diamondSword);

        alertItems[5] =
                AlertItem.forItem("diamond_pickaxe", "§bDiamond Pickaxe", this.diamondPickaxe);

        alertItems[6] = AlertItem.forItem("ender_pearl", "§3Ender Pearl", this.enderPearl);

        alertItems[7] = AlertItem.forItem("egg", "§eBridge Egg", this.bridgeEgg);

        alertItems[8] = AlertItem.forItem("milk_bucket", "§fMilk Bucket", this.milkBucket);

        alertItems[9] = AlertItem.forItem("fire_charge", "§6Fireball", this.fireball);

        alertItems[10] = AlertItem.forItem("obsidian", "§5Obsidian", this.obsidian);

        alertItems[11] = AlertItem.forItem("tnt", "§cT§fN§cT", this.tnt);

        alertItems[12] = AlertItem.forDisplayName("Bow", "§2Bow", this.bow);

        alertItems[13] = AlertItem.forItem("prismarine_shard", "§3Block Zapper", this.rotationals);

        alertItems[14] =
                AlertItem.forDisplayName(
                        "Speed II Potion (45 seconds)", "§bSpeed Potion", this.rotationals);

        alertItems[15] =
                AlertItem.forDisplayName(
                        "Jump V Potion (45 seconds)", "§aJump Boost Potion", this.rotationals);

        alertItems[16] =
                AlertItem.forDisplayName(
                        "Invisibility Potion (30 seconds)",
                        "§fInvisibility Potion",
                        this.rotationals);

        alertItems[17] =
                AlertItem.forDisplayName("§cDream Defender", "§fIron Golem", this.rotationals);

        alertItems[18] =
                AlertItem.forDisplayName("Machine Gun Bow", "§4Machine Gun Bow", this.rotationals);

        alertItems[19] =
                AlertItem.forDisplayName(
                        "Charlie the Unicorn", "§dCharlie the Unicorn", this.rotationals);

        alertItems[20] = AlertItem.forDisplayName("Ice Bridge", "§bIce Bridge", this.rotationals);

        alertItems[21] =
                AlertItem.forDisplayName("Sleeping Dust", "§cSleeping Dust", this.rotationals);

        alertItems[22] =
                AlertItem.forDisplayName(
                        "Unstable Teleportation Device",
                        "§eUnstable Teleportation Device",
                        this.rotationals);

        alertItems[23] =
                AlertItem.forDisplayName("Devastator Bow", "§2Devastator Bow", this.rotationals);

        alertItems[24] =
                AlertItem.forDisplayName(
                        "Miracle of the Stars", "§eMiracle of the Stars", this.rotationals);

        alertItems[25] =
                AlertItem.forDisplayName("Mystic Mirror", "§dMystic Mirror", this.rotationals);
        this.trackedItems = List.of(alertItems);
        this.announcedLeggings = new HashMap<>();
        this.heldItemCooldowns = new HashMap<>();
        this.announcedTeamUpgrades = new HashSet<>();
    }

    @EventTarget
    public void onPacket(PacketEvent packetEvent) {
        if (packetEvent.getStage() == EventStage.RECEIVE
                && packetEvent.getPacket()
                        instanceof ClientboundSetEquipmentPacket clientboundSetEquipmentPacket) {
            if (Bedwars.isInBedwarsGame()) {
                if (mc.level.getEntity(clientboundSetEquipmentPacket.getEntity())
                                instanceof Player player2
                        && !this.shouldIgnorePlayer(player2)) {
                    for (Pair equipmentEntry : clientboundSetEquipmentPacket.getSlots()) {
                        this.checkTeamUpgrades(
                                player2,
                                (EquipmentSlot) equipmentEntry.getFirst(),
                                (ItemStack) equipmentEntry.getSecond());
                        if (equipmentEntry.getFirst() == EquipmentSlot.MAINHAND) {
                            this.checkHeldItem(player2, (ItemStack) equipmentEntry.getSecond());
                        } else if (equipmentEntry.getFirst() == EquipmentSlot.LEGS) {
                            this.checkLeggings(player2, (ItemStack) equipmentEntry.getSecond());
                        }
                    }
                }
            }
        }
    }

    private AlertItem findHeldItem(ItemStack itemStack) {

        if (itemStack != null && !itemStack.isEmpty()) {
            AlertItem bowItem = null;
            String hoverName = itemStack.getHoverName().getString();

            for (AlertItem nameCandidate : this.trackedItems) {
                if (!nameCandidate.armor() && nameCandidate.byDisplayName()) {
                    if (nameCandidate.setting() == this.bow) {
                        bowItem = nameCandidate;
                    } else if (nameCandidate.matchesName(hoverName)
                            && nameCandidate.setting().getValue()) {
                        return nameCandidate;
                    }
                }
            }

            String heldItemKey = getItemKey(itemStack);
            if (bowItem != null && bowItem.setting().getValue()) {

                if ("bow".equals(heldItemKey)) {
                    return bowItem;
                }
            }

            for (AlertItem keyCandidate : this.trackedItems) {
                if (!keyCandidate.armor()
                        && !keyCandidate.byDisplayName()
                        && keyCandidate.setting().getValue()
                        && keyCandidate.matchesItemKey(heldItemKey)) {
                    return keyCandidate;
                }
            }

            return null;
        } else {
            return null;
        }
    }

    public SettingGroup getSettingsGroup() {
        return this.settingsGroup;
    }

    private AlertItem findArmorItem(ItemStack itemStack) {
        if (itemStack != null && !itemStack.isEmpty()) {
            String itemKey = getItemKey(itemStack);

            for (AlertItem candidate : this.trackedItems) {
                if (candidate.armor()
                        && candidate.setting().getValue()
                        && candidate.matchesItemKey(itemKey)) {
                    return candidate;
                }
            }

            return null;
        } else {
            return null;
        }
    }

    private static String getItemKey(ItemStack itemStack) {
        return itemStack != null && !itemStack.isEmpty()
                ? itemStack.getItem().builtInRegistryHolder().key().identifier().getPath()
                : "";
    }

    private static BooleanSetting createAlertSetting(String settingName) {
        return new BooleanSetting(settingName, true);
    }

    @EventTarget
    public void onTick(TickEvent tickEvent) {
        if (tickEvent.getStage() == EventStage.POST) {
            if (!Bedwars.isInBedwarsGame()) {
                this.resetTracking();
            } else {

                if (mc.player.tickCount % 100 == 0) {
                    for (Player player2 : mc.level.players()) {
                        if (!this.shouldIgnorePlayer(player2)) {
                            this.checkHeldItem(player2, player2.getMainHandItem());
                            this.checkLeggings(player2, player2.getItemBySlot(EquipmentSlot.LEGS));
                        }
                    }
                }
            }
        }
    }

    private void sendAlert(
            Player player2, String action, String itemLabel, boolean includeDistance) {
        StringBuilder nameBuilder =
                new StringBuilder()
                        .append(getTeamColorCode(player2))
                        .append(this.getFormattedPlayerName(player2));

        StringBuilder actionBuilder = nameBuilder.append(" §7").append(action);

        StringBuilder messageBuilder = actionBuilder.append(" §r").append(itemLabel);
        if (includeDistance && this.showDistance.getValue()) {
            StringBuilder distanceBuilder =
                    messageBuilder
                            .append(" §7(§f")
                            .append(Math.round(player2.distanceTo(mc.player)));
            distanceBuilder.append("m§7)");
        }

        ChatUtil.print(messageBuilder.toString());
        if (this.ping.getValue()) {
            mc.getSoundManager()
                    .play(
                            SimpleSoundInstance.forUI(
                                    SoundEvents.NOTE_BLOCK_PLING.value(), 1.8F, 1.0F));
        }
    }

    private String getFormattedPlayerName(Player player2) {
        if (mc.getConnection() == null) {
            return player2.getGameProfile().name();
        }

        PlayerInfo playerInfo = mc.getConnection().getPlayerInfo(player2.getUUID());
        if (playerInfo == null) {
            return player2.getGameProfile().name();
        }

        if (playerInfo.getTabListDisplayName() != null) {
            String tabListName = playerInfo.getTabListDisplayName().getString();
            if (!tabListName.isBlank()) {
                return tabListName;
            }
        }

        String teamFormattedName =
                PlayerTeam.formatNameForTeam(
                                playerInfo.getTeam(),
                                Component.literal(playerInfo.getProfile().name()))
                        .getString();
        return teamFormattedName.isBlank() ? player2.getGameProfile().name() : teamFormattedName;
    }

    @EventTarget
    public void onWorldChange(WorldChangeEvent worldChangeEvent) {
        this.resetTracking();
    }

    private static String getTeamColorCode(Player player2) {

        PlayerTeam playerTeam = player2.getTeam();
        Optional teamColor = playerTeam == null ? Optional.empty() : playerTeam.getColor();
        if (teamColor.isEmpty()) {

            return "§f";
        }

        try {
            return ChatFormatting.valueOf(
                            ((TeamColor) teamColor.get())
                                    .getSerializedName()
                                    .toUpperCase(Locale.ROOT))
                    .toString();
        } catch (IllegalArgumentException ignored) {

            return "§f";
        }
    }

    private void checkLeggings(Player player2, ItemStack itemStack) {
        AlertItem armorItem = this.findArmorItem(itemStack);
        if (armorItem != null
                && armorItem.setting().getValue()
                && !armorItem.dedupeKey().equals(this.announcedLeggings.get(player2.getId()))) {
            this.announcedLeggings.put(player2.getId(), armorItem.dedupeKey());

            this.sendAlert(player2, "purchased", armorItem.label(), true);
        }
    }

    private void resetTracking() {
        this.announcedLeggings.clear();
        this.heldItemCooldowns.clear();
        this.announcedTeamUpgrades.clear();
    }

    @Override
    public void onDisable() {
        this.resetTracking();
    }

    private void checkTeamUpgrades(
            Player player2, EquipmentSlot equipmentSlot, ItemStack itemStack) {

        String teamColorCode = getTeamColorCode(player2);

        if (!"§f".equals(teamColorCode)) {
            String teamName = teamColorCode.substring(1);
            if (equipmentSlot == EquipmentSlot.LEGS
                    && this.protection.getValue()
                    && ItemUtil.getEnchantmentLevel(itemStack, Enchantments.PROTECTION) > 0) {

                if (this.announcedTeamUpgrades.add("protection:" + teamName)) {

                    this.sendAlert(player2, "purchased", "§bReinforced Armor", false);
                    return;
                }
            }

            if (equipmentSlot == EquipmentSlot.MAINHAND
                    && this.sharpness.getValue()
                    && ItemUtil.getEnchantmentLevel(itemStack, Enchantments.SHARPNESS) > 0) {
                String heldItemKey = getItemKey(itemStack);

                if (heldItemKey.endsWith("_sword")) {

                    if (this.announcedTeamUpgrades.add("sharpness:" + teamName)) {

                        this.sendAlert(player2, "purchased", "§bSharpened Swords", false);
                    }
                }
            }
        }
    }

    private boolean shouldIgnorePlayer(Player player2) {
        return player2 == null
                || player2 == mc.player
                || player2.getGameProfile()
                        .name()
                        .equalsIgnoreCase(mc.player.getGameProfile().name())
                || TeamUtil.isTeammate(player2);
    }

    private void checkHeldItem(Player player2, ItemStack itemStack) {

        AlertItem alertItem = this.findHeldItem(itemStack);
        if (alertItem != null && alertItem.setting().getValue()) {
            String label = alertItem.label();
            String dedupeKey = alertItem.dedupeKey();
            String heldItemKey = getItemKey(itemStack);
            if (alertItem.setting() == this.bow) {

                if ("bow".equals(heldItemKey)) {
                    int enchantmentCount = itemStack.getEnchantments().size();
                    if (enchantmentCount >= 2) {

                        label = "§6Punch Bow";

                        dedupeKey = dedupeKey + ":punch";
                    } else if (enchantmentCount == 1) {

                        label = "§dPower Bow";

                        dedupeKey = dedupeKey + ":power";
                    } else {

                        dedupeKey = dedupeKey + ":normal";
                    }
                }
            }

            String cooldownKey = player2.getId() + ":" + dedupeKey;
            long now = System.currentTimeMillis();
            Long cooldownEnd = this.heldItemCooldowns.get(cooldownKey);
            if (cooldownEnd == null || now > cooldownEnd) {
                this.heldItemCooldowns.put(
                        cooldownKey, now + (long) (this.alertDelay.getValue() * 1000.0F));

                this.sendAlert(player2, "is holding", label, true);
            }
        }
    }

    public SettingGroup getItemsGroup() {
        return this.itemsGroup;
    }
}
