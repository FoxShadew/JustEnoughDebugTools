package dev.runefox.jedt.mixin;

import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(MinecraftServer.class)
public class MinecraftServerMixin {
//    @Redirect(method = "tickChildren", at = @At(value = "FIELD", target = "Lnet/minecraft/SharedConstants;IS_RUNNING_IN_IDE:Z", opcode = Opcodes.GETSTATIC))
//    private boolean redirectIsRunningInIDE() {
//        return true;
//    }
}