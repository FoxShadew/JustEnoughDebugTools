package dev.runefox.jedt.mixin;

import dev.runefox.jedt.Debug;
import dev.runefox.jedt.util.DebugNetwork;
import net.minecraft.network.Connection;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerList.class)
public class PlayerManagerMixin {
    @Inject(method = "placeNewPlayer", at = @At("TAIL"))
    private void onOnPlayerConnect(Connection connection, ServerPlayer player, CommonListenerCookie cookie, CallbackInfo info) {
        DebugNetwork.sendServerStatus(Debug.serverDebugStatus, player);
    }
}
