package data.scripts.cosmicon.state;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;

import java.util.List;

public class CosmiconEventState {

    private static final String KEY_OPPONENT_CHAR = "$cos_temp_opponent_char";
    private static final String KEY_OPPONENT_PRISMATIC = "$cos_temp_opponent_prismatic";
    private static final String KEY_IS_BAR_EVENT = "$cos_temp_is_bar_event";
    private static final String KEY_IS_STANDALONE_ENTRY = "$cos_temp_is_standalone_entry";
    private static final String KEY_IS_TUTORIAL = "$cos_temp_is_tutorial";
    private static final String KEY_REPLAY_TUTORIAL = "$cos_replay_tutorial_game";
    private static final String KEY_SESSION_WON = "$cos_temp_session_won";
    private static final String KEY_OPPONENT_USES_TRUE = "$cos_temp_opponent_uses_true";

    private static final String KEY_CASINO_BATTLE_MODE = "$cos_casino_battle_mode";
    private static final String KEY_CASINO_BATTLE_IS_BOSS = "$cos_casino_battle_is_boss";
    private static final String KEY_CASINO_BATTLE_OPPONENT = "$cos_casino_battle_opponent";
    private static final String KEY_CASINO_BATTLE_BONUS_HP = "$cos_casino_battle_bonus_hp";
    private static final String KEY_CASINO_BATTLE_USE_TRUE = "$cos_casino_battle_use_true";
    private static final String KEY_CASINO_BATTLE_RESULT_DAMAGE = "$cos_casino_battle_result_damage";
    private static final String KEY_CASINO_BATTLE_OPPONENT_KILLED = "$cos_casino_battle_opponent_killed";
    private static final String KEY_LEGEND_SKIP_ENABLED = "$cos_legend_skip_enabled";
    private static final String KEY_TRASHCAN_HUNTER_LEVEL = "$cos_trashcan_hunter_level";
    private static final String KEY_ORIGINAL_NPC_CHAR = "$cos_original_npc_char";

    private static final String KEY_TOURNAMENT_WINS = "$cos_tournament_wins";
    private static final String KEY_TOURNAMENT_LOSSES = "$cos_tournament_losses";
    private static final String KEY_TOURNAMENT_IN_LOSER_BRACKET = "$cos_tournament_in_loser_bracket";
    private static final String KEY_TOURNAMENT_GRAND_FINAL = "$cos_tournament_grand_final";
    private static final String KEY_TOURNAMENT_SERIES_SCORE = "$cos_tournament_series_score";
    private static final String KEY_TOURNAMENT_BRACKET_DATA = "$cos_tournament_bracket_data";
    private static final String KEY_TOURNAMENT_PENDING_REWARDS = "$cos_tournament_pending_rewards";

    private static final List<String> ALL_KEYS = List.of(
        KEY_OPPONENT_CHAR, KEY_OPPONENT_PRISMATIC,
        KEY_IS_BAR_EVENT, KEY_IS_STANDALONE_ENTRY, KEY_IS_TUTORIAL, KEY_REPLAY_TUTORIAL,
        KEY_OPPONENT_USES_TRUE, KEY_SESSION_WON,
        KEY_CASINO_BATTLE_MODE, KEY_CASINO_BATTLE_IS_BOSS,
        KEY_CASINO_BATTLE_OPPONENT, KEY_CASINO_BATTLE_BONUS_HP,
        KEY_CASINO_BATTLE_USE_TRUE, KEY_CASINO_BATTLE_RESULT_DAMAGE,
        KEY_CASINO_BATTLE_OPPONENT_KILLED, KEY_LEGEND_SKIP_ENABLED,
        KEY_TOURNAMENT_WINS, KEY_TOURNAMENT_LOSSES,
        KEY_TOURNAMENT_IN_LOSER_BRACKET, KEY_TOURNAMENT_GRAND_FINAL,
        KEY_TOURNAMENT_SERIES_SCORE, KEY_TOURNAMENT_BRACKET_DATA,
        KEY_TOURNAMENT_PENDING_REWARDS,
        KEY_ORIGINAL_NPC_CHAR
    );

