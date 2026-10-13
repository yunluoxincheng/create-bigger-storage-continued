package uwu.lopyluna.create_bs.content.vault;

import com.mojang.logging.LogUtils;
import com.simibubi.create.foundation.blockEntity.IMultiBlockEntityContainer;
import com.simibubi.create.content.logistics.vault.ItemVaultBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Server-thread transaction: preserve every member's items, keep orientation, then consume components. */
public final class VaultUpgradeService {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ThreadLocal<Boolean> UPDATING = ThreadLocal.withInitial(() -> false);
    private static final int UPDATE_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;

    private record Snapshot(BlockPos pos, BlockState state, CompoundTag data, ItemStackHandler items) {}
    private record Payment(int slot, int count) {}

    private VaultUpgradeService() {}

    public static boolean isUpdating() {
        return UPDATING.get();
    }

    public static InteractionResult upgrade(UseOnContext context, VaultUpgradeItem upgrade) {
        if (!(context.getLevel() instanceof ServerLevel level)
            || !(context.getPlayer() instanceof ServerPlayer player) || player instanceof FakePlayer)
            return InteractionResult.FAIL;
        if (player.isSpectator() || !level.getServer().isSameThread() || isUpdating())
            return fail(player, "unavailable");

        List<Snapshot> snapshots;
        List<Payment> payment;
        BlockState targetState;
        int targetSlots;
        try {
            BlockEntity clicked = level.getBlockEntity(context.getClickedPos());
            if (!(clicked instanceof IMultiBlockEntityContainer.Inventory part)) return fail(player, "structure");
            BlockEntity root = part.getControllerBE();
            if (!(root instanceof IMultiBlockEntityContainer.Inventory structure)) return fail(player, "structure");
            if (root.saveWithFullMetadata().contains("Uninitialized")) return fail(player, "structure");
            BlockPos anchor = root.getBlockPos();
            Direction.Axis axis = structure.getMainConnectionAxis();
            int width = structure.getWidth();
            int length = structure.getHeight();
            if (width < 1 || width > 3 || length < 1 || length > 64) return fail(player, "structure");

            Block target = upgrade.to.target(axis == Direction.Axis.Y);
            if (!(target instanceof EntityBlock entityBlock)) return fail(player, "connected");
            targetState = target.defaultBlockState();
            if (targetState.hasProperty(TieredVaultBlock.HORIZONTAL_AXIS))
                targetState = targetState.setValue(TieredVaultBlock.HORIZONTAL_AXIS, axis);
            if (targetState.hasProperty(TieredVaultBlock.LARGE))
                targetState = targetState.setValue(TieredVaultBlock.LARGE, width > 2);
            BlockEntity prototype = entityBlock.newBlockEntity(anchor, targetState);
            if (!(prototype instanceof IMultiBlockEntityContainer.Inventory targetStructure))
                return fail(player, "structure");
            ItemStackHandler targetInventory = inventory(prototype);
            if (targetInventory == null || width > targetStructure.getMaxWidth()
                || length > targetStructure.getMaxLength(axis, width)) return fail(player, "geometry");
            targetSlots = targetInventory.getSlots();

            snapshots = new ArrayList<>();
            for (int l = 0; l < length; l++) {
                for (int a = 0; a < width; a++) {
                    for (int b = 0; b < width; b++) {
                        BlockPos pos = VaultGeometry.position(anchor, axis, l, a, b);
                        if (!level.hasChunkAt(pos) || !level.mayInteract(player, pos)
                            || !player.mayUseItemAt(pos, context.getClickedFace(), context.getItemInHand()))
                            return fail(player, "access");
                        BlockState state = level.getBlockState(pos);
                        BlockEntity member = level.getBlockEntity(pos);
                        if (!upgrade.from.matches(state)
                            || !(member instanceof IMultiBlockEntityContainer.Inventory memberStructure)
                            || !memberStructure.getController().equals(anchor)
                            || memberStructure.getMainConnectionAxis() != axis)
                            return fail(player, "structure");
                        ItemStackHandler contents = inventory(member);
                        if (contents == null || contents.getSlots() > targetSlots) return fail(player, "capacity");
                        snapshots.add(new Snapshot(pos.immutable(), state, member.saveWithFullMetadata(), copy(contents)));
                    }
                }
            }
            payment = payment(player, upgrade, snapshots.size());
            if (!player.getAbilities().instabuild && payment.isEmpty())
                return fail(player, "components", snapshots.size());
        } catch (RuntimeException exception) {
            LOGGER.error("Could not prepare a vault upgrade", exception);
            return fail(player, "prepare");
        }

        UPDATING.set(true);
        try {
            for (Snapshot snapshot : snapshots) {
                BlockEntity old = level.getBlockEntity(snapshot.pos());
                ItemStackHandler oldInventory = inventory(old);
                if (!level.getBlockState(snapshot.pos()).equals(snapshot.state())
                    || oldInventory == null || !sameContents(oldInventory, snapshot.items()))
                    throw new IllegalStateException("Vault changed during upgrade preparation");
                clear(oldInventory);
            }
            // All old members are now silently empty. Even vanilla/Connected removal hooks
            // cannot ship an item that will later be restored from our snapshot.
            for (Snapshot snapshot : snapshots) {
                if (!level.setBlock(snapshot.pos(), targetState, UPDATE_FLAGS))
                    throw new IllegalStateException("Could not replace a vault member");
            }
            // Restore contents only after the complete target geometry exists.
            for (Snapshot snapshot : snapshots) {
                BlockEntity replacement = level.getBlockEntity(snapshot.pos());
                ItemStackHandler resized = new ItemStackHandler(targetSlots);
                for (int slot = 0; slot < snapshot.items().getSlots(); slot++)
                    resized.setStackInSlot(slot, snapshot.items().getStackInSlot(slot).copy());
                CompoundTag data = snapshot.data().copy();
                data.putString("id", Objects.requireNonNull(ForgeRegistries.BLOCK_ENTITY_TYPES
                    .getKey(Objects.requireNonNull(replacement).getType())).toString());
                data.put("Inventory", resized.serializeNBT());
                data.remove("StorageType");
                replacement.load(data);
                if (!sameContents(Objects.requireNonNull(inventory(replacement)), snapshot.items()))
                    throw new IllegalStateException("Vault contents did not survive conversion");
            }
            // Inspect all component slots again before any consumption.
            for (Payment step : payment) {
                ItemStack current = player.getInventory().getItem(step.slot());
                if (!current.is(upgrade) || current.getCount() < step.count())
                    throw new IllegalStateException("Upgrade components changed during conversion");
            }
        } catch (RuntimeException exception) {
            LOGGER.error("Vault upgrade failed; attempting inventory-preserving rollback", exception);
            try {
                rollback(level, snapshots);
                return fail(player, "rollback");
            } catch (RuntimeException recoveryFailure) {
                exception.addSuppressed(recoveryFailure);
                boolean saved = false;
                try {
                    UpgradeRecoveryData.preserve(level, recoverySnapshot(snapshots, upgrade));
                    saved = true;
                } catch (RuntimeException storageFailure) {
                    LOGGER.error("Could not save the upgrade recovery record", storageFailure);
                }
                LOGGER.error("Vault rollback requires recovery from the saved upgrade record", recoveryFailure);
                return fail(player, saved ? "recovery" : "recovery_unsaved");
            }
        } finally {
            UPDATING.remove();
        }

        // No fallible world mutations between the verified payment plan and inventory consumption.
        for (Payment step : payment) player.getInventory().getItem(step.slot()).shrink(step.count());
        player.getInventory().setChanged();
        finish(level, snapshots);
        level.playSound(null, context.getClickedPos(), SoundEvents.SMITHING_TABLE_USE,
            SoundSource.BLOCKS, 0.8F, 1.0F);
        player.displayClientMessage(Component.translatable("create_bs.upgrade.success",
            snapshots.size(), player.getAbilities().instabuild ? 0 : snapshots.size()), true);
        return InteractionResult.SUCCESS;
    }

