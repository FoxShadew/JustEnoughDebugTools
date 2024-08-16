package dev.runefox.jedt.test;

import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.gametest.framework.TestReporter;

import java.util.ArrayList;
import java.util.List;

@Deprecated
public class MultiTestReporter implements TestReporter {
    private final List<TestReporter> reporters = new ArrayList<>();

    @Override
    public void onTestFailed(GameTestInfo info) {
        reporters.forEach(r -> r.onTestFailed(info));
    }

    @Override
    public void onTestSuccess(GameTestInfo info) {
        reporters.forEach(r -> r.onTestSuccess(info));
    }

    public void addReporter(TestReporter reporter) {
        reporters.add(reporter);
    }
}
