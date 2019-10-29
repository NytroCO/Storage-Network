package mrriegel.storagenetwork.block.control;

import mrriegel.storagenetwork.block.TileConnectable;
import mrriegel.storagenetwork.data.EnumSortType;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants;

import java.util.HashMap;
import java.util.Map;

public class TileControl extends TileConnectable {

    private Map<Integer, ItemStack> matrix = new HashMap<>();
    private boolean downwards;
    private EnumSortType sort = EnumSortType.NAME;

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        setDownwards(compound.getBoolean("dir"));
        setSort(EnumSortType.valueOf(compound.getString("sort")));
        NBTTagList invList = compound.getTagList("matrix", Constants.NBT.TAG_COMPOUND);
        matrix = new HashMap<>();
        for (int i = 0; i < invList.tagCount(); i++) {
            NBTTagCompound stackTag = invList.getCompoundTagAt(i);
            int slot = stackTag.getByte("Slot");
            ItemStack s = new ItemStack(stackTag);
            matrix.put(slot, s);
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setBoolean("dir", isDownwards());
        compound.setString("sort", getSort().toString());
        NBTTagList invList = new NBTTagList();
        invList = new NBTTagList();
        for (int i = 0; i < 9; i++) {
            if (matrix.get(i) != null && !matrix.get(i).isEmpty()) {
                NBTTagCompound stackTag = new NBTTagCompound();
                stackTag.setByte("Slot", (byte) i);
                matrix.get(i).writeToNBT(stackTag);
                invList.appendTag(stackTag);
            }
        }
        compound.setTag("matrix", invList);
        return compound;
    }

    private boolean isDownwards() {
        return downwards;
    }

    private void setDownwards(boolean downwards) {
        this.downwards = downwards;
    }

    private EnumSortType getSort() {
        return sort;
    }

    private void setSort(EnumSortType sort) {
        this.sort = sort;
    }
}
