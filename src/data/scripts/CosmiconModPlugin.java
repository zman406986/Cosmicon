package data.scripts;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;

import data.scripts.cosmicon.battle.CharacterRegistry;
import data.scripts.cosmicon.battle.CosmiconSprites;
import data.scripts.cosmicon.state.CosmiconStats;

@SuppressWarnings("unused")
public class CosmiconModPlugin extends BaseModPlugin {

    @Override
    public void onApplicationLoad() {
        Global.getLogger(this.getClass()).info("Cosmicon Dice Loaded");
        CosmiconConfig.loadSettings();
        Strings.loadStrings();
        CharacterRegistry.loadCards();
        CosmiconSprites.load();
    }

    @Override
    public void onGameLoad(boolean newGame) {
        Global.getLogger(this.getClass()).info("Cosmicon Dice: Game Loaded");
        CosmiconStats.initialize();
    }
}