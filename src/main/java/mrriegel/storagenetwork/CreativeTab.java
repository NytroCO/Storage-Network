package mrriegel.storagenetwork;

import mrriegel.storagenetwork.registry.ModBlocks;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;

public class CreativeTab {

    public static final CreativeTabs tab = new CreativeTabs(StorageNetwork.MODID) {

        @Override
        public String getTranslatedTabLabel() {
            return StorageNetwork.MODNAME;
        }

        @Override
        public ItemStack getTabIconItem() {
            return new ItemStack(ModBlocks.request);
        }
    };
}
