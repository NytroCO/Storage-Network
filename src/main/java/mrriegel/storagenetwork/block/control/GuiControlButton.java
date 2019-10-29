package mrriegel.storagenetwork.block.control;

import mrriegel.storagenetwork.StorageNetwork;
import mrriegel.storagenetwork.network.CableDataMessage.CableMessageType;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.client.config.GuiButtonExt;

import java.util.ArrayList;
import java.util.List;

class GuiControlButton extends GuiButtonExt {

    private static final ResourceLocation widgets = new ResourceLocation(StorageNetwork.MODID, "textures/gui/enderio-publicdomain-widgetsv2.png");
    public ProcessWrapper cable;
    public int textureX = -1, textureY;
    final CableMessageType messageType;
    private List<String> tooltips = new ArrayList<>();

    public GuiControlButton(int id, CableMessageType type, int x, int y, int w, int h, String z) {
        super(id, x, y, w, h, z);
        messageType = type;
    }

    public void clearAndSetTooltip(String s) {
        tooltips.clear();
        tooltips.add(s);
    }

    public List<String> getTooltips() {
        return tooltips;
    }

    public void setTooltips(List<String> tooltips) {
        this.tooltips = tooltips;
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY, float partial) {//drawButon
        super.drawButton(mc, mouseX, mouseY, partial);
        mc.getTextureManager().bindTexture(widgets);
        //green check
        if (this.visible && textureX >= 0) {
            this.drawTexturedModalRect(x, y, textureX, textureY, 16, 16);
        }
    }
}
