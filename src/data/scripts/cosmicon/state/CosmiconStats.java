package data.scripts.cosmicon.state;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import data.scripts.CosmiconConfig;
import data.scripts.cosmicon.util.CharacterIds;
import data.scripts.cosmicon.util.CosmiconLogger;
import data.scripts.cosmicon.util.CosmiconRandom;

import java.util.HashSet;
import java.util.Set;

public class CosmiconStats {

    private static final String KEY_GAMES_PLAYED = "$cos_games_played";
    private static final String KEY_GAMES_WON = "$cos_games_won";
    private static final String KEY_UNLOCKED_CHARACTERS = "$cos_unlocked_characters";
    private static final String KEY_UNLOCKED_PRISMATIC = "$cos_unlocked_prismatic";
    private static final String KEY_UNLOCKED_PRISMATIC_TRUE = "$cos_unlocked_prismatic_true";
    private static final String KEY_PRISMATIC_FEATURE_UNLOCKED = "$cos_prismatic_feature_unlocked";
    private static final String KEY_TOURNAMENT_UNLOCKED = "$cos_tournament_unlocked";
    private static final String KEY_GATEKEEPER_999_UNLOCKED = "$cos_gatekeeper_999_unlocked";
    private static final String KEY_LEGEND_TITLE_INHERITED = "$cos_legend_title_inherited";
    private static final String KEY_TUTORIAL_1_COMPLETED = "$cos_tutorial_1_completed";
    private static final String KEY_TUTORIAL_2_COMPLETED = "$cos_tutorial_2_completed";
    private static final String KEY_BONUS_HP_UNLOCKED = "$cos_bonus_hp_unlocked";
    private static final String KEY_BONUS_ATK_UNLOCKED = "$cos_bonus_atk_unlocked";
    private static final String KEY_BONUS_DEF_UNLOCKED = "$cos_bonus_def_unlocked";
    private static final String KEY_DATA_VERSION = "$cos_data_version";
    private static final String KEY_MIGRATED_FROM_PREREWORK = "$cos_migrated_from_prerework";
    private static final String KEY_EASY_MODE_UPDATE_MSG_SHOWN = "$cos_easy_mode_update_msg_shown";

    private static final int CURRENT_DATA_VERSION = 1;
    private static final String REPEATER_ID = "repeater";

    private static MemoryAPI getMemory() {
        return Global.getSector().getPlayerMemoryWithoutUpdate();
    }

    public static int getGamesPlayed() {
        return (int) getMemory().getFloat(KEY_GAMES_PLAYED);
    }

    public static void incrementGamesPlayed() {
        int newCount = getGamesPlayed() + 1;
        getMemory().set(KEY_GAMES_PLAYED, newCount);
    }

    public static int getGamesWon() {
        return (int) getMemory().getFloat(KEY_GAMES_WON);
    }

    public static void incrementGamesWon() {
        MemoryAPI mem = getMemory();
        mem.set(KEY_GAMES_WON, getGamesWon() + 1);
    }

    public static boolean isInTutorialMode() {
        return !getMemory().getBoolean(KEY_TUTORIAL_1_COMPLETED);
    }

    public static int getRemainingTutorialGames() {
        return isTutorial1Completed() ? 0 : 1;
    }

    public static boolean isTutorial1Completed() {
        return getMemory().getBoolean(KEY_TUTORIAL_1_COMPLETED);
    }

    public static boolean isTutorial2Completed() {
        return getMemory().getBoolean(KEY_TUTORIAL_2_COMPLETED);
    }

    public static boolean isInEasyMode() {
        return isTutorial1Completed() && !isTutorial2Completed();
    }

    public static boolean isMigratedFromPrerework() {
        return getMemory().getBoolean(KEY_MIGRATED_FROM_PREREWORK);
    }

    public static void skipTutorial() {
        getMemory().set(KEY_TUTORIAL_1_COMPLETED, true);
    }

    public static void forceCompleteTutorial() {
        completeTutorial1();
        completeTutorial2();
        getMemory().set(KEY_MIGRATED_FROM_PREREWORK, false);
    }

    public static boolean isPrismaticFeatureUnlocked() {
        MemoryAPI mem = getMemory();
        if (!mem.contains(KEY_PRISMATIC_FEATURE_UNLOCKED)) return true;
        return mem.getBoolean(KEY_PRISMATIC_FEATURE_UNLOCKED);
    }

    static void setPrismaticFeatureUnlocked() {
        getMemory().set(KEY_PRISMATIC_FEATURE_UNLOCKED, true);
    }

    @SuppressWarnings("unchecked")
    private static Set<String> getStringSet(String key) {
        MemoryAPI mem = getMemory();
        if (!mem.contains(key)) {
            Set<String> empty = new HashSet<>();
            mem.set(key, empty);
            return empty;
        }
        Object obj = mem.get(key);
        if (obj instanceof Set) {
            return (Set<String>) obj;
        }
        return new HashSet<>();
    }

    public static Set<String> getUnlockedCharacters() {
        return getStringSet(KEY_UNLOCKED_CHARACTERS);
    }

