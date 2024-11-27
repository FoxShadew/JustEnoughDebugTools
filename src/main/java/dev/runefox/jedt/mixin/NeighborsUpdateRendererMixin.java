package dev.runefox.jedt.mixin;

import net.minecraft.client.renderer.debug.DebugRenderer;
import net.minecraft.client.renderer.debug.NeighborsUpdateRenderer;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Map;

@Mixin(NeighborsUpdateRenderer.class)
public abstract class NeighborsUpdateRendererMixin implements DebugRenderer.SimpleDebugRenderer {
    @Shadow @Final private Map<Long, Map<BlockPos, Integer>> lastUpdate;

    // Mojang forgot about this or somehow removed it.
    @Override
    public void clear() {
        lastUpdate.clear();
    }
}
