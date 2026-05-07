package data.scripts.cosmicon.state;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;

import java.util.List;

public class CosmiconEventState {

    private static final String KEY_OPPONENT_CHAR = "$cos_temp_opponent_char";
    private static final String KEY_PLAYER_CHAR = "$cos_temp_player_char";
    private static final String KEY_OPPONENT_PRISMATIC = "$cos_temp_opponent_prismatic";
    private static final String KEY_IS_BAR_EVENT = "$cos_temp_is_bar_event";
    private static final String KEY_IS_TUTORIAL = "$cos_temp_is_tutorial";
    private static final String KEY_REPLAY_TUTORIAL = "$cos_replay_tutorial_game";
    private static final String KEY_SESSION_WON = "$cos_temp_session_won";
    private static final String KEY_OPPONENT_USES_TRUE = "$cos_temp_opponent_uses_true";

    private static final List<String> ALL_KEYS = List.of(
        KEY_OPPONENT_CHAR, KEY_PLAYER_CHAR, KEY_OPPONENT_PRISMATIC,
        KEY_IS_BAR_EVENT, KEY_IS_TUTORIAL, KEY_REPLAY_TUTORIAL,
        KEY_OPPONENT_USES_TRUE, KEY_SESSION_WON
    );

    private static MemoryAPI memory;

    private static MemoryAPI getMemory() {
        if (memory == null) {
            memory = Global.getSector().getMemory();
        }
        return memory;
    }

    public static void setOpponentCharacter(String charId) {
        if (charId != null) {
            getMemory().set(KEY_OPPONENT_CHAR, charId);
        } else {
            getMemory().unset(KEY_OPPONENT_CHAR);
        }
    }

    public static String getOpponentCharacter() {
        MemoryAPI mem = getMemory();
        if (!mem.contains(KEY_OPPONENT_CHAR)) return null;
        return mem.getString(KEY_OPPONENT_CHAR);
    }

    public static void setPlayerCharacter(String charId) {
        getMemory().set(KEY_PLAYER_CHAR, charId);
    }

    public static void setOpponentPrismatic(String diceId) {
        getMemory().set(KEY_OPPONENT_PRISMATIC, diceId);
    }

    public static String getOpponentPrismatic() {
        MemoryAPI mem = getMemory();
        if (!mem.contains(KEY_OPPONENT_PRISMATIC)) return null;
        return mem.getString(KEY_OPPONENT_PRISMATIC);
    }

    public static boolean hasOpponentPrismatic() {
        return getMemory().contains(KEY_OPPONENT_PRISMATIC);
    }

    public static void setIsBarEvent(boolean isBarEvent) {
        getMemory().set(KEY_IS_BAR_EVENT, isBarEvent);
    }

    public static boolean isBarEvent() {
        MemoryAPI mem = getMemory();
        if (!mem.contains(KEY_IS_BAR_EVENT)) return false;
        return mem.getBoolean(KEY_IS_BAR_EVENT);
    }

    public static void setIsTutorialMode(boolean isTutorial) {
        getMemory().set(KEY_IS_TUTORIAL, isTutorial);
    }

    public static boolean isTutorialMode() {
        MemoryAPI mem = getMemory();
        if (!mem.contains(KEY_IS_TUTORIAL)) return false;
        return mem.getBoolean(KEY_IS_TUTORIAL);
    }

    public static void clearBattleState() {
        MemoryAPI mem = getMemory();
        mem.unset(KEY_PLAYER_CHAR);
        mem.unset(KEY_REPLAY_TUTORIAL);
    }

    public static void clearAll() {
        MemoryAPI mem = getMemory();
        for (String key : ALL_KEYS) {
            mem.unset(key);
        }
    }

    public static void setReplayTutorialGame(int gameNumber) {
        getMemory().set(KEY_REPLAY_TUTORIAL, gameNumber);
    }

    public static int getReplayTutorialGame() {
        MemoryAPI mem = getMemory();
        if (!mem.contains(KEY_REPLAY_TUTORIAL)) return -1;
        return (int) mem.getFloat(KEY_REPLAY_TUTORIAL);
    }

    public static boolean isReplayTutorial() {
        return getReplayTutorialGame() >= 0;
    }

    public static void setSessionWon(boolean won) {
        getMemory().set(KEY_SESSION_WON, won);
    }

    public static boolean isSessionWon() {
        MemoryAPI mem = getMemory();
        if (!mem.contains(KEY_SESSION_WON)) return false;
        return mem.getBoolean(KEY_SESSION_WON);
    }

    public static void setOpponentUsesTrue(boolean usesTrue) {
        getMemory().set(KEY_OPPONENT_USES_TRUE, usesTrue);
    }

    public static boolean getOpponentUsesTrue() {
        MemoryAPI mem = getMemory();
        if (!mem.contains(KEY_OPPONENT_USES_TRUE)) return false;
        return mem.getBoolean(KEY_OPPONENT_USES_TRUE);
    }
}
