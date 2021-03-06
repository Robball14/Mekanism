package mekanism.client.gui;

import mekanism.client.gui.element.GuiProgress.ProgressBar;
import mekanism.common.inventory.container.tile.CrusherContainer;
import mekanism.common.tile.TileEntityCrusher;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.text.ITextComponent;

public class GuiCrusher extends GuiElectricMachine<TileEntityCrusher, CrusherContainer> {

    public GuiCrusher(CrusherContainer container, PlayerInventory inv, ITextComponent title) {
        super(container, inv, title);
    }

    @Override
    public ProgressBar getProgressType() {
        return ProgressBar.CRUSH;
    }
}