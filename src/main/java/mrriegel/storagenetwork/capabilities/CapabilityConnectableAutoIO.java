package mrriegel.storagenetwork.capabilities;

import mrriegel.storagenetwork.StorageNetwork;
import mrriegel.storagenetwork.api.capability.IConnectable;
import mrriegel.storagenetwork.api.capability.IConnectableItemAutoIO;
import mrriegel.storagenetwork.api.data.DimPos;
import mrriegel.storagenetwork.api.data.EnumStorageDirection;
import mrriegel.storagenetwork.api.data.EnumUpgradeType;
import mrriegel.storagenetwork.api.data.IItemStackMatcher;
import mrriegel.storagenetwork.api.network.INetworkMaster;
import mrriegel.storagenetwork.data.ItemStackMatcher;
import mrriegel.storagenetwork.util.inventory.FilterItemStackHandler;
import mrriegel.storagenetwork.util.inventory.UpgradesItemStackHandler;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

public class CapabilityConnectableAutoIO implements INBTSerializable<NBTTagCompound>, IConnectableItemAutoIO {

    public final IConnectable connectable;
    public EnumStorageDirection direction;
    public final UpgradesItemStackHandler upgrades = new UpgradesItemStackHandler();
    public final FilterItemStackHandler filters = new FilterItemStackHandler();
    public ItemStack operationStack = ItemStack.EMPTY;
    public int operationLimit = 0;
    public boolean operationMustBeSmaller = true;
    public int priority = 0;
    private EnumFacing inventoryFace;

    private CapabilityConnectableAutoIO(EnumStorageDirection direction) {
        this.connectable = new CapabilityConnectable();
        this.direction = direction;
    }

    public CapabilityConnectableAutoIO(TileEntity tile, EnumStorageDirection direction) {
        this.connectable = tile.getCapability(StorageNetworkCapabilities.CONNECTABLE_CAPABILITY, null);
        this.direction = direction;
        // Set some defaults
        if (direction == EnumStorageDirection.OUT) {
            filters.setIsWhitelist(true);
        }
    }

    public void setInventoryFace(EnumFacing inventoryFace) {
        this.inventoryFace = inventoryFace;
    }

    @Override
    public NBTTagCompound serializeNBT() {
        NBTTagCompound result = new NBTTagCompound();
        NBTTagCompound upgrades = this.upgrades.serializeNBT();
        result.setTag("upgrades", upgrades);
        NBTTagCompound filters = this.filters.serializeNBT();
        result.setTag("filters", filters);
        NBTTagCompound operation = new NBTTagCompound();
        operation.setTag("stack", operationStack.serializeNBT());
        operation.setBoolean("mustBeSmaller", operationMustBeSmaller);
        operation.setInteger("limit", operationLimit);
        result.setTag("operation", operation);
        result.setInteger("prio", priority);
        if (inventoryFace != null) {
            result.setString("inventoryFace", inventoryFace.toString());
        }
        return result;
    }

    @Override
    public void deserializeNBT(NBTTagCompound nbt) {
        NBTTagCompound upgrades = nbt.getCompoundTag("upgrades");
        this.upgrades.deserializeNBT(upgrades);
        NBTTagCompound filters = nbt.getCompoundTag("filters");
        this.filters.deserializeNBT(filters);
        NBTTagCompound operation = nbt.getCompoundTag("operation");
        this.operationLimit = operation.getInteger("limit");
        this.operationMustBeSmaller = operation.getBoolean("mustBeSmaller");
        if (operation.hasKey("stack", Constants.NBT.TAG_COMPOUND)) {
            this.operationStack = new ItemStack(operation.getCompoundTag("stack"));
        } else {
            this.operationStack = ItemStack.EMPTY;
        }
        this.priority = nbt.getInteger("prio");
        if (nbt.hasKey("inventoryFace")) {
            this.inventoryFace = EnumFacing.byName(nbt.getString("inventoryFace"));
        }
    }

    @Override
    public EnumStorageDirection ioDirection() {
        return direction;
    }

