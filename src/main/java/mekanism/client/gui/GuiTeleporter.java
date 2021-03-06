package mekanism.client.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import mekanism.api.TileNetworkList;
import mekanism.api.text.EnumColor;
import mekanism.client.gui.button.MekanismButton;
import mekanism.client.gui.button.MekanismImageButton;
import mekanism.client.gui.button.TranslationButton;
import mekanism.client.gui.element.GuiRedstoneControl;
import mekanism.client.gui.element.GuiScrollList;
import mekanism.client.gui.element.GuiSlot;
import mekanism.client.gui.element.GuiSlot.SlotOverlay;
import mekanism.client.gui.element.GuiSlot.SlotType;
import mekanism.client.gui.element.bar.GuiBar.IBarInfoHandler;
import mekanism.client.gui.element.bar.GuiVerticalPowerBar;
import mekanism.client.gui.element.tab.GuiSecurityTab;
import mekanism.client.gui.element.tab.GuiUpgradeTab;
import mekanism.client.render.MekanismRenderer;
import mekanism.common.Mekanism;
import mekanism.common.MekanismLang;
import mekanism.common.frequency.Frequency;
import mekanism.common.frequency.FrequencyManager;
import mekanism.common.inventory.container.tile.TeleporterContainer;
import mekanism.common.network.PacketTileEntity;
import mekanism.common.tile.TileEntityTeleporter;
import mekanism.common.util.MekanismUtils;
import mekanism.common.util.MekanismUtils.ResourceType;
import mekanism.common.util.text.EnergyDisplay;
import mekanism.common.util.text.OwnerDisplay;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import org.lwjgl.glfw.GLFW;

public class GuiTeleporter extends GuiMekanismTile<TileEntityTeleporter, TeleporterContainer> {

    private MekanismButton publicButton;
    private MekanismButton privateButton;
    private MekanismButton setButton;
    private MekanismButton deleteButton;
    private GuiScrollList scrollList;
    private TextFieldWidget frequencyField;
    private boolean privateMode;
    private Frequency clientFreq;
    private byte clientStatus;
    private List<Frequency> clientPublicCache = new ArrayList<>();
    private List<Frequency> clientPrivateCache = new ArrayList<>();

    public GuiTeleporter(TeleporterContainer container, PlayerInventory inv, ITextComponent title) {
        super(container, inv, title);
        if (tile.frequency != null) {
            privateMode = !tile.frequency.publicFreq;
        }
        ySize += 64;
    }

    @Override
    public void init() {
        super.init();
        ResourceLocation resource = getGuiLocation();
        addButton(new GuiRedstoneControl(this, tile, resource));
        addButton(new GuiUpgradeTab(this, tile, resource));
        addButton(new GuiSecurityTab<>(this, tile, resource));
        addButton(new GuiVerticalPowerBar(this, new IBarInfoHandler() {
            @Override
            public ITextComponent getTooltip() {
                return EnergyDisplay.of(getEnergy(), getMaxEnergy()).getTextComponent();
            }

            @Override
            public double getLevel() {
                return getEnergy() / getMaxEnergy();
            }
        }, resource, 158, 26));
        addButton(new GuiSlot(SlotType.NORMAL, this, resource, 152, 6).with(SlotOverlay.POWER));
        addButton(scrollList = new GuiScrollList(this, resource, 28, 37, 120, 40));

        addButton(publicButton = new TranslationButton(this, getGuiLeft() + 27, getGuiTop() + 14, 60, 20, MekanismLang.PUBLIC, () -> {
            privateMode = false;
            updateButtons();
        }));
        addButton(privateButton = new TranslationButton(this, getGuiLeft() + 89, getGuiTop() + 14, 60, 20, MekanismLang.PRIVATE, () -> {
            privateMode = true;
            updateButtons();
        }));
        addButton(setButton = new TranslationButton(this, getGuiLeft() + 27, getGuiTop() + 116, 60, 20, MekanismLang.BUTTON_SET, () -> {
            int selection = scrollList.getSelection();
            if (selection != -1) {
                Frequency freq = privateMode ? getPrivateCache().get(selection) : getPublicCache().get(selection);
                setFrequency(freq.name);
            }
            updateButtons();
        }));
        addButton(deleteButton = new TranslationButton(this, getGuiLeft() + 89, getGuiTop() + 116, 60, 20, MekanismLang.BUTTON_DELETE, () -> {
            int selection = scrollList.getSelection();
            if (selection != -1) {
                Frequency freq = privateMode ? getPrivateCache().get(selection) : getPublicCache().get(selection);
                TileNetworkList data = TileNetworkList.withContents(1, freq.name, freq.publicFreq);
                Mekanism.packetHandler.sendToServer(new PacketTileEntity(tile, data));
                scrollList.clearSelection();
            }
            updateButtons();
        }));
        addButton(frequencyField = new TextFieldWidget(font, getGuiLeft() + 50, getGuiTop() + 104, 86, 11, ""));
        frequencyField.setMaxStringLength(FrequencyManager.MAX_FREQ_LENGTH);
        frequencyField.setEnableBackgroundDrawing(false);
        addButton(new MekanismImageButton(this, getGuiLeft() + 137, getGuiTop() + 103, 11, 12, getButtonLocation("checkmark"), () -> {
            setFrequency(frequencyField.getText());
            frequencyField.setText("");
            updateButtons();
        }));
        updateButtons();
    }

