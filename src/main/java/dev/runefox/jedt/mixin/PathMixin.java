package dev.runefox.jedt.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.Target;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Set;

@Mixin(Path.class)
public abstract class PathMixin {
    @Shadow
    abstract void setDebug(Node[] nodes, Node[] nodes2, Set<Target> set);

    @Inject(
        method = "<init>",
        at = @At("TAIL")
    )
    private void onNew(List<Node> nodes, BlockPos dest, boolean reached, CallbackInfo ci) {
        setDebug(new Node[0], new Node[0], Set.of(new Target(dest.getX(), dest.getY(), dest.getZ())));
    }
}
