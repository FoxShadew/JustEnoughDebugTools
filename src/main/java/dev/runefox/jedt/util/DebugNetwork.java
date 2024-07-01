package dev.runefox.jedt.util;

import dev.runefox.jedt.impl.status.ServerDebugStatusImpl;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;
import org.apache.commons.lang3.mutable.MutableInt;

public class DebugNetwork implements ModInitializer {
    public static final ResourceLocation GAME_RULES_PACKET_ID = ResourceLocation.parse("jedt:game_rules");
    public static final ResourceLocation SERVER_STATUS_PACKET_ID = ResourceLocation.parse("jedt:server_status");

    @Override
    public void onInitialize() {
        PayloadTypeRegistry.playS2C().register(
            DebugPayload.TYPE,
            DebugPayload.CODEC
        );
    }

    public static void sendServerStatus(ServerDebugStatusImpl status, ServerPlayer entity) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        status.serialize(buf);
        ServerPlayNetworking.send(entity, new DebugPayload(SERVER_STATUS_PACKET_ID, buf));
    }

    public static void sendServerStatus(ServerDebugStatusImpl status, MinecraftServer server) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        status.serialize(buf);
        server.getPlayerList()
              .getPlayers()
              .forEach(player -> ServerPlayNetworking.send(player, new DebugPayload(SERVER_STATUS_PACKET_ID, buf)));
    }

    private static FriendlyByteBuf serializeGameRules(MinecraftServer server) {
        MutableInt count = new MutableInt(0);
        FriendlyByteBuf buf = PacketByteBufs.create();
        FriendlyByteBuf rulesBuf = PacketByteBufs.create();

        GameRules rules = server.getGameRules();
        GameRules.visitGameRuleTypes(new GameRules.GameRuleTypeVisitor() {
            @Override
            public void visitBoolean(GameRules.Key<GameRules.BooleanValue> key, GameRules.Type<GameRules.BooleanValue> type) {
                count.increment();
                rulesBuf.writeByte(0);
                rulesBuf.writeUtf(key.getId());
                rulesBuf.writeByte(rules.getBoolean(key) ? 1 : 0);
            }

            @Override
            public void visitInteger(GameRules.Key<GameRules.IntegerValue> key, GameRules.Type<GameRules.IntegerValue> type) {
                count.increment();
                rulesBuf.writeByte(1);
                rulesBuf.writeUtf(key.getId());
                rulesBuf.writeInt(rules.getInt(key));
            }
        });
        buf.writeInt(count.getValue());
        buf.writeBytes(rulesBuf);

        rulesBuf.release();

        return buf;
    }

    public static void sendGameRules(MinecraftServer server) {
        FriendlyByteBuf buf = serializeGameRules(server);
        server.getPlayerList()
              .getPlayers()
              .forEach(player -> ServerPlayNetworking.send(player, new DebugPayload(GAME_RULES_PACKET_ID, buf)));
    }

    public static void sendGameRules(ServerPlayer player) {
        FriendlyByteBuf buf = serializeGameRules(player.server);
        ServerPlayNetworking.send(player, new DebugPayload(GAME_RULES_PACKET_ID, buf));
    }
}
