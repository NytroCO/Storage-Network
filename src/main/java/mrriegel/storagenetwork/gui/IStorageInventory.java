package mrriegel.storagenetwork.gui;

import java.util.List;
import net.minecraft.item.ItemStack;

public interface IStorageInventory {

  void setStacks(List<ItemStack> stacks);

  void setCraftableStacks(List<ItemStack> stacks);
}
