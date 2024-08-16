package dev.runefox.jedt.util;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.GameRules;

import dev.runefox.jedt.DebugClient;
import dev.runefox.jedt.api.status.DebugStatusEvents;

import java.util.HashMap;
import java.util.Map;

public class DebugNetworkClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(DebugPayload.TYPE, (payload, context) -> {
            var buf = payload.payload();
            var client = Minecraft.getInstance();

            if (payload.id().equals(DebugNetwork.GAME_RULES_PACKET_ID)) {
                Map<String, Integer> values = new HashMap<>();
                int c = buf.readInt();
                for (int i = 0; i < c; i++) {
                    int t = buf.readByte();
                    String k = buf.readUtf();
                    int v = t == 0 ? buf.readByte() : buf.readInt();
                    values.put(k, v);
                }

                client.execute(() -> {
                    if (client.level == null) {
                        return;
                    }

                    GameRules gameRules = client.level.getGameRules();
                    GameRules.visitGameRuleTypes(new GameRules.GameRuleTypeVisitor() {
                        @Override
                        public void visitBoolean(GameRules.Key<GameRules.BooleanValue> key, GameRules.Type<GameRules.BooleanValue> type) {
                            String name = key.getId();
                            gameRules.getRule(key).set(values.get(name) != 0, null);
                        }

                        @Override
                        public void visitInteger(GameRules.Key<GameRules.IntegerValue> key, GameRules.Type<GameRules.IntegerValue> type) {
                            String name = key.getId();
                            gameRules.getRule(key).set(values.get(name), null);
                        }
                    });
                });
                return;
            }

            if (payload.id().equals(DebugNetwork.SERVER_STATUS_PACKET_ID)) {
                DebugClient.serverDebugStatus.deserialize(buf);
                client.execute(() -> {
                    DebugStatusEvents.CLIENT_READY.invoker().ready(DebugClient.serverDebugStatus);
                    DebugClient.serverDebugStatus.log(DebugClient.LOGGER);
                });
            }
        });
    }
}
