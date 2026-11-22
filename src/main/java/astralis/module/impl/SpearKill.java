package astralis.module.impl;

import astralis.event.EventTarget;
import astralis.event.impl.EntityMoveEvent;
import astralis.module.Module;
import astralis.rotation.RotationManager;
import astralis.setting.BooleanSetting;
import astralis.setting.NumberSetting;
import astralis.setting.SettingGroup;
import astralis.util.BreakUtil;
import astralis.util.InventoryUtil;
import astralis.util.RotationUtil;
import astralis.util.target.TargetSortOrder;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import pub.astralis.client.module.core.Category;

import java.util.List;

public class SpearKill extends Module {
    private final NumberSetting range;
    private final NumberSetting motion;
    private final BooleanSetting elytraOnly;
    private final BooleanSetting fallFlyingOnly;
    private final BooleanSetting monsters;
    private final BooleanSetting friendly;
    private final BooleanSetting invisible;
    private final BooleanSetting throughWalls;
    private final BooleanSetting teams;

    public SpearKill() {

        super(MODULE_ID, "", "", Category.Misc);

        this.range = new NumberSetting(RANGE_SETTING_ID, 100.0F, 1.0F, 200.0F, 1.0F);

        this.motion = new NumberSetting(MOTION_SETTING_ID, 1.0F, 0.1F, 10.0F, 0.1F);

        this.elytraOnly = new BooleanSetting(ELYTRA_ONLY_SETTING_ID, true);

        this.fallFlyingOnly = new BooleanSetting(FALL_FLYING_ONLY_SETTING_ID, true);

        this.monsters = new BooleanSetting(MONSTERS_SETTING_ID, false);

        this.friendly = new BooleanSetting(FRIENDLY_SETTING_ID, false);

        this.invisible = new BooleanSetting(INVISIBLE_SETTING_ID, false);

        this.throughWalls = new BooleanSetting(THROUGH_WALLS_SETTING_ID, false);

        this.teams = new BooleanSetting(TEAMS_SETTING_ID, false);
        SettingGroup settingGroup =
                new SettingGroup(TARGETS_GROUP_ID)
                        .addValues(
                                this.monsters,
                                this.friendly,
                                this.invisible,
                                this.throughWalls,
                                this.teams);
        SettingGroup settingGroup2 =
                new SettingGroup(CHECKS_GROUP_ID).addValues(this.elytraOnly, this.fallFlyingOnly);
        this.addSettings(this.range, this.motion, settingGroup2, settingGroup);
    }

    @EventTarget
    public void onEntityMove(EntityMoveEvent event) {
        if (mc.options.keyUse.isDown()
                && InventoryUtil.isSpear(mc.player.getInventory().getSelectedItem())) {
            if (this.elytraOnly.getValue()
                    && !mc.player.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA)) {
                return;
            }

            if (this.fallFlyingOnly.getValue() && !mc.player.isFallFlying()) {
                return;
            }

            List targets =
                    BreakUtil.getTargets(
                            this.friendly.getValue(),
                            this.monsters.getValue(),
                            this.teams.getValue(),
                            this.invisible.getValue(),
                            this.throughWalls.getValue(),
                            this.range.getValue(),
                            TargetSortOrder.DISTANCE,
                            true);
            if (targets == null || targets.isEmpty()) {
                return;
            }

            LivingEntity livingEntity = (LivingEntity) targets.getFirst();
            float[] rotations = RotationUtil.getRotationsToEntity(livingEntity);
            RotationManager.getInstance().submit(rotations[0], rotations[1], 360.0F, 360.0F);
            Vec3 vector3 = livingEntity.position().subtract(mc.player.position());
            Vec3 vector32 = vector3.normalize();
            double speed = this.motion.getValue().floatValue();
            boolean isFar = vector3.length() > 8.0;
            Vec3 vector33 =
                    new Vec3(
                            vector32.x * speed * (isFar ? 1 : -1),
                            vector32.y * speed * (isFar ? 1 : -1),
                            vector32.z * speed * (isFar ? 1 : -1));
            event.setMovement(vector33);
        }
    }

    private static final String MODULE_ID = "8268841356de1dd12244a8338c2cc6e5";
    private static final String RANGE_SETTING_ID = "4ed84ffdb32f200ee53f8c90db3cab6d";
    private static final String MOTION_SETTING_ID = "04b3179de5d88360b561d10861012a5c";
    private static final String ELYTRA_ONLY_SETTING_ID = "07ab1078cb21f8d5ecc5e693ce8538ef";
    private static final String FALL_FLYING_ONLY_SETTING_ID = "63f928815be1bdaeb2435ae858d971ca";
    private static final String MONSTERS_SETTING_ID = "b2e39a7090da277f100b28beac526e55";
    private static final String FRIENDLY_SETTING_ID = "3591a85a0cad9ff80c1df14006606ca8";
    private static final String INVISIBLE_SETTING_ID = "a63ea6c796b6ef2ae55074f6ee3726db";
    private static final String THROUGH_WALLS_SETTING_ID = "4fb927fdbee561036f15bca7ddf5b4c4";
    private static final String TEAMS_SETTING_ID = "46334632fc765d791744b4e27384ec4d";
    private static final String TARGETS_GROUP_ID = "6ef0e612f8bb85c79eef6a9949fb6be4";
    private static final String CHECKS_GROUP_ID = "6b0341c82f929a90d2899b657641d3e1";
}
