package dev.runefox.jedt.render;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import net.minecraft.Util;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

import java.util.SortedMap;

public class DebugBuffers {
    private final SortedMap<RenderType, ByteBufferBuilder> fixedBuffers = Util.make(new Object2ObjectLinkedOpenHashMap<>(), map -> map.put(RenderType.LINES, new ByteBufferBuilder(256)));


    private final MultiBufferSource.BufferSource bufferSource;

    public DebugBuffers() {
        bufferSource = MultiBufferSource.immediateWithBuffers(fixedBuffers, new ByteBufferBuilder(256));
    }

    public MultiBufferSource.BufferSource getBufferSource() {
        return bufferSource;
    }
}
