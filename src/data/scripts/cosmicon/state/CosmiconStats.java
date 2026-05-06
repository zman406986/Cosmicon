package data.scripts.cosmicon.state;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import data.scripts.cosmicon.battle.CharacterCard;
import data.scripts.cosmicon.battle.CharacterRegistry;
import data.scripts.cosmicon.prismatic.PrismaticDiceRegistry;

import java.util.HashSet;
import java.util.Set;

public class CosmiconStats {

    private static final String KEY_GAMES_PLAYED = "$cos_games_played";
    private static final String KEY_GAMES_WON = "$cos_games_won";
    private static final String KEY_UNLOCKED_CHARACTERS = "$cos_unlocked_characters";
    private static final String KEY_UNLOCKED_PRISMATIC = "$cos_unlocked_prismatic";
    private static final String KEY_PRISMATIC_FEATURE_UNLOCKED = "$cos_prismatic_feature_unlocked";
    private static final String KEY_STARTING_CHAR_GRANTED = "$cos_starting_char_granted";
    private static final String KEY_HAS_GALLERY_CHARACTERS = "$cos_has_gallery_characters";

    private static final int TUTORIAL_GAMES = 2;

    private static MemoryAPI getMemory() {
        return Global.getSector().getPlayerMemoryWithoutUpdate();
    }

    public static int getGamesPlayed() {
        MemoryAPI mem = getMemory();
        if (!mem.contains(KEY_GAMES_PLAYED)) return 0;
        return (int) mem.getFloat(KEY_GAMES_PLAYED);
    }

    public static void incrementGamesPlayed() {
        MemoryAPI mem = getMemory();
        mem.set(KEY_GAMES_PLAYED, getGamesPlayed() + 1);
        if (getGamesPlayed() >= TUTORIAL_GAMES && !isPrismaticFeatureUnlocked()) {
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

    public static void forceCompleteTutorial() {
        MemoryAPI mem = getMemory();
        mem.set(KEY_GAMES_PLAYED, TUTORIAL_GAMES);
        setPrismaticFeatureUnlocked();
        unlockAllPrismaticDice();
        unlockAllCharacters();
    }

    private static void unlockAllCharacters() {
        MemoryAPI mem = getMemory();
        Set<String> unlockedChars = getUnlockedCharacters();
        for (CharacterCard card : CharacterRegistry.getAllCards()) {
            String cardId = card.getId();
            unlockedChars.add(cardId);
        }
        mem.set(KEY_UNLOCKED_CHARACTERS, unlockedChars);
        mem.set(KEY_HAS_GALLERY_CHARACTERS, true);
    }

    private static void unlockAllPrismaticDice() {
        for (String diceId : PrismaticDiceRegistry.getAll().keySet()) {
            unlockPrismaticDice(diceId);
        }
    }

    public static boolean isPrismaticFeatureUnlocked() {
        MemoryAPI mem = getMemory();
        if (!mem.contains(KEY_PRISMATIC_FEATURE_UNLOCKED)) return false;
        return mem.getBoolean(KEY_PRISMATIC_FEATURE_UNLOCKED);
    }

    static void setPrismaticFeatureUnlocked() {
        getMemory().set(KEY_PRISMATIC_FEATURE_UNLOCKED, true);
    }

    @SuppressWarnings("unchecked")
    public static Set<String> getUnlockedCharacters() {
        MemoryAPI mem = getMemory();
        if (!mem.contains(KEY_UNLOCKED_CHARACTERS)) {
            Set<String> empty = new HashSet<>();
            mem.set(KEY_UNLOCKED_CHARACTERS, empty);
            return empty;
        }
        Object obj = mem.get(KEY_UNLOCKED_CHARACTERS);
        if (obj instanceof Set) {
            return (Set<String>) obj;
        }
        return new HashSet<>();
    }

    public static boolean isCharacterUnlocked(String charId) {
        return getUnlockedCharacters().contains(charId);
    }

    public static void unlockCharacter(String charId) {
        getUnlockedCharacters().add(charId);
    }

    public static boolean hasAnyCharacterUnlocked() {
        return !getUnlockedCharacters().isEmpty();
    }

    @SuppressWarnings("unchecked")
    public static Set<String> getUnlockedPrismaticDice() {
        MemoryAPI mem = getMemory();
        if (!mem.contains(KEY_UNLOCKED_PRISMATIC)) {
            Set<String> empty = new HashSet<>();
            mem.set(KEY_UNLOCKED_PRISMATIC, empty);
            return empty;
        }
        Object obj = mem.get(KEY_UNLOCKED_PRISMATIC);
        if (obj instanceof Set) {
            return (Set<String>) obj;
        }
        return new HashSet<>();
    }

    public static boolean isPrismaticDiceUnlocked(String diceId) {
        return getUnlockedPrismaticDice().contains(diceId);
    }

    public static void unlockPrismaticDice(String diceId) {
        getUnlockedPrismaticDice().add(diceId);
    }

    public static boolean isStartingCharGranted() {
        MemoryAPI mem = getMemory();
        if (!mem.contains(KEY_STARTING_CHAR_GRANTED)) return false;
        return mem.getBoolean(KEY_STARTING_CHAR_GRANTED);
    }

    public static void setStartingCharGranted(boolean granted) {
        getMemory().set(KEY_STARTING_CHAR_GRANTED, granted);
    }

    public static void grantStartingCharacter(String charId) {
        if (isStartingCharGranted()) return;
        unlockCharacter(charId);
        setStartingCharGranted(true);
    }

    public static void completeTutorialGame1() {
        unlockCharacter("sparxie");
        CosmiconPlayerState.saveCharacter("sparxie");
    }

    public static void completeTutorialGame2() {
        unlockCharacter("acheron");
        unlockPrismaticDice("repeater");
        if (!isPrismaticFeatureUnlocked()) {
            setPrismaticFeatureUnlocked();
        }
        CosmiconPlayerState.saveCharacter("acheron");
        CosmiconPlayerState.savePrismaticDice("repeater");
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
            if (!isCharacterUnlocked("sparxie")) {
                unlockCharacter("sparxie");
            }
            if (!isCharacterUnlocked("acheron")) {
                unlockCharacter("acheron");
            }
            if (!isPrismaticDiceUnlocked("repeater")) {
                unlockPrismaticDice("repeater");
            }
            if (!isPrismaticFeatureUnlocked()) {
                setPrismaticFeatureUnlocked();
            }
        }
    }
}
