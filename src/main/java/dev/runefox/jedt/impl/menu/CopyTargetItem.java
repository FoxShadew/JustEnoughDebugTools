package dev.runefox.jedt.impl.menu;

import dev.runefox.jedt.api.menu.ActionItem;
import dev.runefox.jedt.api.menu.OptionSelectContext;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Locale;

public class CopyTargetItem extends ActionItem {
    public CopyTargetItem(Component name) {
        super(name);
    }

    @Override
    public void onClick(OptionSelectContext context) {
        copyLookAt(context.minecraft(), context, true, !context.minecraft().showOnlyReducedInfo());
    }

    private void copyLookAt(Minecraft client, OptionSelectContext context, boolean copyNbt, boolean server) {
        assert client.player != null;
        assert client.level != null;

        HitResult target = client.hitResult;
        if (target != null) {
            switch (target.getType()) {
                case BLOCK -> {
                    BlockPos pos = ((BlockHitResult) target).getBlockPos();
                    BlockState state = client.level.getBlockState(pos);
                    if (copyNbt) {
                        if (server) {
                            client.player.connection.getDebugQueryHandler().queryBlockEntityTag(pos, nbt -> {
                                copyBlock(context, state, pos, nbt);
                                context.spawnResponse(Component.translatable("debug.options.jedt.copy_targeted.response_server_block"));
                            });
                        } else {
                            BlockEntity be = client.level.getBlockEntity(pos);
                            CompoundTag nbt = be != null ? be.saveWithId(client.level.registryAccess()) : null;
                            copyBlock(context, state, pos, nbt);
                            context.spawnResponse(Component.translatable("debug.options.jedt.copy_targeted.response_client_block"));
                        }
                    } else {
                        copyBlock(context, state, pos, null);
                        context.spawnResponse(Component.translatable("debug.options.jedt.copy_targeted.response_client_state"));
                    }
                }
                case ENTITY -> {
                    Entity entity = ((EntityHitResult) target).getEntity();
                    ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
                    if (copyNbt) {
                        if (server) {
                            client.player.connection.getDebugQueryHandler().queryEntityTag(entity.getId(), nbt -> {
                                copyEntity(context, id, entity.position(), nbt);
                                context.spawnResponse(Component.translatable("debug.options.jedt.copy_targeted.response_server_entity"));
                            });
                        } else {
                            CompoundTag nbt = entity.saveWithoutId(new CompoundTag());
                            copyEntity(context, id, entity.position(), nbt);
                            context.spawnResponse(Component.translatable("debug.options.jedt.copy_targeted.response_client_entity"));
                        }
                    } else {
                        copyEntity(context, id, entity.position(), null);
                        context.spawnResponse(Component.translatable("debug.options.jedt.copy_targeted.response_client_location"));
                    }
                }
            }

        }
    }

    protected void copyBlock(OptionSelectContext context, BlockState state, BlockPos pos, CompoundTag tag) {
        if (tag != null) {
            tag.remove("x");
            tag.remove("y");
            tag.remove("z");
            tag.remove("id");
        }

        StringBuilder builder = new StringBuilder(BlockStateParser.serialize(state));
        if (tag != null) {
            builder.append(tag);
        }

        String str = String.format(Locale.ROOT, "/setblock %d %d %d %s", pos.getX(), pos.getY(), pos.getZ(), builder);
        context.copyToClipboard(str);
    }

    protected void copyEntity(OptionSelectContext context, ResourceLocation id, Vec3 pos, CompoundTag nbt) {
        String str;
        if (nbt != null) {
            nbt.remove("UUID");
            nbt.remove("Pos");
            nbt.remove("Dimension");
            String nbtStr = nbt.toString();
            str = String.format(Locale.ROOT, "/summon %s %.2f %.2f %.2f %s", id.toString(), pos.x, pos.y, pos.z, nbtStr);
        } else {
            str = String.format(Locale.ROOT, "/summon %s %.2f %.2f %.2f", id.toString(), pos.x, pos.y, pos.z);
        }

        context.copyToClipboard(str);
    }
}
