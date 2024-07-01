package dev.runefox.jedt.test;

import dev.runefox.jedt.api.gametest.GameTestEvents;
import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.gametest.framework.GameTestListener;
import net.minecraft.gametest.framework.GameTestRunner;

public class TestEventsListener implements GameTestListener {
    public static final TestEventsListener INSTANCE = new TestEventsListener();

    @Override
    public void testStructureLoaded(GameTestInfo gameTestInfo) {
        GameTestEvents.TEST_STRUCTURE_LOADED.invoker().onTestStructureLoaded(gameTestInfo);
    }

    @Override
    public void testPassed(GameTestInfo gameTestInfo, GameTestRunner gameTestRunner) {
        GameTestEvents.TEST_PASSED.invoker().onTestPassed(gameTestInfo);
    }

    @Override
    public void testFailed(GameTestInfo gameTestInfo, GameTestRunner gameTestRunner) {
        GameTestEvents.TEST_FAILED.invoker().onTestFailed(gameTestInfo);
    }

    @Override
    public void testAddedForRerun(GameTestInfo gameTestInfo, GameTestInfo gameTestInfo2, GameTestRunner gameTestRunner) {

    }
}
