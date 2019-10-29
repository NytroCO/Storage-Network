package mrriegel.storagenetwork.block.master;

import com.google.common.collect.Lists;
import mrriegel.storagenetwork.StorageNetwork;
import mrriegel.storagenetwork.api.capability.IConnectable;
import mrriegel.storagenetwork.api.capability.IConnectableItemAutoIO;
import mrriegel.storagenetwork.api.capability.IConnectableLink;
import mrriegel.storagenetwork.api.data.DimPos;
import mrriegel.storagenetwork.api.data.EnumStorageDirection;
import mrriegel.storagenetwork.api.data.IItemStackMatcher;
import mrriegel.storagenetwork.api.network.INetworkMaster;
import mrriegel.storagenetwork.block.cable.processing.ProcessRequestModel;
import mrriegel.storagenetwork.block.cable.processing.TileCableProcess;
import mrriegel.storagenetwork.block.control.ProcessWrapper;
import mrriegel.storagenetwork.capabilities.StorageNetworkCapabilities;
import mrriegel.storagenetwork.config.ConfigHandler;
import mrriegel.storagenetwork.data.ItemStackMatcher;
import mrriegel.storagenetwork.util.UtilInventory;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

import java.util.*;
import java.util.stream.Collectors;

public class TileMaster extends TileEntity implements ITickable, INetworkMaster {

    public static String[] blacklist;
    private Set<DimPos> connectables;
    private Map<String, DimPos> importCache = new HashMap<>();
    private boolean shouldRefresh = true;

    public static boolean isTargetAllowed(IBlockState iBlockState) {
        String blockId = iBlockState.getBlock().getRegistryName().toString();
        for (String s : blacklist) {
            if (blockId.equals(s)) {
                StorageNetwork.info(iBlockState + " Connection blocked by config ");
                return false;
            }
        }
        return true;
    }

    private DimPos getDimPos() {
        return new DimPos(world, pos);
    }

    public int emptySlots() {
        int countEmpty = 0;
        for (IConnectableLink storage : getSortedConnectableStorage()) {
            countEmpty += storage.getEmptySlots();
        }
        return countEmpty;
    }

    private List<IConnectableLink> getSortedConnectableStorage() {
        return getConnectableStorage().stream().sorted(Comparator.comparingInt(IConnectableLink::getPriority)).collect(Collectors.toList());
    }

    private Set<IConnectableLink> getConnectableStorage() {
        Set<IConnectableLink> result = new HashSet<>();
        for (final DimPos dimpos : getConnectablePositions()) {
            if (!dimpos.isLoaded()) {
                continue;
            }
            TileEntity tileEntity = dimpos.getTileEntity(TileEntity.class);
            if (tileEntity == null) {
                continue;
            }
            if (!tileEntity.hasCapability(StorageNetworkCapabilities.CONNECTABLE_CAPABILITY, null)) {
                StorageNetwork.instance.logger.error("Somehow stored a dimpos that is not connectable... Skipping " + dimpos);
                continue;
            }
            if (!tileEntity.hasCapability(StorageNetworkCapabilities.CONNECTABLE_ITEM_STORAGE_CAPABILITY, null)) {
                continue;
            }
            result.add(tileEntity.getCapability(StorageNetworkCapabilities.CONNECTABLE_ITEM_STORAGE_CAPABILITY, null));
        }
        return result;
    }

    /**
     * dont create an iterator over the original one that is being modified
     *
     * @return
     */
    public Set<DimPos> getConnectablePositions() {
        if (connectables == null) {
            connectables = new HashSet<>();
        }
        return new HashSet<>(connectables);
    }

    public int getAmount(ItemStackMatcher fil) {
        if (fil == null) {
            return 0;
        }
        int totalCount = 0;
        for (ItemStack stack : getStacks()) {
            if (!fil.match(stack)) {
                continue;
            }
            totalCount += stack.getCount();
        }
        return totalCount;
    }

    @Override
    public List<ItemStack> getStacks() {
        List<ItemStack> stacks = Lists.newArrayList();
        if (getConnectablePositions() == null) {
            refreshNetwork();
        }
        for (IConnectableLink storage : getSortedConnectableStorage()) {
            for (ItemStack stack : storage.getStoredStacks()) {
                if (stack.isEmpty()) {
                    continue;
                }
                addOrMergeIntoList(stacks, stack.copy());
            }
        }
        return stacks;
    }

    public void refreshNetwork() {
        if (world.isRemote) {
            return;
        }
        shouldRefresh = true;
    }

    private void addOrMergeIntoList(List<ItemStack> list, ItemStack stackToAdd) {
        boolean added = false;
        for (ItemStack stack : list) {
            if (ItemHandlerHelper.canItemStacksStack(stackToAdd, stack)) {
                stack.setCount(stack.getCount() + stackToAdd.getCount());
                added = true;
                break;
            }
        }
        if (!added) {
            list.add(stackToAdd);
        }
    }

