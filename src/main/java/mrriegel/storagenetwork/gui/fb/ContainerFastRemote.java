package mrriegel.storagenetwork.gui.fb;

import mrriegel.storagenetwork.StorageNetwork;
import mrriegel.storagenetwork.block.master.TileMaster;
import mrriegel.storagenetwork.item.remote.ItemRemote;
import mrriegel.storagenetwork.network.StackRefreshClientMessage;
import mrriegel.storagenetwork.registry.PacketRegistry;
import mrriegel.storagenetwork.util.NBTHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayList;

public class ContainerFastRemote extends ContainerFastNetworkCrafter {

    private final EnumHand hand;
    final ItemStack remoteItemStack;

    public ContainerFastRemote(EntityPlayer player, World world, EnumHand hand) {
        super(player, world, BlockPos.ORIGIN);
        remoteItemStack = player.getHeldItem(hand);
        this.inventorySlots.clear();
        this.inventoryItemStacks.clear();
        for (int i = 0; i < 9; i++) {
            if (i != 8) this.craftMatrix.stackList.set(i, NBTHelper.getItemStack(remoteItemStack, "c" + i));
            else this.craftMatrix.setInventorySlotContents(i, NBTHelper.getItemStack(remoteItemStack, "c" + i));
        }
        SlotCraftingNetwork slotCraftOutput = new SlotCraftingNetwork(player, craftMatrix, craftResult, 0, 101, 128);
        slotCraftOutput.setTileMaster(this.getTileMaster());
        this.addSlotToContainer(slotCraftOutput);
        bindGrid();
        bindPlayerInvo(player.inventory);
        bindHotbar(player);
        this.hand = hand;
    }

    @Override
    public TileMaster getTileMaster() {
        return ItemRemote.getTile(remoteItemStack);
    }

    @Override
    public void bindHotbar(EntityPlayer player) {
        for (int i = 0; i < 9; ++i) {
            if (hand == EnumHand.MAIN_HAND && i == player.inventory.currentItem)
                this.addSlotToContainer(new Slot(player.inventory, i, 8 + i * 18, 232) {

                    @Override
                    public boolean isItemValid(ItemStack stack) {
                        return false;
                    }

                    @Override
                    public boolean canTakeStack(EntityPlayer playerIn) {
                        return false;
                    }
                });
            else this.addSlotToContainer(new Slot(player.inventory, i, 8 + i * 18, 232));
        }
    }

    @Override
    public boolean canInteractWith(EntityPlayer playerIn) {
        TileMaster tileMaster = this.getTileMaster();
        if (tileMaster == null) {
            StorageNetwork.info("Container closing, master tile not found");
            return false;
        }
        if (!playerIn.world.isRemote && (forceSync || playerIn.world.getTotalWorldTime() % 40 == 0)) {
            forceSync = false;
            PacketRegistry.INSTANCE.sendTo(new StackRefreshClientMessage(tileMaster.getStacks(), new ArrayList<>()), (EntityPlayerMP) playerIn);
        }
        return playerIn.getHeldItem(this.hand) == remoteItemStack;
    }

    @Override
    public void onContainerClosed(EntityPlayer player) {
        for (int i = 0; i < 9; i++) {
            NBTHelper.setItemStack(remoteItemStack, "c" + i, craftMatrix.getStackInSlot(i));
        }
    }

    @Override
    public boolean isRequest() {
        return false;
    }

    public static class Client extends ContainerFastRemote {

        public Client(EntityPlayer player, World world, EnumHand hand) {
            super(player, world, hand);
        }

        @Override
        public void onCraftMatrixChanged(IInventory inventoryIn) {
        }
    }
}