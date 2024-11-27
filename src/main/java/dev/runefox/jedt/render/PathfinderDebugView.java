package dev.runefox.jedt.render;

import com.google.common.collect.Maps;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import dev.runefox.jedt.api.render.DebugView;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.Util;
import net.minecraft.client.renderer.CoreShaders;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.debug.DebugRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import org.apache.commons.lang3.mutable.MutableBoolean;

import java.util.Locale;
import java.util.Map;

@Environment(EnvType.CLIENT)
public class PathfinderDebugView implements DebugView {
    private static final long TIMEOUT = 5000;
    private static final float MAX_RENDER_DIST = 80f;
    private static final float LINE_WIDTH = 6 / 256f;
    private static final boolean SHOW_OPEN_CLOSED = true;
    private static final boolean SHOW_OPEN_CLOSED_COST_MALUS = false;
    private static final boolean SHOW_OPEN_CLOSED_NODE_TYPE_WITH_TEXT = false;
    private static final boolean SHOW_OPEN_CLOSED_NODE_TYPE_WITH_BOX = true;
    private static final boolean SHOW_GROUND_LABELS = true;
    private static final float TEXT_SCALE = 0.02f;

    private final MutableBoolean enabledConfig;
    private final Map<Integer, Path> paths = Maps.newHashMap();
    private final Map<Integer, Float> pathRanges = Maps.newHashMap();
    private final Map<Integer, Long> creationTimes = Maps.newHashMap();

    public PathfinderDebugView(MutableBoolean enabledConfig) {
        this.enabledConfig = enabledConfig;
    }

    public void addPath(int id, Path path, float range) {
        this.paths.put(id, path);
        this.creationTimes.put(id, Util.getMillis());
        this.pathRanges.put(id, range);
    }

    @Override
    public void clear() {

    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffSrc, double cameraX, double cameraY, double cameraZ) {
        if (!paths.isEmpty()) {
            long time = Util.getMillis();

            for (Integer integer : paths.keySet()) {
                Path path = paths.get(integer);
                float range = pathRanges.get(integer);
                renderPath(pose, buffSrc, path, range, SHOW_OPEN_CLOSED, SHOW_GROUND_LABELS, cameraX, cameraY, cameraZ);
            }

            for (int id : creationTimes.keySet().toArray(new Integer[0])) {
                if (time - creationTimes.get(id) > TIMEOUT) {
                    paths.remove(id);
                    creationTimes.remove(id);
                }
            }
        }
    }

    @Override
    public boolean isEnabled() {
        return enabledConfig.booleanValue();
    }

