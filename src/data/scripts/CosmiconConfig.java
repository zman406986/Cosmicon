package data.scripts;

import com.fs.starfarer.api.Global;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class CosmiconConfig {
    public static final String MOD_ID = "cosmicon_dice";
    private static final String CONFIG_PATH = "data/config/settings.json";

    public static int MARKET_SIZE_MIN = 3;
    public static int DEFAULT_HP = 20;
    public static int DEFAULT_REROLLS = 2;
    public static boolean COSMICON_ENABLED = true;

    public static final float AI_REVEAL_PER_DICE_DELAY = 0.15f;
    public static final float AI_REROLL_PREVIEW_DELAY = 0.5f;
    public static final float AI_CONFIRM_DELAY = 0.3f;
    public static final float DICE_PREVIEW_DELAY = 0.8f;

    public static void loadSettings() {
        try {
            JSONObject settings = Global.getSettings().loadJSON(CONFIG_PATH, MOD_ID);

            COSMICON_ENABLED = settings.optBoolean("cosmiconDiceEnabled", true);
            MARKET_SIZE_MIN = settings.optInt("marketSizeMin", 3);
            DEFAULT_HP = settings.optInt("defaultHP", 20);
            DEFAULT_REROLLS = settings.optInt("defaultRerolls", 2);

            Global.getLogger(CosmiconConfig.class).info("Cosmicon config loaded: marketSizeMin=" + MARKET_SIZE_MIN);
        } catch (IOException | JSONException e) {
            Global.getLogger(CosmiconConfig.class).error("Error loading Cosmicon config", e);
        }
    }
}