package data.scripts.cosmicon.casino;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class TournamentManager {

    public static final int PLAYER_SLOT = 0;
    public static final int TOTAL_SLOTS = 8;

    public static final int BRACKET_WB = 0;
    public static final int BRACKET_LB = 1;
    public static final int BRACKET_GF = 2;

    private static final int WB_ROUNDS = 3;
    private static final int LB_ROUNDS = 4;
    private static final int[] WB_MATCH_COUNTS = {4, 2, 1};
    private static final int[] LB_MATCH_COUNTS = {2, 2, 1, 1};

    private String[] playerNames;
    private int[][] wbMatchups;
    private int[][] wbResults;
    private int[][] lbMatchups;
    private int[][] lbResults;
    private int[] gfSeries;
    private int gfPlayerWins;
    private int gfOpponentWins;
    private int gfOpponentSlot;

    private int playerWins;
    private int playerLosses;
    private boolean playerInLoserBracket;
    private boolean playerEliminated;
    private boolean playerChampion;
    private int currentBracket;
    private int currentRound;
    private int currentMatchIndex;

    public TournamentManager(List<String> opponentIds) {
        playerNames = new String[TOTAL_SLOTS];
        playerNames[PLAYER_SLOT] = "player";
        for (int i = 1; i < TOTAL_SLOTS; i++) {
            playerNames[i] = opponentIds.get(i - 1);
        }

        wbMatchups = new int[WB_ROUNDS][];
        wbResults = new int[WB_ROUNDS][];
        for (int r = 0; r < WB_ROUNDS; r++) {
            wbMatchups[r] = new int[WB_MATCH_COUNTS[r] * 2];
            wbResults[r] = new int[WB_MATCH_COUNTS[r]];
            for (int m = 0; m < WB_MATCH_COUNTS[r]; m++) {
                wbResults[r][m] = -1;
            }
        }

        lbMatchups = new int[LB_ROUNDS][];
        lbResults = new int[LB_ROUNDS][];
        for (int r = 0; r < LB_ROUNDS; r++) {
            lbMatchups[r] = new int[LB_MATCH_COUNTS[r] * 2];
            lbResults[r] = new int[LB_MATCH_COUNTS[r]];
            for (int m = 0; m < LB_MATCH_COUNTS[r]; m++) {
                lbMatchups[r][m] = -1;
                lbResults[r][m] = -1;
            }
        }

        gfSeries = new int[3];
        for (int i = 0; i < 3; i++) {
            gfSeries[i] = -1;
        }
        gfPlayerWins = 0;
        gfOpponentWins = 0;
        gfOpponentSlot = -1;

        playerWins = 0;
        playerLosses = 0;
        playerInLoserBracket = false;
        playerEliminated = false;
        playerChampion = false;
        currentBracket = BRACKET_WB;
        currentRound = 0;
        currentMatchIndex = 0;

        initializeBracket();
    }

    private TournamentManager() {}

    private void initializeBracket() {
        for (int m = 0; m < 4; m++) {
            wbMatchups[0][m * 2] = m * 2;
            wbMatchups[0][m * 2 + 1] = m * 2 + 1;
        }

        for (int r = 0; r < WB_ROUNDS - 1; r++) {
            for (int m = 0; m < WB_MATCH_COUNTS[r + 1]; m++) {
                wbMatchups[r + 1][m * 2] = -1;
                wbMatchups[r + 1][m * 2 + 1] = -1;
            }
        }

        lbMatchups[0][0] = 1;
        lbMatchups[0][1] = 3;
        lbMatchups[0][2] = 5;
        lbMatchups[0][3] = 7;

        for (int r = 1; r < LB_ROUNDS; r++) {
            for (int m = 0; m < LB_MATCH_COUNTS[r]; m++) {
                lbMatchups[r][m * 2] = -1;
                lbMatchups[r][m * 2 + 1] = -1;
            }
        }
    }

    public static TournamentManager createNew(List<String> opponentIds) {
        return new TournamentManager(opponentIds);
    }

    private int simulateMatch(int slot1, int slot2) {
        return ThreadLocalRandom.current().nextBoolean() ? slot1 : slot2;
    }

    public boolean simulateUpToPlayerMatch() {
        if (playerEliminated || playerChampion) {
            finishTournament();
            return false;
        }

        Integer[] nextMatch = findNextPlayerMatch();
        if (nextMatch == null) {
            if (!playerEliminated && !playerChampion) {
                finishTournament();
            }
            return false;
        }

        int targetBracket = nextMatch[0];
        int targetRound = nextMatch[1];
        int targetMatchIndex = nextMatch[2];

        if (targetBracket == BRACKET_GF && gfSeries[0] < 0) {
            if (lbResults[3][0] >= 0) {
                gfOpponentSlot = lbResults[3][0];
                currentBracket = BRACKET_GF;
                currentRound = 0;
                currentMatchIndex = 0;
            }
            return true;
        }

        for (int r = 0; r < WB_ROUNDS; r++) {
            if (simulateWBRound(r)) return true;
            if (r < WB_ROUNDS - 1) {
                checkAndAdvanceWBRound(r);
            }
        }

        for (int r = 0; r < LB_ROUNDS; r++) {
            if (r == targetRound && targetBracket == BRACKET_LB) break;
            if (simulateLBRound(r)) return true;
            checkAndAdvanceLBRound(r);
        }

        if (targetBracket == BRACKET_GF) {
            if (gfSeries[0] < 0 && lbResults[3][0] >= 0) {
                gfOpponentSlot = lbResults[3][0];
                currentBracket = BRACKET_GF;
                currentRound = 0;
                currentMatchIndex = 0;
            }
        }

        return true;
    }

    private Integer[] findNextPlayerMatch() {
        if (playerEliminated || playerChampion) return null;

        for (int r = 0; r < WB_ROUNDS; r++) {
            for (int m = 0; m < WB_MATCH_COUNTS[r]; m++) {
                int slot0 = wbMatchups[r][m * 2];
                int slot1 = wbMatchups[r][m * 2 + 1];
                if ((slot0 == PLAYER_SLOT || slot1 == PLAYER_SLOT) && wbResults[r][m] < 0) {
                    return new Integer[]{BRACKET_WB, r, m};
                }
            }
        }

        for (int r = 0; r < LB_ROUNDS; r++) {
            for (int m = 0; m < LB_MATCH_COUNTS[r]; m++) {
                int slot0 = lbMatchups[r][m * 2];
                int slot1 = lbMatchups[r][m * 2 + 1];
                if ((slot0 == PLAYER_SLOT || slot1 == PLAYER_SLOT) && lbResults[r][m] < 0) {
                    return new Integer[]{BRACKET_LB, r, m};
                }
            }
        }

        if (currentBracket == BRACKET_GF || playerInLoserBracket && lbResults[3][0] == PLAYER_SLOT) {
            return new Integer[]{BRACKET_GF, 0, 0};
        }

        return null;
    }

    private boolean simulateWBRound(int round) {
        for (int m = 0; m < WB_MATCH_COUNTS[round]; m++) {
            if (wbResults[round][m] >= 0) continue;

            int slot0 = wbMatchups[round][m * 2];
            int slot1 = wbMatchups[round][m * 2 + 1];

            if (slot0 < 0 || slot1 < 0) continue;

            if (slot0 == PLAYER_SLOT || slot1 == PLAYER_SLOT) {
                currentMatchIndex = m;
                currentBracket = BRACKET_WB;
                currentRound = round;
                return true;
            }

            int winner = simulateMatch(slot0, slot1);
            advanceWinner(BRACKET_WB, round, m, winner);
        }
        return false;
    }

    private boolean simulateLBRound(int round) {
        for (int m = 0; m < LB_MATCH_COUNTS[round]; m++) {
            if (lbResults[round][m] >= 0) continue;

            int slot0 = lbMatchups[round][m * 2];
            int slot1 = lbMatchups[round][m * 2 + 1];

            if (slot0 < 0 || slot1 < 0) continue;

            if (slot0 == PLAYER_SLOT || slot1 == PLAYER_SLOT) {
                currentBracket = BRACKET_LB;
                currentRound = round;
                currentMatchIndex = m;
                return true;
            }

            int winner = simulateMatch(slot0, slot1);
            advanceWinner(BRACKET_LB, round, m, winner);
        }
        return false;
    }

    private void checkAndAdvanceWBRound(int round) {
        boolean allDone = true;
        for (int m = 0; m < WB_MATCH_COUNTS[round]; m++) {
            if (wbResults[round][m] < 0) {
                allDone = false;
                break;
            }
        }

        if (allDone && round < WB_ROUNDS - 1) {
            for (int m = 0; m < WB_MATCH_COUNTS[round + 1]; m++) {
                int w1 = wbResults[round][m * 2];
                int w2 = wbResults[round][m * 2 + 1];
                wbMatchups[round + 1][m * 2] = w1;
                wbMatchups[round + 1][m * 2 + 1] = w2;
            }
        }
    }

    private void checkAndAdvanceLBRound(int round) {
        boolean allDone = true;
        for (int m = 0; m < LB_MATCH_COUNTS[round]; m++) {
            if (lbResults[round][m] < 0) {
                allDone = false;
                break;
            }
        }

        if (allDone && round < LB_ROUNDS - 1) {
            if (round == 0) {
                lbMatchups[1][0] = lbResults[0][0];
                lbMatchups[1][2] = lbResults[0][1];
            } else if (round == 1) {
                lbMatchups[2][0] = lbResults[1][0];
                lbMatchups[2][1] = lbResults[1][1];
            } else if (round == 2) {
                lbMatchups[3][0] = lbResults[2][0];
            }
        }
    }

    private void finishTournament() {
        for (int r = 0; r < WB_ROUNDS; r++) {
            for (int m = 0; m < WB_MATCH_COUNTS[r]; m++) {
                if (wbResults[r][m] < 0) {
                    int slot0 = wbMatchups[r][m * 2];
                    int slot1 = wbMatchups[r][m * 2 + 1];
                    if (slot0 >= 0 && slot1 >= 0) {
                        int winner = simulateMatch(slot0, slot1);
                        advanceWinner(BRACKET_WB, r, m, winner);
                    }
                }
            }
        }
        for (int r = 0; r < LB_ROUNDS; r++) {
            for (int m = 0; m < LB_MATCH_COUNTS[r]; m++) {
                if (lbResults[r][m] < 0) {
                    int slot0 = lbMatchups[r][m * 2];
                    int slot1 = lbMatchups[r][m * 2 + 1];
                    if (slot0 >= 0 && slot1 >= 0) {
                        int winner = simulateMatch(slot0, slot1);
                        advanceWinner(BRACKET_LB, r, m, winner);
                    }
                }
            }
        }
        currentBracket = -1;
        currentRound = -1;
        currentMatchIndex = -1;
    }

    public void recordPlayerMatch(boolean playerWon) {
        if (playerEliminated || playerChampion) return;

        int playerSlot = PLAYER_SLOT;
        int opponentSlot = getNextOpponentSlot();

        if (currentBracket == BRACKET_WB) {
            if (playerWon) {
                wbResults[currentRound][currentMatchIndex] = playerSlot;
                advanceWinner(BRACKET_WB, currentRound, currentMatchIndex, playerSlot);
                playerWins++;
            } else {
                wbResults[currentRound][currentMatchIndex] = opponentSlot;
                advanceWinner(BRACKET_WB, currentRound, currentMatchIndex, opponentSlot);
                playerLosses++;
                dropToLoserBracket(currentRound, currentMatchIndex, playerSlot);
            }
        } else if (currentBracket == BRACKET_LB) {
            if (playerWon) {
                lbResults[currentRound][currentMatchIndex] = playerSlot;
                advanceWinner(BRACKET_LB, currentRound, currentMatchIndex, playerSlot);
                playerWins++;
            } else {
                lbResults[currentRound][currentMatchIndex] = opponentSlot;
                advanceWinner(BRACKET_LB, currentRound, currentMatchIndex, opponentSlot);
                playerEliminated = true;
            }
        }

        simulateUpToPlayerMatch();
    }

    public void recordGrandFinalGame(boolean playerWon) {
        if (playerEliminated || playerChampion) return;

        int gameIndex = 0;
        for (int i = 0; i < 3; i++) {
            if (gfSeries[i] < 0) {
                gameIndex = i;
                break;
            }
        }

        if (playerWon) {
            gfSeries[gameIndex] = PLAYER_SLOT;
            gfPlayerWins++;
        } else {
            gfSeries[gameIndex] = gfOpponentSlot;
            gfOpponentWins++;
        }

        if (gfPlayerWins >= 2) {
            playerChampion = true;
        } else if (gfOpponentWins >= 2) {
            playerEliminated = true;
        }
    }

    private void advanceWinner(int bracket, int round, int matchIndex, int winnerSlot) {
        if (bracket == BRACKET_WB) {
            wbResults[round][matchIndex] = winnerSlot;
            if (round < WB_ROUNDS - 1) {
                int nextMatch = matchIndex / 2;
                int nextSlotPos = (matchIndex % 2 == 0) ? nextMatch * 2 : nextMatch * 2 + 1;
                wbMatchups[round + 1][nextSlotPos] = winnerSlot;
            }

            boolean allDone = true;
            for (int m = 0; m < WB_MATCH_COUNTS[round]; m++) {
                if (wbResults[round][m] < 0) {
                    allDone = false;
                    break;
                }
            }

            if (allDone && round < WB_ROUNDS - 1) {
                for (int m = 0; m < WB_MATCH_COUNTS[round + 1]; m++) {
                    int w1 = wbResults[round][m * 2];
                    int w2 = wbResults[round][m * 2 + 1];
                    wbMatchups[round + 1][m * 2] = w1;
                    wbMatchups[round + 1][m * 2 + 1] = w2;
                }
            }

        } else if (bracket == BRACKET_LB) {
            lbResults[round][matchIndex] = winnerSlot;

            boolean allDone = true;
            for (int m = 0; m < LB_MATCH_COUNTS[round]; m++) {
                if (lbResults[round][m] < 0) {
                    allDone = false;
                    break;
                }
            }

            if (allDone && round < LB_ROUNDS - 1) {
                if (round == 0) {
                    lbMatchups[1][0] = lbResults[0][0];
                    lbMatchups[1][2] = lbResults[0][1];
                } else if (round == 1) {
                    lbMatchups[2][0] = lbResults[1][0];
                    lbMatchups[2][1] = lbResults[1][1];
                } else if (round == 2) {
                    lbMatchups[3][0] = lbResults[2][0];
                }
            }
        }
    }

    private void dropToLoserBracket(int wbRound, int wbMatchIndex, int loserSlot) {
        switch (wbRound) {
            case 0 -> {
                int lbSlot = (wbMatchIndex % 2 == 0)
                    ? wbMatchIndex * 2 + 1
                    : (wbMatchIndex - 1) * 2 + 3;
                lbMatchups[0][lbSlot] = loserSlot;
            }
            case 1 -> {
                int lbMatch = wbMatchIndex;
                lbMatchups[1][lbMatch * 2 + 1] = loserSlot;
            }
            case 2 -> {
                lbMatchups[3][1] = loserSlot;
                playerInLoserBracket = true;
            }
        }
    }

    public String getNextOpponentId() {
        int slot = getNextOpponentSlot();
        if (slot < 0 || slot >= TOTAL_SLOTS) return null;
        return playerNames[slot];
    }

    public int getNextOpponentSlot() {
        if (playerEliminated || playerChampion) return -1;

        if (currentBracket == BRACKET_GF) {
            return gfOpponentSlot;
        }

        int[][] matchups = (currentBracket == BRACKET_WB) ? wbMatchups : lbMatchups;
        int slot0 = matchups[currentRound][currentMatchIndex * 2];
        int slot1 = matchups[currentRound][currentMatchIndex * 2 + 1];

        if (slot0 == PLAYER_SLOT) return slot1;
        if (slot1 == PLAYER_SLOT) return slot0;
        return -1;
    }

    public boolean isGrandFinal() {
        return currentBracket == BRACKET_GF;
    }

    public boolean isPlayerEliminated() {
        return playerEliminated;
    }

    public boolean isPlayerChampion() {
        return playerChampion;
    }

    public String getPlayerBracketPosition() {
        if (playerChampion) return "Champion";
        if (playerEliminated) return "Eliminated";

        return switch (currentBracket) {
            case BRACKET_WB -> {
                if (currentRound == 2) yield "WB Final";
                yield "WB Round " + (currentRound + 1);
            }
            case BRACKET_LB -> {
                if (currentRound == 3) yield "LB Final";
                yield "LB Round " + (currentRound + 1);
            }
            case BRACKET_GF -> "Grand Final";
            default -> "Unknown";
        };
    }

    public BracketData getBracketData() {
        BracketData data = new BracketData();
        data.playerNames = playerNames.clone();
        data.wbMatchups = deepCopy2D(wbMatchups);
        data.wbResults = deepCopy2D(wbResults);
        data.lbMatchups = deepCopy2D(lbMatchups);
        data.lbResults = deepCopy2D(lbResults);
        data.gfSeries = gfSeries.clone();
        data.gfPlayerWins = gfPlayerWins;
        data.gfOpponentWins = gfOpponentWins;
        data.playerWins = playerWins;
        data.playerLosses = playerLosses;
        data.currentBracket = currentBracket;
        data.currentRound = currentRound;
        data.currentMatchIndex = currentMatchIndex;
        data.playerInLoserBracket = playerInLoserBracket;
        data.playerEliminated = playerEliminated;
        data.playerChampion = playerChampion;
        return data;
    }

    private static int[][] deepCopy2D(int[][] src) {
        int[][] copy = new int[src.length][];
        for (int i = 0; i < src.length; i++) {
            copy[i] = src[i].clone();
        }
        return copy;
    }

    public String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");

        sb.append("\"playerNames\":[");
        for (int i = 0; i < TOTAL_SLOTS; i++) {
            if (i > 0) sb.append(",");
            sb.append("\"").append(escapeJson(playerNames[i])).append("\"");
        }
        sb.append("],");

        appendIntArray2D(sb, "wbMatchups", wbMatchups);
        sb.append(",");
        appendIntArray2D(sb, "wbResults", wbResults);
        sb.append(",");
        appendIntArray2D(sb, "lbMatchups", lbMatchups);
        sb.append(",");
        appendIntArray2D(sb, "lbResults", lbResults);
        sb.append(",");

        sb.append("\"gfSeries\":[");
        for (int i = 0; i < 3; i++) {
            if (i > 0) sb.append(",");
            sb.append(gfSeries[i]);
        }
        sb.append("],");

        sb.append("\"gfPlayerWins\":").append(gfPlayerWins);
        sb.append(",\"gfOpponentWins\":").append(gfOpponentWins);
        sb.append(",\"gfOpponentSlot\":").append(gfOpponentSlot);
        sb.append(",\"playerWins\":").append(playerWins);
        sb.append(",\"playerLosses\":").append(playerLosses);
        sb.append(",\"playerInLoserBracket\":").append(playerInLoserBracket);
        sb.append(",\"playerEliminated\":").append(playerEliminated);
        sb.append(",\"playerChampion\":").append(playerChampion);
        sb.append(",\"currentBracket\":").append(currentBracket);
        sb.append(",\"currentRound\":").append(currentRound);
        sb.append(",\"currentMatchIndex\":").append(currentMatchIndex);

        sb.append("}");
        return sb.toString();
    }

    public static TournamentManager fromJson(String json) {
        if (json == null || json.isEmpty()) return null;

        try {
            JSONObject obj = new JSONObject(json);
            TournamentManager tm = new TournamentManager();

            JSONArray namesArr = obj.getJSONArray("playerNames");
            tm.playerNames = new String[TOTAL_SLOTS];
            for (int i = 0; i < TOTAL_SLOTS && i < namesArr.length(); i++) {
                tm.playerNames[i] = namesArr.getString(i);
            }

            tm.wbMatchups = parseIntArray2D(obj.getJSONArray("wbMatchups"));
            tm.wbResults = parseIntArray2D(obj.getJSONArray("wbResults"));
            tm.lbMatchups = parseIntArray2D(obj.getJSONArray("lbMatchups"));
            tm.lbResults = parseIntArray2D(obj.getJSONArray("lbResults"));

            JSONArray gfArr = obj.getJSONArray("gfSeries");
            tm.gfSeries = new int[3];
            for (int i = 0; i < 3 && i < gfArr.length(); i++) {
                tm.gfSeries[i] = gfArr.getInt(i);
            }

            tm.gfPlayerWins = obj.getInt("gfPlayerWins");
            tm.gfOpponentWins = obj.getInt("gfOpponentWins");
            tm.gfOpponentSlot = obj.optInt("gfOpponentSlot", -1);
            tm.playerWins = obj.getInt("playerWins");
            tm.playerLosses = obj.getInt("playerLosses");
            tm.playerInLoserBracket = obj.getBoolean("playerInLoserBracket");
            tm.playerEliminated = obj.getBoolean("playerEliminated");
            tm.playerChampion = obj.getBoolean("playerChampion");
            tm.currentBracket = obj.getInt("currentBracket");
            tm.currentRound = obj.getInt("currentRound");
            tm.currentMatchIndex = obj.getInt("currentMatchIndex");

            return tm;
        } catch (JSONException e) {
            return null;
        }
    }

    private static int[][] parseIntArray2D(JSONArray outer) throws JSONException {
        int[][] result = new int[outer.length()][];
        for (int i = 0; i < outer.length(); i++) {
            JSONArray inner = outer.getJSONArray(i);
            result[i] = new int[inner.length()];
            for (int j = 0; j < inner.length(); j++) {
                result[i][j] = inner.getInt(j);
            }
        }
        return result;
    }

    private void appendIntArray2D(StringBuilder sb, String key, int[][] arr) {
        sb.append("\"").append(key).append("\":[");
        for (int i = 0; i < arr.length; i++) {
            if (i > 0) sb.append(",");
            sb.append("[");
            for (int j = 0; j < arr[i].length; j++) {
                if (j > 0) sb.append(",");
                sb.append(arr[i][j]);
            }
            sb.append("]");
        }
        sb.append("]");
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    public static class BracketData {
        public String[] playerNames;
        public int[][] wbMatchups;
        public int[][] wbResults;
        public int[][] lbMatchups;
        public int[][] lbResults;
        public int[] gfSeries;
        public int gfPlayerWins;
        public int gfOpponentWins;
        public int playerWins;
        public int playerLosses;
        public int currentBracket;
        public int currentRound;
        public int currentMatchIndex;
        public boolean playerInLoserBracket;
        public boolean playerEliminated;
        public boolean playerChampion;
    }
}
