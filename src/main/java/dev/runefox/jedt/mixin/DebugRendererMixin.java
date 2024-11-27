package dev.runefox.jedt.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.runefox.jedt.render.DebugRenderers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.debug.DebugRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DebugRenderer.class)
public class DebugRendererMixin {
    @Inject(method = "clear", at = @At("HEAD"))
    private void onReset(CallbackInfo info) {
        DebugRenderers.clear();
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void onRender(PoseStack pose, Frustum frustum, MultiBufferSource.BufferSource buffSource, double cameraX, double cameraY, double cameraZ, CallbackInfo info) {
        pose.pushPose();
        DebugRenderers.render(pose, frustum, buffSource, cameraX, cameraY, cameraZ);
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void onRenderPost(PoseStack pose, Frustum frustum, MultiBufferSource.BufferSource buffSource, double cameraX, double cameraY, double cameraZ, CallbackInfo info) {
        pose.popPose();
    }
}
