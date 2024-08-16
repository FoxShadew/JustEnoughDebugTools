package dev.runefox.jedt.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.gametest.framework.RetryOptions;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Rotation;

import dev.runefox.jedt.test.TestEventsListener;

@Mixin(GameTestInfo.class)
@SuppressWarnings("deprecation")
public class GameTestInfoMixin {
    @Inject(method = "<init>", at = @At("RETURN"))
    private void constructorHook(TestFunction testFunction, Rotation rotation, ServerLevel serverLevel, RetryOptions retryOptions, CallbackInfo info) {
        GameTestInfo.class.cast(this).addListener(TestEventsListener.INSTANCE);
    }
}
