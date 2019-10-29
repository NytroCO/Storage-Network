package mrriegel.storagenetwork.gui.fb;

import mrriegel.storagenetwork.StorageNetwork;
import mrriegel.storagenetwork.api.data.DimPos;
import mrriegel.storagenetwork.block.master.TileMaster;
import mrriegel.storagenetwork.block.request.TileRequest;
import mrriegel.storagenetwork.network.StackRefreshClientMessage;
import mrriegel.storagenetwork.registry.PacketRegistry;
import mrriegel.storagenetwork.util.UtilTileEntity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayList;

public class ContainerFastRequest extends ContainerFastNetworkCrafter {

    private TileRequest tile;

    public ContainerFastRequest(TileRequest tile, EntityPlayer player, World world, BlockPos pos) {
        super(player, world, pos);
        this.setTileRequest(tile);
        this.inventorySlots.clear();
        this.inventoryItemStacks.clear();
        for (int i = 0; i < 9; i++) {
            if (i != 8) this.craftMatrix.stackList.set(i, tile.matrix.getOrDefault(i, ItemStack.EMPTY));
            else this.craftMatrix.setInventorySlotContents(i, tile.matrix.getOrDefault(i, ItemStack.EMPTY));
        }
        SlotCraftingNetwork slotCraftOutput = new SlotCraftingNetwork(player, craftMatrix, craftResult, 0, 101, 128);
        DimPos masterPos = tile.getMaster();
        TileMaster masterTile = masterPos.getTileEntity(TileMaster.class);
        slotCraftOutput.setTileMaster(masterTile);
        this.addSlotToContainer(slotCraftOutput);
        bindGrid();
        bindPlayerInvo(player.inventory);
        bindHotbar(player);
    }

    @Override
    public boolean canInteractWith(EntityPlayer playerIn) {
        TileMaster tileMaster = this.getTileMaster();
        if (tileMaster == null) {
            StorageNetwork.info("Container closing, master tile not found");
            return false;
        }
        if (!getTileRequest().getWorld().isRemote && (forceSync || getTileRequest().getWorld().getTotalWorldTime() % 40 == 0)) {
            forceSync = false;
            PacketRegistry.INSTANCE.sendTo(new StackRefreshClientMessage(tileMaster.getStacks(), new ArrayList<>()), (EntityPlayerMP) playerIn);
        }
        return playerIn.getDistanceSq(getTileRequest().getPos().getX() + 0.5D, getTileRequest().getPos().getY() + 0.5D, getTileRequest().getPos().getZ() + 0.5D) <= 64.0D;
    }

    @Override
    public void onContainerClosed(EntityPlayer player) {
        for (int i = 0; i < 9; i++) {
            getTileRequest().matrix.put(i, craftMatrix.getStackInSlot(i));
        }
        UtilTileEntity.updateTile(getTileRequest().getWorld(), getTileRequest().getPos());
    }

    private TileRequest getTileRequest() {
        return tile;
    }

    private void setTileRequest(TileRequest tile) {
        this.tile = tile;
    }

    @Override
    public TileMaster getTileMaster() {
        if (tile == null || tile.getMaster() == null) {
            return null;
        }
        return tile.getMaster().getTileEntity(TileMaster.class);
    }

    @Override
    public void bindHotbar(EntityPlayer player) {
        for (int i = 0; i < 9; ++i) {
            this.addSlotToContainer(new Slot(player.inventory, i, 8 + i * 18, 232));
        }
    }

    @Override
    public boolean isRequest() {
        return true;
    }

    public static class Client extends ContainerFastRequest {

        public Client(TileRequest tile, EntityPlayer player, World world, BlockPos pos) {
            super(tile, player, world, pos);
        }

        @Override
        public void onCraftMatrixChanged(IInventory inventoryIn) {
        }
    }
}
