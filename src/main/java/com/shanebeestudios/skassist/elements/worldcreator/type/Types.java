package com.shanebeestudios.skassist.elements.worldcreator.type;

import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.registrations.Classes;
import com.shanebeestudios.skassist.api.util.SkriptUtils;
import com.shanebeestudios.skassist.api.util.Util;
import com.shanebeestudios.skassist.api.wrapper.EnumWrapper;
import com.shanebeestudios.skassist.elements.worldcreator.objects.BeeWorldCreator;
import org.bukkit.WorldType;

public class Types {

    static {
        Classes.registerClass(new ClassInfo<>(BeeWorldCreator.class, "worldcreator")
                .user("world ?creators?")
                .name("World Creator")
                .description("Used to create new worlds.")
                .examples("set {_creator} to new world creator named \"my-world\"")
                .since("1.8.0")
                .parser(SkriptUtils.getDefaultParser()));

        if (Classes.getExactClassInfo(WorldType.class) == null) {
            EnumWrapper<WorldType> WORLD_TYPE_ENUM = new EnumWrapper<>(WorldType.class);
            Classes.registerClass(WORLD_TYPE_ENUM.getClassInfo("worldtype")
                    .user("world ?types?")
                    .name("World Type")
                    .description("The type of a world")
                    .examples("set world type of {_creator} to flat")
                    .since("1.8.0"));
        } else {
            Util.log("It looks like another addon registered 'world type' already. ");
            Util.log("You may have to use their world type options in SkAssist's 'world creator' system.");
        }
    }

}
