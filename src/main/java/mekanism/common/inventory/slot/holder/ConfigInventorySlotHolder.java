package mekanism.common.inventory.slot.holder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import mekanism.api.RelativeSide;
import mekanism.api.inventory.slot.IInventorySlot;
import mekanism.api.transmitters.TransmissionType;
import mekanism.common.tile.component.TileComponentConfig;
import mekanism.common.tile.component.config.ConfigInfo;
import mekanism.common.tile.component.config.slot.ISlotInfo;
import mekanism.common.tile.component.config.slot.InventorySlotInfo;
import net.minecraft.util.Direction;

public class ConfigInventorySlotHolder implements IInventorySlotHolder {

    private final List<IInventorySlot> inventorySlots = new ArrayList<>();
    private final Supplier<TileComponentConfig> configSupplier;
    private final Supplier<Direction> facingSupplier;


    ConfigInventorySlotHolder(Supplier<Direction> facingSupplier, Supplier<TileComponentConfig> configSupplier) {
        this.facingSupplier = facingSupplier;
        this.configSupplier = configSupplier;
    }

    void addSlot(@Nonnull IInventorySlot slot) {
        inventorySlots.add(slot);
    }

    @Nonnull
    @Override
    public List<IInventorySlot> getInventorySlots(@Nullable Direction direction) {
        if (direction == null) {
            //If we want the internal, give all of our slots
            return inventorySlots;
        }
        TileComponentConfig config = configSupplier.get();
        if (config == null) {
            //If we don't have a config (most likely case is it hasn't been setup yet), just return all slots
            return inventorySlots;
        }
        ConfigInfo configInfo = config.getConfig(TransmissionType.ITEM);
        if (configInfo == null) {
            //We don't support items in our configuration at all so just return all
            return inventorySlots;
        }
        RelativeSide side = RelativeSide.fromDirections(facingSupplier.get(), direction);
        ISlotInfo slotInfo = configInfo.getSlotInfo(side);
        //TODO: Re-evaluate bothering to check if it is enabled
        if (slotInfo instanceof InventorySlotInfo && slotInfo.isEnabled()) {
            return ((InventorySlotInfo) slotInfo).getSlots();
        }
        return Collections.emptyList();
    }
}