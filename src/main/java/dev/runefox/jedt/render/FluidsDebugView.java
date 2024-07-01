package dev.runefox.jedt.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.runefox.jedt.api.render.DebugView;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.debug.DebugRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

@Environment(EnvType.CLIENT)
public record FluidsDebugView(Minecraft client) implements DebugView {
    @Override
    public void clear() {

    }

    @SuppressWarnings("UnnecessaryLocalVariable")
    @Override
    public void render(PoseStack pose, MultiBufferSource buffSrc, double cameraX, double cameraY, double cameraZ) {
        assert client.player != null;

        BlockPos playerPos = client.player.blockPosition();
        LevelAccessor world = client.player.level();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(0, 1, 0, 0.75f);
        RenderSystem.lineWidth(1);
        VertexConsumer buff = buffSrc.getBuffer(RenderType.lines());

        pose.pushPose();
        pose.translate(-cameraX, -cameraY, -cameraZ);

        PoseStack.Pose lastPose = pose.last();
        Matrix4f matrix = lastPose.pose();
        Matrix3f nmatrix = lastPose.normal();
        for (BlockPos pos : BlockPos.betweenClosed(playerPos.offset(-8, -8, -8), playerPos.offset(8, 8, 8))) {
            FluidState fluid = world.getFluidState(pos);
            if (fluid.getAmount() <= 0) {
                continue;
            }
            float fheight = fluid.getHeight(world, pos);

            double height = pos.getY() + fheight;

            LevelRenderer.renderLineBox(pose, buff, pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, height + 0.01, pos.getZ() + 1, 1, 1, 1, 1);

            Vec3 flow = fluid.getFlow(world, pos);
            if (fheight < 0.4) {
                flow = flow.scale(fheight);
            }

            float x1 = pos.getX() + 0.5f;
            float y1 = pos.getY() + fheight + 0.06f;
            float z1 = pos.getZ() + 0.5f;
            float x2 = x1 + (float) flow.x * 0.8f;
            float y2 = y1;
            float z2 = z1 + (float) flow.z * 0.8f;

            try { // TODO
                buff.addVertex(matrix, x1, y1, z1).setColor(0f, 0f, 1f, 1f).setNormal(lastPose, 0, 1, 0);
                buff.addVertex(matrix, x2, y2, z2).setColor(0f, 0f, 1f, 1f).setNormal(lastPose, 0, 1, 0);
            } catch (IllegalStateException ignored) {
            }
        }

        pose.popPose();

        for (BlockPos pos : BlockPos.betweenClosed(playerPos.offset(-8, -8, -8), playerPos.offset(8, 8, 8))) {
            FluidState fluid = world.getFluidState(pos);
            if (fluid.getAmount() <= 0) {
                continue;
            }
            DebugRenderer.renderFloatingText(pose, buffSrc, String.valueOf(fluid.getAmount()), pos.getX() + 0.5, pos.getY() + fluid.getHeight(world, pos), pos.getZ() + 0.5, 0xFF000000);
        }

        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderSystem.disableBlend();
    }

    @Override
    public boolean isEnabled() {
        return DebugRenderers.FLUID_LEVELS_SHOWN.booleanValue();
    }
}
