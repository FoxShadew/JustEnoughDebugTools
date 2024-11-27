package dev.runefox.jedt.api.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;

public interface DebugRenderEvents {
    Event<DebugRenderEvents.Clear> CLEAR = EventFactory.createArrayBacked(
        DebugRenderEvents.Clear.class,
        callbacks -> () -> {
            for (DebugRenderEvents.Clear callback : callbacks) {
                callback.clear();
            }
        }
    );

    Event<DebugRenderEvents.Render> RENDER = EventFactory.createArrayBacked(
        DebugRenderEvents.Render.class,
        callbacks -> new Render() {
            @Override
            public void render(PoseStack pose, Frustum frustum, MultiBufferSource.BufferSource buffSource, double cameraX, double cameraY, double cameraZ) {
                for (DebugRenderEvents.Render callback : callbacks) {
                    callback.render(pose, frustum, buffSource, cameraX, cameraY, cameraZ);
                }
            }

            @SuppressWarnings("removal")
            @Override
            public void render(PoseStack pose, MultiBufferSource.BufferSource buffSource, double cameraX, double cameraY, double cameraZ) {
            }
        }
    );

    interface Clear {
        void clear();
    }

    interface Render {
        default void render(PoseStack pose, Frustum frustum,  MultiBufferSource.BufferSource buffSource, double cameraX, double cameraY, double cameraZ) {
            render(pose, buffSource, cameraX, cameraY, cameraZ);
        }

        /**
         * @deprecated Implement {{@link #render(PoseStack, Frustum, MultiBufferSource.BufferSource, double, double, double)}}
         * with {@link Frustum} parameter instead.
         */
        @Deprecated(forRemoval = true)
        void render(PoseStack pose, MultiBufferSource.BufferSource buffSource, double cameraX, double cameraY, double cameraZ);
    }
}