    public void setFrequency(Frequency newFrequency) {
        clientFreq = newFrequency;
    }

    public void setPublicCache(List<Frequency> cache) {
        clientPublicCache = cache;
    }

    public void setPrivateCache(List<Frequency> cache) {
        clientPrivateCache = cache;
    }

    public void setStatus(byte status) {
        clientStatus = status;
    }

    public ITextComponent getSecurity(Frequency freq) {
        if (!freq.publicFreq) {
            return MekanismLang.PRIVATE.translateColored(EnumColor.DARK_RED);
        }
        return MekanismLang.PUBLIC.translate();
    }

    public void updateButtons() {
        if (getOwner() == null) {
            return;
        }
        List<String> text = new ArrayList<>();
        if (privateMode) {
            for (Frequency freq : getPrivateCache()) {
                text.add(freq.name);
            }
        } else {
            for (Frequency freq : getPublicCache()) {
                text.add(freq.name + " (" + freq.clientOwner + ")");
            }
        }
        scrollList.setText(text);
        if (privateMode) {
            publicButton.active = true;
            privateButton.active = false;
        } else {
            publicButton.active = false;
            privateButton.active = true;
        }
        if (scrollList.hasSelection()) {
            Frequency freq = privateMode ? getPrivateCache().get(scrollList.getSelection()) : getPublicCache().get(scrollList.getSelection());
            setButton.active = getFrequency() == null || !getFrequency().equals(freq);
            deleteButton.active = getOwner().equals(freq.ownerUUID);
        } else {
            setButton.active = false;
            deleteButton.active = false;
        }
    }