    private static final List<String> TOURNAMENT_KEYS = List.of(
        KEY_TOURNAMENT_WINS, KEY_TOURNAMENT_LOSSES,
        KEY_TOURNAMENT_IN_LOSER_BRACKET, KEY_TOURNAMENT_GRAND_FINAL,
        KEY_TOURNAMENT_SERIES_SCORE, KEY_TOURNAMENT_BRACKET_DATA,
        KEY_TOURNAMENT_PENDING_REWARDS
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

    public static void setIsEmbeddedEntry(boolean isEmbedded) {
        getMemory().set(KEY_IS_STANDALONE_ENTRY, !isEmbedded);
    }

    public static boolean isEmbeddedEntry() {
        MemoryAPI mem = getMemory();
        if (!mem.contains(KEY_IS_STANDALONE_ENTRY)) return true;
        return !mem.getBoolean(KEY_IS_STANDALONE_ENTRY);
    }

    public static void setSessionWon(boolean won) {
        getMemory().set(KEY_SESSION_WON, won);
    }

    public static boolean isSessionWon() {
        MemoryAPI mem = getMemory();
        if (!mem.contains(KEY_SESSION_WON)) return false;
        return mem.getBoolean(KEY_SESSION_WON);
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
        return mem.getInt(KEY_REPLAY_TUTORIAL);
    }

    public static boolean isReplayTutorial() {
        return getReplayTutorialGame() >= 0;
    }

    public static void setOpponentUsesTrue(boolean usesTrue) {
        getMemory().set(KEY_OPPONENT_USES_TRUE, usesTrue);
    }

    public static boolean getOpponentUsesTrue() {
        MemoryAPI mem = getMemory();
        if (!mem.contains(KEY_OPPONENT_USES_TRUE)) return false;
        return mem.getBoolean(KEY_OPPONENT_USES_TRUE);
    }

    public static void setCasinoBattleMode(boolean enabled) {
        getMemory().set(KEY_CASINO_BATTLE_MODE, enabled);
    }

    public static boolean isCasinoBattleMode() {
        MemoryAPI mem = getMemory();
        if (!mem.contains(KEY_CASINO_BATTLE_MODE)) return false;
        return mem.getBoolean(KEY_CASINO_BATTLE_MODE);
    }

    public static void setCasinoBattleIsBoss(boolean isBoss) {
        getMemory().set(KEY_CASINO_BATTLE_IS_BOSS, isBoss);
    }

    public static boolean isCasinoBattleBoss() {
        MemoryAPI mem = getMemory();
        if (!mem.contains(KEY_CASINO_BATTLE_IS_BOSS)) return false;
        return mem.getBoolean(KEY_CASINO_BATTLE_IS_BOSS);
    }

    public static void setCasinoBattleOpponent(String charId) {
        getMemory().set(KEY_CASINO_BATTLE_OPPONENT, charId);
    }

    public static String getCasinoBattleOpponent() {
        MemoryAPI mem = getMemory();
        if (!mem.contains(KEY_CASINO_BATTLE_OPPONENT)) return null;
        return mem.getString(KEY_CASINO_BATTLE_OPPONENT);
    }

    public static void setCasinoBattleBonusHp(int bonusHp) {
        getMemory().set(KEY_CASINO_BATTLE_BONUS_HP, bonusHp);
    }

    public static int getCasinoBattleBonusHp() {
        MemoryAPI mem = getMemory();
        if (!mem.contains(KEY_CASINO_BATTLE_BONUS_HP)) return 0;
        return mem.getInt(KEY_CASINO_BATTLE_BONUS_HP);
    }

    public static void setCasinoBattleUseTrue(boolean useTrue) {
        getMemory().set(KEY_CASINO_BATTLE_USE_TRUE, useTrue);
    }

    public static boolean getCasinoBattleUseTrue() {
        MemoryAPI mem = getMemory();
        if (!mem.contains(KEY_CASINO_BATTLE_USE_TRUE)) return false;
        return mem.getBoolean(KEY_CASINO_BATTLE_USE_TRUE);
    }

    public static void setCasinoBattleResultDamage(int damage) {
        getMemory().set(KEY_CASINO_BATTLE_RESULT_DAMAGE, damage);
    }

    public static int getCasinoBattleResultDamage() {
        MemoryAPI mem = getMemory();
        if (!mem.contains(KEY_CASINO_BATTLE_RESULT_DAMAGE)) return 0;
        return mem.getInt(KEY_CASINO_BATTLE_RESULT_DAMAGE);
    }

    public static void setCasinoBattleOpponentKilled(boolean killed) {
        getMemory().set(KEY_CASINO_BATTLE_OPPONENT_KILLED, killed);
    }

    public static boolean isCasinoBattleOpponentKilled() {
        MemoryAPI mem = getMemory();
        if (!mem.contains(KEY_CASINO_BATTLE_OPPONENT_KILLED)) return false;
        return mem.getBoolean(KEY_CASINO_BATTLE_OPPONENT_KILLED);
    }

    public static void setLegendSkipEnabled(boolean enabled) {
        getMemory().set(KEY_LEGEND_SKIP_ENABLED, enabled);
    }

    public static boolean isLegendSkipEnabled() {
        MemoryAPI mem = getMemory();
        if (!mem.contains(KEY_LEGEND_SKIP_ENABLED)) return false;
        return mem.getBoolean(KEY_LEGEND_SKIP_ENABLED);
    }

    public static void setTrashcanHunterLevel(int level) {
        getMemory().set(KEY_TRASHCAN_HUNTER_LEVEL, level);
    }

    public static int getTrashcanHunterLevel() {
        MemoryAPI mem = getMemory();
        if (!mem.contains(KEY_TRASHCAN_HUNTER_LEVEL)) return 0;
        return mem.getInt(KEY_TRASHCAN_HUNTER_LEVEL);
    }

    public static void setOriginalNpcCharId(String charId) {
        if (charId != null) {
            getMemory().set(KEY_ORIGINAL_NPC_CHAR, charId);
        } else {
            getMemory().unset(KEY_ORIGINAL_NPC_CHAR);
        }
    }

    public static String getOriginalNpcCharId() {
        MemoryAPI mem = getMemory();
        if (!mem.contains(KEY_ORIGINAL_NPC_CHAR)) return null;
        return mem.getString(KEY_ORIGINAL_NPC_CHAR);
    }

    public static void setTournamentWins(int wins) {
        getMemory().set(KEY_TOURNAMENT_WINS, wins);
    }

    public static int getTournamentWins() {
        MemoryAPI mem = getMemory();
        if (!mem.contains(KEY_TOURNAMENT_WINS)) return 0;
        return mem.getInt(KEY_TOURNAMENT_WINS);
    }

    public static void setTournamentLosses(int losses) {
        getMemory().set(KEY_TOURNAMENT_LOSSES, losses);
    }

    public static int getTournamentLosses() {
        MemoryAPI mem = getMemory();
        if (!mem.contains(KEY_TOURNAMENT_LOSSES)) return 0;
        return mem.getInt(KEY_TOURNAMENT_LOSSES);
    }

    public static void setTournamentInLoserBracket(boolean inLoserBracket) {
        getMemory().set(KEY_TOURNAMENT_IN_LOSER_BRACKET, inLoserBracket);
    }

    public static boolean isTournamentInLoserBracket() {
        MemoryAPI mem = getMemory();
        if (!mem.contains(KEY_TOURNAMENT_IN_LOSER_BRACKET)) return false;
        return mem.getBoolean(KEY_TOURNAMENT_IN_LOSER_BRACKET);
    }

    public static void setTournamentGrandFinal(boolean grandFinal) {
        getMemory().set(KEY_TOURNAMENT_GRAND_FINAL, grandFinal);
    }

    public static boolean isTournamentGrandFinal() {
        MemoryAPI mem = getMemory();
        if (!mem.contains(KEY_TOURNAMENT_GRAND_FINAL)) return false;
        return mem.getBoolean(KEY_TOURNAMENT_GRAND_FINAL);
    }

    public static void setTournamentSeriesScore(String score) {
        if (score != null) {
            getMemory().set(KEY_TOURNAMENT_SERIES_SCORE, score);
        } else {
            getMemory().unset(KEY_TOURNAMENT_SERIES_SCORE);
        }
    }

    public static void setTournamentBracketData(String jsonData) {
        if (jsonData != null) {
            getMemory().set(KEY_TOURNAMENT_BRACKET_DATA, jsonData);
        } else {
            getMemory().unset(KEY_TOURNAMENT_BRACKET_DATA);
        }
    }

    public static String getTournamentBracketData() {
        MemoryAPI mem = getMemory();
        if (!mem.contains(KEY_TOURNAMENT_BRACKET_DATA)) return null;
        return mem.getString(KEY_TOURNAMENT_BRACKET_DATA);
    }

    public static boolean isTournamentActive() {
        return getTournamentBracketData() != null;
    }

    public static void setTournamentPendingRewards(int count) {
        getMemory().set(KEY_TOURNAMENT_PENDING_REWARDS, count);
    }

    public static int getTournamentPendingRewards() {
        MemoryAPI mem = getMemory();
        if (!mem.contains(KEY_TOURNAMENT_PENDING_REWARDS)) return 0;
        return mem.getInt(KEY_TOURNAMENT_PENDING_REWARDS);
    }

    public static void clearTournamentState() {
        MemoryAPI mem = getMemory();
        for (String key : TOURNAMENT_KEYS) {
            mem.unset(key);
        }
    }

    public static void clearTournamentAll() {
        MemoryAPI mem = getMemory();
        for (String key : TOURNAMENT_KEYS) {
            mem.unset(key);
        }
    }

    public static void clearCasinoBattleState() {
        MemoryAPI mem = getMemory();
        mem.unset(KEY_CASINO_BATTLE_MODE);
        mem.unset(KEY_CASINO_BATTLE_IS_BOSS);
        mem.unset(KEY_CASINO_BATTLE_OPPONENT);
        mem.unset(KEY_CASINO_BATTLE_BONUS_HP);
        mem.unset(KEY_CASINO_BATTLE_USE_TRUE);
        mem.unset(KEY_CASINO_BATTLE_RESULT_DAMAGE);
        mem.unset(KEY_CASINO_BATTLE_OPPONENT_KILLED);
        mem.unset(KEY_LEGEND_SKIP_ENABLED);
    }
}
