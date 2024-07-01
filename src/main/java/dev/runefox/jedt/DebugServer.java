package dev.runefox.jedt;

import com.google.gson.JsonParser;
import dev.runefox.jedt.api.DebugServerInitializer;
import dev.runefox.jedt.util.DebugNetwork;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;

import java.nio.file.Files;
import java.nio.file.Path;

public class DebugServer implements DedicatedServerModInitializer {
    @Override
    public void onInitializeServer() {
        if (!Debug.GAMETEST)
            Debug.loadServerTests();

        Debug.entrypoint(
            "jedt:server", DebugServerInitializer.class,
            init -> init.onInitializeDebugServer(Debug.serverDebugStatus)
        );

        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, serverResourceManager, success) -> {
            if (success) {
                reloadStatus(server);
            }
        });
    }

    private static void reloadStatus(MinecraftServer server) {
        Path statusFile = server.getFile("debug_config.json");
        if (!Files.exists(statusFile)) {
            Debug.LOGGER.info("debug_config.json not found");
            Debug.serverDebugStatus.setDebugAvailable(false);
        } else {
            try {
                Debug.serverDebugStatus.read(JsonParser.parseReader(Files.newBufferedReader(statusFile)).getAsJsonObject());
                Debug.LOGGER.info("Loaded debug_config.json");
            } catch (Exception exc) {
                Debug.LOGGER.error("Failed to load debug_config.json", exc);
            }
        }

        DebugNetwork.sendServerStatus(Debug.serverDebugStatus, server);
        Debug.serverDebugStatus.log(Debug.LOGGER);
    }
}
