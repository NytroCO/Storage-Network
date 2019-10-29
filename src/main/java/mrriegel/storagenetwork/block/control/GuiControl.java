package mrriegel.storagenetwork.block.control;

import mrriegel.storagenetwork.StorageNetwork;
import mrriegel.storagenetwork.network.CableControlMessage;
import mrriegel.storagenetwork.network.CableDataMessage.CableMessageType;
import mrriegel.storagenetwork.network.RequestCableMessage;
import mrriegel.storagenetwork.registry.PacketRegistry;
import mrriegel.storagenetwork.util.UtilInventory;
import mrriegel.storagenetwork.util.UtilTileEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@SideOnly(Side.CLIENT)
public class GuiControl extends GuiContainer {

    private static final int HEIGHT = 256;
    private static final int WIDTH = 176;
    private static final ResourceLocation texture = new ResourceLocation(StorageNetwork.MODID, "textures/gui/request_full.png");
    final int rowHeight = 25;
    //private TileControl tile;
    private GuiTextField searchBar;
    private Map<Integer, CableRow> allRows = new HashMap<>();
    private int hiddenOffset = 0;
    private final int FONT = 14737632;
    //list includes search bar
    //private List<GuiTextField> textBoxes = new ArrayList<>();
    private List<ProcessWrapper> processors = null;
    //  List<CableRow> visibleRows = new ArrayList<>();
    private boolean rowsCreated;
    //how many rows to skip over
    private int page = 0;
    private int maxPage = 0;//TODO
    private GuiSliderInteger slider;

    public GuiControl(ContainerControl inventorySlotsIn) {
        super(inventorySlotsIn);
        processors = new ArrayList<>();
        this.xSize = WIDTH;
        this.ySize = HEIGHT;
        //tile = inventorySlotsIn.getTileControl();
        //  request the list of tiles
        rowsCreated = false;
        refreshData();
    }

    private void refreshData() {
        PacketRegistry.INSTANCE.sendToServer(new RequestCableMessage());
    }

