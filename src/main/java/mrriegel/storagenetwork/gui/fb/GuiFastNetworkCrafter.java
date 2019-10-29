package mrriegel.storagenetwork.gui.fb;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.mojang.realmsclient.gui.ChatFormatting;
import mrriegel.storagenetwork.StorageNetwork;
import mrriegel.storagenetwork.data.EnumSortType;
import mrriegel.storagenetwork.gui.IPublicGuiContainer;
import mrriegel.storagenetwork.gui.IStorageInventory;
import mrriegel.storagenetwork.gui.ItemSlotNetwork;
import mrriegel.storagenetwork.jei.JeiHooks;
import mrriegel.storagenetwork.jei.JeiSettings;
import mrriegel.storagenetwork.network.ClearRecipeMessage;
import mrriegel.storagenetwork.network.InsertMessage;
import mrriegel.storagenetwork.network.RequestMessage;
import mrriegel.storagenetwork.network.SortMessage;
import mrriegel.storagenetwork.registry.PacketRegistry;
import mrriegel.storagenetwork.util.UtilTileEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag.TooltipFlags;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.oredict.OreDictionary;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import shadows.fastbench.gui.GuiFastBench;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Base class for Request table inventory and Remote inventory
 */
public abstract class GuiFastNetworkCrafter extends GuiFastBench implements IPublicGuiContainer, IStorageInventory {

    private static final int HEIGHT = 256;
    private static final int WIDTH = 176;
    private static final ResourceLocation texture = new ResourceLocation(StorageNetwork.MODID, "textures/gui/request.png");
    private List<ItemStack> stacks;
    private List<ItemStack> craftableStacks;
    private int page = 1;
    private int maxPage = 1;
    private ItemStack stackUnderMouse = ItemStack.EMPTY;
    private GuiTextField searchBar;
    private GuiStorageButton directionBtn;
    private GuiStorageButton sortBtn;
    private GuiStorageButton jeiBtn;
    private GuiStorageButton clearTextBtn;
    private List<ItemSlotNetwork> slots;
    private long lastClick;
    private boolean forceFocus;

    GuiFastNetworkCrafter(EntityPlayer player, World world, BlockPos pos) {
        super(player.inventory, world, pos);
        this.xSize = WIDTH;
        this.ySize = HEIGHT;
        this.stacks = Lists.newArrayList();
        this.craftableStacks = Lists.newArrayList();
        PacketRegistry.INSTANCE.sendToServer(new RequestMessage());
        lastClick = System.currentTimeMillis();
    }

    @Override
    public void setStacks(List<ItemStack> stacks) {
        this.stacks = stacks;
    }

    @Override
    public void setCraftableStacks(List<ItemStack> stacks) {
        this.craftableStacks = stacks;
    }

    @Override
    public void drawGradientRectP(int left, int top, int right, int bottom, int startColor, int endColor) {
        super.drawGradientRect(left, top, right, bottom, startColor, endColor);
    }

    @Override
    public FontRenderer getFont() {
        return this.fontRenderer;
    }

    @Override
    public boolean isPointInRegionP(int rectX, int rectY, int rectWidth, int rectHeight, int pointX, int pointY) {
        return super.isPointInRegion(rectX, rectY, rectWidth, rectHeight, pointX, pointY);
    }

    @Override
    public void renderToolTipP(ItemStack stack, int x, int y) {
        super.renderToolTip(stack, x, y);
    }

