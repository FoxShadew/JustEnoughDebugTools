package dev.runefox.jedt;

import dev.runefox.jedt.api.MenuInitializer;
import dev.runefox.jedt.api.menu.*;
import dev.runefox.jedt.api.status.DebugStatusKey;
import dev.runefox.jedt.api.status.ServerDebugStatus;
import dev.runefox.jedt.api.status.StandardStatusKeys;
import dev.runefox.jedt.gui.*;
import dev.runefox.jedt.impl.menu.Item;
import dev.runefox.jedt.impl.menu.*;
import dev.runefox.jedt.render.DebugRenderers;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.GameRules;

import java.util.Locale;
import java.util.function.BooleanSupplier;

public class DefaultMenuInitializer implements MenuInitializer {
    private final Minecraft minecraft = Minecraft.getInstance();

    @Override
    public void onInitializeDebugMenu(Menu root, MenuManager factory, ServerDebugStatus debugStatus) {
        Menu commands = factory.getMenu(Menu.COMMANDS);
        Menu timeCommands = factory.getMenu(Menu.TIME_COMMANDS);
        Menu gamemodeCommands = factory.getMenu(Menu.GAMEMODE_COMMANDS);
        Menu weatherCommands = factory.getMenu(Menu.WEATHER_COMMANDS);
        Menu difficultyCommands = factory.getMenu(Menu.DIFFICULTY_COMMANDS);
        Menu tickSpeedCommands = factory.getMenu(Menu.TICK_SPEED_COMMANDS);
        Menu miscCommands = factory.getMenu(Menu.MISC_COMMANDS);
        Menu actions = factory.getMenu(Menu.ACTIONS);
        Menu copy = factory.getMenu(Menu.COPY);
        Menu display = factory.getMenu(Menu.DISPLAY);
        Menu gametest = factory.getMenu(Menu.GAMETEST);

        root.addOption(new MenuOption(commands).desc(Component.translatable("debug.menu.jedt.commands.desc")));
        root.addOption(new MenuOption(actions).desc(Component.translatable("debug.menu.jedt.actions.desc")));
        root.addOption(new MenuOption(copy).desc(Component.translatable("debug.menu.jedt.copy.desc")));
        root.addOption(new MenuOption(display).desc(Component.translatable("debug.menu.jedt.display.desc")));
        root.addOption(new MenuOption(gametest).desc(Component.translatable("debug.menu.jedt.gametest.desc")));

        commands.addOption(new MenuOption(timeCommands).longName(Component.translatable("debug.menu.jedt.time_commands.long")));
        commands.addOption(new MenuOption(gamemodeCommands).longName(Component.translatable("debug.menu.jedt.gamemode_commands.long")));
        commands.addOption(new MenuOption(weatherCommands).longName(Component.translatable("debug.menu.jedt.weather_commands.long")));
        commands.addOption(new MenuOption(difficultyCommands).longName(Component.translatable("debug.menu.jedt.difficulty_commands.long")));
        commands.addOption(new MenuOption(tickSpeedCommands).longName(Component.translatable("debug.menu.jedt.tick_speed_commands.long")));
        commands.addOption(new MenuOption(miscCommands).longName(Component.translatable("debug.menu.jedt.misc_commands.long")));
        commands.addOption(new CommandItem(text("commands.start_profiler"), "debug start", text("commands.start_profiler")).longName(longn("commands.start_profiler")));
        commands.addOption(new CommandItem(text("commands.stop_profiler"), "debug stop", text("commands.stop_profiler")).longName(longn("commands.stop_profiler")));

        Component timeResponse = text("commands.time.response");
        timeCommands.addOption(new BooleanGameruleItem(text("commands.time.enabled"), GameRules.RULE_DAYLIGHT).onlyIf(debugStatus, StandardStatusKeys.GAME_RULE_SYNC).longName(longn("commands.time.enabled")));
        timeCommands.addOption(new CommandItem(text("commands.time.day"), "time set day", timeResponse).longName(longn("commands.time.day")));
        timeCommands.addOption(new CommandItem(text("commands.time.noon"), "time set noon", timeResponse).longName(longn("commands.time.noon")));
        timeCommands.addOption(new CommandItem(text("commands.time.night"), "time set night", timeResponse).longName(longn("commands.time.night")));
        timeCommands.addOption(new CommandItem(text("commands.time.midnight"), "time set midnight", timeResponse).longName(longn("commands.time.midnight")));

        Component gamemodeResponse = text("commands.gamemode.response");
        gamemodeCommands.addOption(new CommandItem(text("commands.gamemode.creative"), "gamemode creative", gamemodeResponse).longName(longn("commands.gamemode.creative")));
        gamemodeCommands.addOption(new CommandItem(text("commands.gamemode.survival"), "gamemode survival", gamemodeResponse).longName(longn("commands.gamemode.survival")));
        gamemodeCommands.addOption(new CommandItem(text("commands.gamemode.adventure"), "gamemode adventure", gamemodeResponse).longName(longn("commands.gamemode.adventure")));
        gamemodeCommands.addOption(new CommandItem(text("commands.gamemode.spectator"), "gamemode spectator", gamemodeResponse).longName(longn("commands.gamemode.spectator")));

        Component weatherResponse = text("commands.weather.response");
        weatherCommands.addOption(new BooleanGameruleItem(text("commands.weather.enabled"), GameRules.RULE_WEATHER_CYCLE).onlyIf(debugStatus, StandardStatusKeys.GAME_RULE_SYNC).longName(longn("commands.weather.enabled")));
        weatherCommands.addOption(new CommandItem(text("commands.weather.clear"), "weather clear", weatherResponse).longName(longn("commands.weather.clear")));
        weatherCommands.addOption(new CommandItem(text("commands.weather.rain"), "weather rain", weatherResponse).longName(longn("commands.weather.rain")));
        weatherCommands.addOption(new CommandItem(text("commands.weather.thunder"), "weather thunder", weatherResponse).longName(longn("commands.weather.thunder")));

        Component difficultyResponse = text("commands.difficulty.response");
        difficultyCommands.addOption(new CommandItem(Component.translatable("options.difficulty.peaceful"), "difficulty peaceful", difficultyResponse).longName(longn("commands.difficulty.peaceful")));
        difficultyCommands.addOption(new CommandItem(Component.translatable("options.difficulty.easy"), "difficulty easy", difficultyResponse).longName(longn("commands.difficulty.easy")));
        difficultyCommands.addOption(new CommandItem(Component.translatable("options.difficulty.normal"), "difficulty normal", difficultyResponse).longName(longn("commands.difficulty.normal")));
        difficultyCommands.addOption(new CommandItem(Component.translatable("options.difficulty.hard"), "difficulty hard", difficultyResponse).longName(longn("commands.difficulty.hard")));

        miscCommands.addOption(new BooleanGameruleItem(text("commands.misc.insomnia"), GameRules.RULE_DOINSOMNIA).onlyIf(debugStatus, StandardStatusKeys.GAME_RULE_SYNC).longName(longn("commands.misc.insomnia")));
        miscCommands.addOption(new BooleanGameruleItem(text("commands.misc.mob_spawning"), GameRules.RULE_DOMOBSPAWNING).onlyIf(debugStatus, StandardStatusKeys.GAME_RULE_SYNC).longName(longn("commands.misc.mob_spawning")));
        miscCommands.addOption(new BooleanGameruleItem(text("commands.misc.block_drops"), GameRules.RULE_DOBLOCKDROPS).onlyIf(debugStatus, StandardStatusKeys.GAME_RULE_SYNC).longName(longn("commands.misc.block_drops")));
        miscCommands.addOption(new BooleanGameruleItem(text("commands.misc.entity_drops"), GameRules.RULE_DOENTITYDROPS).onlyIf(debugStatus, StandardStatusKeys.GAME_RULE_SYNC).longName(longn("commands.misc.entity_drops")));
        miscCommands.addOption(new BooleanGameruleItem(text("commands.misc.command_output"), GameRules.RULE_COMMANDBLOCKOUTPUT).onlyIf(debugStatus, StandardStatusKeys.GAME_RULE_SYNC).longName(longn("commands.misc.command_output")));
        miscCommands.addOption(new CommandItem(text("commands.misc.debug_stick"), "give @s minecraft:debug_stick", text("commands.misc.debug_stick.response")));
        miscCommands.addOption(new CommandItem(text("commands.misc.command_block"), "give @s minecraft:command_block", text("commands.misc.command_block.response")));
        miscCommands.addOption(new CommandItem(text("commands.misc.command_block_cart"), "give @s minecraft:command_block_minecart", text("commands.misc.command_block_cart.response")));
        miscCommands.addOption(new CommandItem(text("commands.misc.structure_block"), "give @s minecraft:structure_block", text("commands.misc.structure_block.response")));
        miscCommands.addOption(new CommandItem(text("commands.misc.barrier"), "give @s minecraft:barrier", text("commands.misc.barrier.response")));
        miscCommands.addOption(new CommandItem(text("commands.misc.light"), "give @s minecraft:light", text("commands.misc.light.response")));
        miscCommands.addOption(new CommandItem(text("commands.misc.night_vision"), "effect give @s minecraft:night_vision 1000000 0 true", text("commands.misc.night_vision.response")));
        miscCommands.addOption(new CommandItem(text("commands.misc.saturation"), "effect give @s minecraft:saturation 1000000 100 true", text("commands.misc.saturation.response")));
        miscCommands.addOption(new CommandItem(text("commands.misc.regeneration"), "effect give @s minecraft:regeneration 1000000 100 true", text("commands.misc.regeneration.response")));
        miscCommands.addOption(new CommandItem(text("commands.misc.clear_effects"), "effect clear @s", text("commands.misc.clear_effects.response")));

        tickSpeedCommands.addOption(new NumberGameruleItem(text("commands.tick_speed"), GameRules.RULE_RANDOMTICKING).onlyIf(debugStatus, StandardStatusKeys.GAME_RULE_SYNC).longName(longn("commands.tick_speed")));
        tickSpeedCommands.addOption(new CommandItem(text("commands.tick_speed.0"), "gamerule randomTickSpeed 0", text("commands.tick_speed.response.0")).longName(longn("commands.tick_speed.0")));
        tickSpeedCommands.addOption(new CommandItem(text("commands.tick_speed.3"), "gamerule randomTickSpeed 3", text("commands.tick_speed.response.3")).longName(longn("commands.tick_speed.3")));
        tickSpeedCommands.addOption(new CommandItem(text("commands.tick_speed.10"), "gamerule randomTickSpeed 10", text("commands.tick_speed.response.10")).longName(longn("commands.tick_speed.10")));
        tickSpeedCommands.addOption(new CommandItem(text("commands.tick_speed.100"), "gamerule randomTickSpeed 100", text("commands.tick_speed.response.100")).longName(longn("commands.tick_speed.100")));
        tickSpeedCommands.addOption(new CommandItem(text("commands.tick_speed.1000"), "gamerule randomTickSpeed 1000", text("commands.tick_speed.response.1000")).longName(longn("commands.tick_speed.1000")));
        tickSpeedCommands.addOption(new CommandItem(text("commands.tick_speed.10000"), "gamerule randomTickSpeed 10000", text("commands.tick_speed.response.10000")).longName(longn("commands.tick_speed.10000")));

        actions.addOption(new SimpleActionItem(text("reload_resources"), this::reloadResources).desc(desc("reload_resources")));
        actions.addOption(new CommandItem(text("reload_datapacks"), "reload", text("reload_datapacks.response")).desc(desc("reload_datapacks")));
        actions.addOption(new SimpleActionItem(text("reload_chunks"), this::reloadChunks).desc(desc("reload_chunks")));
        actions.addOption(new SimpleActionItem(text("clear_chat"), this::clearChat).desc(desc("clear_chat")));
        actions.addOption(new RenderDistanceItem(text("render_distance")).desc(desc("render_distance")));
        actions.addOption(new LostFocusPauseItem(text("pause_unfocus")).longName(longn("pause_unfocus")).desc(desc("pause_unfocus")));

        copy.addOption(new SimpleActionItem(text("copy_tp"), this::copyTpLocation).hideIf(Minecraft::showOnlyReducedInfo).longName(longn("copy_tp")).desc(desc("copy_tp")));
        copy.addOption(new CopyTargetItem(text("copy_targeted")).desc(desc("copy_targeted")));

        display.addOption(new EntityHitboxItem(text("entity_hitboxes")).desc(desc("entity_hitboxes")));
        display.addOption(new ChunkBordersItem(text("chunk_borders")).desc(desc("chunk_borders")));
        display.addOption(new AdvancedTooltipsItem(text("advanced_tooltips")).desc(desc("advanced_tooltips")));
        display.addOption(hideReduced(new dev.runefox.jedt.impl.menu.Item(text("mob_paths"), DebugRenderers.PATHFINDING_ENABLED), debugStatus, StandardStatusKeys.SEND_PATHFINDING_INFO).desc(desc("mob_paths")));
        display.addOption(hideReduced(new dev.runefox.jedt.impl.menu.Item(text("neighbor_updates"), DebugRenderers.NEIGHBOR_UPDATES_SHOWN), debugStatus, StandardStatusKeys.SEND_PATHFINDING_INFO).desc(desc("neighbor_updates")));
        display.addOption(new dev.runefox.jedt.impl.menu.Item(text("heightmaps"), DebugRenderers.HEIGHTMAPS_SHOWN).desc(desc("heightmaps")));
        display.addOption(new dev.runefox.jedt.impl.menu.Item(text("fluid_levels"), DebugRenderers.FLUID_LEVELS_SHOWN).desc(desc("fluid_levels")));
        display.addOption(new Item(text("collisions"), DebugRenderers.COLLISIONS_SHOWN).desc(desc("collisions")));

        BooleanSupplier gametestEnabled = () -> DebugClient.serverDebugStatus.getStatus(StandardStatusKeys.ALLOW_GAMETEST);
        gametest.addOption(new CommandItem(text("gametest.runthis"), "test runthis").closeScreenOnClick().onlyIf(gametestEnabled).desc(desc("gametest.runthis")));
        gametest.addOption(new CommandItem(text("gametest.runthese"), "test runthese").closeScreenOnClick().onlyIf(gametestEnabled).desc(desc("gametest.runthese")));
        gametest.addOption(new DisplayScreenOption(text("gametest.runall"), (component, parentScreen) -> createTestAllScreen(parentScreen)).closeScreenOnClick().onlyIf(gametestEnabled).desc(desc("gametest.runall")));
        gametest.addOption(new DisplayScreenOption(text("gametest.runfunction"), (component, parentScreen) -> createTestFunctionScreen(parentScreen)).closeScreenOnClick().onlyIf(gametestEnabled).desc(desc("gametest.runfunction")));
        gametest.addOption(new DisplayScreenOption(text("gametest.runclass"), (component, parentScreen) -> createTestClassScreen(parentScreen)).closeScreenOnClick().onlyIf(gametestEnabled).desc(desc("gametest.runclass")));
        gametest.addOption(new CommandItem(text("gametest.runfailed"), "test runfailed").closeScreenOnClick().onlyIf(gametestEnabled).desc(desc("gametest.runfailed")));
        gametest.addOption(new CommandItem(text("gametest.exportthis"), "test exportthis").closeScreenOnClick().onlyIf(gametestEnabled).desc(desc("gametest.exportthis")));
        gametest.addOption(new DisplayScreenOption(text("gametest.export"), (component, parentScreen) -> createTestExportScreen(parentScreen)).closeScreenOnClick().onlyIf(gametestEnabled).desc(desc("gametest.export")));
        gametest.addOption(new DisplayScreenOption(text("gametest.import"), (component, parentScreen) -> createTestImportScreen(parentScreen)).closeScreenOnClick().onlyIf(gametestEnabled).desc(desc("gametest.import")));
        gametest.addOption(new CommandItem(text("gametest.pos"), "test pos").closeScreenOnClick().onlyIf(gametestEnabled).desc(desc("gametest.pos")));
        gametest.addOption(new CommandItem(text("gametest.clearall"), "test clearall").closeScreenOnClick().onlyIf(gametestEnabled).desc(desc("gametest.clearall")));
    }

