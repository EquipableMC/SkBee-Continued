package site.equipable.skassist;

import ch.njol.skript.Skript;
import ch.njol.skript.SkriptAddon;
import ch.njol.skript.localization.Noun;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.test.runner.TestMode;
import ch.njol.skript.util.Version;
import site.equipable.skassist.api.listener.EntityListener;
import site.equipable.skassist.api.listener.NBTListener;
import site.equipable.skassist.api.nbt.NBTApi;
import site.equipable.skassist.api.scoreboard.BoardManager;
import site.equipable.skassist.api.structure.StructureManager;
import site.equipable.skassist.api.util.LoggerBee;
import site.equipable.skassist.api.util.SkriptUtils;
import site.equipable.skassist.api.util.Util;
import site.equipable.skassist.config.BoundConfig;
import site.equipable.skassist.config.Config;
import site.equipable.skassist.elements.virtualfurnace.listener.VirtualFurnaceListener;
import site.equipable.skassist.elements.worldcreator.objects.BeeWorldConfig;
import com.shanebeestudios.vf.api.VirtualFurnaceAPI;
import de.tr7zw.changeme.nbtapi.utils.MinecraftVersion;
import org.bukkit.Bukkit;
import org.bukkit.Statistic;
import org.bukkit.boss.BossBar;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Team;

import java.io.IOException;

/**
 * @hidden
 */
@SuppressWarnings("CallToPrintStackTrace")
public class AddonLoader {

    private final SkAssist plugin;
    private final PluginManager pluginManager;
    private final Config config;
    private final Plugin skriptPlugin;
    private SkriptAddon addon;
    private boolean textComponentEnabled;

    public AddonLoader(SkAssist plugin) {
        this.plugin = plugin;
        this.pluginManager = plugin.getServer().getPluginManager();
        this.config = plugin.getPluginConfig();
        MinecraftVersion.replaceLogger(LoggerBee.getLogger());
        this.skriptPlugin = pluginManager.getPlugin("Skript");
    }

    boolean canLoadPlugin() {
        if (skriptPlugin == null) {
            Util.log("&cDependency Skript was not found, plugin disabling.");
            return false;
        }
        if (!skriptPlugin.isEnabled()) {
            Util.log("&cDependency Skript is not enabled, plugin disabling.");
            Util.log("&cThis could mean SkAssist is being forced to load before Skript.");
            return false;
        }
        Version skriptVersion = Skript.getVersion();
        if (skriptVersion.isSmallerThan(new Version(2, 7))) {
            Util.log("&cDependency Skript outdated, plugin disabling.");
            Util.log("&eSkAssist requires Skript 2.7+ but found Skript " + skriptVersion);
            return false;
        }
        if (!Skript.isAcceptRegistrations()) {
            // SkAssist should be loading right after Skript, during Skript's registration period
            // If a plugin is delaying SkAssist's loading, this causes issues with registrations and no longer works
            // We need to find the route of this issue, so far the only plugin I know that does this is PlugMan
            Util.log("&cSkript is no longer accepting registrations, addons can no longer be loaded!");
            Plugin plugMan = Bukkit.getPluginManager().getPlugin("PlugMan");
            if (plugMan != null && plugMan.isEnabled()) {
                Util.log("&cIt appears you're running PlugMan.");
                Util.log("&cIf you're trying to reload/enable SkAssist with PlugMan.... you can't.");
                Util.log("&ePlease restart your server!");
            } else {
                Util.log("&cNo clue how this could happen.");
                Util.log("&cSeems a plugin is delaying SkAssist loading, which is after Skript stops accepting registrations.");
            }
            return false;
        }
        Version version = new Version(SkAssist.EARLIEST_VERSION);
        if (!Skript.isRunningMinecraft(version)) {
            Util.log("&cYour server version &7'&bMC %s&7'&c is not supported, only &7'&bMC %s+&7'&c is supported!", Skript.getMinecraftVersion(), version);
            return false;
        }
        loadSkriptElements();
        return true;
    }