    @Override
    public void initGui() {
        super.initGui();
        Keyboard.enableRepeatEvents(true);
        searchBar = new GuiTextField(0, fontRenderer, guiLeft + 81, guiTop + 96, 85, fontRenderer.FONT_HEIGHT);
        searchBar.setMaxStringLength(30);
        searchBar.setEnableBackgroundDrawing(false);
        searchBar.setVisible(true);
        searchBar.setTextColor(16777215);
        searchBar.setFocused(true);
        if (JeiSettings.isJeiLoaded() && JeiSettings.isJeiSearchSynced()) {
            searchBar.setText(JeiHooks.getFilterText());
        }
        directionBtn = new GuiStorageButton(0, guiLeft + 7, guiTop + 93, "");
        this.addButton(directionBtn);
        sortBtn = new GuiStorageButton(1, guiLeft + 21, guiTop + 93, "");
        this.addButton(sortBtn);
        jeiBtn = new GuiStorageButton(4, guiLeft + 35, guiTop + 93, "");
        if (JeiSettings.isJeiLoaded()) {
            this.addButton(jeiBtn);
        }
        clearTextBtn = new GuiStorageButton(5, guiLeft + 64, guiTop + 93, "X");
        this.addButton(clearTextBtn);
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        if (searchBar != null) {
            searchBar.updateCursorCounter();
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
        super.renderHoveredToolTip(mouseX, mouseY);
        if (!this.isScreenValid()) {
            mc.player.closeScreen();
            return;
        }
        drawTooltips(mouseX, mouseY);
    }

    @Override
    public void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT))
            this.fontRenderer.drawString("f", 0, 0, 4210752);
        this.fontRenderer.drawString(I18n.format("container.inventory"), 8, this.ySize - 96 + 4, 4210752);
        if (!this.isScreenValid()) {
            return;
        }
        if (forceFocus) {
            this.searchBar.setFocused(true);
            if (this.searchBar.isFocused()) {
                this.forceFocus = false;
            }
        }
    }

    @Override
    public void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        if (!this.isScreenValid()) {
            return;
        }
        renderTextures();
        List<ItemStack> stacksToDisplay = applySearchTextToSlots();
        sortItemStacks(stacksToDisplay);
        applyScrollPaging(stacksToDisplay);
        rebuildItemSlots(stacksToDisplay);
        renderItemSlots(mouseX, mouseY);
        searchBar.drawTextBox();
    }

    protected abstract boolean isScreenValid();

    private void renderTextures() {
        this.drawDefaultBackground();//dim the background as normal
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager().bindTexture(texture);
        int xCenter = (this.width - this.xSize) / 2;
        int yCenter = (this.height - this.ySize) / 2;
        this.drawTexturedModalRect(xCenter, yCenter, 0, 0, this.xSize, this.ySize);
    }

    private List<ItemStack> applySearchTextToSlots() {
        String searchText = searchBar.getText();
        List<ItemStack> stacksToDisplay = searchText.equals("") ? Lists.newArrayList(stacks) : Lists.newArrayList();
        if (!searchText.equals("")) {
            for (ItemStack stackWrapper : stacks) {
                if (doesStackMatchSearch(stackWrapper)) {
                    stacksToDisplay.add(stackWrapper);
                }
            }
        }
        return stacksToDisplay;
    }

    private void sortItemStacks(List<ItemStack> stacksToDisplay) {
        stacksToDisplay.sort(new Comparator<ItemStack>() {

            final int mul = getDownwards() ? -1 : 1;

            @Override
            public int compare(ItemStack o2, ItemStack o1) {
                switch (getSort()) {
                    case AMOUNT:
                        return Integer.compare(o1.getCount(), o2.getCount()) * mul;
                    case NAME:
                        return o2.getDisplayName().compareToIgnoreCase(o1.getDisplayName()) * mul;
                    case MOD:
                        return UtilTileEntity.getModNameForItem(o2.getItem()).compareToIgnoreCase(UtilTileEntity.getModNameForItem(o1.getItem())) * mul;
                }
                return 0;
            }
        });
    }

    private void applyScrollPaging(List<ItemStack> stacksToDisplay) {
        maxPage = stacksToDisplay.size() / (getColumns());
        if (stacksToDisplay.size() % (getColumns()) != 0) {
            maxPage++;
        }
        maxPage -= (getLines() - 1);
        if (maxPage < 1) {
            maxPage = 1;
        }
        if (page < 1) {
            page = 1;
        }
        if (page > maxPage) {
            page = maxPage;
        }
    }

    private void rebuildItemSlots(List<ItemStack> stacksToDisplay) {
        slots = Lists.newArrayList();
        int index = (page - 1) * (getColumns());
        for (int row = 0; row < getLines(); row++) {
            for (int col = 0; col < getColumns(); col++) {
                if (index >= stacksToDisplay.size()) {
                    break;
                }
                int in = index;
                slots.add(new ItemSlotNetwork(this, stacksToDisplay.get(in), guiLeft + 8 + col * 18, guiTop + 10 + row * 18, stacksToDisplay.get(in).getCount(), guiLeft, guiTop, true));
                index++;
            }
        }
    }

    private void renderItemSlots(int mouseX, int mouseY) {
        stackUnderMouse = ItemStack.EMPTY;
        for (ItemSlotNetwork slot : slots) {
            slot.drawSlot(mouseX, mouseY);
            if (slot.isMouseOverSlot(mouseX, mouseY)) {
                stackUnderMouse = slot.getStack();
                //        break;
            }
        }
        if (slots.isEmpty()) {
            stackUnderMouse = ItemStack.EMPTY;
        }
    }

    private boolean doesStackMatchSearch(ItemStack stack) {
        String searchText = searchBar.getText();
        if (searchText.startsWith("@")) {
            String name = UtilTileEntity.getModNameForItem(stack.getItem());
            return name.toLowerCase().contains(searchText.toLowerCase().substring(1));
        } else if (searchText.startsWith("#")) {
            String tooltipString;
            List<String> tooltip = stack.getTooltip(mc.player, TooltipFlags.NORMAL);
            tooltipString = Joiner.on(' ').join(tooltip).toLowerCase();
            tooltipString = ChatFormatting.stripFormatting(tooltipString);
            return tooltipString.toLowerCase().contains(searchText.toLowerCase().substring(1));
        } else if (searchText.startsWith("$")) {
            StringBuilder oreDictStringBuilder = new StringBuilder();
            for (int oreId : OreDictionary.getOreIDs(stack)) {
                String oreName = OreDictionary.getOreName(oreId);
                oreDictStringBuilder.append(oreName).append(' ');
            }
            return oreDictStringBuilder.toString().toLowerCase().contains(searchText.toLowerCase().substring(1));
        } else if (searchText.startsWith("%")) {
            StringBuilder creativeTabStringBuilder = new StringBuilder();
            for (CreativeTabs creativeTab : stack.getItem().getCreativeTabs()) {
                if (creativeTab != null) {
                    String creativeTabName = creativeTab.getTranslatedTabLabel();
                    creativeTabStringBuilder.append(creativeTabName).append(' ');
                }
            }
            return creativeTabStringBuilder.toString().toLowerCase().contains(searchText.toLowerCase().substring(1));
        } else {
            return stack.getDisplayName().toLowerCase().contains(searchText.toLowerCase());
        }
    }

    protected abstract boolean getDownwards();

    protected abstract void setDownwards(boolean d);

    protected abstract EnumSortType getSort();

    private int getColumns() {
        return 9;
    }

    private int getLines() {
        return 4;
    }

    protected abstract void setSort(EnumSortType s);

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        searchBar.setFocused(false);
        if (inSearchbar(mouseX, mouseY)) {
            searchBar.setFocused(true);
            if (mouseButton == UtilTileEntity.MOUSE_BTN_RIGHT) {
                clearSearch();
            }
        } else if (inX(mouseX, mouseY)) {
            PacketRegistry.INSTANCE.sendToServer(new ClearRecipeMessage());
            PacketRegistry.INSTANCE.sendToServer(new RequestMessage(0, ItemStack.EMPTY, false, false));
        } else {
            ItemStack stackCarriedByMouse = mc.player.inventory.getItemStack();
            if (!stackUnderMouse.isEmpty() &&
                    (mouseButton == UtilTileEntity.MOUSE_BTN_LEFT
                            || mouseButton == UtilTileEntity.MOUSE_BTN_RIGHT
                            || mouseButton == UtilTileEntity.MOUSE_BTN_MIDDLE_CLICK)
                    && stackCarriedByMouse.isEmpty() && canClick()) {
                PacketRegistry.INSTANCE.sendToServer(new RequestMessage(mouseButton, stackUnderMouse, isShiftKeyDown(),
                        mouseButton == UtilTileEntity.MOUSE_BTN_MIDDLE_CLICK));
                lastClick = System.currentTimeMillis();
            } else if (!stackCarriedByMouse.isEmpty() && inField(mouseX, mouseY) && canClick()) {
                PacketRegistry.INSTANCE.sendToServer(new InsertMessage(getDim(), mouseButton));
                lastClick = System.currentTimeMillis();
            }
        }
    }

    private boolean inX(int mouseX, int mouseY) {
        return isPointInRegion(63, 110, 7, 7, mouseX, mouseY);
    }

    private boolean canClick() {
        return System.currentTimeMillis() > lastClick + 100L;
    }

    private boolean inField(int mouseX, int mouseY) {
        return mouseX > (guiLeft + 7) && mouseX < (guiLeft + xSize - 7) && mouseY > (guiTop + 7) && mouseY < (guiTop + 90);
    }

    protected abstract int getDim();

    @Override
    public void actionPerformed(GuiButton button) throws IOException {
        super.actionPerformed(button);
        boolean doSort = true;
        if (button.id == directionBtn.id) {
            setDownwards(!getDownwards());
        } else if (button.id == sortBtn.id) {
            setSort(getSort().next());
        } else if (button.id == jeiBtn.id) {
            doSort = false;
            JeiSettings.setJeiSearchSync(!JeiSettings.isJeiSearchSynced());
        } else if (button.id == this.clearTextBtn.id) {
            doSort = false;
            clearSearch();
            //      this.fieldOperationLimit.setFocused(true);//doesnt work..somethings overriding it?
            this.forceFocus = true;//we have to force it to go next-tick
        }
        if (doSort) {
            PacketRegistry.INSTANCE.sendToServer(new SortMessage(getPos(), getDownwards(), getSort()));
        }
    }

    private void clearSearch() {
        searchBar.setText("");
        if (JeiSettings.isJeiSearchSynced()) {
            JeiHooks.setFilterText("");
        }
    }

    protected abstract BlockPos getPos();

    @Override
    public void keyTyped(char typedChar, int keyCode) throws IOException {
        if (!this.checkHotbarKeys(keyCode)) {
            Keyboard.enableRepeatEvents(true);
            if (searchBar.isFocused() && this.searchBar.textboxKeyTyped(typedChar, keyCode)) {
                PacketRegistry.INSTANCE.sendToServer(new RequestMessage(0, ItemStack.EMPTY, false, false));
                if (JeiSettings.isJeiLoaded() && JeiSettings.isJeiSearchSynced()) {
                    JeiHooks.setFilterText(searchBar.getText());
                }
            } else if (!this.stackUnderMouse.isEmpty()) {
                JeiHooks.testJeiKeybind(keyCode, this.stackUnderMouse);
            } else {
                super.keyTyped(typedChar, keyCode);
            }
        }
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        Keyboard.enableRepeatEvents(false);
    }

    private void drawTooltips(int mouseX, int mouseY) {
        for (ItemSlotNetwork s : slots) {
            if (s.isMouseOverSlot(mouseX, mouseY)) {
                s.drawTooltip(mouseX, mouseY);
            }
        }
        if (inSearchbar(mouseX, mouseY)) {
            List<String> lis = Lists.newArrayList();
            if (!isShiftKeyDown()) {
                lis.add(I18n.format("gui.storagenetwork.shift"));
            } else {
                lis.add(I18n.format("gui.storagenetwork.fil.tooltip_0"));
                lis.add(I18n.format("gui.storagenetwork.fil.tooltip_1"));
                lis.add(I18n.format("gui.storagenetwork.fil.tooltip_2"));
                lis.add(I18n.format("gui.storagenetwork.fil.tooltip_3"));
            }
            drawHoveringText(lis, mouseX, mouseY);
        }
        if (clearTextBtn.isMouseOver()) {
            drawHoveringText(Lists.newArrayList(I18n.format("gui.storagenetwork.tooltip_clear")), mouseX, mouseY);
        }
        if (sortBtn.isMouseOver()) {
            drawHoveringText(Lists.newArrayList(I18n.format("gui.storagenetwork.req.tooltip_" + getSort().toString())), mouseX, mouseY);
        }
        if (directionBtn.isMouseOver()) {
            drawHoveringText(Lists.newArrayList(I18n.format("gui.storagenetwork.sort")), mouseX, mouseY);
        }
        if (jeiBtn != null && jeiBtn.isMouseOver()) {
            String s = I18n.format(JeiSettings.isJeiSearchSynced() ? "gui.storagenetwork.fil.tooltip_jei_on" : "gui.storagenetwork.fil.tooltip_jei_off");
            drawHoveringText(Lists.newArrayList(s), mouseX, mouseY);
        }
    }

    private boolean inSearchbar(int mouseX, int mouseY) {
        return isPointInRegion(81, 96, 85, fontRenderer.FONT_HEIGHT, mouseX, mouseY);
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int i = Mouse.getX() * this.width / this.mc.displayWidth;
        int j = this.height - Mouse.getY() * this.height / this.mc.displayHeight - 1;
        if (inField(i, j)) {
            int mouse = Mouse.getEventDWheel();
            if (mouse > 0 && page > 1) {
                page--;
            }
            if (mouse < 0 && page < maxPage) {
                page++;
            }
        }
    }

    protected class GuiStorageButton extends GuiButton {

        GuiStorageButton(int id, int x, int y, String str) {
            super(id, x, y, 14, 14, str);
        }

        public GuiStorageButton(int id, int x, int y, int width, String str) {
            super(id, x, y, width, 14, str);
        }

        @Override
        public void drawButton(Minecraft mc, int x, int y, float pticks) {
            if (this.visible) {
                FontRenderer fontrenderer = mc.fontRenderer;
                mc.getTextureManager().bindTexture(texture);
                GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
                this.hovered = x >= this.x && y >= this.y && x < this.x + this.width && y < this.y + this.height;
                int k = this.getHoverState(this.hovered);
                GlStateManager.enableBlend();
                GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
                GlStateManager.blendFunc(770, 771);
                this.drawTexturedModalRect(this.x, this.y, 162 + 14 * k, 0, width, height);
                if (id == directionBtn.id) {
                    this.drawTexturedModalRect(this.x + 4, this.y + 3, WIDTH + (getDownwards() ? 6 : 0), 14, 6, 8);
                }
                if (id == sortBtn.id) {
                    this.drawTexturedModalRect(this.x + 4, this.y + 3, 188 + (getSort() == EnumSortType.AMOUNT ? 6 : getSort() == EnumSortType.MOD ? 12 : 0), 14, 6, 8);
                }
                if (id == jeiBtn.id) {
                    this.drawTexturedModalRect(this.x + 4, this.y + 3, WIDTH + (JeiSettings.isJeiSearchSynced() ? 0 : 6), 22, 6, 8);
                }
                this.mouseDragged(mc, x, y);
                int l = 14737632;
                if (packedFGColour != 0) {
                    l = packedFGColour;
                } else if (!this.enabled) {
                    l = 10526880;
                } else if (this.hovered) {
                    l = 16777120;
                }
                this.drawCenteredString(fontrenderer, this.displayString, this.x + this.width / 2, this.y + (this.height - 8) / 2, l);
            }
        }
    }
}
