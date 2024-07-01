package dev.runefox.jedt.mixin;

import dev.runefox.jedt.api.gametest.GameTestEvents;
import net.minecraft.CrashReport;
import net.minecraft.gametest.framework.GameTestServer;
import net.minecraft.gametest.framework.MultipleTestTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameTestServer.class)
public class GameTestServerMixin {
    @Shadow private MultipleTestTracker testTracker;

    @Inject(method = "onServerExit", at = @At(value = "INVOKE", target = "Ljava/lang/System;exit(I)V"))
    private void onFinish(CallbackInfo info) {
        GameTestServer.class.cast(this).getRunningThread().interrupt();
        GameTestEvents.TEST_SERVER_DONE.invoker().onTestServerDone(GameTestServer.class.cast(this));

        // Halt runtime here, prevent the server from hanging which it somehow does
        Runtime.getRuntime().halt(testTracker.getFailedRequiredCount());
    }

    @Inject(method = "onServerCrash", at = @At(value = "INVOKE", target = "Ljava/lang/System;exit(I)V"))
    private void onCrash(CrashReport crashReport, CallbackInfo info) {
        // Halt runtime here, prevent the server from hanging which it somehow does
        Runtime.getRuntime().halt(1);
    }
}
