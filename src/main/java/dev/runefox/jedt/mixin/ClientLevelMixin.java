package dev.runefox.jedt.mixin;

import dev.runefox.jedt.util.GameRuleAccessHook;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.level.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ClientLevel.class)
public abstract class ClientLevelMixin implements GameRuleAccessHook {
    @Shadow
    public abstract FeatureFlagSet enabledFeatures();

    @Unique
    private GameRules syncedGameRules;

    @Override
    public GameRules jedt$getGameRules() {
        if (syncedGameRules == null)
            syncedGameRules = new GameRules(enabledFeatures());

        return syncedGameRules;
    }
}
