package dev.runefox.jedt.api.gametest;

import com.google.gson.JsonElement;
import dev.runefox.jedt.test.ProperJUnitLikeTestReporter;
import dev.runefox.jedt.util.PathUtil;
import net.minecraft.gametest.framework.LogTestReporter;
import net.minecraft.gametest.framework.TestReporter;
import net.minecraft.resources.ResourceLocation;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

class TestReporterRegistry {
    static final Map<ResourceLocation, TestReporterType> REG = new HashMap<>();

    static {
        REG.put(ResourceLocation.parse("jedt:log"), new Log());
        REG.put(ResourceLocation.parse("jedt:junit"), new JUnit());
        REG.put(ResourceLocation.parse("log"), new Log());
        REG.put(ResourceLocation.parse("junit"), new JUnit());
    }

    private static class Log implements TestReporterType {
        @Override
        public TestReporter setup(JsonElement config, File serverDir) {
            return new LogTestReporter();
        }
    }

    private static class JUnit implements TestReporterType {
        @Override
        public TestReporter setup(JsonElement config, File serverDir) throws Exception {
            if (config == null) {
                throw new RuntimeException("No export file for junit reporter");
            }
            String path = config.getAsString();
            if (path.startsWith("[") && path.endsWith("]")) {
                path = System.getProperty(path.substring(1, path.length() - 1));
                if (path == null) {
                    throw new RuntimeException("No export file for junit reporter");
                }
            }
            return new ProperJUnitLikeTestReporter(PathUtil.resolve(serverDir.toPath(), config.getAsString()).toFile());
        }
    }
}