    public static void renderPath(PoseStack pose, MultiBufferSource buffSrc, Path path, float range, boolean renderOpenClosedSet, boolean renderDistance, double camX, double camY, double camZ) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1, 1, 1, 1);
        // RenderSystem.disableTexture();
        RenderSystem.lineWidth(6);
        doRenderPath(pose, buffSrc, path, range, renderOpenClosedSet, renderDistance, camX, camY, camZ);
        // RenderSystem.enableTexture();
        RenderSystem.disableBlend();
    }

    private static void doRenderPath(PoseStack pose, MultiBufferSource buffSrc, Path path, float range, boolean renderOpenClosedSet, boolean renderDistance, double camX, double camY, double camZ) {
        renderPathLine(path, camX, camY, camZ);
        BlockPos blockPos = path.getTarget();

        if (distanceToCamera(blockPos, camX, camY, camZ) <= MAX_RENDER_DIST) {
            DebugRenderer.renderFilledBox(
                pose, buffSrc,
                new AABB(
                    blockPos.getX() + 0.25, blockPos.getY() + 0.25, blockPos.getZ() + 0.25,
                    blockPos.getX() + 0.75, blockPos.getY() + 0.75, blockPos.getZ() + 0.75
                ).move(-camX, -camY, -camZ),
                0f, 1f, 0f, 0.5f
            );

            for (int i = 0; i < path.getNodeCount(); i++) {
                Node node = path.getNode(i);
                if (distanceToCamera(node.asBlockPos(), camX, camY, camZ) <= MAX_RENDER_DIST) {
                    float red = i == path.getNextNodeIndex() ? 1 : 0;
                    float blue = i == path.getNextNodeIndex() ? 0 : 1;
                    DebugRenderer.renderFilledBox(
                        pose, buffSrc,
                        new AABB(
                            node.x + 0.5 - range, node.y + 0.01 * i, node.z + 0.5 - range,
                            node.x + 0.5 + range, node.y + 0.25 + 0.01 * i, node.z + 0.5 + range
                        ).move(-camX, -camY, -camZ),
                        red, 0, blue, 0.5f
                    );
                }
            }
        }

        if (renderOpenClosedSet) {
            for (Node node : path.debugData().closedSet()) {
                if (distanceToCamera(node.asBlockPos(), camX, camY, camZ) <= MAX_RENDER_DIST) {
                    DebugRenderer.renderFilledBox(
                        pose, buffSrc,
                        new AABB(
                            node.x + 0.5 - range / 2, node.y + 0.01, node.z + 0.5 - range / 2,
                            node.x + 0.5 + range / 2, node.y + 0.1, node.z + 0.5 + range / 2
                        ).move(-camX, -camY, -camZ),
                        1, 0.8f, 0.8f, 0.5f
                    );
                    if (SHOW_OPEN_CLOSED_NODE_TYPE_WITH_TEXT) {
                        DebugRenderer.renderFloatingText(
                            pose, buffSrc,
                            String.format("%s", node.type),
                            node.x + 0.5, node.y + 0.75, node.z + 0.5,
                            0xFFFFFFFF, TEXT_SCALE, true, 0, true
                        );
                    }
                    if (SHOW_OPEN_CLOSED_COST_MALUS) {
                        DebugRenderer.renderFloatingText(
                            pose, buffSrc,
                            String.format(Locale.ROOT, "%.2f", node.costMalus),
                            node.x + 0.5, node.y + 0.25, node.z + 0.5,
                            0xFFFFFFFF, TEXT_SCALE, true, 0, true
                        );
                    }
                }
            }

            for (Node node : path.debugData().openSet()) {
                if (distanceToCamera(node.asBlockPos(), camX, camY, camZ) <= MAX_RENDER_DIST) {
                    DebugRenderer.renderFilledBox(
                        pose, buffSrc,
                        new AABB(
                            node.x + 0.5 - range / 2, node.y + 0.01, node.z + 0.5 - range / 2.0,
                            node.x + 0.5 + range / 2, node.y + 0.1, node.z + 0.5 + range / 2.0
                        ).move(-camX, -camY, -camZ),
                        0.8f, 1, 1, 0.5f
                    );
                    if (SHOW_OPEN_CLOSED_NODE_TYPE_WITH_TEXT) {
                        DebugRenderer.renderFloatingText(
                            pose, buffSrc,
                            String.format("%s", node.type),
                            node.x + 0.5, node.y + 0.75, node.z + 0.5,
                            0xFFFFFFFF, TEXT_SCALE, true, 0, true
                        );
                    }
                    if (SHOW_OPEN_CLOSED_COST_MALUS) {
                        DebugRenderer.renderFloatingText(
                            pose, buffSrc,
                            String.format(Locale.ROOT, "%.2f", node.costMalus),
                            node.x + 0.5, node.y + 0.25, node.z + 0.5,
                            0xFFFFFFFF, TEXT_SCALE, true, 0, true
                        );
                    }
                }
            }
        }

        if (renderDistance) {
            for (int i = 0; i < path.getNodeCount(); i++) {
                Node node = path.getNode(i);
                if (distanceToCamera(node.asBlockPos(), camX, camY, camZ) <= MAX_RENDER_DIST) {
                    DebugRenderer.renderFloatingText(
                        pose, buffSrc,
                        String.format("%s", node.type),
                        node.x + 0.5, node.y + 0.75, node.z + 0.5,
                        0xFFFFFFFF, TEXT_SCALE, true, 0, true
                    );
                    DebugRenderer.renderFloatingText(
                        pose, buffSrc,
                        String.format(Locale.ROOT, "%.2f", node.costMalus),
                        node.x + 0.5, node.y + 0.25, node.z + 0.5,
                        0xFFFFFFFF, TEXT_SCALE, true, 0, true
                    );
                }
            }
        }

    }

    public static void renderPathLine(Path path, double camX, double camY, double camZ) {
        RenderSystem.disableCull();
        RenderSystem.setShader(CoreShaders.POSITION_COLOR);

        Tesselator tess = Tesselator.getInstance();
        BufferBuilder buff = tess.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        Node lastNode = null;
        for (int i = 0; i < path.getNodeCount(); ++i) {
            Node node = path.getNode(i);
            if (lastNode != null && distanceToCamera(node.asBlockPos(), camX, camY, camZ) <= MAX_RENDER_DIST) {
                float hue = (float) i / path.getNodeCount() * 0.33f;
                int color = Mth.hsvToRgb(hue, 0.9f, 0.9f);

                int red = color >> 16 & 255;
                int green = color >> 8 & 255;
                int blue = color & 255;

                float
                    n1x = lastNode.x - (float) camX + 0.5f,
                    n1y = lastNode.y - (float) camY + 0.5f,
                    n1z = lastNode.z - (float) camZ + 0.5f;
                float
                    n2x = node.x - (float) camX + 0.5f,
                    n2y = node.y - (float) camY + 0.5f,
                    n2z = node.z - (float) camZ + 0.5f;
                float
                    dx = n2x - n1x,
                    dz = n2z - n1z,
                    dl = Mth.sqrt(dx * dx + dz * dz);
                float // normalized cross product with up vector 0 1 0, ny = 0
                    nx = -dz / dl,
                    nz = dx / dl;

                buff.addVertex(n1x - LINE_WIDTH * nx, n1y, n1z - LINE_WIDTH * nz).setColor(red, green, blue, 255);
                buff.addVertex(n1x + LINE_WIDTH * nx, n1y, n1z + LINE_WIDTH * nz).setColor(red, green, blue, 255);
                buff.addVertex(n2x + LINE_WIDTH * nx, n2y, n2z + LINE_WIDTH * nz).setColor(red, green, blue, 255);
                buff.addVertex(n2x - LINE_WIDTH * nx, n2y, n2z - LINE_WIDTH * nz).setColor(red, green, blue, 255);

                buff.addVertex(n1x, n1y - LINE_WIDTH, n1z).setColor(red, green, blue, 255);
                buff.addVertex(n1x, n1y + LINE_WIDTH, n1z).setColor(red, green, blue, 255);
                buff.addVertex(n2x, n2y + LINE_WIDTH, n2z).setColor(red, green, blue, 255);
                buff.addVertex(n2x, n2y - LINE_WIDTH, n2z).setColor(red, green, blue, 255);
            }
            lastNode = node;
        }

        BufferUploader.drawWithShader(buff.buildOrThrow());
        RenderSystem.enableCull();
    }

    private static float distanceToCamera(BlockPos blockPos, double camX, double camY, double camZ) {
        return (float) (Math.abs(blockPos.getX() - camX) + Math.abs(blockPos.getY() - camY) + Math.abs(blockPos.getZ() - camZ));
    }
}
