package mrriegel.storagenetwork.block.cable.processing;

import com.google.common.collect.Lists;
import mrriegel.storagenetwork.StorageNetwork;
import mrriegel.storagenetwork.block.cable.ContainerCable;
import mrriegel.storagenetwork.block.cable.GuiCableBase;
import mrriegel.storagenetwork.block.cable.GuiCableButton;
import mrriegel.storagenetwork.gui.IPublicGuiContainer;
import mrriegel.storagenetwork.gui.ItemSlotNetwork;
import mrriegel.storagenetwork.network.CableDataMessage;
import mrriegel.storagenetwork.network.CableDataMessage.CableMessageType;
import mrriegel.storagenetwork.network.CableFilterMessage;
import mrriegel.storagenetwork.registry.PacketRegistry;
import mrriegel.storagenetwork.util.inventory.FilterItemStackHandler;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.client.config.GuiCheckBox;

import java.io.IOException;

public class GuiCableProcessing extends GuiCableBase implements IPublicGuiContainer {

    private GuiCableButton pbtnReset;
    private GuiCableButton pbtnBottomface;
    private GuiCableButton pbtnTopface;
    private final TileCableProcess tile;

    public GuiCableProcessing(TileCableProcess tileEntity, ContainerCable inventorySlotsIn) {
        super(inventorySlotsIn);
        this.tile = tileEntity;
    }

