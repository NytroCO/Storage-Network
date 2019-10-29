package mrriegel.storagenetwork.util.inventory;

import net.minecraft.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;

import java.util.ArrayList;
import java.util.List;

public class ItemStackHandlerEx extends ItemStackHandler {

    ItemStackHandlerEx(int size) {
        super(size);
    }

    public List<ItemStack> getStacks() {
        List<ItemStack> result = new ArrayList<>();
        for (int slot = 0; slot < this.getSlots(); slot++) {
            ItemStack stack = this.getStackInSlot(slot);
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            result.add(stack);
        }
        return result;
    }
}
