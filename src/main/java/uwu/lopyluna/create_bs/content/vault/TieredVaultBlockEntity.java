package uwu.lopyluna.create_bs.content.vault;

import com.simibubi.create.api.connectivity.ConnectivityHandler;
import com.simibubi.create.api.packager.InventoryIdentifier;
import com.simibubi.create.foundation.blockEntity.IMultiBlockEntityContainer;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.inventory.VersionedInventoryWrapper;
import com.simibubi.create.foundation.item.ItemHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.wrapper.CombinedInvWrapper;
import org.jetbrains.annotations.NotNull;
import uwu.lopyluna.create_bs.content.TierMaterials;
import uwu.lopyluna.create_bs.registry.BSBlockEntities;

import java.util.*;

public class TieredVaultBlockEntity extends SmartBlockEntity implements IMultiBlockEntityContainer.Inventory {

    protected LazyOptional<IItemHandler> itemCapability;
    protected InventoryIdentifier invId;

    protected ItemStackHandler inventory;
    protected BlockPos controller;
    protected BlockPos lastKnownPos;
    protected boolean updateConnectivity;
    protected int radius;
    protected int length;
    protected Direction.Axis axis;
    TierMaterials tierMaterials;
    private boolean comparatorsDirty;
    private boolean visibleInventoryDirty;
    private int comparatorOutput = -1;
    private long lastInventorySaveTick = Long.MIN_VALUE;

