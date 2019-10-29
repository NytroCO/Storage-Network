package mrriegel.storagenetwork.jei;

import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mrriegel.storagenetwork.gui.IStorageContainer;
import mrriegel.storagenetwork.network.RecipeMessage;
import mrriegel.storagenetwork.registry.PacketRegistry;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.nbt.NBTTagCompound;

class RequestRecipeTransferHandlerRemote<C extends Container & IStorageContainer> implements IRecipeTransferHandler<C> {

    private final Class<C> clazz;

    public RequestRecipeTransferHandlerRemote(Class<C> clazz) {
        this.clazz = clazz;
    }

    @Override
    public Class<C> getContainerClass() {
        return clazz;
    }

    @Override
    public IRecipeTransferError transferRecipe(Container container, IRecipeLayout recipeLayout, EntityPlayer player, boolean maxTransfer, boolean doTransfer) {
        if (doTransfer) {
            NBTTagCompound nbt = RequestRecipeTransferHandler.recipeToTag(container, recipeLayout);
            PacketRegistry.INSTANCE.sendToServer(new RecipeMessage(nbt));
        }
        return null;
    }
}
