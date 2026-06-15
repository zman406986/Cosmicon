package data.scripts;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Map;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.impl.campaign.intel.bar.PortsideBarData;
import com.fs.starfarer.api.impl.campaign.intel.bar.PortsideBarEvent;
import com.fs.starfarer.api.impl.campaign.intel.bar.events.BarEventManager;
import com.fs.starfarer.api.util.Misc;
import com.thoughtworks.xstream.XStream;

import data.scripts.cosmicon.battle.CharacterRegistry;
import data.scripts.cosmicon.battle.CosmiconSprites;
import data.scripts.cosmicon.events.CosmiconBarEvent;
import data.scripts.cosmicon.events.CosmiconBarEventCreator;
import data.scripts.cosmicon.npc.CosmiconCampaignListener;
import data.scripts.cosmicon.npc.CosmiconNPCManager;
import data.scripts.cosmicon.state.CosmiconStats;

public class CosmiconModPlugin extends BaseModPlugin {

    @Override
    public void configureXStream(XStream x) {
        x.alias("CosmiconCampaignListener", CosmiconCampaignListener.class);
        x.alias("CosmiconBarEvent", CosmiconBarEvent.class);
        x.alias("CosmiconBarEventCreator", CosmiconBarEventCreator.class);
    }

    @Override
    public void onApplicationLoad() {
        try {
            CosmiconConfig.loadSettings();
            Strings.loadStrings();
            CharacterRegistry.loadCards();
            CosmiconSprites.load();
            Global.getLogger(this.getClass()).info("Cosmicon Dice Loaded");
        } catch (Exception e) {
            Global.getLogger(this.getClass()).error("Failed to load Cosmicon Dice", e);
            throw e;
        }

        try {
            Global.getSettings().getScriptClassLoader()
                .loadClass("data.scripts.casino.interaction.CasinoLoungeRegistry");
            data.scripts.casino.interaction.CasinoLoungeRegistry.registerProvider(
                new data.scripts.cosmicon.casino.CosmiconLoungeProvider()
            );
            Global.getLogger(this.getClass()).info("Cosmicon: Registered Casino lounge provider");
        } catch (Exception e) {
            Global.getLogger(this.getClass()).info("Cosmicon: Casino not detected, running standalone");
        }
    }

    @Override
    public void onGameLoad(boolean newGame) {
        Global.getLogger(this.getClass()).info("Cosmicon Dice: Game Loaded");
        CosmiconStats.initialize();

        if (CosmiconStats.shouldShowEasyModeUpdateMessage()) {
            CosmiconStats.setEasyModeUpdateMessageShown();
            Global.getSector().addTransientScript(new EveryFrameScript() {
                float timer = 0f;
                boolean shown = false;

                @Override
                public void advance(float amount) {
                    if (shown) return;
                    timer += amount;
                    if (timer < 1.0f) return;
                    if (CosmiconStats.isEasyModeComplete() || !CosmiconStats.hasAdvancedCharacters()) {
                        shown = true;
                        return;
                    }
                    shown = true;
                    Global.getSector().getCampaignUI().addMessage(
                        Strings.get("update.easy_mode_enforced_title"), Misc.getPositiveHighlightColor());
                    Global.getSector().getCampaignUI().addMessage(
                        Strings.get("update.easy_mode_enforced_1"), Misc.getGrayColor());
                    Global.getSector().getCampaignUI().addMessage(
                        Strings.get("update.easy_mode_enforced_2"), Misc.getGrayColor());
                    Global.getSector().getCampaignUI().addMessage(
                        Strings.get("update.easy_mode_enforced_3"), Misc.getGrayColor());
                }

                @Override
                public boolean isDone() { return shown; }
                @Override
                public boolean runWhilePaused() { return true; }
            });
        }

        CosmiconMusicPlugin.resetStaleState();

        Global.getSector().removeScriptsOfClass(CosmiconNPCManager.class);
        Global.getSector().removeTransientScriptsOfClass(CosmiconNPCManager.class);

        CosmiconNPCManager.cleanupAllNPCs();
        CosmiconNPCManager.cleanupStaleMemoryKeys();

        cleanupBarEventReferences();

        Global.getSector().getListenerManager().addListener(
            new CosmiconCampaignListener(), true);

        Global.getLogger(this.getClass()).info("Cosmicon NPC listener registered");
    }

    private void cleanupBarEventReferences() {
        try {
            BarEventManager bar = BarEventManager.getInstance();
            if (bar == null) return;

            // Remove our creator from the creators list
            bar.getCreators().removeIf(c -> c instanceof CosmiconBarEventCreator);

            // Remove our creator from the timeout tracker
            for (BarEventManager.GenericBarEventCreator c : new ArrayList<>(bar.getTimeout().getItems())) {
                if (c instanceof CosmiconBarEventCreator) {
                    bar.getTimeout().remove(c);
                }
            }

            // Remove our events from the active tracker
            for (PortsideBarEvent e : new ArrayList<>(bar.getActive().getItems())) {
                if (e instanceof CosmiconBarEvent) {
                    bar.getActive().remove(e);
                }
            }

            // Remove barEventCreators map entries referencing our creator/events (requires reflection)
            try {
                Field f = BarEventManager.class.getDeclaredField("barEventCreators");
                f.setAccessible(true);
                Map<?, ?> map = (Map<?, ?>) f.get(bar);
                map.entrySet().removeIf(entry -> {
                    Object value = entry.getValue();
                    Object key = entry.getKey();
                    return value instanceof CosmiconBarEventCreator || key instanceof CosmiconBarEvent;
                });
            } catch (Exception e) {
                Global.getLogger(this.getClass()).warn("Could not clean barEventCreators map", e);
            }

            // Remove our events from PortsideBarData
            PortsideBarData portData = PortsideBarData.getInstance();
            if (portData != null) {
                for (PortsideBarEvent e : new ArrayList<>(portData.getEvents())) {
                    if (e instanceof CosmiconBarEvent) {
                        portData.removeEvent(e);
                    }
                }
            }

            Global.getLogger(this.getClass()).info("Cosmicon: Cleaned up bar event references");
        } catch (Exception e) {
            Global.getLogger(this.getClass()).warn("Cosmicon: Error cleaning up bar events", e);
        }
    }
}
