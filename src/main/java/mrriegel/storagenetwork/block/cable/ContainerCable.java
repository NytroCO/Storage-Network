package mrriegel.storagenetwork.block.cable;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.util.math.BlockPos;

public class ContainerCable extends Container {

    public final TileCable tile;
    protected final int sq = 18;

    protected ContainerCable(TileCable tile, InventoryPlayer playerInv) {
        this.tile = tile;
        //player inventory
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlotToContainer(new Slot(playerInv, j + i * 9 + 9, 8 + j * sq, 55 + 34 + i * sq));
            }
        }
        //player hotbar
        for (int i = 0; i < 9; ++i) {
            this.addSlotToContainer(new Slot(playerInv, i, 8 + i * sq, 113 + 34));
        }
    }

    @Override
    public boolean canInteractWith(EntityPlayer playerIn) {
        BlockPos pos = tile.getPos();
        return playerIn.getDistanceSq(
                pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= 64.0D;
    }
}
