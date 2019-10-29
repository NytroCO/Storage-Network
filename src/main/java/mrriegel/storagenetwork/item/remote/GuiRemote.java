package mrriegel.storagenetwork.item.remote;

import mrriegel.storagenetwork.StorageNetwork;
import mrriegel.storagenetwork.data.EnumSortType;
import mrriegel.storagenetwork.gui.GuiContainerStorageInventory;
import mrriegel.storagenetwork.util.NBTHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nullable;

public class GuiRemote extends GuiContainerStorageInventory {

    private final ContainerRemote container;

    public GuiRemote(ContainerRemote container) {
        super(container);
        this.container = container;
        isSimple = container.getItemRemote().getMetadata() == RemoteType.SIMPLE.ordinal();
        if (isSimple) {
            //set different texture for simple
            texture = new ResourceLocation(StorageNetwork.MODID, "textures/gui/request_full.png");
            this.setSort(EnumSortType.NAME);
            this.setDownwards(false);
        }
    }

    @Override
    protected boolean isScreenValid() {
        return !container.getItemRemote().isEmpty();
    }

    @Override
    public boolean getDownwards() {
        ItemStack remote = container.getItemRemote();
        if (!remote.isEmpty())
            return NBTHelper.getBoolean(remote, "down");
        return false;
    }

    @Override
    public void setDownwards(boolean d) {
        ItemStack remote = container.getItemRemote();
        if (!remote.isEmpty())
            NBTHelper.setBoolean(remote, "down", d);
    }

    @Override
    public @Nullable
    EnumSortType getSort() {
        ItemStack remote = container.getItemRemote();
        if (!remote.isEmpty())
            return EnumSortType.valueOf(NBTHelper.getString(remote, "sort"));
        return null;
    }

    @Override
    public void setSort(EnumSortType s) {
        ItemStack remote = container.getItemRemote();
        if (!remote.isEmpty())
            NBTHelper.setString(remote, "sort", s.toString());
    }

    @Override
    protected int getDim() {
        ItemStack remote = container.getItemRemote();
        if (!remote.isEmpty())
            return NBTHelper.getInteger(remote, "dim");
        return 0;
    }

    @Override
    public BlockPos getPos() {
        return BlockPos.ORIGIN;
    }
}
