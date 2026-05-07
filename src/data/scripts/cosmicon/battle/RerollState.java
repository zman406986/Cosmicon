package data.scripts.cosmicon.battle;

public class RerollState {

    private int playerRemainingRerolls;
    private int opponentRemainingRerolls;
    private int playerRerollsUsedThisTurn;
    private int opponentRerollsUsedThisTurn;

    public RerollState() {
        this.playerRemainingRerolls = 0;
        this.opponentRemainingRerolls = 0;
        this.playerRerollsUsedThisTurn = 0;
        this.opponentRerollsUsedThisTurn = 0;
    }

    public int getRemainingRerolls() {
        return playerRemainingRerolls;
    }

    public int getRerollsUsedThisTurn() {
        return playerRerollsUsedThisTurn;
    }

    public int getRemainingRerolls(boolean forPlayer) {
        return forPlayer ? playerRemainingRerolls : opponentRemainingRerolls;
    }

    public int getRerollsUsedThisTurn(boolean forPlayer) {
        return forPlayer ? playerRerollsUsedThisTurn : opponentRerollsUsedThisTurn;
    }

    public void setRemainingRerolls(boolean isPlayer, int count) {
        if (isPlayer) playerRemainingRerolls = count;
        else opponentRemainingRerolls = count;
    }

    public void decrementRerolls(boolean forPlayer) {
        if (forPlayer) playerRemainingRerolls--;
        else opponentRemainingRerolls--;
    }

    public void incrementRerollsUsed(boolean forPlayer) {
        if (forPlayer) playerRerollsUsedThisTurn++;
        else opponentRerollsUsedThisTurn++;
    }

    public void resetRerollsUsedThisTurn() {
        playerRerollsUsedThisTurn = 0;
        opponentRerollsUsedThisTurn = 0;
    }

    public void cleanup() {
        playerRemainingRerolls = 0;
        opponentRemainingRerolls = 0;
        playerRerollsUsedThisTurn = 0;
        opponentRerollsUsedThisTurn = 0;
    }
}
