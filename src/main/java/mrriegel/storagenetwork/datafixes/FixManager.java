package mrriegel.storagenetwork.datafixes;

import mrriegel.storagenetwork.StorageNetwork;
import net.minecraft.util.datafix.DataFixer;
import net.minecraft.util.datafix.FixTypes;
import net.minecraftforge.common.util.CompoundDataFixer;
import net.minecraftforge.common.util.ModFixs;
import net.minecraftforge.fml.common.FMLCommonHandler;

public class FixManager {

    private final DataFixer fixer;
    private final ModFixs fixs;

    public FixManager() {
        this.fixer = FMLCommonHandler.instance().getDataFixer();
        this.fixs = ((CompoundDataFixer) this.fixer).init(StorageNetwork.MODID, 0);
        this.fixs.registerFix(FixTypes.CHUNK, new ChunkBasedFixer());
    }
}
