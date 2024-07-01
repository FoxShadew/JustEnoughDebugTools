package dev.runefox.jedt.impl.status;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import dev.runefox.jedt.api.status.DebugStatusKey;
import dev.runefox.jedt.api.status.ServerDebugStatus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("unchecked")
public class ServerDebugStatusImpl implements ServerDebugStatus.Mutable, CustomPacketPayload {
    private boolean available;
    private final ImmutableSet<DebugStatusKey<?>> keys;
    private final ImmutableMap<String, DebugStatusKey<?>> keysByName;
    private final Map<String, Object> statuses = new HashMap<>();
    private final ImmutableMap<String, Object> defaultValues;

    private ServerDebugStatusImpl(ImmutableSet<DebugStatusKey<?>> keys, ImmutableMap<String, Object> defaultValues) {
        this.keys = keys;
        this.defaultValues = defaultValues;

        for (DebugStatusKey<?> key : keys) {
            statuses.put(key.getName(), defaultValues.get(key.getName()));
        }

        ImmutableMap.Builder<String, DebugStatusKey<?>> byNameBuilder = ImmutableMap.builder();
        for (DebugStatusKey<?> key : keys) {
            byNameBuilder.put(key.getName(), key);
        }
        this.keysByName = byNameBuilder.build();
    }

    @Override
    public boolean isDebugAvailable() {
        return available;
    }

    @Override
    public <T> T getStatus(DebugStatusKey<T> key) {
        if (key == null) {
            throw new NullPointerException("Key can't be null");
        }
        if (!keys.contains(key)) {
            throw new IllegalArgumentException("Unknown key: '" + key.getName() + "'");
        }
        if (!isDebugAvailable()) {
            return key.getUnavailableValue();
        }
        return (T) statuses.get(key.getName());
    }

    @Override
    public <T> boolean isAvailable(DebugStatusKey<T> key) {
        if (key == null) {
            throw new NullPointerException("Key can't be null");
        }
        if (!keys.contains(key)) {
            throw new IllegalArgumentException("Unknown key: '" + key.getName() + "'");
        }
        return isDebugAvailable() && key.isAvailable(getStatus(key));
    }

    @Override
    public void setDebugAvailable(boolean available) {
        this.available = available;
    }

    @Override
    public void disableDebug() {
        setDebugAvailable(false);
    }

    @Override
    public void enableDebug() {
        setDebugAvailable(true);
    }

    @Override
    public <T> void setStatus(DebugStatusKey<T> key, T value) {
        if (key == null) {
            throw new NullPointerException("Key can't be null");
        }
        if (!keys.contains(key)) {
            throw new IllegalArgumentException("Unknown key: '" + key.getName() + "'");
        }
        if (value == null) {
            throw new NullPointerException("Value can't be null");
        }
        statuses.put(key.getName(), value);
    }

    @Override
    public <T> void reset(DebugStatusKey<T> key) {
        if (key == null) {
            throw new NullPointerException("Key can't be null");
        }
        if (!keys.contains(key)) {
            throw new IllegalArgumentException("Unknown key: '" + key.getName() + "'");
        }
        setStatus(key, (T) defaultValues.get(key.getName()));
    }

    @Override
    public <T> void disable(DebugStatusKey<T> key) {
        if (key == null) {
            throw new NullPointerException("Key can't be null");
        }
        if (!keys.contains(key)) {
            throw new IllegalArgumentException("Unknown key: '" + key.getName() + "'");
        }
        setStatus(key, key.getUnavailableValue());
    }

    public void disableAll() {
        setDebugAvailable(false);
        for (DebugStatusKey<?> key : keys) {
            disable(key);
        }
    }

    public void resetAll() {
        setDebugAvailable(true);
        for (DebugStatusKey<?> key : keys) {
            reset(key);
        }
    }

    public void read(JsonObject object) {
        setDebugAvailable(false);
        if (object.has("available")) {
            JsonElement available = object.get("available");
            if (available.isJsonPrimitive()) {
                JsonPrimitive p = available.getAsJsonPrimitive();
                if (p.isBoolean()) {
                    setDebugAvailable(p.getAsBoolean());
                }
            }
        }

        if (!isDebugAvailable()) {
            return;
        }

        for (DebugStatusKey<?> key : keys) {
            String name = key.getName();
            if (object.has(name)) {
                statuses.put(name, key.read(object.get(name)));
            } else {
                reset(key);
            }
        }
    }

    public void serialize(FriendlyByteBuf buf) {
        buf.writeBoolean(isDebugAvailable());
        if (isDebugAvailable()) {
            buf.writeInt(keys.size());
            for (DebugStatusKey<?> key : keys) {
                writeKey(buf, key);
            }
        }
    }

    public void deserialize(FriendlyByteBuf buf) {
        synchronized (this) {
            setDebugAvailable(buf.readBoolean());
            if (isDebugAvailable()) {
                int count = buf.readInt();
                while (count > 0) {
                    readKey(buf);
                    count--;
                }
            }
        }
    }

    private <T> void writeKey(FriendlyByteBuf buf, DebugStatusKey<T> key) {
        buf.writeUtf(key.getName(), Short.MAX_VALUE);
        key.write(getStatus(key), buf);
    }

    private void readKey(FriendlyByteBuf buf) {
        String name = buf.readUtf();
        DebugStatusKey<?> key = keysByName.get(name);
        if (key == null) {
            throw new IllegalStateException("Received unknown status key: " + name);
        }
        readKey(buf, key);
    }

    private <T> void readKey(FriendlyByteBuf buf, DebugStatusKey<T> key) {
        setStatus(key, key.read(buf));
    }

    public void log(Logger logger) {
        if (!isDebugAvailable()) {
            logger.info("Server debug status: unavailable");
        } else {
            logger.info("Server debug status: available");
            for (DebugStatusKey<?> key : keys) {
                logger.info(" - {}: {}", key.getLoggableName(), getLoggableValue(key));
            }
        }
    }

    private <T> String getLoggableValue(DebugStatusKey<T> key) {
        return key.getLoggableValue(getStatus(key));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return null;
    }

    public static class Builder implements ServerDebugStatus.Builder {
        private final Set<DebugStatusKey<?>> keys = new HashSet<>();
        private final Map<String, Object> defaultValues = new HashMap<>();

        @Override
        public <T> void registerKey(DebugStatusKey<T> key, T defaultValue) {
            if (key == null) {
                throw new NullPointerException("Key can't be null");
            }
            if (key.getName().equals("available")) {
                throw new IllegalStateException("Key name can't be 'available'");
            }
            if (keys.contains(key) || defaultValues.containsKey(key.getName())) {
                throw new IllegalStateException("Key '" + key.getName() + "' already registered");
            }
            if (defaultValue == null) {
                throw new NullPointerException("Default value can't be null");
            }

            keys.add(key);
            defaultValues.put(key.getName(), defaultValue);
        }

        public ServerDebugStatusImpl build() {
            return new ServerDebugStatusImpl(ImmutableSet.copyOf(keys), ImmutableMap.copyOf(defaultValues));
        }
    }
}