    private static Screen createTestFunctionScreen(Screen parent) {
        return new TestFunctionPopupScreen(parent, (name, rs) -> {
            Minecraft mc = Minecraft.getInstance();
            assert mc.player != null;
            mc.player.connection.sendCommand("test run " + name + " " + rs);
        });
    }

    private static Screen createTestClassScreen(Screen parent) {
        return new TestClassPopupScreen(parent, (name, rs) -> {
            Minecraft mc = Minecraft.getInstance();
            assert mc.player != null;
            mc.player.connection.sendCommand("test runall " + name + " " + rs);
        });
    }

    private static Screen createTestAllScreen(Screen parent) {
        return new TestAllPopupScreen(parent, rs -> {
            Minecraft mc = Minecraft.getInstance();
            assert mc.player != null;
            mc.player.connection.sendCommand("test runall " + rs);
        });
    }

    private static Screen createTestExportScreen(Screen parent) {
        return new TestExportPopupScreen(parent, name -> {
            Minecraft mc = Minecraft.getInstance();
            assert mc.player != null;
            mc.player.connection.sendCommand("test export " + name);
        });
    }

    private static Screen createTestImportScreen(Screen parent) {
        return new TestImportPopupScreen(parent, name -> {
            Minecraft mc = Minecraft.getInstance();
            assert mc.player != null;
            mc.player.connection.sendCommand("test import " + name);
        });
    }

