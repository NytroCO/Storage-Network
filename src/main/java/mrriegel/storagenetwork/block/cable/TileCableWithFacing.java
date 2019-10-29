package mrriegel.storagenetwork.block.cable;

import mrriegel.storagenetwork.block.master.TileMaster;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.items.CapabilityItemHandler;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class TileCableWithFacing extends TileCable {

    @Nullable
    private
    EnumFacing direction = null;

    public boolean hasDirection() {
        return direction != null;
    }

    public EnumFacing getDirection() {
        return direction;
    }

    public void setDirection(@Nullable EnumFacing direction) {
        this.direction = direction;
    }

    public BlockPos getFacingPosition() {
        return this.getPos().offset(direction);
    }

    public void findNewDirection() {
        if (isValidLinkNeighbor(direction)) {
            return;
        }
        for (EnumFacing facing : EnumFacing.values()) {
            if (isValidLinkNeighbor(facing)) {
                setDirection(facing);
                return;
            }
        }
        setDirection(null);
    }

    private boolean isValidLinkNeighbor(EnumFacing facing) {
        if (facing == null) {
            return false;
        }
        if (!TileMaster.isTargetAllowed(world.getBlockState(pos.offset(facing)))) {
            return false;
        }
        TileEntity neighbor = world.getTileEntity(pos.offset(facing));
        return neighbor != null && neighbor.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, facing.getOpposite());
    }

    public void rotate() {
        EnumFacing previous = direction;
        List<EnumFacing> targetFaces = Arrays.asList(EnumFacing.values());
        Collections.shuffle(targetFaces);
        for (EnumFacing facing : EnumFacing.values()) {
            if (previous == facing) {
                continue;
            }
            if (isValidLinkNeighbor(facing)) {
                setDirection(facing);
                this.markDirty();
                if (previous != direction) {
                    TileMaster master = getTileMaster();
                    if (master != null) {
                        master.refreshNetwork();
                    }
                }
                return;
            }
        }
    }

    private TileMaster getTileMaster() {
        if (getMaster() == null) {
            return null;
        }
        return getMaster().getTileEntity(TileMaster.class);
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        if (compound.hasKey("direction")) {
            this.direction = EnumFacing.getFront(compound.getInteger("direction"));
        } else {
            this.direction = null;
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        if (direction != null) {
            compound.setInteger("direction", this.direction.ordinal());
        }
        return super.writeToNBT(compound);
    }
}