    private void loadSkriptElements() {
        this.addon = Skript.registerAddon(this.plugin);
        this.addon.setLanguageFileDirectory("lang");

        int[] elementCountBefore = SkriptUtils.getElementCount();
        // Load first as it's the base for many things
        loadOtherElements();
        // Load next as both are used in other places
        loadNBTElements();
        loadTextElements();

        // Load in alphabetical order (to make "/skassist info" easier to read)
        loadAdvancementElements();
        loadBossBarElements();
        loadBoundElements();
        loadDamageSourceElements();
        loadDisplayEntityElements();
        loadFishingElements();
        loadGameEventElements();
        loadItemComponentElements();
        loadParticleElements();
        loadRayTraceElements();
        loadRecipeElements();
        loadScoreboardElements();
        loadScoreboardObjectiveElements();
        loadStatisticElements();
        loadStructureElements();
        loadTagElements();
        loadTeamElements();
        loadTickManagerElements();
        loadVillagerElements();
        loadVirtualFurnaceElements();
        loadWorldBorderElements();
        loadWorldCreatorElements();
        loadChunkGenElements();

        int[] elementCountAfter = SkriptUtils.getElementCount();
        int[] finish = new int[elementCountBefore.length];
        int total = 0;
        for (int i = 0; i < elementCountBefore.length; i++) {
            finish[i] = elementCountAfter[i] - elementCountBefore[i];
            total += finish[i];
        }
        String[] elementNames = new String[]{"event", "effect", "expression", "condition", "section"};

        Util.log("Loaded (%s) elements:", total);
        for (int i = 0; i < finish.length; i++) {
            Util.log(" - %s %s%s", finish[i], elementNames[i], finish[i] == 1 ? "" : "s");
        }

        if (this.config.SETTINGS_DEBUG) {
            // Print names of ClassInfos with missing lang entry
            Bukkit.getScheduler().runTaskLater(this.plugin, () -> Classes.getClassInfos().forEach(classInfo -> {
                Noun name = classInfo.getName();
                if (name.toString().contains("types.")) {
                    Util.log("ClassInfo missing lang entry for: &c%s", name);
                }
            }), 1);
        }
    }

    private void loadNBTElements() {
        if (!this.config.ELEMENTS_NBT) {
            Util.logLoading("&9NBT Elements &cdisabled via config");
            return;
        }
        NBTApi.initializeAPI();
        if (!NBTApi.isEnabled()) {
            String ver = Skript.getMinecraftVersion().toString();
            Util.logLoading("&9NBT Elements &cDISABLED!");
            Util.logLoading(" - Your server version [&b" + ver + "&7] is not currently supported by the NBT-API");
            Util.logLoading(" - This is not a bug!");
            Util.logLoading(" - NBT elements will resume once the API is updated to work with [&b" + ver + "&7]");
            return;
        }
        try {
            addon.loadClasses("com.shanebeestudios.skassist.elements.nbt");
            new NBTListener(this.plugin);
            Util.logLoading("&9NBT Elements &asuccessfully loaded");
        } catch (IOException ex) {
            ex.printStackTrace();
            pluginManager.disablePlugin(this.plugin);
        }
    }

    private void loadRecipeElements() {
        if (!this.config.ELEMENTS_RECIPE) {
            Util.logLoading("&9Recipe Elements &cdisabled via config");
            return;
        }
        try {
            addon.loadClasses("com.shanebeestudios.skassist.elements.recipe");
            Util.logLoading("&9Recipe Elements &asuccessfully loaded");
        } catch (IOException ex) {
            ex.printStackTrace();
            pluginManager.disablePlugin(this.plugin);
        }
    }

