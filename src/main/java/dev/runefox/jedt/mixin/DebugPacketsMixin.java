package dev.runefox.jedt.mixin;

import dev.runefox.jedt.Debug;
import dev.runefox.jedt.api.status.StandardStatusKeys;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.*;
import net.minecraft.network.protocol.game.DebugPackets;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.Path;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(DebugPackets.class)
public abstract class DebugPacketsMixin {
    // These are all a couple of methods whose contents were removed by Mojang
    // This mixin is dedicated to add these contents again
    // Injects do work, although overwrites are much easier and never is the target class intended to send the dedicated
    // packets twice - which can happen if multiple mods (or multiple instances of this mod) try to inject the same
    // method contents. Hence this mixin uses overwrites, to force that the methods only do what they are supposed to
    // do. Supposedly nobody is going to mixin into this class for some other reason than just re-adding the method
    // contents that Mojang removed.

    @Shadow
    private static void sendPacketToAllPlayers(ServerLevel serverLevel, CustomPacketPayload customPacketPayload) {
        // Shadowed content
    }

    /**
     * @reason See {@link DebugPacketsMixin}s comment
     * @author Samū
     */
    @Overwrite
    public static void sendPathFindingPacket(Level world, Mob mob, @Nullable Path path, float nodeReachProximity) {
        if (path == null || !(world instanceof ServerLevel slevel)) {
            return;
        }

        if (slevel.getGameRules().getBoolean(GameRules.RULE_REDUCEDDEBUGINFO)) {
            return;
        }

        if (!Debug.serverDebugStatus.getStatus(StandardStatusKeys.SEND_PATHFINDING_INFO)) {
            return;
        }

        sendToAllWatching(slevel, new PathfindingDebugPayload(mob.getId(), path, nodeReachProximity), mob);
    }

    /**
     * @reason See {@link DebugPacketsMixin}s comment
     * @author Samū
     */
    @Overwrite
    public static void sendNeighborsUpdatePacket(Level world, BlockPos pos) {
        if (!(world instanceof ServerLevel slevel)) {
            return;
        }

        if (slevel.getGameRules().getBoolean(GameRules.RULE_REDUCEDDEBUGINFO)) {
            return;
        }

        if (!Debug.serverDebugStatus.getStatus(StandardStatusKeys.SEND_NEIGHBOR_UPDATES)) {
            return;
        }

        sendToAllWatching(slevel, new NeighborUpdatesDebugPayload(slevel.getGameTime(), pos), pos);
    }

    /**
     * @reason See {@link DebugPacketsMixin}s comment
     * @author Samū
     */
    @Overwrite
    public static void sendPoiPacketsForChunk(ServerLevel world, ChunkPos pos) {
        var pois = world.getPoiManager().getInChunk(it -> true, pos, PoiManager.Occupancy.ANY).toList();
        for (PoiRecord record : pois) {
            sendPacketToAllPlayers(world, new PoiAddedDebugPayload(record.getPos(), record.getPoiType() + "", record.getFreeTickets()));
        }
    }

    /**
     * @reason See {@link DebugPacketsMixin}s comment
     * @author Samū
     */
    @Overwrite
    public static void sendPoiAddedPacket(ServerLevel level, BlockPos pos) {
        sendVillageSectionsPacket(level, pos);
        var type = level.getPoiManager().getType(pos);
        var tickets = level.getPoiManager().getFreeTickets(pos);

        sendPacketToAllPlayers(level, new PoiAddedDebugPayload(pos, type + "", tickets));
    }

    /**
     * @reason See {@link DebugPacketsMixin}s comment
     * @author Samū
     */
    @Overwrite
    public static void sendPoiRemovedPacket(ServerLevel level, BlockPos pos) {
        sendVillageSectionsPacket(level, pos);
        sendPacketToAllPlayers(level, new PoiRemovedDebugPayload(pos));
    }

    /**
     * @reason See {@link DebugPacketsMixin}s comment
     * @author Samū
     */
    @Overwrite
    public static void sendPoiTicketCountPacket(ServerLevel level, BlockPos pos) {
        sendVillageSectionsPacket(level, pos);

        var tickets = level.getPoiManager().getFreeTickets(pos);

        sendPacketToAllPlayers(level, new PoiTicketCountDebugPayload(pos, tickets));
    }

    /**
     * @reason See {@link DebugPacketsMixin}s comment
     * @author Samū
     */
    @Overwrite
    private static void sendVillageSectionsPacket(ServerLevel level, BlockPos pos) {
        // Not sure yet what this is supposed to do but to be implemented someday if needed
    }

    @Unique
    private static void sendToAllWatching(ServerLevel world, CustomPacketPayload payload, Entity watch) {
        Packet<?> packet = new ClientboundCustomPayloadPacket(payload);
        world.getLevel().getChunkSource().broadcast(watch, packet);
    }

    @Unique
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
