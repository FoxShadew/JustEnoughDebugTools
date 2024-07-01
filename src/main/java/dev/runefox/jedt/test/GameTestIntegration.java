package dev.runefox.jedt.test;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.runefox.jedt.Debug;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.world.level.block.Rotation;
import org.apache.logging.log4j.Logger;

import java.io.Reader;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

public class GameTestIntegration {
    private static final Logger LOGGER = Debug.LOGGER;



    //
    // RUNTIME TEST CONFIG
    //

    public static RuntimeTestConfig loadRuntimeTestConfig(Path path) {
        RuntimeTestConfig config = new RuntimeTestConfig();

        if (Files.exists(path) && Files.isRegularFile(path)) {
            JsonElement json;
            try (Reader reader = Files.newBufferedReader(path)) {
                json = new JsonParser().parse(reader);
            } catch (Exception exc) {
                LOGGER.error("Failed to load gametest runtime configuration", exc);
                return null;
            }
            try {
                loadRuntimeTestConfig(json, config);
            } catch (Exception exc) {
                LOGGER.error("Failed to load gametest runtime configuration", exc);
                return null;
            }
        }
        return config;
    }

    private static void loadRuntimeTestConfig(JsonElement el, RuntimeTestConfig config) {
        JsonObject obj = el.getAsJsonObject();

        // Mods to load tests from
        if (obj.has("mods")) {
            JsonArray arr = obj.getAsJsonArray("mods");
            for (JsonElement mod : arr)
                config.addMod(mod.getAsString());
        }

        // Sets to include functions from
        if (obj.has("sets")) {
            JsonArray arr = obj.getAsJsonArray("sets");
            for (JsonElement mod : arr)
                config.addSet(mod.getAsString());
        }

        // Filters
        loadFilter(obj.get("include")).ifPresent(config::addFilter);
        loadFilter(obj.get("exclude")).ifPresent(pred -> config.addFilter(pred.negate()));

        // Start location
        if (obj.has("start_pos")) {
            JsonArray arr = obj.getAsJsonArray("start_pos");
            if (arr.size() == 2) {
                int x = arr.get(0).getAsInt();
                int z = arr.get(1).getAsInt();
                config.start(x, z);
            } else {
                int x = arr.get(0).getAsInt();
                int y = arr.get(1).getAsInt();
                int z = arr.get(2).getAsInt();
                config.start(x, y, z);
            }
        }

        // World zip export path, allows for a java property as input to use with Gradle
        if (obj.has("export")) {
            config.exportPath(path(obj.get("export").getAsString()));
        }

        // Test structures directory, allows for a java property as input to use with Gradle
        if (obj.has("test_structures_dir")) {
            config.testStructuresPath(path(obj.get("test_structures_dir").getAsString()));
        }

        // Test server datapacks directory, allows for a java property as input to use with Gradle
        if (obj.has("datapacks_dir")) {
            config.datapacksPath(path(obj.get("datapacks_dir").getAsString()));
        }

        // Dimension to test in
        if (obj.has("dimension")) {
            config.dimension(obj.get("dimension").getAsString());
        }

        // Test reporter type + config. Loaded dynamically later on
        if (obj.has("reporter")) {
            config.reporter(obj.get("reporter"));
        }

        // Max simultaneous tests. Larger batches are split up into groups of this size
        if (obj.has("max_simultaneous")) {
            config.maxSimultaneous(obj.get("max_simultaneous").getAsInt());
        }

        // Tests per row
        if (obj.has("tests_per_row")) {
            config.testsPerRow(obj.get("tests_per_row").getAsInt());
        }

        // Tests rotation
        if (obj.has("rotation")) {
            config.testRotation(rotation(obj.get("rotation").getAsInt()));
        }
    }

    private static Rotation rotation(int angle) {
        if (angle % 90 != 0)
            throw new RuntimeException("'rotation' must be a multiple of 90 degrees");

        angle %= 360;
        if (angle < 0)
            angle += 360;

        return switch (angle) {
            default -> Rotation.NONE;
            case 90 -> Rotation.CLOCKWISE_90;
            case 180 -> Rotation.CLOCKWISE_180;
            case 270 -> Rotation.COUNTERCLOCKWISE_90;
        };
    }

    private static String path(String v) {
        if (v != null && v.startsWith("[") && v.endsWith("]")) {
            return System.getProperty(v.substring(1, v.length() - 1));
        }
        return v;
    }

    private static Optional<Predicate<TestFunction>> loadFilter(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return Optional.empty();
        }

