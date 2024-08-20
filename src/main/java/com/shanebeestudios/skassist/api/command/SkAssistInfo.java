package com.shanebeestudios.skassist.api.command;

import ch.njol.skript.Skript;
import com.shanebeestudios.skassist.SkAssist;
import com.shanebeestudios.skassist.api.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static com.shanebeestudios.skassist.api.util.Util.sendColMsg;

public class SkAssistInfo implements TabExecutor {

    private final PluginDescriptionFile desc;

    @SuppressWarnings("deprecation")
    public SkAssistInfo(SkAssist plugin) {
        this.desc = plugin.getDescription();
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("info")) {
            sendColMsg(sender, "&7--- [&bSkAssist Loading Info&7] ---");
            Util.getDebugs().forEach(debug -> sendColMsg(sender, "- &7" + debug));
            sendColMsg(sender, "&7--- [&bServer Info&7] ---");
            sendColMsg(sender, "&7Server Version: &b" + Bukkit.getVersion());
            sendColMsg(sender, "&7Skript Version: &b" + Skript.getVersion());
            sendColMsg(sender, "&7Skript Addons:");
            Skript.getAddons().forEach(addon -> {
                String name = addon.getName();
                if (!name.contains("SkAssist")) {
                    sendColMsg(sender, "&7- &b" + name + " v" + addon.plugin.getDescription().getVersion());
                }
            });
            sendColMsg(sender, "&7SkAssist Version: &b" + desc.getVersion());
            sendColMsg(sender, "&7SkAssist Website: &b" + desc.getWebsite());
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            return StringUtil.copyPartialMatches(args[0], List.of("info"), new ArrayList<>());
        }
        return null;
    }

}
