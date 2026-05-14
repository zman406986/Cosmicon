package data.scripts;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;

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

        Global.getSector().removeScriptsOfClass(CosmiconNPCManager.class);
        Global.getSector().removeTransientScriptsOfClass(CosmiconNPCManager.class);

        CosmiconNPCManager.cleanupAllNPCs();

        Global.getSector().getListenerManager().addListener(
            new CosmiconCampaignListener(), true);

        Global.getLogger(this.getClass()).info("Cosmicon NPC listener registered");
    }
}