    private void loadScoreboardElements() {
        if (!this.config.ELEMENTS_BOARD) {
            Util.logLoading("&9Scoreboard Elements &cdisabled via config");
            return;
        }
        try {
            addon.loadClasses("com.shanebeestudios.skassist.elements.scoreboard");
            pluginManager.registerEvents(new BoardManager(), this.plugin);
            Util.logLoading("&9Scoreboard Elements &asuccessfully loaded");
        } catch (IOException ex) {
            ex.printStackTrace();
            pluginManager.disablePlugin(this.plugin);
        }
    }

    private void loadScoreboardObjectiveElements() {
        if (!this.config.ELEMENTS_OBJECTIVE) {
            Util.logLoading("&9Scoreboard Objective Elements &cdisabled via config");
            return;
        }
        if (Classes.getClassInfoNoError("objective") != null || Classes.getExactClassInfo(Objective.class) != null) {
            Util.logLoading("&9Scoreboard Objective Elements &cdisabled");
            Util.logLoading("&7It appears another Skript addon may have registered Scoreboard Objective syntax.");
            Util.logLoading("&7To use SkAssist Scoreboard Objectives, please remove the addon which has registered Scoreboard Objective already.");
            return;
        }
        try {
            addon.loadClasses("com.shanebeestudios.skassist.elements.objective");
            Util.logLoading("&9Scoreboard Objective Elements &asuccessfully loaded");
        } catch (IOException ex) {
            ex.printStackTrace();
            pluginManager.disablePlugin(this.plugin);
        }
    }

    private void loadTeamElements() {
        if (!this.config.ELEMENTS_TEAM) {
            Util.logLoading("&9Team Elements &cdisabled via config");
            return;
        }
        if (Classes.getClassInfoNoError("team") != null || Classes.getExactClassInfo(Team.class) != null) {
            Util.logLoading("&9Team Elements &cdisabled");
            Util.logLoading("&7It appears another Skript addon may have registered Team syntax.");
            Util.logLoading("&7To use SkAssist Teams, please remove the addon which has registered Teams already.");
            return;
        }
        try {
            addon.loadClasses("com.shanebeestudios.skassist.elements.team");
            Util.logLoading("&9Team Elements &asuccessfully loaded");
        } catch (IOException ex) {
            ex.printStackTrace();
            pluginManager.disablePlugin(this.plugin);
        }
    }

    private void loadTickManagerElements() {
        if (!this.config.ELEMENTS_TICK_MANAGER) {
            Util.logLoading("&9Tick Manager elements &cdisabled via config");
            return;
        }
        if (!Skript.classExists("org.bukkit.ServerTickManager")) {
            Util.logLoading("&9Tick Manager elements &cdisabled &7(&eRequires Minecraft 1.20.4+&7)");
            return;
        }
        try {
            addon.loadClasses("com.shanebeestudios.skassist.elements.tickmanager");
            Util.logLoading("&9Tick Manager elements &asuccessfully loaded");
        } catch (IOException ex) {
            ex.printStackTrace();
            pluginManager.disablePlugin(this.plugin);
        }
    }

    private void loadBoundElements() {
        if (!this.config.ELEMENTS_BOUND) {
            Util.logLoading("&9Bound Elements &cdisabled via config");
            return;
        }
        try {
            this.plugin.boundConfig = new BoundConfig(this.plugin);
            addon.loadClasses("com.shanebeestudios.skassist.elements.bound");
            Util.logLoading("&9Bound Elements &asuccessfully loaded");
        } catch (IOException ex) {
            ex.printStackTrace();
            pluginManager.disablePlugin(this.plugin);
        }
    }

