package astralis.module.impl.freecam;

import astralis.module.impl.Freecam;

import com.astralis.scripting.InstanceAccess;

import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.Vec3;

public final class FreecamTeleportUtil {

    private FreecamTeleportUtil() {}

    static void teleportToCrosshair(Freecam freecam) {
        Vec3 vector3 = Vec3.directionFromRotation(freecam.getPitch(), freecam.getYaw());
        Vec3 vector32 = freecam.getPosition();
        Vec3 vector33 = vector32.add(vector3.scale(300.0));
        BlockHitResult blockHitResult =
                InstanceAccess.mc.level.clip(
                        new ClipContext(
                                vector32,
                                vector33,
                                Block.OUTLINE,
                                Fluid.NONE,
                                InstanceAccess.mc.player));
        if (blockHitResult.getType() != Type.MISS) {
            Vec3 vector34 = blockHitResult.getLocation().subtract(vector3.scale(0.1));
            freecam.setPosition(vector34);
            freecam.setPreviousPosition(vector34);
        }
    }
}
