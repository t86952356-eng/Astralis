package astralis.module.impl;

import astralis.event.EventTarget;
import astralis.event.impl.BlockCollisionShapeEvent;
import astralis.module.Module;
import astralis.setting.BooleanSetting;

import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.phys.shapes.Shapes;

import pub.astralis.client.module.core.Category;

public class AntiHazard extends Module {
    private final BooleanSetting antiPowderSnow;
    private final BooleanSetting antiFire;

    public AntiHazard() {

        super(MODULE_ID, "", "", Category.Misc);

        this.antiPowderSnow = new BooleanSetting(ANTI_POWDER_SNOW_SETTING_ID, true);

        this.antiFire = new BooleanSetting(ANTI_FIRE_SETTING_ID, true);
        this.addSettings(this.antiPowderSnow, this.antiFire);
    }

    public BooleanSetting getAntiPowderSnowSetting() {
        return this.antiPowderSnow;
    }

    @EventTarget
    public void onBlockCollisionShape(BlockCollisionShapeEvent event) {
        if (event.getBlockState().getBlock() instanceof FireBlock && this.antiFire.getValue()) {
            event.setShape(Shapes.block());
        }
    }

    private static final String MODULE_ID = "4be09b0ecd7b1d0422b28cfa6d512c3d";
    private static final String ANTI_POWDER_SNOW_SETTING_ID = "5d579f2e11c7cf9657e354f9a912190f";
    private static final String ANTI_FIRE_SETTING_ID = "485c0e72293689846cdef4412a90a25e";
}
