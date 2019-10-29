package mrriegel.storagenetwork.util;

import com.google.common.collect.Lists;
import mrriegel.storagenetwork.StorageNetwork;
import net.minecraft.block.Block;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.oredict.OreDictionary;
import org.apache.commons.lang3.text.WordUtils;

import javax.annotation.Nonnull;
import java.util.*;

public class UtilTileEntity {

    public static final int MOUSE_BTN_LEFT = 0;
    public static final int MOUSE_BTN_RIGHT = 1;
    public static final int MOUSE_BTN_MIDDLE_CLICK = 2;
    private static final Map<String, String> modNamesForIds = new HashMap<>();

    public static void init() {
        Map<String, ModContainer> modMap = Loader.instance().getIndexedModList();
        for (Map.Entry<String, ModContainer> modEntry : modMap.entrySet()) {
            String lowercaseId = modEntry.getKey().toLowerCase(Locale.ENGLISH);
            String modName = modEntry.getValue().getName();
            modNamesForIds.put(lowercaseId, modName);
        }
    }

    @Nonnull
    public static String getModNameForItem(@Nonnull Object object) {
        ResourceLocation itemResourceLocation;
        if (object instanceof Item) {
            itemResourceLocation = Item.REGISTRY.getNameForObject((Item) object);
        } else if (object instanceof Block) {
            itemResourceLocation = Block.REGISTRY.getNameForObject((Block) object);
        } else {
            return null;
        }
        String modId = itemResourceLocation.getResourceDomain();
        String lowercaseModId = modId.toLowerCase(Locale.ENGLISH);
        String modName = modNamesForIds.get(lowercaseModId);
        if (modName == null) {
            modName = WordUtils.capitalize(modId);
            modNamesForIds.put(lowercaseModId, modName);
        }
        return modName;
    }

    public static boolean equalOreDict(ItemStack a, ItemStack b) {
        if (a.isEmpty() || b.isEmpty()) {
            return false;
        }
        int[] ar = OreDictionary.getOreIDs(a);
        int[] br = OreDictionary.getOreIDs(b);
        for (int item : ar)
            for (int value : br)
                if (item == value)
                    return true;
        return false;
    }

    public static <E> boolean contains(List<E> list, E e, Comparator<? super E> c) {
        for (E a : list)
            if (c.compare(a, e) == 0)
                return true;
        return false;
    }

    public static void spawnItemStack(World worldIn, double x, double y, double z, ItemStack stack) {
        if (stack == null || stack.isEmpty() || worldIn.isRemote) {
            return;
        }
        float f = 0.1F;
        float f1 = 0.8F;
        float f2 = 0.1F;
        EntityItem entityitem = new EntityItem(worldIn, x + f, y + f1, z + f2, stack);
        worldIn.spawnEntity(entityitem);
    }

    public static List<BlockPos> getSides(BlockPos pos) {
        List<BlockPos> lis = Lists.newArrayList();
        for (EnumFacing face : EnumFacing.values()) {
            lis.add(pos.offset(face));
        }
        return lis;
    }

    public static void updateTile(World world, BlockPos pos) {
        if (world == null || world.isRemote || world.getTileEntity(pos) == null || !world.getChunkFromBlockCoords(pos).isLoaded())
            return;
        WorldServer w = (WorldServer) world;
        for (EntityPlayer p : w.playerEntities) {
            if (p.getPosition().getDistance(pos.getX(), pos.getY(), pos.getZ()) < 32) {
                try {
                    ((EntityPlayerMP) p).connection.sendPacket(world.getTileEntity(pos).getUpdatePacket());
                    world.markChunkDirty(pos, world.getTileEntity(pos));
                } catch (Error e) {
                    StorageNetwork.instance.logger.error("Update Tile error", e);
                }
            }
        }
    }
}
