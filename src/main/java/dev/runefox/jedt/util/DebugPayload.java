package dev.runefox.jedt.util;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record DebugPayload(ResourceLocation id, FriendlyByteBuf payload) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<DebugPayload> TYPE = new Type<>(ResourceLocation.parse("jedt:debug"));

    public static final StreamCodec<FriendlyByteBuf, DebugPayload> CODEC = CustomPacketPayload.codec(
        (payload, buf) -> {
            payload.payload.readerIndex(0);
            buf.writeResourceLocation(payload.id);
            buf.writeBytes(payload.payload);
        },
        buf -> {
            ResourceLocation id = buf.readResourceLocation();
            FriendlyByteBuf newBuf = PacketByteBufs.create();
            newBuf.writeBytes(buf);
            return new DebugPayload(id, newBuf);
        }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @Override
    public FriendlyByteBuf payload() {
        payload.readerIndex(0);
        return payload;
    }
}
