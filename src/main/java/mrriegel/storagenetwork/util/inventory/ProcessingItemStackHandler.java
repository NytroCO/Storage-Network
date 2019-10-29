package mrriegel.storagenetwork.util.inventory;

import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class ProcessingItemStackHandler extends FilterItemStackHandler {

    public boolean isOutputEmpty() {
        return getOutputs().isEmpty();
    }

    public List<ItemStack> getOutputs() {
        List<ItemStack> result = new ArrayList<>();
        for (int slot = 9; slot < 18; slot++) {
            ItemStack stack = this.getStackInSlot(slot);
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            result.add(stack);
        }
        return result;
    }

    public boolean isInputEmpty() {
        return getInputs().isEmpty();
    }

    public List<ItemStack> getInputs() {
        List<ItemStack> result = new ArrayList<>();
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = this.getStackInSlot(slot);
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            result.add(stack);
        }
        return result;
    }
}
