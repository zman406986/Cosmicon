package data.scripts.cosmicon.state;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import data.scripts.cosmicon.util.CharacterIds;

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

    private static final int TUTORIAL_GAMES = 2;
    private static final String REPEATER_ID = "repeater";

    private static MemoryAPI memory;

    private static MemoryAPI getMemory() {
        if (memory == null) {
            memory = Global.getSector().getPlayerMemoryWithoutUpdate();
        }
        return memory;
    }

    public static int getGamesPlayed() {
        MemoryAPI mem = getMemory();
        if (!mem.contains(KEY_GAMES_PLAYED)) return 0;
        return (int) mem.getFloat(KEY_GAMES_PLAYED);
    }

    public static void incrementGamesPlayed() {
        MemoryAPI mem = getMemory();
        mem.set(KEY_GAMES_PLAYED, getGamesPlayed() + 1);
        if (getGamesPlayed() >= TUTORIAL_GAMES && isPrismaticFeatureUnlocked()) {
            setPrismaticFeatureUnlocked();
        }
    }

    public static int getGamesWon() {
        MemoryAPI mem = getMemory();
        if (!mem.contains(KEY_GAMES_WON)) return 0;
        return (int) mem.getFloat(KEY_GAMES_WON);
    }

    public static void incrementGamesWon() {
        MemoryAPI mem = getMemory();
        mem.set(KEY_GAMES_WON, getGamesWon() + 1);
    }

    public static boolean isInTutorialMode() {
        return getGamesPlayed() < TUTORIAL_GAMES;
    }

    public static int getRemainingTutorialGames() {
        return Math.max(0, TUTORIAL_GAMES - getGamesPlayed());
    }

    public static void skipTutorial() {
        MemoryAPI mem = getMemory();
        mem.set(KEY_GAMES_PLAYED, TUTORIAL_GAMES);
        if (isPrismaticFeatureUnlocked()) {
            setPrismaticFeatureUnlocked();
        }
    }

    public static void forceCompleteTutorial() {
        skipTutorial();
        completeTutorialGame1();
        completeTutorialGame2();
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

    public static void completeTutorialGame1() {
        unlockCharacter(CharacterIds.SPARXIE);
        CosmiconPlayerState.saveCharacter(CharacterIds.SPARXIE);
    }

    public static void completeTutorialGame2() {
        unlockCharacter(CharacterIds.ACHERON);
        unlockPrismaticDice(REPEATER_ID);
        if (isPrismaticFeatureUnlocked()) {
            setPrismaticFeatureUnlocked();
        }
        CosmiconPlayerState.saveCharacter(CharacterIds.ACHERON);
        CosmiconPlayerState.savePrismaticDice(REPEATER_ID);
        CosmiconPlayerState.savePrismaticDiceTrueVersion(false);
    }

    public static boolean isTournamentUnlocked() {
        MemoryAPI mem = getMemory();
        if (!mem.contains(KEY_TOURNAMENT_UNLOCKED)) return false;
        return mem.getBoolean(KEY_TOURNAMENT_UNLOCKED);
    }

    public static void setTournamentUnlocked(boolean unlocked) {
        getMemory().set(KEY_TOURNAMENT_UNLOCKED, unlocked);
    }

    public static boolean isGatekeeper999Unlocked() {
        MemoryAPI mem = getMemory();
        if (!mem.contains(KEY_GATEKEEPER_999_UNLOCKED)) return false;
        return mem.getBoolean(KEY_GATEKEEPER_999_UNLOCKED);
    }

    public static void setGatekeeper999Unlocked(boolean unlocked) {
        getMemory().set(KEY_GATEKEEPER_999_UNLOCKED, unlocked);
    }

    public static int calculateCreditReward(int playerLevel) {
        return 3000 * Math.max(1, playerLevel);
    }

    public static void initialize() {
        MemoryAPI mem = getMemory();

        if (!mem.contains(KEY_GAMES_PLAYED)) {
            mem.set(KEY_GAMES_PLAYED, 0);
        }
        if (!mem.contains(KEY_GAMES_WON)) {
            mem.set(KEY_GAMES_WON, 0);
        }

        if (getGamesPlayed() >= TUTORIAL_GAMES) {
            if (!isCharacterUnlocked(CharacterIds.SPARXIE)) {
                unlockCharacter(CharacterIds.SPARXIE);
            }
            if (!isCharacterUnlocked(CharacterIds.ACHERON)) {
                unlockCharacter(CharacterIds.ACHERON);
            }
            if (!isPrismaticDiceUnlocked(REPEATER_ID)) {
                unlockPrismaticDice(REPEATER_ID);
            }
            if (isPrismaticFeatureUnlocked()) {
                setPrismaticFeatureUnlocked();
            }
        }
    }
}