    @Override
    public void tick() {
        super.tick();
        //TODO: Why do we call updateButtons every tick?
        updateButtons();
        frequencyField.tick();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        //TODO: Move this upwards to GuiMekanism and if nothing happened from the click don't bother even calling updateButtons
        updateButtons();
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected ResourceLocation getGuiLocation() {
        return MekanismUtils.getResource(ResourceType.GUI, "teleporter.png");
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (frequencyField.isFocused()) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                //Manually handle hitting escape making the field lose focus
                frequencyField.setFocused2(false);
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ENTER) {
                setFrequency(frequencyField.getText());
                frequencyField.setText("");
                return true;
            }
            return frequencyField.keyPressed(keyCode, scanCode, modifiers);
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char c, int keyCode) {
        if (frequencyField.isFocused()) {
            if (Character.isDigit(c) || Character.isLetter(c) || FrequencyManager.SPECIAL_CHARS.contains(c)) {
                //Only allow a subset of characters to be entered into the frequency text box
                return frequencyField.charTyped(c, keyCode);
            }
            return false;
        }
        return super.charTyped(c, keyCode);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        drawString(getName(), (getXSize() / 2) - (getStringWidth(getName()) / 2), 4, 0x404040);
        drawString(OwnerDisplay.of(getOwner(), tile.getSecurity().getClientOwner()).getTextComponent(), 8, getYSize() - 92, 0x404040);
        ITextComponent frequencyComponent = MekanismLang.FREQUENCY.translate();
        drawString(frequencyComponent, 32, 81, 0x404040);
        ITextComponent securityComponent = MekanismLang.SECURITY.translate("");
        drawString(securityComponent, 32, 91, 0x404040);
        Frequency frequency = getFrequency();
        int frequencyOffset = getStringWidth(frequencyComponent) + 1;
        if (frequency != null) {
            drawString(frequency.name, 32 + frequencyOffset, 81, 0x797979);
            drawString(getSecurity(frequency), 32 + getStringWidth(securityComponent), 91, 0x797979);
        } else {
            drawString(MekanismLang.NONE.translateColored(EnumColor.DARK_RED), 32 + frequencyOffset, 81, 0x797979);
            drawString(MekanismLang.NONE.translateColored(EnumColor.DARK_RED), 32 + getStringWidth(securityComponent), 91, 0x797979);
        }
        renderScaledText(MekanismLang.SET.translate(), 27, 104, 0x404040, 20);
        //TODO: 1.14 Convert to GuiElement
        int xAxis = mouseX - getGuiLeft();
        int yAxis = mouseY - getGuiTop();
        if (xAxis >= 6 && xAxis <= 24 && yAxis >= 6 && yAxis <= 24) {
            if (frequency == null) {
                displayTooltip(MekanismLang.NO_FREQUENCY.translateColored(EnumColor.DARK_RED), xAxis, yAxis);
            } else {
                displayTooltip(getStatusDisplay(), xAxis, yAxis);
            }
        }
        super.drawGuiContainerForegroundLayer(mouseX, mouseY);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(int xAxis, int yAxis) {
        super.drawGuiContainerBackgroundLayer(xAxis, yAxis);
        int y = getFrequency() == null ? 94 : getStatus() == 2 ? 22 : getStatus() == 3 ? 40 : getStatus() == 4 ? 58 : 76;
        drawTexturedRect(getGuiLeft() + 6, getGuiTop() + 6, 176, y, 18, 18);
        MekanismRenderer.resetColor();
    }

    public ITextComponent getStatusDisplay() {
        switch (getStatus()) {
            case 1:
                return MekanismLang.TELEPORTER_READY.translateColored(EnumColor.DARK_GREEN);
            case 2:
                return MekanismLang.TELEPORTER_NO_FRAME.translateColored(EnumColor.DARK_RED);
            case 4:
                return MekanismLang.TELEPORTER_NEEDS_ENERGY.translateColored(EnumColor.DARK_RED);
            case 3:
            default:
                return MekanismLang.TELEPORTER_NO_LINK.translateColored(EnumColor.DARK_RED);
        }
    }

    private UUID getOwner() {
        return tile.getSecurity().getOwnerUUID();
    }

    private byte getStatus() {
        return tile != null ? tile.status : clientStatus;
    }

    private List<Frequency> getPublicCache() {
        return tile != null ? tile.publicCache : clientPublicCache;
    }

    private List<Frequency> getPrivateCache() {
        return tile != null ? tile.privateCache : clientPrivateCache;
    }

    private Frequency getFrequency() {
        return tile != null ? tile.frequency : clientFreq;
    }

    public void setFrequency(String freq) {
        if (freq.isEmpty()) {
            return;
        }
        TileNetworkList data = TileNetworkList.withContents(0, freq, !privateMode);
        Mekanism.packetHandler.sendToServer(new PacketTileEntity(tile, data));
    }

    private ITextComponent getName() {
        return tile.getName();
    }

    private double getEnergy() {
        return tile.getEnergy();
    }

    private double getMaxEnergy() {
        return tile.getMaxEnergy();
    }
}