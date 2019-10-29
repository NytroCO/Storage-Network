package mrriegel.storagenetwork.block.control;

import mrriegel.storagenetwork.api.data.DimPos;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public class ProcessWrapper {

    public boolean alwaysOn;
    public String name;
    public DimPos pos;
    public ItemStack output;
    public int count;
    public List<ItemStack> ingredients;
    public ResourceLocation blockId;

    public ProcessWrapper(DimPos p, ItemStack s, int c, String name, boolean on) {
        pos = p;
        output = s;
        count = c;
        this.name = name;
        alwaysOn = on;
    }

    public ProcessWrapper() {
    }

    public void readFromNBT(NBTTagCompound compound) {
        name = compound.getString("sname");
        blockId = new ResourceLocation(compound.getString("blockId"));
        alwaysOn = compound.getBoolean("aon");
        int x = compound.getInteger("xx");
        int y = compound.getInteger("yy");
        int z = compound.getInteger("zz");
        pos = new DimPos(compound.getCompoundTag("pos"));
        output = new ItemStack(compound);
        this.count = compound.getInteger("cou");
        NBTTagList nbttaglist = compound.getTagList("Items", 10);
        ingredients = new ArrayList<>();
        for (int i = 0; i < nbttaglist.tagCount(); ++i) {
            NBTTagCompound nbttagcompound = nbttaglist.getCompoundTagAt(i);
            ingredients.add(new ItemStack(nbttagcompound));
        }
    }

    public void writeToNBT(NBTTagCompound compound) {
        output.writeToNBT(compound);
        compound.setString("blockId", blockId.toString());
        compound.setString("sname", name);
        compound.setBoolean("aon", alwaysOn);
        compound.setTag("pos", pos.serializeNBT());
        compound.setInteger("cou", count);
        NBTTagList nbttaglist = new NBTTagList();
        for (ItemStack ingredient : ingredients) {
            NBTTagCompound nbttagcompound = new NBTTagCompound();
            ingredient.writeToNBT(nbttagcompound);
            nbttaglist.appendTag(nbttagcompound);
        }
        compound.setTag("Items", nbttaglist);
    }

    public void init() {
    }
}
