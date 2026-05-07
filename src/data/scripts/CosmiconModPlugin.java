package data.scripts;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;

import com.fs.starfarer.api.impl.campaign.intel.bar.events.BarEventManager;
import data.scripts.cosmicon.battle.CharacterRegistry;
import data.scripts.cosmicon.battle.CosmiconSprites;
import data.scripts.cosmicon.events.CosmiconBarEventCreator;
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
    }

    @Override
    public void onGameLoad(boolean newGame) {
        Global.getLogger(this.getClass()).info("Cosmicon Dice: Game Loaded");
        CosmiconStats.initialize();

        BarEventManager barManager = BarEventManager.getInstance();
        if (!barManager.hasEventCreator(CosmiconBarEventCreator.class)) {
            barManager.addEventCreator(new CosmiconBarEventCreator());
            Global.getLogger(this.getClass()).info("Cosmicon bar event creator registered");
        }
    }
}