        if (element.isJsonPrimitive()) {
            // "test"
            String str = element.getAsString();
            return Optional.of(filter(str));
        } else if (element.isJsonObject()) {
            // {"from": "class", "exclude": ["tests"]}
            JsonObject object = element.getAsJsonObject();

            String from = object.get("from").getAsString();
            JsonElement include = object.get("include");
            JsonElement exclude = object.get("exclude");

            return Optional.of(filter(from, include, exclude));
        } else if (element.isJsonArray()) {
            // ["tests"]
            JsonArray array = element.getAsJsonArray();
            List<Predicate<TestFunction>> filters = new ArrayList<>();
            for (JsonElement el : array) {
                if (el.isJsonPrimitive()) {
                    // [..., "test", ...]
                    filters.add(filter(el.getAsString()));
                } else if (el.isJsonObject()) {
                    // [..., {"from": "class", "exclude": ["tests"]}, ...]
                    JsonObject object = el.getAsJsonObject();

                    String from = object.get("from").getAsString();
                    JsonElement include = object.get("include");
                    JsonElement exclude = object.get("exclude");

                    filters.add(filter(from, include, exclude));
                } else {
                    // [..., null, ["tests"], ...] - not allowed
                    throw new RuntimeException("Filter in inclusion/exclusion list must be string or object");
                }
            }

            if (filters.isEmpty()) {
                return Optional.empty(); // Return empty so we don't necessarily apply an extra filter
            } else {
                return Optional.of(fn -> {
                    for (Predicate<TestFunction> filter : filters) {
                        if (!filter.test(fn)) return false;
                    }
                    return true;
                });
            }
        } else {
            throw new AssertionError("owo");
        }
    }

    private static Predicate<TestFunction> filter(String str, JsonElement includeEl, JsonElement excludeEl) {

        Predicate<TestFunction> theMutableFilter = fn -> true;

        // Filter out not-included
        Optional<Predicate<TestFunction>> include = loadFilter(includeEl);
        if (include.isPresent())
            theMutableFilter = include.get();

        // Filter out excluded
        Optional<Predicate<TestFunction>> exclude = loadFilter(excludeEl);
        if (exclude.isPresent())
            theMutableFilter = theMutableFilter.and(exclude.get().negate());

        // Java...
        Predicate<TestFunction> theAlmightyFinalFilter = theMutableFilter;

        if (str.startsWith("@")) {
            String batch = str.substring(1);
            return fn -> fn.batchName().equalsIgnoreCase(batch) && theAlmightyFinalFilter.test(fn);
        } else if (str.contains(".")) {
            throw new RuntimeException("Cannot include/exclude from one single test");
        } else {
            return fn -> isTestFunctionPartOfClass(fn, str) && theAlmightyFinalFilter.test(fn);
        }
    }

    private static Predicate<TestFunction> filter(String str) {
        if (str.startsWith("@")) {
            String batch = str.substring(1);
            return fn -> fn.batchName().equalsIgnoreCase(batch);
        } else if (str.contains(".")) {
            return fn -> fn.testName().equalsIgnoreCase(str);
        } else {
            return fn -> isTestFunctionPartOfClass(fn, str);
        }
    }

    private static boolean isTestFunctionPartOfClass(TestFunction fn, String cls) {
        return fn.testName().toLowerCase().startsWith(cls.toLowerCase() + ".");
    }



    //
    // MOD TEST CONFIG
    //

    public static ModTestConfig loadModTestConfig(ModContainer container) {
        ModTestConfig config = new ModTestConfig(container);

        Path path = container.getPath("jedt.tests.json");

        if (Files.exists(path) && Files.isRegularFile(path)) {
            JsonElement json;
            try (Reader reader = Files.newBufferedReader(path)) {
                json = new JsonParser().parse(reader);
            } catch (Exception exc) {
                LOGGER.error("Failed to load jedt.tests.json for mod '" + container.getMetadata().getId() + "'", exc);
                return null;
            }
            try {
                loadModTestConfig(json, config);
            } catch (Exception exc) {
                LOGGER.error("Failed to load jedt.tests.json for mod '" + container.getMetadata().getId() + "'", exc);
                return null;
            }
        }

        return config;
    }

    private static void loadModTestConfig(JsonElement el, ModTestConfig config) throws Exception {
        JsonObject obj = el.getAsJsonObject();
        for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
            loadTestClasses(entry.getValue().getAsJsonArray(), config.builder(entry.getKey()));
        }
    }

    private static void loadTestClasses(JsonArray array, ModTestConfig.Builder builder) throws Exception {
        for (JsonElement element : array) {
            String test = element.getAsString();

            if (test.startsWith("#")) {
                // Inherited test set
                builder.inheritSet(test.substring(1));
                continue;
            }

            int sepIndex = test.indexOf("::");

            String cn = test;
            String mn = null;
            if (sepIndex >= 0) {
                cn = test.substring(0, sepIndex);
                mn = test.substring(sepIndex + 2);
            }

            try {
                Class<?> c = Class.forName(cn);
                if (mn == null) {
                    builder.allMethods(c);
                } else {
                    for (Method method : c.getDeclaredMethods()) {
                        if (method.getName().equals(mn)) {
                            if (!Modifier.isPrivate(method.getModifiers())) {
                                builder.singleMethod(method);
                            }
                        }
                    }
                }
            } catch (ClassNotFoundException exc) {
                LOGGER.error("Test class " + cn + " not found");
            }
        }
    }
}