    public static boolean isCharacterUnlocked(String charId) {
        return getUnlockedCharacters().contains(charId);
    }

    public static void unlockCharacter(String charId) {
        getUnlockedCharacters().add(charId);
    }

    public static Set<String> getUnlockedPrismaticDice() {
        return getStringSet(KEY_UNLOCKED_PRISMATIC);
    }

    public static boolean isPrismaticDiceUnlocked(String diceId) {
        return getUnlockedPrismaticDice().contains(diceId);
    }

    public static void unlockPrismaticDice(String diceId) {
        getUnlockedPrismaticDice().add(diceId);
    }

    public static Set<String> getUnlockedPrismaticTrueDice() {
        return getStringSet(KEY_UNLOCKED_PRISMATIC_TRUE);
    }

    public static boolean isPrismaticTrueUnlocked(String diceId) {
        return getUnlockedPrismaticTrueDice().contains(diceId);
    }

    public static void unlockPrismaticTrue(String diceId) {
        getUnlockedPrismaticTrueDice().add(diceId);
    }

    public static void completeTutorial1() {
        getMemory().set(KEY_TUTORIAL_1_COMPLETED, true);
        unlockCharacter(CharacterIds.TUTORIAL_1_DEFAULT_CHARACTER);
        CosmiconPlayerState.saveCharacter(CharacterIds.TUTORIAL_1_DEFAULT_CHARACTER);
    }

    public static void completeTutorial2() {
        getMemory().set(KEY_TUTORIAL_2_COMPLETED, true);
        unlockCharacter(CharacterIds.ACHERON);
        unlockPrismaticDice(REPEATER_ID);
        if (!isPrismaticFeatureUnlocked()) {
            setPrismaticFeatureUnlocked();
        }
        CosmiconPlayerState.saveCharacter(CharacterIds.ACHERON);
        CosmiconPlayerState.savePrismaticDice(REPEATER_ID);
        CosmiconPlayerState.savePrismaticDiceTrueVersion(false);
    }

    public static void completeTutorial2ForMigration() {
        MemoryAPI mem = getMemory();
        mem.set(KEY_TUTORIAL_2_COMPLETED, true);
        mem.set(KEY_MIGRATED_FROM_PREREWORK, false);

        if (!isCharacterUnlocked(CharacterIds.ACHERON)) {
            unlockCharacter(CharacterIds.ACHERON);
        }
        if (!isPrismaticDiceUnlocked(REPEATER_ID)) {
            unlockPrismaticDice(REPEATER_ID);
        }
        if (!isPrismaticFeatureUnlocked()) {
            setPrismaticFeatureUnlocked();
        }
    }

    public static boolean isTournamentUnlocked() {
        return getMemory().getBoolean(KEY_TOURNAMENT_UNLOCKED);
    }

    public static void setTournamentUnlocked(boolean unlocked) {
        getMemory().set(KEY_TOURNAMENT_UNLOCKED, unlocked);
    }

    public static boolean isGatekeeper999Unlocked() {
        return getMemory().getBoolean(KEY_GATEKEEPER_999_UNLOCKED);
    }

    public static void setGatekeeper999Unlocked(boolean unlocked) {
        getMemory().set(KEY_GATEKEEPER_999_UNLOCKED, unlocked);
    }

    public static boolean isLegendTitleInherited() {
        return getMemory().getBoolean(KEY_LEGEND_TITLE_INHERITED);
    }

    public static void setLegendTitleInherited(boolean inherited) {
        getMemory().set(KEY_LEGEND_TITLE_INHERITED, inherited);
    }

    public static int countUnlockedEasyModeCharacters() {
        int count = 0;
        Set<String> unlocked = getUnlockedCharacters();
        for (String id : CharacterIds.EASY_MODE_CHARACTERS) {
            if (unlocked.contains(id)) count++;
        }
        return count;
    }

    public static boolean isEasyModeComplete() {
        return countUnlockedEasyModeCharacters() >= CharacterIds.EASY_MODE_CHARACTERS.size();
    }

    public static String unlockRandomEasyModeCharacter() {
        Set<String> unlocked = getUnlockedCharacters();
        java.util.List<String> unowned = new java.util.ArrayList<>();
        for (String id : CharacterIds.EASY_MODE_CHARACTERS) {
            if (!unlocked.contains(id)) unowned.add(id);
        }
        if (unowned.isEmpty()) return null;
        String chosen = unowned.get(CosmiconRandom.nextInt(unowned.size()));
        unlockCharacter(chosen);
        return chosen;
    }

    public static boolean isHpBonusUnlocked() {
        return getMemory().getBoolean(KEY_BONUS_HP_UNLOCKED);
    }

    public static boolean isAtkBonusUnlocked() {
        return getMemory().getBoolean(KEY_BONUS_ATK_UNLOCKED);
    }

    public static boolean isDefBonusUnlocked() {
        return getMemory().getBoolean(KEY_BONUS_DEF_UNLOCKED);
    }

