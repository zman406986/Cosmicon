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

    public static int NORMAL_ENCOUNTER_CREDIT_PER_LEVEL = 1500;
    public static int BASE_CREDIT_REWARD_PER_LEVEL = 3000;
    public static float CREDIT_RANDOM_FACTOR_MIN = 0.9f;
    public static float CREDIT_RANDOM_FACTOR_MAX = 1.1f;

    public static int GATEKEEPER_COST = 5000;
    public static int TOURNAMENT_COST = 15000;

    public static int GATEKEEPER_BONUS_HP = 74;
    public static int GATEKEEPER_999_BONUS_HP = 974;
    public static int BOSS_BONUS_HP = 15;

    public static int BOSS_CREDIT_MULTIPLIER = 3;
    public static int GATEKEEPER_WIN_CREDIT_MULTIPLIER = 4;
    public static int GATEKEEPER_LOSS_CREDIT_MULTIPLIER = 2;
    public static int TOURNAMENT_CREDIT_PER_LEVEL_MULTIPLIER = 5;

    public static int TOURNAMENT_CHAMPION_REWARDS = 3;
    public static int TOURNAMENT_RUNNER_UP_REWARDS = 2;
    public static int TOURNAMENT_ELIMINATED_REWARDS = 1;

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

            JSONObject creditsSettings = settings.optJSONObject("credits");
            if (creditsSettings != null) {
                NORMAL_ENCOUNTER_CREDIT_PER_LEVEL = creditsSettings.optInt("normalEncounterPerLevel", 1500);
                BASE_CREDIT_REWARD_PER_LEVEL = creditsSettings.optInt("baseRewardPerLevel", 3000);
                CREDIT_RANDOM_FACTOR_MIN = (float) creditsSettings.optDouble("randomFactorMin", 0.9);
                CREDIT_RANDOM_FACTOR_MAX = (float) creditsSettings.optDouble("randomFactorMax", 1.1);
            }

            JSONObject casinoSettings = settings.optJSONObject("casino");
            if (casinoSettings != null) {
                GATEKEEPER_COST = casinoSettings.optInt("gatekeeperCost", 5000);
                TOURNAMENT_COST = casinoSettings.optInt("tournamentCost", 15000);
                GATEKEEPER_BONUS_HP = casinoSettings.optInt("gatekeeperBonusHp", 74);
                GATEKEEPER_999_BONUS_HP = casinoSettings.optInt("gatekeeper999BonusHp", 974);
                BOSS_BONUS_HP = casinoSettings.optInt("bossBonusHp", 15);
                BOSS_CREDIT_MULTIPLIER = casinoSettings.optInt("bossCreditMultiplier", 3);
                GATEKEEPER_WIN_CREDIT_MULTIPLIER = casinoSettings.optInt("gatekeeperWinCreditMultiplier", 4);
                GATEKEEPER_LOSS_CREDIT_MULTIPLIER = casinoSettings.optInt("gatekeeperLossCreditMultiplier", 2);
                TOURNAMENT_CREDIT_PER_LEVEL_MULTIPLIER = casinoSettings.optInt("tournamentCreditPerLevelMultiplier", 5);
                TOURNAMENT_CHAMPION_REWARDS = casinoSettings.optInt("tournamentChampionRewards", 3);
                TOURNAMENT_RUNNER_UP_REWARDS = casinoSettings.optInt("tournamentRunnerUpRewards", 2);
                TOURNAMENT_ELIMINATED_REWARDS = casinoSettings.optInt("tournamentEliminatedRewards", 1);
            }

            Global.getLogger(CosmiconConfig.class).info("Cosmicon config loaded: marketSizeMin=" + MARKET_SIZE_MIN);
        } catch (IOException | JSONException e) {
            Global.getLogger(CosmiconConfig.class).error("Error loading Cosmicon config", e);
        }
    }
}