    @Override
    public void initGui() {
        super.initGui();
        int x = 0, y = 0;
        //we need some way to let players know if recipe is invalid
        //    buttonRecipe = new GuiCableButton(CableMessageType.TOGGLE_P_RESTARTTRIGGER, guiLeft + 5, guiTop + 5, "S");
        //    buttonRecipe.setCable(tile);
        //    this.addButton(buttonRecipe);
        btnImport = new GuiCableButton(CableMessageType.IMPORT_FILTER, guiLeft + 78, guiTop + 5, "I");
        this.addButton(btnImport);
        btnImport.x += 56;
        //move priority over
        //add custom buttons
        //a click will swap it to EXPORTING with CableDataMessage
        pbtnReset = new GuiCableButton(CableMessageType.TOGGLE_P_RESTARTTRIGGER, guiLeft + 154, guiTop + 5, "R");
        this.addButton(pbtnReset);
        int column = 76, ctr = 24;
        pbtnBottomface = new GuiCableButton(CableMessageType.P_FACE_BOTTOM, guiLeft + column + 20, guiTop + ctr, "");
        this.addButton(pbtnBottomface);
        pbtnTopface = new GuiCableButton(CableMessageType.P_FACE_TOP, guiLeft + column - 12, guiTop + ctr, "");
        this.addButton(pbtnTopface);
        x = 64;
        y = 62;
        checkOreBtn = new GuiCheckBox(10, guiLeft + x, guiTop + y, I18n.format("gui.storagenetwork.checkbox.ore"), true);
        checkOreBtn.setIsChecked(tile.filters.ores);
        this.addButton(checkOreBtn);
        y += 12;
        checkMetaBtn = new GuiCheckBox(11, guiLeft + x, guiTop + y, I18n.format("gui.storagenetwork.checkbox.meta"), true);
        checkMetaBtn.setIsChecked(tile.filters.meta);
        this.addButton(checkMetaBtn);
        //
        y -= 24;
        checkNbtBtn = new GuiCheckBox(12, guiLeft + x, guiTop + y, I18n.format("gui.storagenetwork.checkbox.nbt"), true);
        checkNbtBtn.setIsChecked(tile.filters.nbt);
        this.addButton(checkNbtBtn);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        super.drawGuiContainerForegroundLayer(mouseX, mouseY);
        pbtnBottomface.displayString = tile.getFacingBottomRow().name().substring(0, 2);
        pbtnTopface.displayString = tile.getFacingTopRow().name().substring(0, 2);
        int x = -90;
        int y = 48;
        if (tile.filters.isOutputEmpty() || tile.filters.isInputEmpty()) {
            ///also tooltip here?
            x = -102;
            this.drawString("tile.storagenetwork:recipe.invalid", x, y);
            if (tile.filters.isOutputEmpty())
                this.drawString("tile.storagenetwork:recipe.invalidright", x, y += 12);
            if (tile.filters.isInputEmpty())
                this.drawString("tile.storagenetwork:recipe.invalidleft", x, y += 12);
        } else {
            this.drawString("tile.storagenetwork:recipe.valid", x, y);
        }
        ProcessRequestModel p = tile.getProcessModel();
        x = -90;
        y = 4;
        this.drawString("tile.storagenetwork:controller.name", x, y);
        x += 12;
        y += 18;
        TextFormatting f = (p.isAlwaysActive()) ? TextFormatting.GREEN : TextFormatting.BLUE;
        String txt = StorageNetwork.lang("processing.alwayson." + p.isAlwaysActive());
        if (!p.isAlwaysActive()) {
            txt += p.getCount();
        }
        this.drawString(f + txt, x, y);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        this.drawDefaultBackground();//dim the background as normal
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager().bindTexture(texture);
        int xMiddle = (this.width - this.xSize) / 2;
        int yMiddle = (this.height - this.ySize) / 2;
        this.drawTexturedModalRect(xMiddle, yMiddle, 0, 0, this.xSize, this.ySize);
        int u = 176, v = 34;
        int rows = 3, cols = 6;
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                int x = xMiddle + 7 + SLOT_SIZE * row + (col / 3) * 108;
                int y = yMiddle + 25 + SLOT_SIZE * col;//if col > 3, add jump
                if (col > 2) {
                    y -= 3 * SLOT_SIZE;
                }
                this.drawTexturedModalRect(x, y, u, v, SLOT_SIZE, SLOT_SIZE);
            }
        }
        itemSlotsGhost = Lists.newArrayList();
        // left side
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int index = col + (3 * row);
                ItemStack stack = tile.filters.getStackInSlot(index);
                int num = stack.getCount();
                int x = col * SLOT_SIZE + 8;
                int y = row * SLOT_SIZE + 26;
                itemSlotsGhost.add(new ItemSlotNetwork(this, stack, guiLeft + x, guiTop + y, num, guiLeft, guiTop, true));
            }
        }
        //right side
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int index = 9 + col + (3 * row);
                ItemStack stack = tile.filters.getStackInSlot(index);
                int num = stack.getCount();
                //
                int x = col * SLOT_SIZE + 116;
                int y = row * SLOT_SIZE + 26;
                itemSlotsGhost.add(new ItemSlotNetwork(this, stack, guiLeft + x, guiTop + y, num, guiLeft, guiTop, true));
            }
        }
        for (ItemSlotNetwork s : itemSlotsGhost) {
            s.drawSlot(mouseX, mouseY);
        }
    }

    private void drawString(String s, int x, int y) {
        int color = 0xE0E0E0;
        this.drawString(this.fontRenderer, StorageNetwork.lang(s), x, y, color);
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        super.actionPerformed(button);
        if (pbtnTopface != null && button.id == pbtnTopface.id) {
            int newFace = (tile.getFacingTopRow().ordinal() + 1) % EnumFacing.values().length;
            tile.processingTop = EnumFacing.values()[newFace];
            PacketRegistry.INSTANCE.sendToServer(new CableDataMessage(button.id, newFace));
        } else if (pbtnBottomface != null && button.id == pbtnBottomface.id) {
            //
            int newFace = (tile.getFacingBottomRow().ordinal() + 1) % EnumFacing.values().length;
            tile.processingBottom = EnumFacing.values()[newFace];
            PacketRegistry.INSTANCE.sendToServer(new CableDataMessage(button.id, newFace));
        }
    }

    @Override
    public FilterItemStackHandler getFilterHandler() {
        return tile.filters;
    }

    @Override
    protected void mouseWheelOverSlot(int slot, boolean wheelUp) {
        super.mouseWheelOverSlot(slot, wheelUp);
        if (tile == null || tile.filters == null) {
            return;
        }
        FilterItemStackHandler filters = tile.filters;
        ItemStack filter = filters.getStackInSlot(slot);
        boolean changed = false;
        if (wheelUp && filter.getCount() < 64) {
            filter.setCount(filter.getCount() + 1);
            changed = true;
        } else if (!wheelUp && filter.getCount() > 1) {
            filter.setCount(filter.getCount() - 1);
            changed = true;
        }
        if (changed) {
            PacketRegistry.INSTANCE.sendToServer(new CableFilterMessage(slot, filter, filters.ores, filters.meta, checkNbtBtn.isChecked()));
        }
    }

    @Override
    protected void drawTooltips(int mouseX, int mouseY) {
        super.drawTooltips(mouseX, mouseY);
        if (pbtnReset.isMouseOver()) {
            drawHoveringText(Lists.newArrayList(I18n.format("gui.storagenetwork.refresh")), mouseX, mouseY);
        }
        if (pbtnTopface.isMouseOver()) {
            this.drawHoveringText(Lists.newArrayList(I18n.format("gui.storagenetwork.processing.recipe")), mouseX, mouseY, fontRenderer);
        }
        if (pbtnBottomface.isMouseOver()) {
            this.drawHoveringText(Lists.newArrayList(I18n.format("gui.storagenetwork.processing.extract")), mouseX, mouseY, fontRenderer);
        }
    }

    @Override
    public void drawGradientRectP(int left, int top, int right, int bottom, int startColor, int endColor) {
        super.drawGradientRect(left, top, right, bottom, startColor, endColor);
    }

    @Override
    public boolean isPointInRegionP(int rectX, int rectY, int rectWidth, int rectHeight, int pointX, int pointY) {
        return super.isPointInRegion(rectX, rectY, rectWidth, rectHeight, pointX, pointY);
    }

    @Override
    public void renderToolTipP(ItemStack stack, int x, int y) {
        super.renderToolTip(stack, x, y);
    }
}
