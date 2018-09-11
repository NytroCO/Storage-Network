package mrriegel.storagenetwork.gui.fb;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import mrriegel.storagenetwork.item.remote.ItemRemote;
import mrriegel.storagenetwork.util.NBTHelper;
import mrriegel.storagenetwork.util.data.EnumSortType;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class GuiFastRemote extends GuiFastNetworkCrafter {

	public GuiFastRemote(EntityPlayer player, World world) {
		super(player, world, BlockPos.ORIGIN);
		this.inventorySlots = new ContainerFastRemote(player, world, BlockPos.ORIGIN) {
			public void onCraftMatrixChanged(IInventory inventoryIn) {
			}
		};
	}

	@Override
	public boolean getDownwards() {
		ItemStack remote = getItemRemote();
		if (remote.isEmpty() == false) return NBTHelper.getBoolean(remote, "down");
		return false;
	}

	@Override
	public void setDownwards(boolean d) {
		ItemStack remote = getItemRemote();
		if (remote.isEmpty() == false) NBTHelper.setBoolean(remote, "down", d);
	}

	@Override
	public @Nullable EnumSortType getSort() {
		ItemStack remote = getItemRemote();
		if (remote.isEmpty() == false) return EnumSortType.valueOf(NBTHelper.getString(remote, "sort"));
		return null;
	}

	public @Nonnull ItemStack getItemRemote() {
		ItemStack remote = mc.player.inventory.getCurrentItem();
		if (remote.getItem() instanceof ItemRemote == false) { return ItemStack.EMPTY; }
		return remote;
	}

	@Override
	public void setSort(EnumSortType s) {
		ItemStack remote = getItemRemote();
		if (remote.isEmpty() == false) NBTHelper.setString(remote, "sort", s.toString());
	}

	@Override
	protected int getDim() {
		ItemStack remote = getItemRemote();
		if (remote.isEmpty() == false) return NBTHelper.getInteger(remote, "dim");
		return 0;
	}

	@Override
	protected boolean isScreenValid() {
		return this.getItemRemote().isEmpty() == false;
	}

	@Override
	public BlockPos getPos() {
		return BlockPos.ORIGIN;
	}
}
