package mrriegel.storagenetwork.block.cable.link;

import javax.annotation.Nullable;
import mrriegel.storagenetwork.api.capability.IConnectableLink;
import mrriegel.storagenetwork.block.cable.ContainerCable;
import mrriegel.storagenetwork.block.cable.TileCable;
import mrriegel.storagenetwork.capabilities.CapabilityConnectableLink;
import mrriegel.storagenetwork.capabilities.StorageNetworkCapabilities;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;

public class ContainerCableLink extends ContainerCable {

  @Nullable
  public CapabilityConnectableLink link;

  public ContainerCableLink(TileCable tile, InventoryPlayer playerInv) {
    super(tile, playerInv);
    if (!tile.hasCapability(StorageNetworkCapabilities.CONNECTABLE_ITEM_STORAGE_CAPABILITY, null)) {
      return;
    }
    IConnectableLink rawLink = tile.getCapability(StorageNetworkCapabilities.CONNECTABLE_ITEM_STORAGE_CAPABILITY, null);
    if (!(rawLink instanceof CapabilityConnectableLink)) {
      return;
    }
    this.link = (CapabilityConnectableLink) rawLink;
  }

  @Override
  public ItemStack transferStackInSlot(EntityPlayer player, int slotIndex) {
    return ItemStack.EMPTY;
  }
}
