package dev.runefox.jedt.test;

import net.fabricmc.loader.api.ModContainer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

@Deprecated
public class ModTestConfig {
    private static final Logger LOGGER = LogManager.getLogger();

    private final String modId;
    private final ModContainer container;
    private final Map<String, Builder> builders = new HashMap<>();

    public ModTestConfig(ModContainer container) {
        this.container = container;
        this.modId = container.getMetadata().getId();
    }

    public String getModId() {
        return modId;
    }

    public ModContainer getContainer() {
        return container;
    }

    public Stream<String> getSets() {
        return builders.keySet().stream();
    }

    public Builder builder(String set) {
        return builders.computeIfAbsent(set, k -> new Builder());
    }

    public Stream<Method> getMethods(String... sets) {
        if (sets.length == 0) {
            return Stream.empty();
        }

        if (sets.length == 1) {
            return getMethods(sets[0], new HashSet<>());
        }

        Set<String> loaded = new HashSet<>();

        List<Stream<Method>> streams = new ArrayList<>();

        // Don't use stream API for this loop, it tracks an external state outside of the loop
        for (String set : sets) {
            streams.add(getMethods(set, loaded));
        }

        return streams.stream().flatMap(Function.identity()); // concatenates all streams in the stream
    }

    private Stream<Method> getMethods(String set, Set<String> loaded) {
        if (loaded.contains(set)) {
            LOGGER.debug("Attempt to load set '{}' for mod '{}' twice", set, modId);
            return Stream.empty();
        }

        loaded.add(set);

        Builder builder = builders.get(set);
        if (builder == null) {
            LOGGER.debug("Attempt to load undefined set '{}' for mod '{}'", set, modId);
            return Stream.empty();
        }

        List<Provider> providers = builder.list;
        if (providers.isEmpty()) {
            LOGGER.debug("Attempt to load undefined set '{}' for mod '{}'", set, modId);
            return Stream.empty();
        }

        List<Stream<Method>> streams = new ArrayList<>();

        // Don't use stream API for this loop, it tracks an external state outside of the loop
        for (Provider provider : providers) {
            streams.add(provider.getMethods(this, loaded));
        }

        return streams.stream().flatMap(Function.identity()); // concatenates all streams in the stream
    }

    private static Provider singleMethod(Method method) {
        return (config, loaded) -> Stream.of(method);
    }

    private static Provider allMethods(Class<?> cls) {
        return (config, loaded) -> Stream.of(cls.getDeclaredMethods()).filter(mtd -> !Modifier.isPrivate(mtd.getModifiers()));
    }

    private static Provider inheritSet(String set) {
        return (config, loaded) -> config.getMethods(set, loaded);
    }

    private interface Provider {
        Stream<Method> getMethods(ModTestConfig config, Set<String> loaded);
    }

    public static class Builder {
        private final List<Provider> list;

        private Builder() {
            this.list = new ArrayList<>();
        }

        public Builder singleMethod(Method method) {
            list.add(ModTestConfig.singleMethod(method));
            return this;
        }

        public Builder allMethods(Class<?> cls) {
            list.add(ModTestConfig.allMethods(cls));
            return this;
        }

        public Builder inheritSet(String set) {
            list.add(ModTestConfig.inheritSet(set));
            return this;
        }
    }
}
