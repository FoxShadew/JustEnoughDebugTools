package dev.runefox.jedt.api.gametest;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.server.MinecraftServer;

/**
 * @deprecated Use Fabric's testing API.
 */
@Deprecated(forRemoval = true)
public interface GameTestEvents {
    @Deprecated(forRemoval = true)
    Event<TestStructureLoaded> TEST_STRUCTURE_LOADED = EventFactory.createArrayBacked(
        TestStructureLoaded.class,
        callbacks -> info -> {
            for (TestStructureLoaded callback : callbacks) {
                callback.onTestStructureLoaded(info);
            }
        }
    );
    @Deprecated(forRemoval = true)
    Event<TestPassed> TEST_PASSED = EventFactory.createArrayBacked(
        TestPassed.class,
        callbacks -> info -> {
            for (TestPassed callback : callbacks) {
                callback.onTestPassed(info);
            }
        }
    );
    @Deprecated(forRemoval = true)
    Event<TestFailed> TEST_FAILED = EventFactory.createArrayBacked(
        TestFailed.class,
        callbacks -> info -> {
            for (TestFailed callback : callbacks) {
                callback.onTestFailed(info);
            }
        }
    );
    @Deprecated(forRemoval = true)
    Event<TestServerDone> TEST_SERVER_DONE = EventFactory.createArrayBacked(
        TestServerDone.class,
        callbacks -> server -> {
            for (TestServerDone callback : callbacks) {
                callback.onTestServerDone(server);
            }
        }
    );

    @Deprecated(forRemoval = true)
    interface TestStructureLoaded {
        void onTestStructureLoaded(GameTestInfo info);
    }

    @Deprecated(forRemoval = true)
    interface TestPassed {
        void onTestPassed(GameTestInfo info);
    }

    @Deprecated(forRemoval = true)
    interface TestFailed {
        void onTestFailed(GameTestInfo info);
    }

    @Deprecated(forRemoval = true)
    interface TestServerDone {
        void onTestServerDone(MinecraftServer server);
    }
}