    private void loadTextElements() {
        if (!this.config.ELEMENTS_TEXT_COMPONENT) {
            Util.logLoading("&9Text Component Elements &cdisabled via config");
            return;
        }
        if (!Skript.classExists("io.papermc.paper.event.player.AsyncChatEvent")) {
            Util.logLoading("&9Text Component Elements &cdisabled");
            Util.logLoading("&7- Text components require a PaperMC server.");
            return;
        }
        if (Classes.getClassInfoNoError("textcomponent") != null) {
            Util.logLoading("&9Text Component Elements &cdisabled");
            Util.logLoading("&7It appears another Skript addon may have registered Text Component syntax.");
            Util.logLoading("&7To use SkAssist Text Components, please remove the addon which has registered Text Components already.");
            return;
        }
        try {
            addon.loadClasses("com.shanebeestudios.skassist.elements.text");
            Util.logLoading("&9Text Component Elements &asuccessfully loaded");
            this.textComponentEnabled = true;
        } catch (IOException ex) {
            ex.printStackTrace();
            pluginManager.disablePlugin(this.plugin);
        }
    }

    private void loadStructureElements() {
        if (!this.config.ELEMENTS_STRUCTURE) {
            Util.logLoading("&9Structure Elements &cdisabled via config");
            return;
        }

        this.plugin.structureManager = new StructureManager();
        try {
            addon.loadClasses("com.shanebeestudios.skassist.elements.structure");
            Util.logLoading("&9Structure Elements &asuccessfully loaded");
        } catch (IOException ex) {
            ex.printStackTrace();
            pluginManager.disablePlugin(this.plugin);
        }
    }

    private void loadVirtualFurnaceElements() {
        // Force load if running tests as this is defaulted to false in the config
        if (!this.config.ELEMENTS_VIRTUAL_FURNACE && !TestMode.ENABLED) {
            Util.logLoading("&9Virtual Furnace Elements &cdisabled via config");
            return;
        }
        // PaperMC check
        if (!Skript.classExists("net.kyori.adventure.text.Component")) {
            Util.logLoading("&9Virtual Furnace Elements &cdisabled");
            Util.logLoading("&7- Virtual Furnace require a PaperMC server.");
            return;
        }
        try {
            this.plugin.virtualFurnaceAPI = new VirtualFurnaceAPI(this.plugin, true);
            pluginManager.registerEvents(new VirtualFurnaceListener(), this.plugin);
            addon.loadClasses("com.shanebeestudios.skassist.elements.virtualfurnace");
            Util.logLoading("&9Virtual Furnace Elements &asuccessfully loaded");
        } catch (IOException e) {
            e.printStackTrace();
            pluginManager.disablePlugin(this.plugin);
        }
    }

    private void loadOtherElements() {
        try {
            pluginManager.registerEvents(new EntityListener(), this.plugin);
            addon.loadClasses("com.shanebeestudios.skassist.elements.other");
        } catch (IOException ex) {
            ex.printStackTrace();
            pluginManager.disablePlugin(this.plugin);
        }
    }

    private void loadWorldCreatorElements() {
        if (!this.config.ELEMENTS_WORLD_CREATOR) {
            Util.logLoading("&9World Creator Elements &cdisabled via config");
            return;
        }
        try {
            this.plugin.beeWorldConfig = new BeeWorldConfig(this.plugin);
            addon.loadClasses("com.shanebeestudios.skassist.elements.worldcreator");
            Util.logLoading("&9World Creator Elements &asuccessfully loaded");
        } catch (IOException ex) {
            ex.printStackTrace();
            pluginManager.disablePlugin(this.plugin);
        }
    }

    private void loadChunkGenElements() {
        if (!this.config.ELEMENTS_CHUNK_GEN) {
            Util.logLoading("&9Chunk Generator Elements &cdisabled via config");
            return;
        }
        if (!this.config.ELEMENTS_WORLD_CREATOR) {
            Util.logLoading("&9Chunk Generator &cdisabled via World Creator config");
            return;
        }
        try {
            addon.loadClasses("com.shanebeestudios.skassist.elements.generator");
            Util.logLoading("&9Chunk Generator Elements &asuccessfully loaded");
        } catch (IOException ex) {
            ex.printStackTrace();
            pluginManager.disablePlugin(this.plugin);
        }
    }

