package data.scripts.cosmicon.state;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;

public class CosmiconEventState {

    private static final String KEY_OPPONENT_CHAR = "$cos_temp_opponent_char";
    private static final String KEY_OPPONENT_PRISMATIC = "$cos_temp_opponent_prismatic";
    private static final String KEY_IS_BAR_EVENT = "$cos_temp_is_bar_event";
    private static final String KEY_IS_TUTORIAL = "$cos_temp_is_tutorial";

    private static MemoryAPI getMemory() {
        return Global.getSector().getMemory();
    }

    public static void setOpponentCharacter(String charId) {
        getMemory().set(KEY_OPPONENT_CHAR, charId, 0f);
    }

    public static String getOpponentCharacter() {
        MemoryAPI mem = getMemory();
        if (!mem.contains(KEY_OPPONENT_CHAR)) return null;
        return mem.getString(KEY_OPPONENT_CHAR);
    }

    public static void setOpponentPrismatic(String diceId) {
        getMemory().set(KEY_OPPONENT_PRISMATIC, diceId, 0f);
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
        getMemory().set(KEY_IS_BAR_EVENT, isBarEvent, 0f);
    }

    public static boolean isBarEvent() {
        MemoryAPI mem = getMemory();
        if (!mem.contains(KEY_IS_BAR_EVENT)) return false;
        return mem.getBoolean(KEY_IS_BAR_EVENT);
    }

    public static void setIsTutorialMode(boolean isTutorial) {
        getMemory().set(KEY_IS_TUTORIAL, isTutorial, 0f);
    }

    public static boolean isTutorialMode() {
        MemoryAPI mem = getMemory();
        if (!mem.contains(KEY_IS_TUTORIAL)) return false;
        return mem.getBoolean(KEY_IS_TUTORIAL);
    }

    public static void clearAll() {
        MemoryAPI mem = getMemory();
        mem.unset(KEY_OPPONENT_CHAR);
        mem.unset(KEY_OPPONENT_PRISMATIC);
        mem.unset(KEY_IS_BAR_EVENT);
        mem.unset(KEY_IS_TUTORIAL);
    }
}