    private static Component text(String optionId) {
        return Component.translatable(Util.makeDescriptionId("debug.options", ResourceLocation.fromNamespaceAndPath("jedt", optionId)));
    }

    private static Component desc(String optionId) {
        return Component.translatable(Util.makeDescriptionId("debug.options", ResourceLocation.fromNamespaceAndPath("jedt", optionId)) + ".desc");
    }

    private static Component longn(String optionId) {
        return Component.translatable(Util.makeDescriptionId("debug.options", ResourceLocation.fromNamespaceAndPath("jedt", optionId)) + ".long");
    }

    private void reloadChunks(OptionSelectContext context) {
        context.spawnResponse(text("reload_chunks.response"));
        minecraft.levelRenderer.allChanged();
    }

    private void clearChat(OptionSelectContext context) {
        context.spawnResponse(text("clear_chat.response"));
        minecraft.gui.getChat().clearMessages(false);
    }

    private void reloadResources(OptionSelectContext context) {
        context.spawnResponse(text("reload_resources.response"));
        minecraft.reloadResourcePacks();
    }

    private void copyTpLocation(OptionSelectContext context) {
        if (!minecraft.showOnlyReducedInfo()) {
            LocalPlayer player = minecraft.player;
            assert player != null;

            ClientPacketListener netHandler = player.connection;

            context.spawnResponse(text("copy_tp.response"));
            context.copyToClipboard(
                String.format(
                    Locale.ROOT,
                    "/execute in %s run tp @s %.2f %.2f %.2f %.2f %.2f",
                    player.level().dimension().location(),
                    player.getX(), player.getY(), player.getZ(),
                    player.getXRot(), player.getYRot()
                )
            );
        }
    }

    private static AbstractItem hideReduced(AbstractItem option, ServerDebugStatus status, DebugStatusKey<?> key) {
        return option.hideIf(() -> Minecraft.getInstance().showOnlyReducedInfo() || !status.isAvailable(key));
    }
}
