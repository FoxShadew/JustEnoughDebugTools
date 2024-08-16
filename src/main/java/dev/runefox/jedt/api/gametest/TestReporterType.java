package dev.runefox.jedt.api.gametest;

import com.google.gson.JsonElement;

import net.minecraft.gametest.framework.TestReporter;
import net.minecraft.resources.ResourceLocation;

import java.io.File;

/**
 * @deprecated Use Fabric's testing API.
 */
@Deprecated(forRemoval = true)
public interface TestReporterType {
    TestReporter setup(JsonElement config, File serverDir) throws Exception;

    @Deprecated(forRemoval = true)
    static void register(ResourceLocation id, TestReporterType type) {
        if (TestReporterRegistry.REG.containsKey(id))
            throw new IllegalArgumentException("Reporter type " + id + " already registered");

        TestReporterRegistry.REG.put(id, type);
    }

    @Deprecated(forRemoval = true)
    static TestReporter instantiate(ResourceLocation id, JsonElement config, File serverDir) throws Exception {
        if (!TestReporterRegistry.REG.containsKey(id))
            throw new IllegalArgumentException("Reporter type " + id + " not found");

        return TestReporterRegistry.REG.get(id).setup(config, serverDir);
    }
}