    public static int getWinsUntilNextBonus() {
        int count = countUnlockedEasyModeCharacters();
        if (isDefBonusUnlocked()) return 0;
        if (isAtkBonusUnlocked()) return 7 - count;
        if (isHpBonusUnlocked()) return 5 - count;
        return 3 - count;
    }

    public static void checkAndUnlockBonuses() {
        int afterTutorial = Math.max(0, countUnlockedEasyModeCharacters() - 1);
        MemoryAPI mem = getMemory();

        if (afterTutorial >= 2 && !isHpBonusUnlocked()) {
            mem.set(KEY_BONUS_HP_UNLOCKED, true);
        }
        if (afterTutorial >= 4 && !isAtkBonusUnlocked()) {
            mem.set(KEY_BONUS_ATK_UNLOCKED, true);
        }
        if (afterTutorial >= 6 && !isDefBonusUnlocked()) {
            mem.set(KEY_BONUS_DEF_UNLOCKED, true);
        }
    }

    public static boolean hasThreeStarCharacters() {
        Set<String> unlocked = getUnlockedCharacters();
        for (String id : unlocked) {
            if (!CharacterIds.EASY_MODE_CHARACTERS.contains(id)) {
                return true;
            }
        }
        return false;
    }

    public static boolean shouldShowEasyModeUpdateMessage() {
        if (getMemory().getBoolean(KEY_EASY_MODE_UPDATE_MSG_SHOWN)) return false;
        return !isEasyModeComplete() && hasThreeStarCharacters();
    }

    public static void setEasyModeUpdateMessageShown() {
        getMemory().set(KEY_EASY_MODE_UPDATE_MSG_SHOWN, true);
    }

    public static int calculateCreditReward(int playerLevel) {
        return CosmiconConfig.BASE_CREDIT_REWARD_PER_LEVEL * Math.max(1, playerLevel);
    }

    public static int calculateNormalEncounterCreditReward(int playerLevel) {
        int base = CosmiconConfig.NORMAL_ENCOUNTER_CREDIT_PER_LEVEL * Math.max(1, playerLevel);
        float factor = CosmiconConfig.CREDIT_RANDOM_FACTOR_MIN + CosmiconRandom.nextFloat() * (CosmiconConfig.CREDIT_RANDOM_FACTOR_MAX - CosmiconConfig.CREDIT_RANDOM_FACTOR_MIN);
        return Math.round(base * factor);
    }

    public static void initialize() {
        MemoryAPI mem = getMemory();

        if (!mem.contains(KEY_GAMES_PLAYED)) {
            mem.set(KEY_GAMES_PLAYED, 0);
        }
        if (!mem.contains(KEY_GAMES_WON)) {
            mem.set(KEY_GAMES_WON, 0);
        }

        int dataVersion = 0;
        if (mem.contains(KEY_DATA_VERSION)) {
            dataVersion = (int) mem.getFloat(KEY_DATA_VERSION);
        }

        if (dataVersion < CURRENT_DATA_VERSION) {
            migrate(dataVersion, mem);
            mem.set(KEY_DATA_VERSION, CURRENT_DATA_VERSION);
        }

        if (isTutorial1Completed()) {
            if (!isCharacterUnlocked(CharacterIds.CHIMERA)) {
                unlockCharacter(CharacterIds.CHIMERA);
            }
        }

        if (isCharacterUnlocked(CharacterIds.TRASHCAN) && !isCharacterUnlocked(CharacterIds.TRASHCAN_2STAR)) {
            unlockCharacter(CharacterIds.TRASHCAN_2STAR);
        }

        if (isTutorial2Completed()) {
            if (!isCharacterUnlocked(CharacterIds.ACHERON)) {
                unlockCharacter(CharacterIds.ACHERON);
            }
            if (!isPrismaticDiceUnlocked(REPEATER_ID)) {
                unlockPrismaticDice(REPEATER_ID);
            }
            if (!isPrismaticFeatureUnlocked()) {
                setPrismaticFeatureUnlocked();
            }
        }

        checkAndUnlockBonuses();
    }

    private static void migrate(int fromVersion, MemoryAPI mem) {
        if (fromVersion < 1) {
            migrateV0toV1(mem);
        }
    }

    private static void migrateV0toV1(MemoryAPI mem) {
        if (getUnlockedCharacters().isEmpty()) {
            return;
        }

        if (mem.contains(KEY_TUTORIAL_1_COMPLETED)) {
            return;
        }

        boolean hasThreeStarChars = false;
        Set<String> unlocked = getUnlockedCharacters();
        for (String id : unlocked) {
            if (!CharacterIds.EASY_MODE_CHARACTERS.contains(id)) {
                hasThreeStarChars = true;
                break;
            }
        }

        if (hasThreeStarChars) {
            mem.set(KEY_MIGRATED_FROM_PREREWORK, true);
            CosmiconLogger.info("Migration V0→V1: Pre-rework save with 3-star characters. "
                + "Player will play Tutorial 1 next encounter.");
        } else {
            completeTutorial1();
            CosmiconLogger.info("Migration V0→V1: Pre-rework save with only 2-star characters. "
                + "Auto-completed Tutorial 1, entering Easy Mode.");
        }
    }
}
