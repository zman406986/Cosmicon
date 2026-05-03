package data.scripts.cosmicon.tutorial;

import java.util.ArrayList;
import java.util.List;

import data.scripts.cosmicon.battle.BattleState;
import data.scripts.cosmicon.battle.DiceType;
import data.scripts.cosmicon.util.CosmiconLogger;

public class TutorialDiceRoller {

    private final TutorialController controller;
    private int playerRollIndex;
    private int opponentRollIndex;
    private boolean rerollDone;

    private static final DiceType[] PLAYER_DICE_SPARXIE = {
        DiceType.ORANGE_D8, DiceType.PURPLE_D6, DiceType.PURPLE_D6,
        DiceType.BLUE_D4, DiceType.BLUE_D4, DiceType.PRISMATIC, DiceType.PRISMATIC
    };

    private static final DiceType[] OPPONENT_DICE_TRASHCAN = {
        DiceType.PURPLE_D6, DiceType.PURPLE_D6,
        DiceType.BLUE_D4, DiceType.BLUE_D4, DiceType.BLUE_D4
    };

    private static final DiceType[] PLAYER_DICE_ACHERON = {
        DiceType.ORANGE_D8, DiceType.PURPLE_D6,
        DiceType.BLUE_D4, DiceType.BLUE_D4, DiceType.BLUE_D4,
        DiceType.PRISMATIC, DiceType.PRISMATIC
    };

    private static final DiceType[] OPPONENT_DICE_ROBIN = {
        DiceType.PURPLE_D6, DiceType.PURPLE_D6,
        DiceType.BLUE_D4, DiceType.BLUE_D4, DiceType.BLUE_D4
    };

    private static final int[][] GAME1_PLAYER_ROLLS = {
        {6, 5, 4, 3, 2, 1, 1},
        {4, 4, 3, 2, 1, 1, 1},
        {1, 1, 2, 2, 3, 3, 4}
    };

    private static final int[][] GAME1_OPPONENT_ROLLS = {
        {3, 2, 2, 1, 1},
        {4, 3, 2, 1, 1},
        {3, 2, 2, 1, 1}
    };

    private static final int[] GAME1_REROLL_RESULT = {6, 5, 4, 4, 3, 3, 4};

    private static final int[][] GAME2_PLAYER_ROLLS = {
        {4, 4, 3, 2, 1, 1, 1},
        {4, 4, 3, 2, 1, 1, 1},
        {4, 3, 2, 1, 1, 1, 1},
        {4, 4, 4, 3, 2, 1, 1}
    };

    private static final int[][] GAME2_OPPONENT_ROLLS = {
        {3, 2, 2, 1, 1},
        {5, 4, 3, 2, 1},
        {5, 4, 3, 2, 1},
        {3, 2, 2, 1, 1}
    };

    public TutorialDiceRoller(TutorialController controller) {
        this.controller = controller;
        this.playerRollIndex = 0;
        this.opponentRollIndex = 0;
        this.rerollDone = false;
    }

    public boolean shouldInterceptRoll(BattleState state, boolean forPlayer) {
        return true;
    }

    public boolean shouldInterceptReroll(BattleState state, boolean forPlayer) {
        return forPlayer && !rerollDone
            && controller.getGame() == TutorialController.TutorialGame.GAME_1_SPARXIE;
    }

    public void rollForParticipant(BattleState state, boolean forPlayer) {
        if (controller.getGame() == TutorialController.TutorialGame.GAME_1_SPARXIE) {
            rollGame1(state, forPlayer);
        } else {
            rollGame2(state, forPlayer);
        }
    }

    public void rerollSelected(BattleState state, boolean forPlayer) {
        if (controller.getGame() == TutorialController.TutorialGame.GAME_1_SPARXIE && !rerollDone) {
            rerollGame1Turn3(state, forPlayer);
            rerollDone = true;
        }
    }

    private void rollGame1(BattleState state, boolean forPlayer) {
        if (forPlayer) {
            int idx = Math.min(playerRollIndex, GAME1_PLAYER_ROLLS.length - 1);
            setFixedRoll(state, true, PLAYER_DICE_SPARXIE, GAME1_PLAYER_ROLLS[idx]);
            playerRollIndex++;
        } else {
            int idx = Math.min(opponentRollIndex, GAME1_OPPONENT_ROLLS.length - 1);
            setFixedRoll(state, false, OPPONENT_DICE_TRASHCAN, GAME1_OPPONENT_ROLLS[idx]);
            opponentRollIndex++;
        }
    }

    private void rerollGame1Turn3(BattleState state, boolean forPlayer) {
        List<Integer> values = state.getDiceValues(forPlayer);
        List<Boolean> selected = state.getDiceSelected(forPlayer);
        List<Integer> rerolledIndices = new ArrayList<>();

        int resultIdx = 0;
        for (int i = 0; i < selected.size(); i++) {
            if (selected.get(i)) {
                if (resultIdx < GAME1_REROLL_RESULT.length) {
                    values.set(i, GAME1_REROLL_RESULT[resultIdx++]);
                }
                rerolledIndices.add(i);
            }
        }

        state.setDiceValues(forPlayer, values);
        state.decrementRerolls(forPlayer);
        state.incrementRerollsUsed(forPlayer);

        CosmiconLogger.debug("TutorialDiceRoller: Game 1 Turn 3 reroll - indices: %s", rerolledIndices);
    }

    private void rollGame2(BattleState state, boolean forPlayer) {
        if (forPlayer) {
            int idx = Math.min(playerRollIndex, GAME2_PLAYER_ROLLS.length - 1);
            setFixedRoll(state, true, PLAYER_DICE_ACHERON, GAME2_PLAYER_ROLLS[idx]);
            playerRollIndex++;
        } else {
            int idx = Math.min(opponentRollIndex, GAME2_OPPONENT_ROLLS.length - 1);
            setFixedRoll(state, false, OPPONENT_DICE_ROBIN, GAME2_OPPONENT_ROLLS[idx]);
            opponentRollIndex++;
        }
    }

    private void setFixedRoll(BattleState state, boolean forPlayer, DiceType[] types, int[] values) {
        List<DiceType> typeList = new ArrayList<>();
        List<Integer> valueList = new ArrayList<>();
        List<Boolean> selectedList = new ArrayList<>();

        for (int i = 0; i < types.length; i++) {
            typeList.add(types[i]);
            valueList.add(values[i]);
            selectedList.add(false);
        }

        state.setDiceValues(forPlayer, valueList);
        state.setDiceTypes(forPlayer, typeList);
        state.setDiceSelected(forPlayer, selectedList);

        state.notifyDiceRolled(forPlayer, typeList, valueList);

        CosmiconLogger.debug("TutorialDiceRoller: Fixed roll for %s - values: %s",
            forPlayer ? "Player" : "Opponent", valueList);
    }
}