    @Override
    public int insertStack(ItemStack rawStack, boolean simulate) {
        if (rawStack.isEmpty()) {
            return 0;
        }
        ItemStack stack = rawStack.copy();
        // 1. Try to insert into a recent slot for the same item.
        //    We do this to avoid having to search for the appropriate inventory repeatedly.
        String key = getStackKey(stack);
        if (hasCachedSlot(stack)) {
            DimPos cachedStoragePos = getCachedSlot(stack);
            IConnectableLink storage = cachedStoragePos.getCapability(StorageNetworkCapabilities.CONNECTABLE_ITEM_STORAGE_CAPABILITY, null);
            if (storage == null) {
                // The block at the cached position is not even an IConnectableLink anymore
                this.importCache.remove(key);
            } else {
                // But if it is, we test whether it can still import that particular stack and do so if it does.
                boolean canStillImport = storage.getSupportedTransferDirection().match(EnumStorageDirection.IN);
                if (canStillImport && storage.insertStack(stack, true).getCount() < stack.getCount()) {
                    stack = storage.insertStack(stack, simulate);
                } else {
                    this.importCache.remove(key);
                }
            }
        }
        // 2. If everything got transferred into the cached storage, end here
        if (stack.isEmpty()) {
            return 0;
        }
        // 3. Otherwise try to find a new inventory that can take the remainder of the itemstack
        List<IConnectableLink> storages = getSortedConnectableStorage();
        for (IConnectableLink storage : storages) {
            // Ignore storages that can not import
            if (!storage.getSupportedTransferDirection().match(EnumStorageDirection.IN)) {
                continue;
            }
            // The given import-capable storage can not import this particular stack
            if (storage.insertStack(stack, true).getCount() >= stack.getCount()) {
                continue;
            }
            // If it can we need to know, i.e. store the remainder
            stack = storage.insertStack(stack, simulate);
        }
        return stack.getCount();
    }

    @Override
    public void clearCache() {
        importCache = new HashMap<>();
    }

    @Override
    public ItemStack request(IItemStackMatcher matcher, final int size, boolean simulate) {
        if (size == 0 || matcher == null) {
            return ItemStack.EMPTY;
        }
        // TODO: Test against storage drawers. There was some issue with it: https://github.com/PrinceOfAmber/Storage-Network/issues/19
        IItemStackMatcher usedMatcher = matcher;
        int alreadyTransferred = 0;
        for (IConnectableLink storage : getSortedConnectableStorage()) {
            int req = size - alreadyTransferred;
            ItemStack simExtract = storage.extractStack(usedMatcher, req, simulate);
            if (simExtract.isEmpty()) {
                continue;
            }
            // Do not stack items of different types together, i.e. make the filter rules more strict for all further items
            usedMatcher = new ItemStackMatcher(simExtract, true, false, true);
            alreadyTransferred += simExtract.getCount();
            if (alreadyTransferred >= size) {
                break;
            }
        }
        if (alreadyTransferred <= 0) {
            return ItemStack.EMPTY;
        }
        return ItemHandlerHelper.copyStackWithSize(usedMatcher.getStack(), alreadyTransferred);
    }

    /**
     * This is a recursively called method that traverses all connectable blocks and stores them in this tiles connectables list.
     *
     * @param sourcePos
     */
    private Set<DimPos> getConnectables(final DimPos sourcePos) {
        HashSet<DimPos> result = new HashSet<>();
        addConnectables(sourcePos, result);
        return result;
    }

    private void addConnectables(final DimPos sourcePos, Set<DimPos> set) {
        if (sourcePos == null || sourcePos.getWorld() == null || !sourcePos.isLoaded()) {
            return;
        }
        // Look in all directions
        for (EnumFacing direction : EnumFacing.values()) {
            DimPos lookPos = sourcePos.offset(direction);
            if (!lookPos.isLoaded()) {
                continue;
            }
            Chunk chunk = lookPos.getChunk();
            if (chunk == null || !chunk.isLoaded()) {
                continue;
            }
            // Prevent having multiple masters on a network and break all others.
            TileMaster maybeMasterTile = lookPos.getTileEntity(TileMaster.class);
            if (maybeMasterTile != null && !lookPos.equals(this.world, this.pos)) {
                nukeAndDrop(lookPos);
                continue;
            }
            TileEntity tileHere = lookPos.getTileEntity(TileEntity.class);
            if (tileHere == null) {
                continue;
            }
            boolean isConnectable = tileHere.hasCapability(StorageNetworkCapabilities.CONNECTABLE_CAPABILITY, direction.getOpposite());
            if (isConnectable) {
                IConnectable capabilityConnectable = tileHere.getCapability(StorageNetworkCapabilities.CONNECTABLE_CAPABILITY, direction.getOpposite());
                capabilityConnectable.setMasterPos(getDimPos());
                DimPos realConnectablePos = capabilityConnectable.getPos();
                boolean beenHereBefore = set.contains(realConnectablePos);
                if (beenHereBefore) {
                    continue;
                }
                set.add(realConnectablePos);
                addConnectables(realConnectablePos, set);
                tileHere.markDirty();
                chunk.setModified(true);
            }
        }
    }

