package mrriegel.storagenetwork.block.cable.link;

import com.google.common.collect.Lists;
import mrriegel.storagenetwork.block.cable.GuiCable;
import mrriegel.storagenetwork.block.cable.GuiCableButton;
import mrriegel.storagenetwork.gui.ItemSlotNetwork;
import mrriegel.storagenetwork.network.CableDataMessage;
import mrriegel.storagenetwork.registry.PacketRegistry;
import mrriegel.storagenetwork.util.inventory.FilterItemStackHandler;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;

import java.io.IOException;

public class GuiCableLink extends GuiCable {

    private GuiCableButton btnInputOutputStorage;
    private final ContainerCableLink containerCableLink;

    public GuiCableLink(ContainerCableLink containerCableLink) {
        super(containerCableLink);
        this.containerCableLink = containerCableLink;
    }

    @Override
    public void importSlotsButtonPressed() {
        super.importSlotsButtonPressed();
        int targetSlot = 0;
        for (ItemStack filterSuggestion : containerCableLink.link.getStoredStacks()) {
            // Ignore stacks that are already filtered
            if (containerCableLink.link.filters.isStackFiltered(filterSuggestion)) {
                continue;
            }
            containerCableLink.link.filters.setStackInSlot(targetSlot, filterSuggestion.copy());
            targetSlot++;
            if (targetSlot >= containerCableLink.link.filters.getSlots()) {
                break;
            }
        }
    }

    @Override
    public FilterItemStackHandler getFilterHandler() {
        return containerCableLink.link.filters;
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        super.drawGuiContainerBackgroundLayer(partialTicks, mouseX, mouseY);
        if (containerCableLink == null || containerCableLink.link == null) {
            return;
        }
        checkOreBtn.setIsChecked(containerCableLink.link.filters.ores);
        checkMetaBtn.setIsChecked(containerCableLink.link.filters.meta);
        checkNbtBtn.setIsChecked(containerCableLink.link.filters.nbt);
        fontRenderer.drawString(String.valueOf(containerCableLink.link.getPriority()),
                guiLeft + 30 - fontRenderer.getStringWidth(String.valueOf(containerCableLink.link.getPriority())) / 2,
                5 + btnMinus.y, 4210752);
        itemSlotsGhost = Lists.newArrayList();
        //TODO: shared with GuiCableIO
        int rows = 2;
        int cols = 9;
        int index = 0;
        int y = 26;
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                ItemStack stack = containerCableLink.link.filters.getStackInSlot(index);
                int x = 8 + col * SLOT_SIZE;
                itemSlotsGhost.add(new ItemSlotNetwork(this, stack, guiLeft + x, guiTop + y, stack.getCount(), guiLeft, guiTop, true));
                index++;
            }
            //move down to second row
            y += SLOT_SIZE;
        }
        for (ItemSlotNetwork s : itemSlotsGhost) {
            s.drawSlot(mouseX, mouseY);
        }
    }

    @Override
    public void initGui() {
        super.initGui();
        btnWhite.setCustomDrawMethod(guiCableButton -> {
            if (this.containerCableLink.link.filters.isWhitelist) {
                this.drawTexturedModalRect(guiCableButton.x + 1, guiCableButton.y + 3, 176, 83, 13, 10);
            } else {
                this.drawTexturedModalRect(guiCableButton.x + 1, guiCableButton.y + 3, 190, 83, 13, 10);
            }
        });
        btnInputOutputStorage = new GuiCableButton(CableDataMessage.CableMessageType.TOGGLE_WAY, guiLeft + 115, guiTop + 5, "");
        btnInputOutputStorage.setCustomDrawMethod(guiCableButton -> this.drawTexturedModalRect(guiCableButton.x + 2, guiCableButton.y + 2, 176 + this.containerCableLink.link.filterDirection.ordinal() * 12, 114, 12, 12));
        this.addButton(btnInputOutputStorage);
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        super.actionPerformed(button);
        if (containerCableLink == null || containerCableLink.link == null) {
            return;
        }
        if (button.id == btnMinus.id) {
            containerCableLink.link.priority--;
            PacketRegistry.INSTANCE.sendToServer(new CableDataMessage(button.id));
        } else if (button.id == btnPlus.id) {
            containerCableLink.link.priority++;
            PacketRegistry.INSTANCE.sendToServer(new CableDataMessage(button.id));
        } else if (button.id == btnInputOutputStorage.id) {
            containerCableLink.link.filterDirection = containerCableLink.link.filterDirection.next();
            PacketRegistry.INSTANCE.sendToServer(new CableDataMessage(button.id));
        }
    }

    @Override
    protected void drawTooltips(int mouseX, int mouseY) {
        super.drawTooltips(mouseX, mouseY);
        if (containerCableLink == null || containerCableLink.link == null) {
            return;
        }
        if (btnInputOutputStorage != null && btnInputOutputStorage.isMouseOver()) {
            drawHoveringText(Lists.newArrayList(I18n.format("gui.storagenetwork.fil.tooltip_" + containerCableLink.link.filterDirection.toString())), mouseX, mouseY);
        }
    }
}
