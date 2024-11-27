package dev.runefox.jedt.api.menu;

import dev.runefox.jedt.util.GameRuleAccessHook;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.GameRules;

public class NumberGameruleItem extends NumberItem {
    private final String command;
    private final Component response;
    private final GameRules.Key<GameRules.IntegerValue> key;
    private final Component commandDesc;

    public NumberGameruleItem(Component name, GameRules.Key<GameRules.IntegerValue> key, Component response) {
        super(name);
        this.command = "gamerule " + key + " ";
        this.response = response;
        this.key = key;
        this.commandDesc = Component.literal("/" + command + "<x>").withStyle(ChatFormatting.AQUA);
    }

    public NumberGameruleItem(Component name, GameRules.Key<GameRules.IntegerValue> key) {
        this(name, key, null);
    }

    @Override
    public Component getDescription() {
        MutableComponent desc = Component.empty().append(commandDesc);
        Component superDesc = super.getDescription();
        if (superDesc != null) {
            desc.append("\n");
            desc.append(superDesc);
        }
        return desc;
    }

    @Override
    protected int get() {
        Minecraft client = Minecraft.getInstance();
        assert client.level != null;
        return ((GameRuleAccessHook) client.level).jedt$getGameRules().getInt(key);
    }

    @Override
    protected void mutate(int delta, OptionSelectContext context) {
        if (!context.hasPermissionLevel(2)) {
            context.spawnResponse(
                Component.translatable("debug.options.jedt.commands.no_permission")
                         .withStyle(ChatFormatting.RED)
            );
            return;
        }

        Minecraft client = Minecraft.getInstance();
        assert client.level != null;

        int newVal = get() + delta;
        context.sendCommand(command + newVal);

        // Temporarily set the gamerule on the client - the server is gonna send an update but we want smooth switching
        // so we don't wait for the server (if we do wait the button might be changed twice without updating)
        ((GameRuleAccessHook) client.level).jedt$getGameRules().getRule(key).set(newVal, null);

        if (response != null) {
            context.spawnResponse(response);
        }
    }
}
