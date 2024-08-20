package com.shanebeestudios.skassist.api.nbt;

import com.shanebeestudios.skassist.SkAssist;
import de.tr7zw.changeme.nbtapi.NBTCompound;
import org.bukkit.NamespacedKey;

public interface NBTCustom {

    NamespacedKey OLD_KEY = new NamespacedKey(SkAssist.getPlugin(), "custom-nbt");
    String KEY = "skassist-custom";

    void deleteCustomNBT();

    NBTCompound getCopy();

}