    public TieredVaultBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, TierMaterials tierMaterials) {
        super(type, pos, state);
        this.tierMaterials = tierMaterials;

        inventory = new ItemStackHandler(tierMaterials.capacity) {
            @Override
            protected void onContentsChanged(int slot) {
                super.onContentsChanged(slot);
                if (VaultUpgradeService.isUpdating()) return;
                updateComparators();
                if (level == null || level.isClientSide) return;
                // Mark the owning chunk immediately, without setChanged()'s
                // synchronous comparator notification on every slot mutation.
                long tick = level.getGameTime();
                if (lastInventorySaveTick != tick) {
                    lastInventorySaveTick = tick;
                    level.blockEntityChanged(worldPosition);
                }
                if (tierMaterials.seeThrough && slot < inventory.getSlots() / 2)
                    visibleInventoryDirty = true;
            }
        };

        itemCapability = LazyOptional.empty();
        radius = 1;
        length = 1;
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {}

    protected void updateConnectivity() {
        updateConnectivity = false;
        if (level == null || level.isClientSide()) return;
        if (VaultUpgradeService.isUpdating()) return;
        if (VaultConnectivity.tryExpandCrossSection(this)) return;
        if (!isController()) return;
        ConnectivityHandler.formMulti(this);
    }

    protected void updateComparators() {
        if (level == null || level.isClientSide || VaultUpgradeService.isUpdating()) return;
        TieredVaultBlockEntity controllerBE = getControllerBE();
        if (controllerBE == null) return;
        controllerBE.comparatorOutput = -1;
        controllerBE.comparatorsDirty = true;
    }

    private void flushComparatorUpdates() {
        if (!comparatorsDirty) return;
        if (!isController()) {
            // A pending update may outlive a split, merge or NBT load.
            comparatorsDirty = false;
            updateComparators();
            return;
        }
        // Clear before notifying: a redstone callback can mutate inventory again.
        comparatorsDirty = false;

        BlockPos pos = getBlockPos();
        Direction.Axis mainAxis = getMainConnectionAxis();
        for (int l = 0; l < length; l++)
            for (int a = 0; a < radius; a++)
                for (int b = 0; b < radius; b++)
                    level.updateNeighbourForOutputSignal(VaultGeometry.position(pos,
                        mainAxis, l, a, b), getBlockState().getBlock());
    }

    public int getComparatorOutput() {
        if (VaultUpgradeService.isUpdating()) return 0;
        TieredVaultBlockEntity root = getControllerBE();
        if (root == null) return 0;
        if (!root.itemCapability.isPresent()) {
            root.comparatorOutput = -1;
            root.initCapability();
        }
        // An incomplete/unloaded structure must not cache a temporary zero.
        if (!root.itemCapability.isPresent()) return 0;
        if (root.comparatorOutput < 0) {
            root.comparatorOutput = root.itemCapability.map(ItemHelper::calcRedstoneFromInventory).orElse(0);
        }
        return root.comparatorOutput;
    }

    @Override
    public void tick() {
        super.tick();

        if (lastKnownPos == null) lastKnownPos = getBlockPos();
        else if (!lastKnownPos.equals(worldPosition)) {
            onPositionChanged();
            return;
        }

        if (updateConnectivity) updateConnectivity();
        if (level == null || level.isClientSide || VaultUpgradeService.isUpdating()) return;
        flushComparatorUpdates();
        if (visibleInventoryDirty) {
            visibleInventoryDirty = false;
            sendData();
        }
    }

    @Override
    public BlockPos getLastKnownPos() {
        return lastKnownPos;
    }

    @Override
    public boolean isController() {
        return controller == null ||
                worldPosition.getX() == controller.getX() &&
                worldPosition.getY() == controller.getY() &&
                worldPosition.getZ() == controller.getZ();
    }

    private void onPositionChanged() {
        removeController(true);
        lastKnownPos = worldPosition;
    }

    @Override
    @SuppressWarnings("unchecked")
    public TieredVaultBlockEntity getControllerBE() {
        if (isController() || level == null) return this;
        BlockEntity blockEntity = level.getBlockEntity(controller);
        if (blockEntity instanceof TieredVaultBlockEntity vault && vault.getType() == getType()) return vault;
        return null;
    }

    public void removeController(boolean keepContents) {
        if (level == null || level.isClientSide()) return;
        updateConnectivity = true;
        controller = null;
        radius = 1;
        length = 1;

        BlockState state = getBlockState();
        if (TieredVaultBlock.isVault(state, tierMaterials)) {
            state = state.setValue(TieredVaultBlock.LARGE, false);
            level.setBlock(worldPosition, state, 22);
        }

        itemCapability.invalidate();
        invId = null;
        comparatorOutput = -1;
        updateComparators();
        setChanged();
        sendData();
    }

    @Override
    public void setController(BlockPos controller) {
        if (level == null || (level.isClientSide && !isVirtual())) return;
        if (controller.equals(this.controller)) return;
        this.controller = controller;
        itemCapability.invalidate();
        invId = null;
        comparatorOutput = -1;
        updateComparators();
        setChanged();
        sendData();
    }

    @Override
    public BlockPos getController() {
        return isController() ? worldPosition : controller;
    }

    @Override
    protected void read(CompoundTag compound, boolean clientPacket) {
        super.read(compound, clientPacket);

        BlockPos controllerBefore = controller;
        int prevSize = radius;
        int prevLength = length;

        updateConnectivity = compound.contains("Uninitialized");
        controller = null;
        lastKnownPos = null;

        if (compound.contains("LastKnownPos")) lastKnownPos = NbtUtils.readBlockPos(compound.getCompound("LastKnownPos"));
        if (compound.contains("Controller")) controller = NbtUtils.readBlockPos(compound.getCompound("Controller"));

        if (isController()) {
            radius = Math.max(1, compound.getInt("Size"));
            length = Math.max(1, compound.getInt("Length"));
        }

        if (compound.contains("Inventory")) {
            CompoundTag saved = compound.getCompound("Inventory").copy();
            saved.putInt("Size", Math.max(tierMaterials.capacity, saved.getInt("Size")));
            inventory.deserializeNBT(saved);
        }
        itemCapability.invalidate();
        invId = null;
        comparatorOutput = -1;
        // Do not resolve another block entity while this one's NBT is loading.
        comparatorsDirty = !clientPacket;
        visibleInventoryDirty = false;
        lastInventorySaveTick = Long.MIN_VALUE;

        if (!clientPacket)
            return;

        boolean changeOfController = !Objects.equals(controllerBefore, controller);
        if (level != null && (changeOfController || prevSize != radius || prevLength != length))
            level.setBlocksDirty(getBlockPos(), Blocks.AIR.defaultBlockState(), getBlockState());
    }

    @Override
    protected void write(CompoundTag compound, boolean clientPacket) {
        if (updateConnectivity) compound.putBoolean("Uninitialized", true);
        if (lastKnownPos != null) compound.put("LastKnownPos", NbtUtils.writeBlockPos(lastKnownPos));
        if (!isController()) compound.put("Controller", NbtUtils.writeBlockPos(controller));
        if (isController()) {
            compound.putInt("Size", radius);
            compound.putInt("Length", length);
        }
        super.write(compound, clientPacket);
        if (!clientPacket) {
            compound.put("Inventory", inventory.serializeNBT());
            compound.putString("StorageType", "CombinedInv");
            // A save/snapshot can occur between two changes in the same tick.
            // The next change must mark the owning chunk dirty again.
            lastInventorySaveTick = Long.MIN_VALUE;
        } else if (tierMaterials.seeThrough) {
            // Only the first half of the local slots is used by the crystal
            // renderer. Keep Size so its slot positions remain unchanged.
            CompoundTag visible = new CompoundTag();
            visible.putInt("Size", inventory.getSlots());
            ListTag items = new ListTag();
            for (int slot = 0; slot < inventory.getSlots() / 2; slot++) {
                ItemStack stack = inventory.getStackInSlot(slot);
                if (stack.isEmpty()) continue;
                CompoundTag item = new CompoundTag();
                item.putInt("Slot", slot);
                stack.save(item);
                items.add(item);
            }
            visible.put("Items", items);
            compound.put("Inventory", visible);
        }
    }

    public ItemStackHandler getInventoryOfBlock() {
        return inventory;
    }

    public InventoryIdentifier getInvId() {
        if (!isController()) {
            TieredVaultBlockEntity root = getControllerBE();
            return root == null ? null : root.getInvId();
        }
        if (invId == null) {
            BlockPos end = VaultGeometry.position(worldPosition, getMainConnectionAxis(),
                Math.max(1, length) - 1, Math.max(1, radius) - 1, Math.max(1, radius) - 1);
            invId = new InventoryIdentifier.Bounds(BoundingBox.fromCorners(worldPosition, end));
        }
        return invId;
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        itemCapability.invalidate();
        invId = null;
        comparatorOutput = -1;
        if (!isController() && level != null && controller != null && level.hasChunkAt(controller)
            && level.getBlockEntity(controller) instanceof TieredVaultBlockEntity root) {
            root.itemCapability.invalidate();
            root.invId = null;
            root.comparatorOutput = -1;
            root.comparatorsDirty = true;
        }
    }

    public void applyInventoryToBlock(ItemStackHandler handler) {
        if (handler.getSlots() > inventory.getSlots()) inventory.setSize(handler.getSlots());
        for (int i = 0; i < inventory.getSlots(); i++) inventory.setStackInSlot(i, i < handler.getSlots() ? handler.getStackInSlot(i).copy() : ItemStack.EMPTY);
    }

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, Direction side) {
        if (isItemHandlerCap(cap)) {
            if (VaultUpgradeService.isUpdating()) return LazyOptional.empty();
            initCapability();
            return itemCapability.cast();
        }
        return super.getCapability(cap, side);
    }

    private void initCapability() {
        if (itemCapability.isPresent())
            return;
        if (!isController()) {
            TieredVaultBlockEntity controllerBE = getControllerBE();
            if (controllerBE == null)
                return;
            controllerBE.initCapability();
            itemCapability = controllerBE.itemCapability;
            return;
        }

        Direction.Axis mainAxis = getMainConnectionAxis();
        IItemHandlerModifiable[] invs = new IItemHandlerModifiable[length * radius * radius];
        for (int yOffset = 0; yOffset < length; yOffset++) for (int xOffset = 0; xOffset < radius; xOffset++) for (int zOffset = 0; zOffset < radius; zOffset++) {
            BlockPos vaultPos = VaultGeometry.position(worldPosition, mainAxis, yOffset, xOffset, zOffset);
            TieredVaultBlockEntity vaultAt = null;
            if (level != null && level.hasChunkAt(vaultPos)
                && level.getBlockEntity(vaultPos) instanceof TieredVaultBlockEntity candidate
                && candidate.getType() == getType() && candidate.getController().equals(getController()))
                vaultAt = candidate;
            if (vaultAt == null) return; // Never expose a writable, unsaved placeholder slot.
            invs[yOffset * radius * radius + xOffset * radius + zOffset] = vaultAt.inventory;
        }
        IItemHandler itemHandler = new VersionedInventoryWrapper(new CombinedInvWrapper(invs));
        itemCapability = LazyOptional.of(() -> itemHandler);
    }

    public static int getMaxLength(int radius, TierMaterials tier) {
        return radius * tier.multiplierLength;
    }

    @Override
    public void preventConnectivityUpdate() { updateConnectivity = false; }

    @Override
    public void notifyMultiUpdated() {
        BlockState state = this.getBlockState();
        if (level != null && TieredVaultBlock.isVault(state, tierMaterials)) level.setBlock(getBlockPos(), state.setValue(TieredVaultBlock.LARGE, radius > 2), 6);
        itemCapability.invalidate();
        invId = null;
        comparatorOutput = -1;
        updateComparators();
        setChanged();
    }

    @Override
    public Direction.Axis getMainConnectionAxis() {
        Direction.Axis main = TieredVaultBlock.getVaultBlockAxis(getBlockState(), tierMaterials);
        return main == null ? Direction.Axis.X : main;
    }

    @Override
    public int getMaxLength(Direction.Axis longAxis, int width) {
        if (longAxis != getMainConnectionAxis()) return getMaxWidth();
        return getMaxLength(width, tierMaterials);
    }

    @Override
    public int getMaxWidth() {
        return 3;
    }

    @Override
    public int getHeight() { return length; }

    @Override
    public int getWidth() { return radius; }

    @Override
    public void setHeight(int height) { this.length = height; }

    @Override
    public void setWidth(int width) { this.radius = width; }

    @Override
    public boolean hasInventory() { return true; }
}
