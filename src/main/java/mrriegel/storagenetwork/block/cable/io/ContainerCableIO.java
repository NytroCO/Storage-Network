package mrriegel.storagenetwork.block.cable.io;

import javax.annotation.Nullable;
import mrriegel.storagenetwork.api.capability.IConnectableItemAutoIO;
import mrriegel.storagenetwork.block.cable.ContainerCable;
import mrriegel.storagenetwork.block.cable.TileCable;
import mrriegel.storagenetwork.capabilities.CapabilityConnectableAutoIO;
import mrriegel.storagenetwork.capabilities.StorageNetworkCapabilities;
import mrriegel.storagenetwork.item.ItemUpgrade;
import mrriegel.storagenetwork.registry.ModItems;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;

public class ContainerCableIO extends ContainerCable {

  public static final int UPGRADE_COUNT = 4;
  @Nullable
  public CapabilityConnectableAutoIO autoIO;

  public ContainerCableIO(TileCable tile, InventoryPlayer playerInv) {
    super(tile, playerInv);
    if (!tile.hasCapability(StorageNetworkCapabilities.CONNECTABLE_AUTO_IO, null)) {
      return;
    }
    IConnectableItemAutoIO rawAutoIO = tile.getCapability(StorageNetworkCapabilities.CONNECTABLE_AUTO_IO, null);
    if (!(rawAutoIO instanceof CapabilityConnectableAutoIO)) {
      return;
    }
    this.autoIO = (CapabilityConnectableAutoIO) rawAutoIO;
    for (int ii = 0; ii < UPGRADE_COUNT; ii++) {
      this.addSlotToContainer(new SlotItemHandler(autoIO.upgrades, ii, 98 + ii * sq, 6) {

        @Override
        public int getSlotStackLimit() {
          return 1;
        }

        @Override
        public boolean isItemValid(ItemStack stack) {
          return stack.getItem() == ModItems.upgrade;
        }
      });
    }
  }

  @Override
  public ItemStack transferStackInSlot(EntityPlayer player, int slotIndex) {
    Slot slot = this.inventorySlots.get(slotIndex);
    //in range [4,39] means its coming FROM inventory
    // [0,3] is the filter list
    if (slot != null && slot.getHasStack()) {
      ItemStack stackInSlot = slot.getStack();
      if (stackInSlot.getItem() instanceof ItemUpgrade) {
        if (0 <= slotIndex && slotIndex <= 35) {
          //FROM inventory to upgrade slots
          if (!this.mergeItemStack(stackInSlot, 36, 40, true)) {
            return ItemStack.EMPTY;
          }
        }
        else if (36 <= slotIndex && slotIndex <= 39) {
          //FROM upgrade slots TO inventory
          if (!this.mergeItemStack(stackInSlot, 0, 35, true)) {
            return ItemStack.EMPTY;
          }
        }
      }
    }
    return ItemStack.EMPTY;
  }
}
