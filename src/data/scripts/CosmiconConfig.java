package data.scripts;

import com.fs.starfarer.api.Global;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class CosmiconConfig {
    private CosmiconConfig() {}

    public static final String MOD_ID = "cosmicon_dice";
    private static final String CONFIG_PATH = "data/config/settings.json";

    public static int MARKET_SIZE_MIN = 1;
    public static int DEFAULT_HP = 20;
    public static int DEFAULT_REROLLS = 2;
    public static boolean COSMICON_ENABLED = true;
    public static boolean DEBUG_ENABLED = true;
    public static boolean REROLL_LOG_ENABLED = false;

    public static boolean NPC_ENABLED = true;
    public static float NPC_SPAWN_CHANCE = 0.5f;
    public static float NPC_SPAWN_INTERVAL_DAYS = 7f;
    public static float NPC_MIN_LIFETIME_DAYS = 30f;
    public static float NPC_MAX_LIFETIME_DAYS = 60f;

    public static final float AI_REVEAL_PER_DICE_DELAY = 0.15f;
    public static final float AI_REROLL_PREVIEW_DELAY = 0.5f;
    public static final float AI_CONFIRM_DELAY = 0.3f;
    public static final float DICE_PREVIEW_DELAY = 0.8f;

    public static void loadSettings() {
        try {
            JSONObject settings = Global.getSettings().loadJSON(CONFIG_PATH, MOD_ID);

            COSMICON_ENABLED = settings.optBoolean("cosmiconDiceEnabled", true);
            MARKET_SIZE_MIN = settings.optInt("marketSizeMin", 1);
            DEFAULT_HP = settings.optInt("defaultHP", 20);
            DEFAULT_REROLLS = settings.optInt("defaultRerolls", 2);
            DEBUG_ENABLED = settings.optBoolean("debugEnabled", true);
            REROLL_LOG_ENABLED = settings.optBoolean("rerollLogEnabled", false);

            NPC_ENABLED = settings.optBoolean("npcEnabled", true);
            NPC_SPAWN_CHANCE = (float) settings.optDouble("npcSpawnChance", 0.5);
            NPC_SPAWN_INTERVAL_DAYS = (float) settings.optDouble("npcSpawnIntervalDays", 7.0);
            NPC_MIN_LIFETIME_DAYS = (float) settings.optDouble("npcMinLifetimeDays", 30.0);
            NPC_MAX_LIFETIME_DAYS = (float) settings.optDouble("npcMaxLifetimeDays", 60.0);

            Global.getLogger(CosmiconConfig.class).info("Cosmicon config loaded: marketSizeMin=" + MARKET_SIZE_MIN);
        } catch (IOException | JSONException e) {
            Global.getLogger(CosmiconConfig.class).error("Error loading Cosmicon config", e);
        }
    }
}