    @Override
    public void initGui() {
        super.initGui();
        Keyboard.enableRepeatEvents(true);
        searchBar = new GuiTextField(0, fontRenderer,
                guiLeft + 10, guiTop + 160, 158, fontRenderer.FONT_HEIGHT);
        searchBar.setMaxStringLength(30);
        searchBar.setEnableBackgroundDrawing(false);
        searchBar.setVisible(true);
        searchBar.setFocused(true);
        searchBar.setTextColor(16777215);
        //mock data only
        slider = new GuiSliderInteger(this, 777,
                guiLeft + 169, guiTop + 16, 6, 130, 0, 0);
        //    slider.setTooltip("dropper.delay");
        this.addButton(slider);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        //  drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        if (this.searchBar != null) {
            this.searchBar.drawTextBox();
        }
        //todo: visible rows
        hiddenOffset = 0;
        for (CableRow row : this.allRows.values()) {
            //update row location based on page index
            if (!row.compareSearch()) {
                row.x = -199;
                hiddenOffset++;
            } else {
                row.x = guiLeft + 8;
            }
            row.updatePagePosition(this.getPage(), hiddenOffset);
            // is it visible or hidden
            if (row.isOffscreen()) {
                row.hideComponents();
            } else {
                //        StorageNetwork.log("hidden == false for " + row.p.output);
                row.drawScreen();
            }
            if (row.isInside(mouseX, mouseY)) {
                this.drawHoveringText(row.tooltips, mouseX, mouseY);
            } else if (row.isInsideItemstack(mouseX, mouseY)) {
                this.drawHoveringText(
                        row.p.output.getTooltip(Minecraft.getMinecraft().player, ITooltipFlag.TooltipFlags.ADVANCED),
                        mouseX, mouseY);
            }
        }
        for (GuiButton btn : this.buttonList) {
            if (btn.isMouseOver() && btn instanceof GuiControlButton) {
                //TOOLTIP
                GuiControlButton button = (GuiControlButton) btn;
                this.drawHoveringText(button.getTooltips(), mouseX, mouseY);
                //      this.drawHoveringText(textLines, x, y)oh ;
            }
        }
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        super.drawGuiContainerForegroundLayer(mouseX, mouseY);
        for (CableRow row : this.allRows.values()) {
            //draw me
            if (!row.isOffscreen()) {
                GlStateManager.pushMatrix();
                RenderHelper.enableGUIStandardItemLighting();
                //    GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                //        GlStateManager.scale(1f, 1f, .3f);
                int x = row.x - guiLeft;
                int y = row.y - guiTop;
                mc.getRenderItem().renderItemAndEffectIntoGUI(row.p.output, x + 20, y);
                /// TODO target blockname  text
                //AND OR  recipe ing list as text
                //TODO maybe tooltip for this
                this.drawString(this.fontRenderer, row.p.name, x + 40, y + 3, FONT);
                if (!row.p.alwaysOn) {
                    //          GlStateManager.scale(.8f, 1f, 1f);
                    this.drawString(this.fontRenderer, row.p.count + "",
                            x + 112, y + 3, 14735632);
                }
                GlStateManager.popMatrix();
            }
        }
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        renderTextures();
        //we could show how many exist. i guess .
        // this.drawString(this.fontRenderer, (this.maxPage - this.hiddenOffset+1) + "/" + this.maxPage, guiLeft + 160, guiTop + 160, FONT);
    }

    private void renderTextures() {
        this.drawDefaultBackground();//dim the background as normal
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager().bindTexture(texture);
        int xCenter = (this.width - this.xSize) / 2;
        int yCenter = (this.height - this.ySize) / 2;
        this.drawTexturedModalRect(xCenter, yCenter, 0, 0, this.xSize, this.ySize);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int btn) throws IOException {
        super.mouseClicked(mouseX, mouseY, btn);
        if (this.searchBar != null) {
            this.searchBar.mouseClicked(mouseX, mouseY, btn);
            if (inSearchbar(mouseX, mouseY)) {
                searchBar.setFocused(true);
                if (btn == UtilTileEntity.MOUSE_BTN_RIGHT) {
                    searchBar.setText("");
                }
            }
        }
        for (CableRow row : this.allRows.values()) {
            if (row.isInside(mouseX, mouseY)) {
                row.mouseClicked();
            }
        }
        //    for (GuiTextField txtNew : this.textBoxes)
        //      txtNew.mouseClicked(x, y, btn);
    }

    private boolean inSearchbar(int mouseX, int mouseY) {
        return isPointInRegion(10, 160,
                searchBar.width, searchBar.height,
                mouseX, mouseY);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        super.keyTyped(typedChar, keyCode);
        if (this.searchBar != null && this.searchBar.isFocused()) {
            this.searchBar.textboxKeyTyped(typedChar, keyCode);
        }
        //    for (GuiTextField txt : this.textBoxes) {
        //      if (txt.isFocused()) {
        //        txt.textboxKeyTyped(typedChar, keyCode);
        //        break;
        //      }
        //    }
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        Keyboard.enableRepeatEvents(false);
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        this.tryRefresh(50);//20ticks == 1second
        if (processors != null && processors.size() > 0) {
            createAllRows();
        }
        if (searchBar != null) {
            searchBar.updateCursorCounter();
        }
        for (GuiButton btn : this.buttonList) {
            if (btn instanceof GuiControlButton) {
                GuiControlButton button = (GuiControlButton) btn;
                //update texture
                switch (button.messageType) {
                    case P_CTRL_LESS:
                        //do we want a big arrow or small
                        //more right for small
                        button.textureY = 448 + 16;
                        if (GuiScreen.isShiftKeyDown())
                            button.textureX = 211;
                        else
                            button.textureX = 243;
                        break;
                    case P_CTRL_MORE:
                        if (GuiScreen.isShiftKeyDown())
                            button.textureX = 211;
                        else
                            button.textureX = 243;
                        button.textureY = 448;
                        break;
                    case P_ONOFF:
                        button.clearAndSetTooltip(StorageNetwork.lang("processing.buttons.toggle." + button.cable.alwaysOn));
                        if (button.cable.alwaysOn) {
                            //set green
                            button.textureX = 0;
                            button.textureY = 449;
                        } else {
                            //set grey
                            button.textureX = 95;
                            button.textureY = 449;
                        }
                        break;
                    default:
                        break;
                }
            }
        }
    }

    private void tryRefresh(int ticks) {
        if (Minecraft.getMinecraft().player.world.getTotalWorldTime() % ticks == 0) {
            this.refreshData();
        }
    }

    private void createAllRows() {
        if (rowsCreated) {
            return;
        }
        final int spacer = 22;
        int row = 0;
        int btnid = 1;
        Map<Integer, CableRow> rows = new HashMap<>();
        for (ProcessWrapper p : processors) {
            if (p.output.isEmpty()) {
                continue;
            }
            CableRow rowModel = new CableRow(this, p);
            rowModel.index = row;
            rowModel.x = guiLeft + 8;
            rowModel.y = guiTop + 10;
            rowModel.width = 150;
            rowModel.height = 20;
            //buttons come later
            GuiControlButton btnOnOff = new GuiControlButton(btnid++, CableMessageType.P_ONOFF,
                    rowModel.x, rowModel.y, 16, 16, "");
            btnOnOff.cable = p;
            btnOnOff.visible = false;
            rowModel.btnOnOff = btnOnOff;
            this.addButton(rowModel.btnOnOff);
            //      GuiTextFieldProcCable txt = new GuiTextFieldProcCable(btnid++, fontRenderer,
            //          x + 64, y + 4);
            //      txt.setMaxStringLength(4);
            //      txt.setEnableBackgroundDrawing(false);
            //      txt.setVisible(true);
            //      txt.setTextColor(16777215);
            //      //mock data only
            //      txt.setText("" + p.count);
            //  textBoxes.add(txt);
            int offset = 66;
            GuiControlButton btnMinus = new GuiControlButton(btnid++, CableMessageType.P_CTRL_LESS,
                    rowModel.x + offset + 74, rowModel.y, 10, 16, "");
            btnMinus.cable = p;
            btnMinus.clearAndSetTooltip(StorageNetwork.lang("processing.buttons.minus"));
            btnMinus.visible = false;
            this.addButton(btnMinus);
            rowModel.btnMinus = btnMinus;
            GuiControlButton btnPlus = new GuiControlButton(btnid++, CableMessageType.P_CTRL_MORE,
                    rowModel.x + offset + 84,
                    rowModel.y, 10, 16, "");
            btnPlus.cable = p;
            btnPlus.visible = false;
            btnPlus.clearAndSetTooltip(StorageNetwork.lang("processing.buttons.plus"));
            this.addButton(btnPlus);
            rowModel.btnPlus = btnPlus;
            //  rowModel.txtBox = txt;
            rows.put(row, rowModel);
            row++;
            //      y += rowHeight;
        }
        this.setMaxPage(rows.size() - 1);
        //  StorageNetwork.log("MP" + maxPage);
        this.allRows = rows;
        rowsCreated = true;
    }

    private int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
        this.slider.setSliderValue(page, false);
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        super.actionPerformed(button);
        if (button instanceof GuiControlButton) {
            GuiControlButton btn = (GuiControlButton) button;
            int change = GuiScreen.isShiftKeyDown() ? 64 : 1;
            if (GuiScreen.isAltKeyDown()) {
                change *= 16;
            }
            saveMessage(btn.cable, btn.messageType, change);
        }
    }

    private void saveMessage(ProcessWrapper cable, CableMessageType messageType, int changePerClick) {
        int value = 0;
        if (messageType == CableMessageType.P_ONOFF) {
            cable.alwaysOn = !cable.alwaysOn;
            value = cable.alwaysOn ? 1 : 0;
        } else if (messageType == CableMessageType.P_CTRL_LESS) {
            cable.count -= changePerClick;
            if (cable.count < 0) cable.count = 0;
            value = cable.count;
        } else if (messageType == CableMessageType.P_CTRL_MORE) {
            cable.count += changePerClick;
            value = cable.count;
        }
        PacketRegistry.INSTANCE.sendToServer(new CableControlMessage(messageType.ordinal(), value, cable.pos));
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int mouseX = Mouse.getX() * this.width / this.mc.displayWidth;
        int mouseY = this.height - Mouse.getY() * this.height / this.mc.displayHeight - 1;
        //      if (Mouse.getEventDWheel() != 0)
        //      StorageNetwork.log(page + "/" + maxPage + "  scrol" + mouseX + "?" + mouseY);
        if (inField(mouseX, mouseY)) {
            int mouse = Mouse.getEventDWheel();
            if (mouse > 0 && getPage() > 0) {
                setPage(getPage() - 1);
            }
            if (mouse < 0 && getPage() < getMaxPage() - hiddenOffset) {
                setPage(getPage() + 1);
            }
        }
    }

    private boolean inField(int mouseX, int mouseY) {
        return mouseX > guiLeft && mouseX < guiLeft + xSize
                && mouseY > guiTop && mouseY < guiTop + ySize;
    }

    private int getMaxPage() {
        return maxPage;
    }

    private void setMaxPage(int m) {
        this.maxPage = m;
        this.slider.setSliderValue(0, false);
        slider.setMax(m);
    }

    /**
     * Set from network response packet
     *
     * @param cables
     */
    public void setTiles(List<ProcessWrapper> cables) {
        cables = cables.stream().sorted(Comparator.comparing(a -> a.name)).collect(Collectors.toList());
        if (processors == null || processors.size() == 0) {
            processors = cables;
        } else {
            for (ProcessWrapper p : this.processors) {
                for (ProcessWrapper pIncoming : cables) {
                    if (p.pos.equals(pIncoming.pos)) {
                        p.count = pIncoming.count;
                    }
                }
            }
        }
    }

    class CableRow {

        final List<String> tooltips;
        public GuiTextFieldProcCable txtBox;
        int x;
        int y;
        int width;
        int height;
        int index;
        final ProcessWrapper p;
        GuiControlButton btnOnOff;
        GuiControlButton btnMinus;
        GuiControlButton btnPlus;
        private final GuiControl gui;
        CableRow(GuiControl gui, ProcessWrapper p) {
            super();
            this.gui = gui;
            this.p = p;
            this.tooltips = new ArrayList<>();
            if (p.ingredients == null || p.ingredients.size() == 0) {
                this.tooltips.add(StorageNetwork.lang("processing.empty.ingredients"));
            } else {
                for (ItemStack s : p.ingredients) {
                    tooltips.add(s.getCount() + " : " + s.getDisplayName());
                    // s.getTooltip(Minecraft.getMinecraft().player, ITooltipFlag.TooltipFlags.NORMAL);
                }
            }
            this.tooltips.add(TextFormatting.DARK_GRAY + (p.blockId + ""));
            this.tooltips.add(TextFormatting.DARK_GRAY + "(" + p.pos.getBlockPos().getX() + ", " + p.pos.getBlockPos().getY() + ", " + p.pos.getBlockPos().getZ() + ")");
        }

        boolean isInsideItemstack(int mouseX, int mouseY) {
            int loffset = 18;
            int roffset = 50;
            return loffset + x < mouseX && mouseX < x + width - roffset &&
                    y < mouseY && mouseY < y + height;
        }

        boolean isInside(int mouseX, int mouseY) {
            int loffset = 36;
            int roffset = 36;
            return loffset + x < mouseX && mouseX < x + width - roffset &&
                    y < mouseY && mouseY < y + height;
        }

        void mouseClicked() {
            //      StorageNetwork.log("row clicked at " + p.output.getDisplayName());
        }

        boolean compareSearch() {
            if (gui.searchBar.getText().isEmpty()) {
                return true;//hide none, sho wall
            }
            return UtilInventory.doOverlap(gui.searchBar.getText(), p.name)
                    || UtilInventory.doOverlap(gui.searchBar.getText(), p.output.getDisplayName());
        }

        boolean isOffscreen() {
            //above the top, or below the bottom
            return this.x < 0 || this.y < gui.guiTop || this.y > gui.guiTop + 150;
        }

        void drawScreen() {
            btnOnOff.visible = true;
            //     row.txtBox.setVisible(!row.p.alwaysOn);
            btnMinus.visible = (!p.alwaysOn);
            btnPlus.visible = (!p.alwaysOn);
        }

        void hideComponents() {
            this.btnOnOff.visible = false;
            this.btnMinus.visible = false;
            this.btnPlus.visible = false;
        }

        void updatePagePosition(final int page, final int hiddenOffset) {
            //if im at index 3, but page has scrolled up once,
            // and one above me has been hidden, i am at position 1
            final int mockIndex = index - page - hiddenOffset;
            final int rowHeight = 25;
            this.y = guiTop + 10 + mockIndex * rowHeight;
            btnMinus.y = this.y;
            btnPlus.y = this.y;
            btnOnOff.y = this.y;
        }
    }
}
