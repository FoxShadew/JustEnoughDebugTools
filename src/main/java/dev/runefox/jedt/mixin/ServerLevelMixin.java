package dev.runefox.jedt.mixin;

import dev.runefox.jedt.util.GameRuleAccessHook;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin implements GameRuleAccessHook {
    @Shadow
    public abstract GameRules getGameRules();

    @Override
    public GameRules jedt$getGameRules() {
        return getGameRules();
    }
}
