package mrriegel.storagenetwork.block.cable.processing;

import mrriegel.storagenetwork.block.cable.ContainerCable;
import mrriegel.storagenetwork.block.cable.TileCable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;

public class ContainerCableProcessing extends ContainerCable {

    public ContainerCableProcessing(TileCable tile, InventoryPlayer playerInv) {
        super(tile, playerInv);
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer playerIn, int index) {
        return ItemStack.EMPTY;
    }
}
