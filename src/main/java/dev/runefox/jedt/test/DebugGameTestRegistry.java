//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package dev.runefox.jedt.test;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.jetbrains.annotations.Nullable;

import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Rotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

// Ever since Fabric added their own GameTest API, there is now a mixin into GameTestRegistry that will cause an
// exception if tests are not registered through the Fabric API ... sigh
//
// This is our own GameTestRegistry that Fabric can't see

/**
 * @deprecated A new more customizable testing system is planned for the future, for now the GameTest system from Mojang
 * will do and Fabric's API for that is now in favour.
 */
@Deprecated
public class DebugGameTestRegistry {
    private static final Collection<TestFunction> TEST_FUNCTIONS = Lists.newArrayList();
    private static final Set<String> TEST_CLASS_NAMES = Sets.newHashSet();
    private static final Map<String, Consumer<ServerLevel>> BEFORE_BATCH_FUNCTIONS = Maps.newHashMap();
    private static final Map<String, Consumer<ServerLevel>> AFTER_BATCH_FUNCTIONS = Maps.newHashMap();
    private static final Collection<TestFunction> LAST_FAILED_TESTS = Sets.newHashSet();

    public DebugGameTestRegistry() {
    }

    public static void register(Class<?> cls) {
        Arrays.stream(cls.getDeclaredMethods()).forEach(DebugGameTestRegistry::register);
    }

    public static void register(Method method) {
        String className = method.getDeclaringClass().getSimpleName();
        GameTest testInfo = method.getAnnotation(GameTest.class);

        if (testInfo != null) {
            TEST_FUNCTIONS.add(turnMethodIntoTestFunction(method));
            TEST_CLASS_NAMES.add(className);
        }

        GameTestGenerator generatorInfo = method.getAnnotation(GameTestGenerator.class);
        if (generatorInfo != null) {
            TEST_FUNCTIONS.addAll(useTestGeneratorMethod(method));
            TEST_CLASS_NAMES.add(className);
        }

        registerBatchFunction(method, BeforeBatch.class, BeforeBatch::batch, BEFORE_BATCH_FUNCTIONS);
        registerBatchFunction(method, AfterBatch.class, AfterBatch::batch, AFTER_BATCH_FUNCTIONS);
    }

    private static <T extends Annotation> void registerBatchFunction(Method method, Class<T> class_, Function<T, String> function, Map<String, Consumer<ServerLevel>> map) {
        T annotation = method.getAnnotation(class_);
        if (annotation != null) {
            String string = function.apply(annotation);
            Consumer<ServerLevel> consumer = map.putIfAbsent(string, turnMethodIntoConsumer(method));
            if (consumer != null) {
                throw new RuntimeException("Hey, there should only be one " + class_ + " method per batch. Batch '" + string + "' has more than one!");
            }
        }

    }

    public static Collection<TestFunction> getTestFunctionsForClassName(String name) {
        return TEST_FUNCTIONS.stream()
                             .filter(testFn -> isTestFunctionPartOfClass(testFn, name))
                             .collect(Collectors.toList());
    }

    public static Collection<TestFunction> getAllTestFunctions() {
        return TEST_FUNCTIONS;
    }

    public static Collection<String> getAllTestClassNames() {
        return TEST_CLASS_NAMES;
    }

    public static boolean isTestClass(String name) {
        return TEST_CLASS_NAMES.contains(name);
    }

    @Nullable
    public static Consumer<ServerLevel> getBeforeBatchFunction(String name) {
        return BEFORE_BATCH_FUNCTIONS.get(name);
    }

    @Nullable
    public static Consumer<ServerLevel> getAfterBatchFunction(String name) {
        return AFTER_BATCH_FUNCTIONS.get(name);
    }

    public static Optional<TestFunction> findTestFunction(String name) {
        return getAllTestFunctions().stream()
                                    .filter(testFn -> testFn.testName().equalsIgnoreCase(name))
                                    .findFirst();
    }

    public static TestFunction getTestFunction(String name) {
        Optional<TestFunction> optional = findTestFunction(name);
        if (optional.isEmpty()) {
            throw new IllegalArgumentException("Can't find the test function for " + name);
        } else {
            return optional.get();
        }
    }

    @SuppressWarnings("unchecked")
    private static Collection<TestFunction> useTestGeneratorMethod(Method generator) {
        try {
            Object instance = generator.getDeclaringClass().newInstance();
            return (Collection<TestFunction>) generator.invoke(instance);
        } catch (ReflectiveOperationException exc) {
            throw new RuntimeException(exc);
        }
    }

    private static TestFunction turnMethodIntoTestFunction(Method method) {
        GameTest testInfo = method.getAnnotation(GameTest.class);
        String clsName = method.getDeclaringClass().getSimpleName();
        String groupName = clsName.toLowerCase();
        String testName = groupName + "." + method.getName().toLowerCase();
        String structureName = testInfo.template().isEmpty() ? testName : groupName + "." + testInfo.template();
        String batchName = testInfo.batch();

        Rotation rotation = StructureUtils.getRotationForRotationSteps(testInfo.rotationSteps());
        return new TestFunction(
            batchName,
            testName,
            structureName,
            rotation,
            testInfo.timeoutTicks(),
            testInfo.setupTicks(),
            testInfo.required(),
            testInfo.manualOnly(),
            testInfo.attempts(),
            testInfo.requiredSuccesses(),
            testInfo.skyAccess(),
            turnMethodIntoConsumer(method)
        );
    }

    private static <T> Consumer<T> turnMethodIntoConsumer(Method method) {
        return arg -> {
            try {
                Object inst = method.getDeclaringClass().newInstance();
                method.invoke(inst, arg);
            } catch (InvocationTargetException exc) {
                if (exc.getCause() instanceof RuntimeException re)
                    throw re;
                else
                    throw new RuntimeException(exc.getCause());
            } catch (ReflectiveOperationException exc) {
                throw new RuntimeException(exc);
            }
        };
    }

    private static boolean isTestFunctionPartOfClass(TestFunction testFn, String className) {
        return testFn.testName().toLowerCase().startsWith(className.toLowerCase() + ".");
    }

    public static Collection<TestFunction> getLastFailedTests() {
        return LAST_FAILED_TESTS;
    }

    public static void rememberFailedTest(TestFunction testFunction) {
        LAST_FAILED_TESTS.add(testFunction);
    }

    public static void forgetFailedTests() {
        LAST_FAILED_TESTS.clear();
    }
}