    private void nukeAndDrop(DimPos lookPos) {
        lookPos.getBlockState().getBlock().dropBlockAsItem(lookPos.getWorld(), lookPos.getBlockPos(), lookPos.getBlockState(), 0);
        lookPos.getWorld().setBlockToAir(lookPos.getBlockPos());
        lookPos.getWorld().removeTileEntity(lookPos.getBlockPos());
    }

    private boolean hasCachedSlot(ItemStack stack) {
        return this.importCache.containsKey(getStackKey(stack));
    }

    private DimPos getCachedSlot(ItemStack stack) {
        return this.importCache.get(getStackKey(stack));
    }

    private String getStackKey(ItemStack stackInCopy) {
        return stackInCopy.getItem().getRegistryName().toString() + "/" + stackInCopy.getItemDamage();
    }

    /**
     * Pull into the network from the relevant linked cables
     */
    private void updateImports() {
        for (IConnectable connectable : getConnectables()) {
            IConnectableItemAutoIO storage = connectable.getPos().getCapability(StorageNetworkCapabilities.CONNECTABLE_AUTO_IO, null);
            if (storage == null) {
                continue;
            }
            // We explicitely don't want to check whether this can do BOTH, because we don't
            // want to import what we've just exported in updateExports().
            if (storage.ioDirection() != EnumStorageDirection.IN) {
                continue;
            }
            // Give the storage a chance to have a cooldown or other conditions that prevent it from running
            if (!storage.runNow(connectable.getPos(), this)) {
                continue;
            }
            // Do a simulation first and abort if we got an empty stack,
            ItemStack stack = storage.extractNextStack(storage.getTransferRate(), true);
            if (stack.isEmpty()) {
                continue;
            }
            // Then try to insert the stack into this masters network and store the number of remaining items in the stack
            int countUnmoved = this.insertStack(stack.copy(), true);
            // Calculate how many items in the stack actually got moved
            int countMoved = stack.getCount() - countUnmoved;
            if (countMoved <= 0) {
                continue;
            }
            // Alright, simulation says we're good, let's do it!
            // First extract from the storage
            ItemStack actuallyExtracted = storage.extractNextStack(countMoved, false);
            connectable.getPos().getChunk().markDirty();
            // Then insert into our network
            this.insertStack(actuallyExtracted.copy(), false);
        }
    }

    private void updateProcess() {
        for (final IConnectable connectable : getConnectables()) {
            TileCableProcess cableProcess = connectable.getPos().getTileEntity(TileCableProcess.class);
            if (cableProcess == null) {
                continue;
            }
            cableProcess.run();
        }
    }

