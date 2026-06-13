package data.scripts;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.util.Misc;

import data.scripts.Strings;
import data.scripts.cosmicon.battle.CharacterRegistry;
import data.scripts.cosmicon.battle.CosmiconSprites;
import data.scripts.cosmicon.npc.CosmiconCampaignListener;
import data.scripts.cosmicon.npc.CosmiconNPCManager;
import data.scripts.cosmicon.state.CosmiconStats;

public class CosmiconModPlugin extends BaseModPlugin {

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
                    if (CosmiconStats.isEasyModeComplete() || !CosmiconStats.hasThreeStarCharacters()) {
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

        Global.getSector().getListenerManager().addListener(
            new CosmiconCampaignListener(), true);

        Global.getLogger(this.getClass()).info("Cosmicon NPC listener registered");
    }
}
