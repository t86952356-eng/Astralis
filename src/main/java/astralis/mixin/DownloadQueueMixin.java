package astralis.mixin;

import com.astralis.scripting.InstanceAccess;

import net.minecraft.server.packs.DownloadQueue;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.nio.file.Path;

@Mixin(DownloadQueue.class)
public abstract class DownloadQueueMixin implements InstanceAccess {
    @Shadow @Final private Path cacheDir;
}
