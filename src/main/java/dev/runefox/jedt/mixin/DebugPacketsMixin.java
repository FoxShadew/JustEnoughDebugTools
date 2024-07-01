package dev.runefox.jedt.mixin;

import dev.runefox.jedt.Debug;
import dev.runefox.jedt.api.status.StandardStatusKeys;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.NeighborUpdatesDebugPayload;
import net.minecraft.network.protocol.common.custom.PathfindingDebugPayload;
import net.minecraft.network.protocol.game.DebugPackets;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.Path;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(DebugPackets.class)
public class DebugPacketsMixin {
    // These are all a couple of methods whose contents were removed by Mojang
    // This mixin is dedicated to add these contents again
    // Injects do work, although overwrites are much easier and never is the target class intended to send the dedicated
    // packets twice - which can happen if multiple mods (or multiple instances of this mod) try to inject the same
    // method contents. Hence this mixin uses overwrites, to force that the methods only do what they are supposed to
    // do. Supposedly nobody is going to mixin into this class for some other reason than just re-adding the method
    // contents that Mojang removed.

    /**
     * @reason See {@link DebugPacketsMixin}s comment
     * @author Samū
     */
    @Overwrite
    public static void sendPathFindingPacket(Level world, Mob mob, @Nullable Path path, float nodeReachProximity) {
        if (path == null || !(world instanceof ServerLevel)) {
            return;
        }

        if (world.getGameRules().getBoolean(GameRules.RULE_REDUCEDDEBUGINFO)) {
            return;
        }

        if (!Debug.serverDebugStatus.getStatus(StandardStatusKeys.SEND_PATHFINDING_INFO)) {
            return;
        }

        sendToAllWatching((ServerLevel) world, new PathfindingDebugPayload(mob.getId(), path, nodeReachProximity), mob);
    }

    /**
     * @reason See {@link DebugPacketsMixin}s comment
     * @author Samū
     */
    @Overwrite
    public static void sendNeighborsUpdatePacket(Level world, BlockPos pos) {
        if (!(world instanceof ServerLevel)) {
            return;
        }

        if (world.getGameRules().getBoolean(GameRules.RULE_REDUCEDDEBUGINFO)) {
            return;
        }

        if (!Debug.serverDebugStatus.getStatus(StandardStatusKeys.SEND_NEIGHBOR_UPDATES)) {
            return;
        }

        sendToAllWatching((ServerLevel) world, new NeighborUpdatesDebugPayload(world.getGameTime(), pos), pos);
    }

    /**
     * @reason See {@link DebugPacketsMixin}s comment
     * @author Samū
     */
    @Overwrite
    public static void sendPoiPacketsForChunk(ServerLevel world, ChunkPos pos) {

    }

    private static void sendToAllWatching(ServerLevel world, CustomPacketPayload payload, Entity watch) {
        Packet<?> packet = new ClientboundCustomPayloadPacket(payload);
        world.getLevel().getChunkSource().broadcast(watch, packet);
    }

    private static void sendToAllWatching(ServerLevel world, CustomPacketPayload payload, BlockPos watch) {
        Packet<?> packet = new ClientboundCustomPayloadPacket(payload);
        int cx = watch.getX() / 16;
        int cz = watch.getZ() / 16;

        ChunkMap storage = world.getLevel().getChunkSource().chunkMap;
        storage.getPlayers(new ChunkPos(cx, cz), false).forEach(
            player -> player.connection.send(packet)
        );
    }
}
