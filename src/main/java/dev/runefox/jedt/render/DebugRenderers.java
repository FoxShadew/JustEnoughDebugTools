package dev.runefox.jedt.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.runefox.jedt.DebugClient;
import dev.runefox.jedt.api.render.DebugRenderEvents;
import dev.runefox.jedt.api.render.DebugView;
import dev.runefox.jedt.api.render.DebugViewManager;
import dev.runefox.jedt.api.render.VanillaDebugView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import org.apache.commons.lang3.mutable.MutableBoolean;

public class DebugRenderers {
    public static final MutableBoolean PATHFINDING_ENABLED = new MutableBoolean(false);
    public static final MutableBoolean NEIGHBOR_UPDATES_SHOWN = new MutableBoolean(false);
    public static final MutableBoolean HEIGHTMAPS_SHOWN = new MutableBoolean(false);
    public static final MutableBoolean FLUID_LEVELS_SHOWN = new MutableBoolean(false);
    public static final MutableBoolean COLLISIONS_SHOWN = new MutableBoolean(false);

    public static final PathfinderDebugView PATHFINDING = register(new PathfinderDebugView(PATHFINDING_ENABLED));
    public static final DebugView NEIGHBOR_UPDATES = register(new VanillaDebugView(r -> r.neighborsUpdateRenderer, NEIGHBOR_UPDATES_SHOWN::booleanValue));
    public static final DebugView HEIGHTMAPS = register(new VanillaDebugView(r -> r.heightMapRenderer, HEIGHTMAPS_SHOWN::booleanValue));
    public static final DebugView FLUID_LEVELS = register(new FluidsDebugView(Minecraft.getInstance()));
    public static final DebugView COLLISIONS = register(new VanillaDebugView(r -> r.collisionBoxRenderer, COLLISIONS_SHOWN::booleanValue));

    public static void clear() {
        DebugRenderEvents.CLEAR.invoker().clear();
    }

    public static void render(PoseStack pose, MultiBufferSource.BufferSource buffSource, double cameraX, double cameraY, double cameraZ) {

        MultiBufferSource.BufferSource buffsrc = DebugClient.BUFFERS.getBufferSource();

        DebugRenderEvents.RENDER.invoker().render(pose, buffsrc, cameraX, cameraY, cameraZ);

        RenderSystem.getModelViewStack().pushMatrix();
        RenderSystem.getModelViewStack().identity();
        RenderSystem.applyModelViewMatrix();
        buffsrc.endBatch(RenderType.lines());
        buffsrc.endBatch();
        RenderSystem.getModelViewStack().popMatrix();
        RenderSystem.applyModelViewMatrix();
    }

    private static <V extends DebugView> V register(V view) {
        DebugViewManager.register(view);
        return view;
    }
}