    /**
     * push OUT of the network to attached export cables
     */
    private void updateExports() {
        for (IConnectable connectable : getConnectables()) {
            IConnectableItemAutoIO storage = connectable.getPos().getCapability(StorageNetworkCapabilities.CONNECTABLE_AUTO_IO, null);
            if (storage == null) {
                continue;
            }
            // We explicitely don't want to check whether this can do BOTH, because we don't
            // want to import what we've just exported in updateExports().
            if (storage.ioDirection() != EnumStorageDirection.OUT) {
                continue;
            }
            // Give the storage a chance to have a cooldown
            if (!storage.runNow(connectable.getPos(), this)) {
                continue;
            }
            for (IItemStackMatcher matcher : storage.getAutoExportList()) {
                boolean stockMode = storage.isStockMode();
                int amtToRequest = storage.getTransferRate();
                if (stockMode) {
                    try {
                        TileEntity tileEntity = world.getTileEntity(connectable.getPos().getBlockPos().offset(storage.facingInventory()));
                        IItemHandler targetInventory = tileEntity.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, EnumFacing.UP);
                        //request with false to see how many even exist in there.
                        int stillNeeds = UtilInventory.containsAtLeastHowManyNeeded(targetInventory, matcher.getStack(), matcher.getStack().getCount());
                        if (stillNeeds == 0) {
                            continue;
                        }
                        amtToRequest = Math.min(stillNeeds, amtToRequest);
                    } catch (Throwable e) {
                        StorageNetwork.log("error thrown " + e.getMessage());
                    }
                }
                ItemStack requestedStack = this.request(matcher, amtToRequest, true);
                if (requestedStack.isEmpty()) {
                    continue;
                }
                // The stack is available in the network, let's simulate inserting it into the storage
                ItemStack insertedSim = storage.insertStack(requestedStack.copy(), true);
                // Determine the amount of items moved in the stack
                ItemStack targetStack = requestedStack.copy();
                if (!insertedSim.isEmpty()) {
                    int movedItems = requestedStack.getCount() - insertedSim.getCount();
                    if (movedItems <= 0) {
                        continue;
                    }
                    targetStack.setCount(movedItems);
                }
                // Alright, some items got moved in the simulation. Let's do it for real this time.
                ItemStack realExtractedStack = this.request(new ItemStackMatcher(requestedStack, true, false, true), targetStack.getCount(), false);
                if (realExtractedStack.isEmpty()) {
                    continue;
                }
                storage.insertStack(realExtractedStack.copy(), false);
                break;
            }
        }
    }

    private Set<IConnectable> getConnectables() {
        Set<DimPos> positions = getConnectablePositions();
        if (positions == null) {
            return new HashSet<>();
        }
        Set<IConnectable> result = new HashSet<>();
        for (DimPos pos : positions) {
            if (!pos.isLoaded()) {
                continue;
            }
            TileEntity tileEntity = pos.getTileEntity(TileEntity.class);
            if (tileEntity == null) {
                continue;
            }
            if (!tileEntity.hasCapability(StorageNetworkCapabilities.CONNECTABLE_CAPABILITY, null)) {
                StorageNetwork.instance.logger.error("Somehow stored a dimpos that is not connectable... Skipping " + pos);
                continue;
            }
            result.add(tileEntity.getCapability(StorageNetworkCapabilities.CONNECTABLE_CAPABILITY, null));
        }
        return result;
    }

    public List<ProcessWrapper> getProcessors() {
        List<ProcessWrapper> result = new ArrayList<>();
        for (DimPos pos : getConnectablePositions()) {
            if (!pos.isLoaded()) {
                continue;
            }
            TileCableProcess cableProcess = pos.getTileEntity(TileCableProcess.class);
            if (cableProcess == null) {
                continue;
            }
            DimPos inventoryPos = pos.offset(cableProcess.getDirection());
            if (inventoryPos == null) {
                StorageNetwork.log("Error: processor null at  " + pos + "," + cableProcess.getDirection());
                continue;
            }
            IBlockState blockState = inventoryPos.getBlockState();
            String name = blockState.getBlock().getLocalizedName();
            try {
                ItemStack pickBlock = blockState.getBlock().getPickBlock(blockState, null, inventoryPos.getWorld(), inventoryPos.getBlockPos(), null);
                if (!pickBlock.isEmpty()) {
                    name = pickBlock.getDisplayName();
                }
            } catch (Exception e) {
                StorageNetwork.instance.logger.error("Error with display name ", e);
            }
            ProcessRequestModel proc = cableProcess.getProcessModel();
            //if list of models then wrapper would not need to change at all
            ProcessWrapper processor = new ProcessWrapper(new DimPos(cableProcess.getWorld(), cableProcess.getPos()), cableProcess.getFirstRecipeOut(), proc.getCount(), name, proc.isAlwaysActive());
            processor.ingredients = cableProcess.getProcessIngredients();
            processor.blockId = blockState.getBlock().getRegistryName();
            result.add(processor);
        }
        return result;
    }

    @Override
    public void update() {
        if (world == null || world.isRemote) {
            return;
        }
        //refresh time in config, default 200 ticks aka 10 seconds
        if (getConnectablePositions() == null || (world.getTotalWorldTime() % (ConfigHandler.refreshTicks) == 0) || shouldRefresh) {
            try {
                this.connectables = getConnectables(getDimPos());
                this.shouldRefresh = false;
                // addInventorys();
                world.getChunkFromBlockCoords(pos).setModified(true);//.setChunkModified();
            } catch (Throwable e) {
                StorageNetwork.instance.logger.error("Refresh network error ", e);
            }
        }
        updateImports();
        updateExports();
        updateProcess();
    }

    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        NBTTagCompound syncData = new NBTTagCompound();
        this.writeToNBT(syncData);
        return new SPacketUpdateTileEntity(this.pos, 1, syncData);
    }

    @Override
    public NBTTagCompound getUpdateTag() {
        return writeToNBT(new NBTTagCompound());
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
        readFromNBT(pkt.getNbtCompound());
    }

    @Override
    public boolean shouldRefresh(World world, BlockPos pos, IBlockState oldState, IBlockState newSate) {
        return oldState.getBlock() != newSate.getBlock();
    }
}
