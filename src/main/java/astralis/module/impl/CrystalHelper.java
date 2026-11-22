package astralis.module.impl;

import astralis.event.EventTarget;
import astralis.event.impl.TickEvent;
import astralis.module.Module;
import astralis.setting.BooleanSetting;
import astralis.setting.NumberSetting;
import astralis.setting.Setting;
import astralis.setting.SettingGroup;
import astralis.util.BreakUtil;
import astralis.util.InventoryUtil;
import astralis.util.player.TeamUtil;
import astralis.util.target.TargetSortOrder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import pub.astralis.client.module.core.Category;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CrystalHelper extends Module {
    private final BooleanSetting breakCrystals;
    private final BooleanSetting onlyOwn;
    private final BooleanSetting breakOnlyWhenHolding;
    private final NumberSetting breakAmount;
    private final NumberSetting breakSwapDelay;
    private final BooleanSetting placeCrystals;
    private final BooleanSetting placeOnlyWhenHolding;
    private final BooleanSetting onlyTopFace;
    private final NumberSetting placeAmount;
    private final NumberSetting placeSwapDelay;
    private final BooleanSetting autoSwap;
    private final BooleanSetting requireTarget;
    private final NumberSetting breakMinDamage;
    private final NumberSetting placeMinDamage;
    private final BooleanSetting players;
    private final BooleanSetting monsters;
    private final BooleanSetting animals;
    private final BooleanSetting teammates;
    private final SettingGroup breakSettings;
    private final SettingGroup placeSettings;
    private final SettingGroup targetSettings;
    private long lastBreakTime;
    private long lastPlaceTime;
    private int swapCooldownTicks;
    private final Map<BlockPos, Long> ownPlacements;

    public CrystalHelper() {

        super(MODULE_ID, "", "", Category.Misc);

        this.breakCrystals = new BooleanSetting(BREAK_SETTING_ID, true);

        this.onlyOwn = new BooleanSetting(ONLY_OWN_SETTING_ID, false);

        this.breakOnlyWhenHolding = new BooleanSetting(ONLY_WHEN_HOLDING_SETTING_ID, false);

        this.breakAmount = new NumberSetting(BREAK_AMOUNT_SETTING_ID, 8.0F, 1.0F, 25.0F, 1.0F);

        this.breakSwapDelay =
                new NumberSetting(BREAK_SWAP_DELAY_SETTING_ID, 5.0F, 0.0F, 20.0F, 1.0F);

        this.placeCrystals = new BooleanSetting(PLACE_SETTING_ID, true);

        this.placeOnlyWhenHolding = new BooleanSetting(ONLY_WHEN_HOLDING_SETTING_ID, false);

        this.onlyTopFace = new BooleanSetting(ONLY_TOP_FACE_SETTING_ID, false);

        this.placeAmount = new NumberSetting(PLACE_AMOUNT_SETTING_ID, 6.0F, 1.0F, 25.0F, 1.0F);

        this.placeSwapDelay =
                new NumberSetting(PLACE_SWAP_DELAY_SETTING_ID, 3.0F, 0.0F, 20.0F, 1.0F);

        this.autoSwap = new BooleanSetting(AUTO_SWAP_SETTING_ID, true);

        this.requireTarget = new BooleanSetting(REQUIRE_TARGET_SETTING_ID, false);

        this.breakMinDamage =
                new NumberSetting(BREAK_MIN_DAMAGE_SETTING_ID, 2.0F, 0.0F, 20.0F, 0.1F);

        this.placeMinDamage =
                new NumberSetting(PLACE_MIN_DAMAGE_SETTING_ID, 4.0F, 0.0F, 20.0F, 0.1F);

        this.players = new BooleanSetting(PLAYERS_SETTING_ID, true);

        this.monsters = new BooleanSetting(MONSTERS_SETTING_ID, false);

        this.animals = new BooleanSetting(ANIMALS_SETTING_ID, false);

        this.teammates = new BooleanSetting(TEAMMATES_SETTING_ID, false);

        this.breakSettings =
                new SettingGroup(BREAK_SETTINGS_GROUP_ID)
                        .addValues(
                                this.onlyOwn,
                                this.breakOnlyWhenHolding,
                                this.breakAmount,
                                this.breakSwapDelay);

        this.placeSettings =
                new SettingGroup(PLACE_SETTINGS_GROUP_ID)
                        .addValues(
                                this.placeOnlyWhenHolding,
                                this.placeAmount,
                                this.placeSwapDelay,
                                this.onlyTopFace);

        SettingGroup settingGroup = new SettingGroup(TARGETS_GROUP_ID);
        this.targetSettings =
                settingGroup.addValues(
                        this.players,
                        this.monsters,
                        this.animals,
                        this.teammates,
                        this.breakMinDamage,
                        this.placeMinDamage);
        this.lastBreakTime = 0L;
        this.lastPlaceTime = 0L;
        this.swapCooldownTicks = 0;
        this.ownPlacements = new HashMap<>();

        Setting[] allSettings =
                new Setting[] {
                    this.breakCrystals,
                    this.breakSettings,
                    this.placeCrystals,
                    this.placeSettings,
                    this.autoSwap,
                    this.requireTarget,
                    null
                };

        allSettings[6] = this.targetSettings;
        this.addSettings(allSettings);
        this.breakSettings.hideIf(
                () -> {
                    return !this.breakCrystals.getValue();
                });
        this.placeSettings.hideIf(
                () -> {
                    return !this.placeCrystals.getValue();
                });
        this.targetSettings.hideIf(
                () -> {
                    return !this.requireTarget.getValue();
                });
    }

    private void placeCrystalOnBlock() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - this.lastPlaceTime >= 1000.0 / this.placeAmount.getValue().floatValue()
                && mc.hitResult instanceof BlockHitResult blockHitResult) {
            BlockPos blockPos = blockHitResult.getBlockPos();
            Block block2 = mc.level.getBlockState(blockPos).getBlock();
            if ((block2 == Blocks.OBSIDIAN || block2 == Blocks.BEDROCK)
                    && this.isValidCrystalBase(blockPos)) {
                InteractionHand interactionHand =
                        mc.player.getOffhandItem().getItem() == Items.END_CRYSTAL
                                ? InteractionHand.OFF_HAND
                                : InteractionHand.MAIN_HAND;
                if (mc.player.getMainHandItem().getItem() != Items.END_CRYSTAL
                        && interactionHand == InteractionHand.MAIN_HAND) {
                    return;
                }

                mc.gameMode.useItemOn(mc.player, interactionHand, blockHitResult);
                mc.player.swing(interactionHand);
                this.ownPlacements.put(blockPos, currentTime);
                this.lastPlaceTime = currentTime;
            }
        }
    }

    private void swapToItem(Item item2) {
        int hotbarSlot = InventoryUtil.getHotbarSlot(item2);
        if (hotbarSlot != -1) {
            InventoryUtil.setSlot(hotbarSlot);

            this.swapCooldownTicks = 5;
        }
    }

    private double estimateDamage(Vec3 vector3, Vec3 vector32) {
        double distance = vector3.distanceTo(vector32);
        return distance > 12.0 ? 0.0 : Math.max(0.0, 6.0 * (1.0 - distance / 12.0));
    }

    private boolean attackCrystal() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - this.lastBreakTime < 1000.0 / this.breakAmount.getValue().floatValue()) {
            return false;
        }

        if (mc.hitResult instanceof EntityHitResult entityHitResult) {
            if (!(entityHitResult.getEntity() instanceof EndCrystal)) {
                return false;
            }

            mc.gameMode.attack(mc.player, entityHitResult.getEntity());
            mc.player.swing(InteractionHand.MAIN_HAND);
            this.lastBreakTime = currentTime;
            return true;
        } else {
            return false;
        }
    }

    private boolean hasValidTarget(Vec3 vector3, boolean forPlacement) {
        List<net.minecraft.world.entity.LivingEntity> targets =
                BreakUtil.getTargets(
                        this.animals.getValue(),
                        this.monsters.getValue(),
                        this.teammates.getValue(),
                        true,
                        true,
                        10.0F,
                        TargetSortOrder.DISTANCE,
                        true);
        if (targets != null && !targets.isEmpty()) {
            List<net.minecraft.world.entity.LivingEntity> validTargets =
                    targets.stream()
                            .filter(
                                    livingEntity -> {
                                        if (livingEntity instanceof Player player2) {
                                            boolean teammate = TeamUtil.isTeammate(player2);
                                            if (this.teammates.getValue()) {
                                                return teammate;
                                            } else {
                                                return teammate ? false : this.players.getValue();
                                            }
                                        } else {
                                            return true;
                                        }
                                    })
                            .toList();
            if (validTargets.isEmpty()) {
                return false;
            }

            float minDamage =
                    forPlacement ? this.placeMinDamage.getValue() : this.breakMinDamage.getValue();
            return validTargets.stream()
                    .anyMatch(
                            livingEntity2 -> {
                                return this.estimateDamage(livingEntity2.position(), vector3)
                                        > minDamage;
                            });
        } else {
            return false;
        }
    }

    private boolean isValidCrystalBase(BlockPos blockPos) {
        return mc.level.getBlockState(blockPos.above()).isAir()
                && (mc.level.getBlockState(blockPos).getBlock() == Blocks.OBSIDIAN
                        || mc.level.getBlockState(blockPos).getBlock() == Blocks.BEDROCK);
    }

    private boolean canBreak() {
        if (mc.player.getMainHandItem().getItem() != Items.END_CRYSTAL
                && mc.player.getOffhandItem().getItem() != Items.END_CRYSTAL) {
            return false;
        } else if (this.breakOnlyWhenHolding.getValue() && !mc.options.keyAttack.isDown()) {
            return false;
        } else if (this.swapCooldownTicks > 0
                && this.swapCooldownTicks > this.breakSwapDelay.getValue()) {
            return false;
        } else if (!(mc.hitResult instanceof EntityHitResult entityHitResult
                && entityHitResult.getEntity() instanceof EndCrystal endCrystal)) {
            return false;
        } else {
            if (this.onlyOwn.getValue()
                    && !this.ownPlacements.containsKey(endCrystal.blockPosition().below())) {
                return false;
            }

            Vec3 vector3 = endCrystal.position();
            return !this.requireTarget.getValue() || this.hasValidTarget(vector3, false);
        }
    }

    @Override
    public void onDisable() {
        this.ownPlacements.clear();
    }

    private void autoSwapToCrystal() {
        Item item2 = null;
        if (this.placeCrystals.getValue()
                && mc.hitResult instanceof BlockHitResult blockHitResult) {
            Block block2 = mc.level.getBlockState(blockHitResult.getBlockPos()).getBlock();
            if ((block2 == Blocks.OBSIDIAN || block2 == Blocks.BEDROCK)
                    && this.isValidCrystalBase(blockHitResult.getBlockPos())) {
                item2 = Items.END_CRYSTAL;
            }
        } else if (this.breakCrystals.getValue()
                && mc.hitResult instanceof EntityHitResult entityHitResult
                && entityHitResult.getEntity() instanceof EndCrystal) {
            item2 = Items.END_CRYSTAL;
        }

        if (item2 != null
                && mc.player.getMainHandItem().getItem() != item2
                && mc.player.getOffhandItem().getItem() != item2) {
            this.swapToItem(item2);
        }
    }

    private boolean canPlace() {
        if (mc.player.getMainHandItem().getItem() != Items.END_CRYSTAL
                && mc.player.getOffhandItem().getItem() != Items.END_CRYSTAL) {
            return false;
        }

        if (this.placeOnlyWhenHolding.getValue() && !mc.options.keyUse.isDown()) {
            return false;
        }

        if (this.swapCooldownTicks > this.placeSwapDelay.getValue()) {
            return false;
        }

        if (mc.hitResult instanceof BlockHitResult blockHitResult) {
            if (this.onlyTopFace.getValue() && blockHitResult.getDirection() != Direction.UP) {
                return false;
            } else {
                BlockPos blockPos = blockHitResult.getBlockPos();
                Block block2 = mc.level.getBlockState(blockPos).getBlock();
                if ((block2 == Blocks.OBSIDIAN || block2 == Blocks.BEDROCK)
                        && this.isValidCrystalBase(blockPos)) {
                    Vec3 vector3 = Vec3.atCenterOf(blockPos.above());
                    return !this.requireTarget.getValue() || this.hasValidTarget(vector3, true);
                } else {
                    return false;
                }
            }
        } else {
            return false;
        }
    }

    @EventTarget
    public void onTick(TickEvent tickEvent) {
        this.ownPlacements
                .entrySet()
                .removeIf(
                        entry -> {
                            return System.currentTimeMillis() - entry.getValue() > 10000L;
                        });
        if (this.autoSwap.getValue() && !this.requireTarget.getValue()) {
            this.autoSwapToCrystal();
        }

        if (this.breakCrystals.getValue() && this.canBreak()) {
            this.attackCrystal();
        }

        if (this.placeCrystals.getValue() && this.canPlace()) {
            this.placeCrystalOnBlock();
        }

        if (this.swapCooldownTicks > 0) {
            this.swapCooldownTicks--;
        }
    }

    @Override
    public void onEnable() {
        this.ownPlacements.clear();
        this.lastBreakTime = 0L;
        this.lastPlaceTime = 0L;
        this.swapCooldownTicks = 0;
    }

    private static final String MODULE_ID = "6f1e935c8baf494a1c30d5ca64ca03a3";
    private static final String BREAK_SETTING_ID = "d7091bf253e41e4160ec68f65f42dade";
    private static final String ONLY_OWN_SETTING_ID = "8f1673957d82156971bde3bb3acb4e6d";
    private static final String ONLY_WHEN_HOLDING_SETTING_ID =
            "ba8da886075d4e134dfd9f8bfadc0927777db141a70cd71d410bd5fda5304c1f";
    private static final String BREAK_AMOUNT_SETTING_ID = "48bed1b2beb873fc7d44b53af2bae8cb";
    private static final String BREAK_SWAP_DELAY_SETTING_ID = "89a2d9ef18c4ccef12de440d1431b259";
    private static final String PLACE_SETTING_ID = "74e4a1ce0fdcffad2dac13898c2d7778";
    private static final String ONLY_TOP_FACE_SETTING_ID = "80e864eaf5c0d38f5a4dbf33e7eb6e2a";
    private static final String PLACE_AMOUNT_SETTING_ID = "2290c611c272b93864be41c146c27014";
    private static final String PLACE_SWAP_DELAY_SETTING_ID = "3d2d693c2c26079848239290860a9259";
    private static final String AUTO_SWAP_SETTING_ID = "3c0f29ce868ea52b8876dafbf56537c3";
    private static final String REQUIRE_TARGET_SETTING_ID = "7479d2dd66fd5cdbf6807e1f9dcdbadc";
    private static final String BREAK_MIN_DAMAGE_SETTING_ID = "d1dc18d5c8fa1849638947149c66099e";
    private static final String PLACE_MIN_DAMAGE_SETTING_ID = "bb3cab9ad4cfe1a3eee57435c48b5ac3";
    private static final String PLAYERS_SETTING_ID = "3fd2243ccef5bfd476e1da3a81f32003";
    private static final String MONSTERS_SETTING_ID = "b2e39a7090da277f100b28beac526e55";
    private static final String ANIMALS_SETTING_ID = "2ba8220ce831391798985d9055975347";
    private static final String TEAMMATES_SETTING_ID = "24f4ec6ea6bb6a99768f41759110541a";
    private static final String BREAK_SETTINGS_GROUP_ID = "6dfd2c6104744747b22896b71907dfe0";
    private static final String PLACE_SETTINGS_GROUP_ID = "6771b8be7fd670a25d4b440eb7ce55be";
    private static final String TARGETS_GROUP_ID = "6ef0e612f8bb85c79eef6a9949fb6be4";
}
