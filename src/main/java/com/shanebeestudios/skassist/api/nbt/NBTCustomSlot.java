package com.shanebeestudios.skassist.api.nbt;

import ch.njol.skript.util.slot.Slot;
import de.tr7zw.changeme.nbtapi.NBTItem;

import java.util.Objects;

/**
 * Wrapper class for {@link NBTItem} using a Skript {@link Slot}
 */
public class NBTCustomSlot extends NBTCustomItemStack {

    private final Slot slot;

    public NBTCustomSlot(Slot slot, boolean isCustomData, boolean isVanilla, boolean isFull) {
        super(Objects.requireNonNull(slot.getItem()), isCustomData, isVanilla, isFull);
        this.slot = slot;
    }

    @Override
    protected void saveCompound() {
        super.saveCompound();
        this.slot.setItem(getItem());
    }

}