    private void loadGameEventElements() {
        if (!this.config.ELEMENTS_GAME_EVENT) {
            Util.logLoading("&9Game Event Elements &cdisabled via config");
            return;
        }
        try {
            addon.loadClasses("com.shanebeestudios.skassist.elements.gameevent");
            Util.logLoading("&9Game Event Elements &asuccessfully loaded");
        } catch (IOException e) {
            e.printStackTrace();
            pluginManager.disablePlugin(this.plugin);
        }

    }

    private void loadBossBarElements() {
        if (!this.config.ELEMENTS_BOSS_BAR) {
            Util.logLoading("&9BossBar Elements &cdisabled via config");
            return;
        }
        if (Classes.getClassInfoNoError("bossbar") != null || Classes.getExactClassInfo(BossBar.class) != null) {
            Util.logLoading("&9BossBar Elements &cdisabled");
            Util.logLoading("&7It appears another Skript addon may have registered BossBar syntax.");
            Util.logLoading("&7To use SkAssist BossBars, please remove the addon which has registered BossBars already.");
            return;
        }
        try {
            addon.loadClasses("com.shanebeestudios.skassist.elements.bossbar");
            Util.logLoading("&9BossBar Elements &asuccessfully loaded");
        } catch (IOException ex) {
            ex.printStackTrace();
            pluginManager.disablePlugin(this.plugin);
        }

    }

    private void loadStatisticElements() {
        if (!this.config.ELEMENTS_STATISTIC) {
            Util.logLoading("&9Statistic Elements &cdisabled via config");
            return;
        }
        if (Classes.getClassInfoNoError("statistic") != null || Classes.getExactClassInfo(Statistic.class) != null) {
            Util.logLoading("&9Statistic Elements &cdisabled");
            Util.logLoading("&7It appears another Skript addon may have registered Statistic syntax.");
            Util.logLoading("&7To use SkAssist Statistics, please remove the addon which has registered Statistic already.");
            return;
        }
        try {
            addon.loadClasses("com.shanebeestudios.skassist.elements.statistic");
            Util.logLoading("&9Statistic Elements &asuccessfully loaded");
        } catch (IOException ex) {
            ex.printStackTrace();
            pluginManager.disablePlugin(this.plugin);
        }
    }

    private void loadVillagerElements() {
        if (!this.config.ELEMENTS_VILLAGER) {
            Util.logLoading("&9Villager Elements &cdisabled via config");
            return;
        }
        try {
            addon.loadClasses("com.shanebeestudios.skassist.elements.villager");
            Util.logLoading("&9Villager Elements &asuccessfully loaded");
        } catch (IOException ex) {
            ex.printStackTrace();
            pluginManager.disablePlugin(this.plugin);
        }
    }

    private void loadAdvancementElements() {
        if (!this.config.ELEMENTS_ADVANCEMENT) {
            Util.logLoading("&9Advancement Elements &cdisabled via config");
            return;
        }
        try {
            addon.loadClasses("com.shanebeestudios.skassist.elements.advancement");
            Util.logLoading("&9Advancement Elements &asuccessfully loaded");
        } catch (IOException ex) {
            ex.printStackTrace();
            pluginManager.disablePlugin(this.plugin);
        }
    }

    private void loadWorldBorderElements() {
        if (!this.config.ELEMENTS_WORLD_BORDER) {
            Util.logLoading("&9World Border Elements &cdisabled via config");
            return;
        }
        try {
            addon.loadClasses("com.shanebeestudios.skassist.elements.worldborder");
            Util.logLoading("&9World Border Elements &asuccessfully loaded");
        } catch (IOException ex) {
            ex.printStackTrace();
            pluginManager.disablePlugin(this.plugin);
        }
    }

    private void loadParticleElements() {
        if (!this.config.ELEMENTS_PARTICLE) {
            Util.logLoading("&9Particle Elements &cdisabled via config");
            return;
        }
        try {
            addon.loadClasses("com.shanebeestudios.skassist.elements.particle");
            Util.logLoading("&9Particle Elements &asuccessfully loaded");
        } catch (IOException ex) {
            ex.printStackTrace();
            pluginManager.disablePlugin(this.plugin);
        }
    }