    @Override
    public ItemStack insertStack(ItemStack stack, boolean simulate) {
        // If this storage is configured to only import into the network, do not
        // insert into the storage, but abort immediately.
        if (direction == EnumStorageDirection.IN) {
            return stack;
        }
        if (inventoryFace == null) {
            return stack;
        }
        DimPos inventoryPos = connectable.getPos().offset(inventoryFace);
        // Test whether the connected block has the IItemHandler capability
        IItemHandler itemHandler = inventoryPos.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, inventoryFace.getOpposite());
        if (itemHandler == null) {
            return stack;
        }
        return ItemHandlerHelper.insertItemStacked(itemHandler, stack, simulate);
    }

    @Override
    public ItemStack extractNextStack(int size, boolean simulate) {
        // If this storage is configured to only export from the network, do not
        // extract from the storage, but abort immediately.
        if (direction == EnumStorageDirection.OUT) {
            return ItemStack.EMPTY;
        }
        if (inventoryFace == null) {
            return ItemStack.EMPTY;
        }
        DimPos inventoryPos = connectable.getPos().offset(inventoryFace);
        // Test whether the connected block has the IItemHandler capability
        IItemHandler itemHandler = inventoryPos.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, inventoryFace.getOpposite());
        if (itemHandler == null) {
            return ItemStack.EMPTY;
        }
        for (int slot = 0; slot < itemHandler.getSlots(); slot++) {
            ItemStack stack = itemHandler.getStackInSlot(slot);
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            // Ignore stacks that are filtered
            if (this.filters.isStackFiltered(stack)) {
                continue;
            }
            int extractSize = Math.min(size, stack.getCount());
            return itemHandler.extractItem(slot, extractSize, simulate);
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean isStockMode() {
        return upgrades.getUpgradesOfType(EnumUpgradeType.STOCK) > 0;
    }

    @Override
    public EnumFacing facingInventory() {
        return inventoryFace;
    }

    @Override
    public int getPriority() {
        return priority;
    }

    @Override
    public int getTransferRate() {
        return upgrades.getUpgradesOfType(EnumUpgradeType.STACK) > 0 ? 64 : 4;
    }

    @Override
    public boolean runNow(DimPos connectablePos, INetworkMaster master) {
        int speed = Math.max(upgrades.getUpgradesOfType(EnumUpgradeType.SPEED) + 1, 1);
        int speedRatio = (30 / speed);
        if (speedRatio <= 1) {
            speedRatio = 1;
        }
        boolean cooldownOk = (connectablePos.getWorld().getTotalWorldTime() % speedRatio == 0);
        boolean operationLimitOk = doesPassOperationFilterLimit(master);
        return cooldownOk && operationLimitOk;
    }

    private boolean doesPassOperationFilterLimit(INetworkMaster master) {
        if (upgrades.getUpgradesOfType(EnumUpgradeType.OPERATION) < 1) {
            return true;
        }
        if (operationStack == null || operationStack.isEmpty()) {
            return true;
        }
        // TODO: Investigate whether the operation limiter should consider the filter toggles
        ItemStack availableStack = master.getAmount(new ItemStackMatcher(operationStack, filters.meta, filters.ores, filters.nbt));
        if (operationMustBeSmaller) {
            return operationLimit >= availableStack.getCount();
        } else {
            return operationLimit < availableStack.getCount();
        }
    }

    @Override
    public List<IItemStackMatcher> getAutoExportList() {
        return filters.getStackMatchers();
    }

    public List<ItemStack> getStacksForFilter() {
        if (inventoryFace == null) {
            return Collections.emptyList();
        }
        DimPos inventoryPos = connectable.getPos().offset(inventoryFace);
        // Test whether the connected block has the IItemHandler capability
        IItemHandler itemHandler = inventoryPos.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, inventoryFace.getOpposite());
        if (itemHandler == null) {
            StorageNetwork.error("getStacksForFilter    null itemhandler connection ");
            return Collections.emptyList();
        }
        // If it does, iterate its stacks, filter them and add them to the result list
        List<ItemStack> result = new ArrayList<>();
        for (int slot = 0; slot < itemHandler.getSlots(); slot++) {
            ItemStack stack = itemHandler.getStackInSlot(slot);
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            StorageNetwork.log(slot + "getStacksForFilter    " + stack
                    + filters.exactStackAlreadyInList(stack));
            if (this.filters.exactStackAlreadyInList(stack)) {
                continue;
            }
            result.add(stack.copy());
            StorageNetwork.log("getStacksForFilter   size up   " + result.size());
            // We can abort after we've found FILTER_SIZE stacks; we don't have more filter slots anyway
            if (result.size() >= FilterItemStackHandler.FILTER_SIZE) {
                return result;
            }
        }
        return result;
    }

    public static class Factory implements Callable<IConnectableItemAutoIO> {

        @Override
        public IConnectableItemAutoIO call() {
            return new CapabilityConnectableAutoIO(EnumStorageDirection.IN);
        }
    }

    public static class Storage implements Capability.IStorage<IConnectableItemAutoIO> {

        @Nullable
        @Override
        public NBTBase writeNBT(Capability<IConnectableItemAutoIO> capability, IConnectableItemAutoIO rawInstance, EnumFacing side) {
            CapabilityConnectableAutoIO instance = (CapabilityConnectableAutoIO) rawInstance;
            return instance.serializeNBT();
        }

        @Override
        public void readNBT(Capability<IConnectableItemAutoIO> capability, IConnectableItemAutoIO rawInstance, EnumFacing side, NBTBase nbt) {
            CapabilityConnectableAutoIO instance = (CapabilityConnectableAutoIO) rawInstance;
            instance.deserializeNBT((NBTTagCompound) nbt);
        }
    }
}
