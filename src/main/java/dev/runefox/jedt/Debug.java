package dev.runefox.jedt;

import dev.runefox.jedt.api.DebugInitializer;
import dev.runefox.jedt.api.DebugStatusInitializer;
import dev.runefox.jedt.impl.status.ServerDebugStatusImpl;
import dev.runefox.jedt.test.GameTestIntegration;
import dev.runefox.jedt.test.ModTestConfig;
import dev.runefox.jedt.test.RuntimeTestConfig;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.entrypoint.EntrypointContainer;
import net.fabricmc.loader.impl.util.ExceptionUtil;
import net.minecraft.gametest.framework.GameTestRegistry;
import net.minecraft.gametest.framework.StructureUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collection;
import java.util.function.Consumer;

public class Debug implements ModInitializer {
    public static final boolean GAMETEST = Boolean.parseBoolean(System.getProperty("jedt.gametest"));
    public static final boolean UWU = Boolean.parseBoolean(System.getProperty("jedt.uwu"));

    public static final Logger LOGGER = LogManager.getLogger();

    private static ServerDebugStatusImpl.Builder serverDebugStatusBuilder;
    public static ServerDebugStatusImpl serverDebugStatus;

    static void loadClientTests() {
        loadTests().allTestMethods("_runtime", "_client").forEach(GameTestRegistry::register);
        LOGGER.info("Loaded {} tests", GameTestRegistry.getAllTestFunctions().size());
    }

    static void loadServerTests() {
        loadTests().allTestMethods("_runtime", "_server").forEach(GameTestRegistry::register);
        LOGGER.info("Loaded {} tests", GameTestRegistry.getAllTestFunctions().size());
    }

    private static RuntimeTestConfig loadTests() {
        RuntimeTestConfig rtTestConfig = new RuntimeTestConfig();
        for (ModContainer container : FabricLoader.getInstance().getAllMods()) {
            ModTestConfig config = GameTestIntegration.loadModTestConfig(container);
            if (config == null) {
                LOGGER.error("Failed to load jedt.tests.json in mod {}", container.getMetadata().getId());
            }
            rtTestConfig.addModConfig(config);
        }
        return rtTestConfig;
    }

    @Override
    public void onInitialize() {
        // Enable GameTest
        // SharedConstants.IS_RUNNING_IN_IDE = true;
        // ... but that's a mixin now

        serverDebugStatus = createStatusInstance();

        if (!GAMETEST) {
            String testStructuresDir = System.getProperty("jedt.test_structures_path");
            if (testStructuresDir != null)
                StructureUtils.testStructuresDir = testStructuresDir;
        }

        entrypoint(
            "jedt:main", DebugInitializer.class,
            init -> init.onInitializeDebug(serverDebugStatus)
        );

        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            if (!server.isDedicatedServer()) {
                serverDebugStatus.resetAll();
            }
        });
    }

    public static ServerDebugStatusImpl createStatusInstance() {
        if (serverDebugStatusBuilder == null) {
            serverDebugStatusBuilder = new ServerDebugStatusImpl.Builder();
            new DefaultStatusInitializer().onInitializeDebugStatus(serverDebugStatusBuilder);


            FabricLoader.getInstance().invokeEntrypoints(
                "jedt:status", DebugStatusInitializer.class,
                init -> init.onInitializeDebugStatus(serverDebugStatusBuilder)
            );
        }

        return serverDebugStatusBuilder.build();
    }

    public static <T> void entrypoint(String name, Class<T> type, Consumer<? super T> invoker) {
        RuntimeException exception = null;
        Collection<EntrypointContainer<T>> entrypoints = FabricLoader.getInstance().getEntrypointContainers(name, type);

        for (EntrypointContainer<T> container : entrypoints) {
            try {
                invoker.accept(container.getEntrypoint());
            } catch (Throwable thr) {
                exception = ExceptionUtil.gatherExceptions(
                    thr, exception,
                    exc -> new RuntimeException(
                        "Could not execute entrypoint stage '%s' due to errors, provided by '%s'!".formatted(
                            name, container.getProvider().getMetadata().getId()
                        ),
                        exc
                    )
                );
            }
        }

        if (exception != null) {
            throw exception;
        }
    }
}
