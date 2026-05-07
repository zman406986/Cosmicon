package data.scripts.cosmicon.battle;

import data.scripts.cosmicon.util.CosmiconLogger;

public class TurnState {

    public enum Phase {
        ROLLING,
        SELECTING_ATTACK,
        DICE_DISPLAY_ATTACK,
        SELECTING_DEFENSE,
        DICE_DISPLAY_DEFENSE,
        RESOLVING_PRE_CLASH,
        RESOLVING_MODIFICATION,
        RESOLVING,
        WAITING_NEXT_TURN,
        ENDED
    }

    public enum TurnType {
        ATTACK,
        DEFENSE
    }

    private int turnNumber;
    private boolean playerIsAttacker;
    private Phase currentPhase;
    private boolean isDefenderRolling;
    private int attackValue;
    private int defenseValue;
    private String winner;
    private int playerWeatherAtkMod;
    private int opponentWeatherAtkMod;
    private int playerWeatherDefMod;
    private int opponentWeatherDefMod;
    private String attackerConfirmedSelectionText;
    private String defenderConfirmedSelectionText;

    public TurnState() {
        this.turnNumber = 1;
        this.playerIsAttacker = true;
        this.currentPhase = Phase.WAITING_NEXT_TURN;
        this.isDefenderRolling = false;
        this.attackValue = 0;
        this.defenseValue = 0;
        this.winner = null;
        this.playerWeatherAtkMod = 0;
        this.opponentWeatherAtkMod = 0;
        this.playerWeatherDefMod = 0;
        this.opponentWeatherDefMod = 0;
        this.attackerConfirmedSelectionText = null;
        this.defenderConfirmedSelectionText = null;
    }

    public int getTurnNumber() {
        return turnNumber;
    }

    public void incrementTurnNumber() {
        this.turnNumber++;
    }

    public void resetTurnNumber() {
        this.turnNumber = 1;
    }

    public boolean isPlayerAttacker() {
        return playerIsAttacker;
    }

    public boolean isAttacker(boolean forPlayer) {
        return (forPlayer && playerIsAttacker) || (!forPlayer && !playerIsAttacker);
    }

    public boolean isDefender(boolean forPlayer) {
        return !isAttacker(forPlayer);
    }

    public void setPlayerIsAttacker(boolean playerIsAttacker) {
        this.playerIsAttacker = playerIsAttacker;
    }

    public void swapAttackerDefender() {
        this.playerIsAttacker = !this.playerIsAttacker;
    }

    public Phase getCurrentPhase() {
        return currentPhase;
    }

    public void setCurrentPhase(Phase phase) {
        Phase oldPhase = this.currentPhase;
        this.currentPhase = phase;
        CosmiconLogger.debug("Phase change: %s -> %s", oldPhase, phase);
        if (isMajorPhaseTransition(phase)) {
            CosmiconLogger.info("Phase: %s -> %s", oldPhase, phase);
        }
    }

    private boolean isMajorPhaseTransition(Phase newPhase) {
        return newPhase == Phase.SELECTING_ATTACK ||
               newPhase == Phase.DICE_DISPLAY_ATTACK ||
               newPhase == Phase.SELECTING_DEFENSE ||
               newPhase == Phase.DICE_DISPLAY_DEFENSE ||
               newPhase == Phase.RESOLVING_PRE_CLASH ||
               newPhase == Phase.RESOLVING_MODIFICATION ||
               newPhase == Phase.RESOLVING ||
               newPhase == Phase.WAITING_NEXT_TURN ||
               newPhase == Phase.ENDED;
    }

    public boolean isDefenderRolling() {
        return isDefenderRolling;
    }

    public void setDefenderRolling(boolean isDefenderRolling) {
        this.isDefenderRolling = isDefenderRolling;
    }

    public int getAttackValue() {
        return attackValue;
    }

    public void setAttackValue(int value) {
        this.attackValue = value;
    }

    public void modifyAttackValue(int delta) {
        attackValue += delta;
    }

    public void multiplyAttackValue(int multiplier) {
        attackValue *= multiplier;
    }

    public int getDefenseValue() {
        return defenseValue;
    }

    public void setDefenseValue(int value) {
        this.defenseValue = value;
    }

    public void modifyDefenseValue(int delta) {
        defenseValue += delta;
    }

    public String getWinner() {
        return winner;
    }

    public void setWinner(String winner) {
        this.winner = winner;
    }

    public String getAttackerConfirmedSelectionText() {
        return attackerConfirmedSelectionText;
    }

    public void setAttackerConfirmedSelection(String text) {
        this.attackerConfirmedSelectionText = text;
    }

    public String getDefenderConfirmedSelectionText() {
        return defenderConfirmedSelectionText;
    }

    public void setDefenderConfirmedSelection(String text) {
        this.defenderConfirmedSelectionText = text;
    }

    public void clearConfirmedSelections() {
        attackerConfirmedSelectionText = null;
        defenderConfirmedSelectionText = null;
    }

    public void modifyWeatherAtkMod(boolean forPlayer, int delta) {
        if (forPlayer) {
            playerWeatherAtkMod += delta;
        } else {
            opponentWeatherAtkMod += delta;
        }
    }

    public void modifyWeatherDefMod(boolean forPlayer, int delta) {
        if (forPlayer) {
            playerWeatherDefMod += delta;
        } else {
            opponentWeatherDefMod += delta;
        }
    }

    public void clearWeatherMods() {
        playerWeatherAtkMod = 0;
        opponentWeatherAtkMod = 0;
        playerWeatherDefMod = 0;
        opponentWeatherDefMod = 0;
    }

    public int getPlayerWeatherAtkMod() {
        return playerWeatherAtkMod;
    }

    public int getOpponentWeatherAtkMod() {
        return opponentWeatherAtkMod;
    }

    public int getPlayerWeatherDefMod() {
        return playerWeatherDefMod;
    }

    public int getOpponentWeatherDefMod() {
        return opponentWeatherDefMod;
    }

    public int getEffectiveAtkLevel(CharacterCard card, boolean forPlayer) {
        if (card == null) return 1;
        int mod = forPlayer ? playerWeatherAtkMod : opponentWeatherAtkMod;
        return Math.max(1, Math.min(card.getAtkLevel() + mod, 5));
    }

    public int getEffectiveDefLevel(CharacterCard card, boolean forPlayer) {
        if (card == null) return 1;
        int mod = forPlayer ? playerWeatherDefMod : opponentWeatherDefMod;
        return Math.max(1, Math.min(card.getDefLevel() + mod, 5));
    }

    public void cleanup() {
        CosmiconLogger.debug("TurnState cleanup");
        turnNumber = 1;
        playerIsAttacker = true;
        currentPhase = Phase.WAITING_NEXT_TURN;
        isDefenderRolling = false;
        attackValue = 0;
        defenseValue = 0;
        winner = null;
        playerWeatherAtkMod = 0;
        opponentWeatherAtkMod = 0;
        playerWeatherDefMod = 0;
        opponentWeatherDefMod = 0;
        attackerConfirmedSelectionText = null;
        defenderConfirmedSelectionText = null;
    }
}