    private void loadTagElements() {
        if (!this.config.ELEMENTS_MINECRAFT_TAG) {
            Util.logLoading("&9Minecraft Tag elements &cdisabled via config");
            return;
        }
        try {
            addon.loadClasses("com.shanebeestudios.skassist.elements.tag");
            Util.logLoading("&9Minecraft Tag elements &asuccessfully loaded");
        } catch (IOException ex) {
            ex.printStackTrace();
            pluginManager.disablePlugin(this.plugin);
        }
    }

    private void loadRayTraceElements() {
        if (!this.config.ELEMENTS_RAYTRACE) {
            Util.logLoading("&9RayTrace elements &cdisabled via config");
            return;
        }
        try {
            addon.loadClasses("com.shanebeestudios.skassist.elements.raytrace");
            Util.logLoading("&9RayTrace elements &asuccessfully loaded");
        } catch (IOException ex) {
            ex.printStackTrace();
            pluginManager.disablePlugin(this.plugin);
        }
    }

    private void loadFishingElements() {
        if (!this.config.ELEMENTS_FISHING) {
            Util.logLoading("&9Fishing elements &cdisabled via config");
            return;
        }
        try {
            addon.loadClasses("com.shanebeestudios.skassist.elements.fishing");
            Util.logLoading("&9Fishing elements &asuccessfully loaded");
        } catch (IOException ex) {
            ex.printStackTrace();
            pluginManager.disablePlugin(this.plugin);
        }
    }

    private void loadDisplayEntityElements() {
        if (!this.config.ELEMENTS_DISPLAY) {
            Util.logLoading("&9Display Entity elements &cdisabled via config");
            return;
        }
        if (!Skript.isRunningMinecraft(1, 19, 4)) {
            Util.logLoading("&9Display Entity elements &cdisabled &7(&eRequires Minecraft 1.19.4+&7)");
            return;
        }
        if (!Skript.classExists("org.bukkit.entity.TextDisplay$TextAlignment")) {
            Util.logLoading("&9Display Entity elements &cdisabled due to a Bukkit API change!");
            Util.logLoading("&7- &eYou need to update your server to fix this issue!");
            return;
        }
        try {
            addon.loadClasses("com.shanebeestudios.skassist.elements.display");
            Util.logLoading("&9Display Entity elements &asuccessfully loaded");
        } catch (IOException ex) {
            ex.printStackTrace();
            pluginManager.disablePlugin(this.plugin);
        }
    }

    private void loadDamageSourceElements() {
        if (!this.config.ELEMENTS_DAMAGE_SOURCE) {
            Util.logLoading("&9Damage Source elements &cdisabled via config");
            return;
        }
        if (!Skript.classExists("org.bukkit.damage.DamageSource")) {
            Util.logLoading("&9Damage Source elements &cdisabled &7(&eRequires Minecraft 1.20.4+&7)");
            return;
        }
        try {
            addon.loadClasses("com.shanebeestudios.skassist.elements.damagesource");
            Util.logLoading("&9Damage Source elements &asuccessfully loaded");
        } catch (IOException ex) {
            ex.printStackTrace();
            pluginManager.disablePlugin(this.plugin);
        }
    }

    private void loadItemComponentElements() {
        if (!this.config.ELEMENTS_ITEM_COMPONENT) {
            Util.logLoading("&9Item Component elements &cdisabled via config");
            return;
        }
        try {
            addon.loadClasses("com.shanebeestudios.skassist.elements.itemcomponent");
            Util.logLoading("&9Item Component Elements &asuccessfully loaded");
        } catch (IOException ex) {
            ex.printStackTrace();
            pluginManager.disablePlugin(this.plugin);
        }
    }

    public boolean isTextComponentEnabled() {
        return this.textComponentEnabled;
    }

}
