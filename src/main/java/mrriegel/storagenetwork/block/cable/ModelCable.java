package mrriegel.storagenetwork.block.cable;

import mrriegel.storagenetwork.registry.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;

class ModelCable extends ModelBase {

    private final ModelRenderer south;
    private final ModelRenderer cube;
    private final ModelRenderer north;
    private final ModelRenderer west;
    private final ModelRenderer east;
    private final ModelRenderer up;
    private final ModelRenderer down;
    private final ModelRenderer southC;
    private final ModelRenderer northC;
    private final ModelRenderer westC;
    private final ModelRenderer eastC;
    private final ModelRenderer upC;
    private final ModelRenderer downC;

    public ModelCable() {
        this.textureWidth = 32;
        this.textureHeight = 32;
        this.upC = new ModelRenderer(this, 16, 0);
        this.upC.setRotationPoint(0.0F, 0.0F, 0.0F);
        this.upC.addBox(-3.0F, -3.0F, 7.0F, 6, 6, 1, 0.0F);
        this.setRotateAngle(upC, 1.5707963267948966F, 0.0F, 0.0F);
        this.downC = new ModelRenderer(this, 16, 0);
        this.downC.setRotationPoint(0.0F, 0.0F, 0.0F);
        this.downC.addBox(-3.0F, -3.0F, 7.0F, 6, 6, 1, 0.0F);
        this.setRotateAngle(downC, -1.5707963267948966F, 0.0F, 0.0F);
        this.south = new ModelRenderer(this, 0, 0);
        this.south.setRotationPoint(0.0F, 0.0F, 0.0F);
        this.south.addBox(-2.0F, -2.0F, 0.0F, 4, 4, 8, 0.0F);
        this.southC = new ModelRenderer(this, 16, 0);
        this.southC.setRotationPoint(0.0F, 0.0F, 0.0F);
        this.southC.addBox(-3.0F, -3.0F, 7.0F, 6, 6, 1, 0.0F);
        this.westC = new ModelRenderer(this, 16, 0);
        this.westC.setRotationPoint(0.0F, 0.0F, 0.0F);
        this.westC.addBox(-3.0F, -3.0F, 7.0F, 6, 6, 1, 0.0F);
        this.setRotateAngle(westC, 0.0F, 1.5707963267948966F, 0.0F);
        this.north = new ModelRenderer(this, 0, 0);
        this.north.setRotationPoint(0.0F, 0.0F, 0.0F);
        this.north.addBox(-2.0F, -2.0F, 0.0F, 4, 4, 8, 0.0F);
        this.setRotateAngle(north, 3.141592653589793F, 0.0F, 0.0F);
        this.northC = new ModelRenderer(this, 16, 0);
        this.northC.setRotationPoint(0.0F, 0.0F, 0.0F);
        this.northC.addBox(-3.0F, -3.0F, 7.0F, 6, 6, 1, 0.0F);
        this.setRotateAngle(northC, 3.141592653589793F, 0.0F, 0.0F);
        this.eastC = new ModelRenderer(this, 16, 0);
        this.eastC.setRotationPoint(0.0F, 0.0F, 0.0F);
        this.eastC.addBox(-3.0F, -3.0F, 7.0F, 6, 6, 1, 0.0F);
        this.setRotateAngle(eastC, 0.0F, -1.5707963267948966F, 0.0F);
        this.cube = new ModelRenderer(this, 0, 12);
        this.cube.setRotationPoint(-3.0F, -3.0F, -3.0F);
        this.cube.addBox(0.0F, 0.0F, 0.0F, 6, 6, 6, 0.0F);
        this.up = new ModelRenderer(this, 0, 0);
        this.up.setRotationPoint(0.0F, 0.0F, 0.0F);
        this.up.addBox(-2.0F, -2.0F, 0.0F, 4, 4, 8, 0.0F);
        this.setRotateAngle(up, 1.5707963267948966F, 0.0F, 0.0F);
        this.east = new ModelRenderer(this, 0, 0);
        this.east.setRotationPoint(0.0F, 0.0F, 0.0F);
        this.east.addBox(-2.0F, -2.0F, 0.0F, 4, 4, 8, 0.0F);
        this.setRotateAngle(east, 0.0F, -1.5707963267948966F, 0.0F);
        this.west = new ModelRenderer(this, 0, 0);
        this.west.setRotationPoint(0.0F, 0.0F, 0.0F);
        this.west.addBox(-2.0F, -2.0F, 0.0F, 4, 4, 8, 0.0F);
        this.setRotateAngle(west, 0.0F, 1.5707963267948966F, 0.0F);
        this.down = new ModelRenderer(this, 0, 0);
        this.down.setRotationPoint(0.0F, 0.0F, 0.0F);
        this.down.addBox(-2.0F, -2.0F, 0.0F, 4, 4, 8, 0.0F);
        this.setRotateAngle(down, -1.5707963267948966F, 0.0F, 0.0F);
    }

    /**
     * This is a helper function from Tabula to set the rotation of model parts
     */
    private void setRotateAngle(ModelRenderer modelRenderer, float x, float y, float z) {
        modelRenderer.rotateAngleX = x;
        modelRenderer.rotateAngleY = y;
        modelRenderer.rotateAngleZ = z;
    }

    public void render(UnlistedPropertyBlockNeighbors.BlockNeighbors neighbors, Block kind) {
        float f5 = 0.0625F;
        if (neighbors.north() == UnlistedPropertyBlockNeighbors.EnumNeighborType.CABLE) {
            this.north.render(f5);
        } else if (neighbors.north() == UnlistedPropertyBlockNeighbors.EnumNeighborType.SPECIAL) {
            this.north.render(f5);
            this.northC.render(f5);
        }
        if (neighbors.south() == UnlistedPropertyBlockNeighbors.EnumNeighborType.CABLE) {
            this.south.render(f5);
        } else if (neighbors.south() == UnlistedPropertyBlockNeighbors.EnumNeighborType.SPECIAL) {
            this.south.render(f5);
            this.southC.render(f5);
        }
        if (neighbors.east() == UnlistedPropertyBlockNeighbors.EnumNeighborType.CABLE) {
            this.east.render(f5);
        } else if (neighbors.east() == UnlistedPropertyBlockNeighbors.EnumNeighborType.SPECIAL) {
            this.east.render(f5);
            this.eastC.render(f5);
        }
        if (neighbors.west() == UnlistedPropertyBlockNeighbors.EnumNeighborType.CABLE) {
            this.west.render(f5);
        } else if (neighbors.west() == UnlistedPropertyBlockNeighbors.EnumNeighborType.SPECIAL) {
            this.west.render(f5);
            this.westC.render(f5);
        }
        if (neighbors.up() == UnlistedPropertyBlockNeighbors.EnumNeighborType.CABLE) {
            this.up.render(f5);
        } else if (neighbors.up() == UnlistedPropertyBlockNeighbors.EnumNeighborType.SPECIAL) {
            this.up.render(f5);
            this.upC.render(f5);
        }
        if (neighbors.down() == UnlistedPropertyBlockNeighbors.EnumNeighborType.CABLE) {
            this.down.render(f5);
        } else if (neighbors.down() == UnlistedPropertyBlockNeighbors.EnumNeighborType.SPECIAL) {
            this.down.render(f5);
            this.downC.render(f5);
        }
        if (neighbors.requiresCube() || kind != ModBlocks.kabel) {
            this.cube.render(f5);
        }
    }

    // @Override
    @Override
    public void render(Entity entity, float f, float f1, float f2, float f3, float f4, float f5) {
    }
}