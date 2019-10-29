package mrriegel.storagenetwork.item;

import mrriegel.storagenetwork.CreativeTab;
import mrriegel.storagenetwork.api.data.EnumUpgradeType;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;

public class ItemUpgrade extends Item {

    public ItemUpgrade() {
        super();
        this.setCreativeTab(CreativeTab.tab);
        this.setHasSubtypes(true);
        this.setRegistryName("upgrade");
        this.setUnlocalizedName(getRegistryName().toString());
        this.setMaxStackSize(64);
    }

    @Override
    public String getUnlocalizedName(ItemStack stack) {
        return this.getUnlocalizedName() + "_" + stack.getItemDamage();
    }

    @Override
    public void addInformation(ItemStack stack, World playerIn, List<String> tooltip, ITooltipFlag advanced) {
        super.addInformation(stack, playerIn, tooltip, advanced);
        tooltip.add(I18n.format("tooltip.storagenetwork.upgrade_" + stack.getItemDamage()));
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> list) {
        if (isInCreativeTab(tab)) {
            for (EnumUpgradeType upgradeType : EnumUpgradeType.values()) {
                list.add(new ItemStack(this, 1, upgradeType.getId()));
            }
        }
    }
}