    private static List<Payment> payment(ServerPlayer player, VaultUpgradeItem item, int required) {
        List<Payment> result = new ArrayList<>();
        if (player.getAbilities().instabuild) return result;
        for (int slot = 0; slot < player.getInventory().getContainerSize() && required > 0; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!stack.is(item)) continue;
            int take = Math.min(required, stack.getCount());
            result.add(new Payment(slot, take));
            required -= take;
        }
        return required == 0 ? result : List.of();
    }

    private static ItemStackHandler inventory(BlockEntity entity) {
        if (entity instanceof TieredVaultBlockEntity vault) return vault.getInventoryOfBlock();
        if (entity instanceof ItemVaultBlockEntity vault) return vault.getInventoryOfBlock();
        if (entity == null || !UpgradeTier.SILO.equals(ForgeRegistries.BLOCKS.getKey(entity.getBlockState().getBlock())))
            return null;
        // Optional interoperability through its public inventory accessor; no Connected implementation is copied.
        try {
            Object result = entity.getClass().getMethod("getInventoryOfBlock").invoke(entity);
            return result instanceof ItemStackHandler handler ? handler : null;
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unsupported Connected item silo inventory API", exception);
        }
    }

    private static ItemStackHandler copy(ItemStackHandler source) {
        ItemStackHandler copy = new ItemStackHandler(source.getSlots());
        for (int i = 0; i < source.getSlots(); i++) copy.setStackInSlot(i, source.getStackInSlot(i).copy());
        return copy;
    }

    private static void clear(ItemStackHandler handler) {
        // These approved vault handlers inherit a no-op onLoad. Avoid per-slot callbacks:
        // comparator changes could synchronously trigger a connected packager mid-transaction.
        handler.deserializeNBT(new ItemStackHandler(handler.getSlots()).serializeNBT());
    }

    private static boolean sameContents(ItemStackHandler actual, ItemStackHandler expected) {
        if (actual.getSlots() < expected.getSlots()) return false;
        for (int i = 0; i < actual.getSlots(); i++) {
            ItemStack wanted = i < expected.getSlots() ? expected.getStackInSlot(i) : ItemStack.EMPTY;
            ItemStack stored = actual.getStackInSlot(i);
            if (stored.isEmpty() && wanted.isEmpty()) continue;
            if (stored.getCount() != wanted.getCount() || !ItemStack.isSameItemSameTags(stored, wanted)) return false;
        }
        return true;
    }

    private static void rollback(ServerLevel level, List<Snapshot> snapshots) {
        // Restore all original blocks first, then all original NBT: removal may affect neighbours' controllers.
        for (Snapshot snapshot : snapshots) {
            if (level.getBlockState(snapshot.pos()).equals(snapshot.state())) continue;
            ItemStackHandler current = inventory(level.getBlockEntity(snapshot.pos()));
            if (current != null) clear(current);
            if (!level.setBlock(snapshot.pos(), snapshot.state(), UPDATE_FLAGS))
                throw new IllegalStateException("Could not restore an original vault member");
        }
        for (Snapshot snapshot : snapshots) {
            BlockEntity restored = Objects.requireNonNull(level.getBlockEntity(snapshot.pos()));
            restored.load(snapshot.data().copy());
            if (!sameContents(Objects.requireNonNull(inventory(restored)), snapshot.items()))
                throw new IllegalStateException("Original vault inventory could not be restored");
        }
        finish(level, snapshots);
    }

    private static void finish(ServerLevel level, List<Snapshot> snapshots) {
        for (Snapshot snapshot : snapshots) {
            try {
                BlockEntity entity = level.getBlockEntity(snapshot.pos());
                if (entity instanceof IMultiBlockEntityContainer.Inventory structure) {
                    BlockEntity controller = structure.getControllerBE();
                    if (controller instanceof IMultiBlockEntityContainer.Inventory root) {
                        structure.setWidth(root.getWidth());
                        structure.setHeight(root.getHeight());
                    }
                    structure.preventConnectivityUpdate();
                    structure.notifyMultiUpdated();
                }
                if (entity != null) entity.setChanged();
                level.sendBlockUpdated(snapshot.pos(), snapshot.state(), level.getBlockState(snapshot.pos()), Block.UPDATE_CLIENTS);
                level.updateNeighborsAt(snapshot.pos(), level.getBlockState(snapshot.pos()).getBlock());
            } catch (RuntimeException exception) {
                LOGGER.error("Could not notify a neighbour after a completed vault transaction", exception);
            }
        }
    }

    private static CompoundTag recoverySnapshot(List<Snapshot> snapshots, VaultUpgradeItem upgrade) {
        CompoundTag result = new CompoundTag();
        result.putString("From", upgrade.from.id);
        result.putString("To", upgrade.to.id);
        ListTag members = new ListTag();
        for (Snapshot snapshot : snapshots) {
            CompoundTag member = new CompoundTag();
            member.put("Pos", NbtUtils.writeBlockPos(snapshot.pos()));
            member.put("State", NbtUtils.writeBlockState(snapshot.state()));
            member.put("BlockEntity", snapshot.data().copy());
            members.add(member);
        }
        result.put("Members", members);
        return result;
    }

    private static InteractionResult fail(ServerPlayer player, String reason, Object... arguments) {
        player.displayClientMessage(Component.translatable("create_bs.upgrade.error." + reason, arguments), true);
        return InteractionResult.FAIL;
    }
}
