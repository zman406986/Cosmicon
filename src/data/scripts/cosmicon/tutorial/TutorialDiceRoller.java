package data.scripts.cosmicon.tutorial;

import java.util.ArrayList;
import java.util.List;

import data.scripts.cosmicon.battle.BattleState;
import data.scripts.cosmicon.battle.CharacterCard;
import data.scripts.cosmicon.battle.DiceType;
import data.scripts.cosmicon.prismatic.PrismaticDiceInstance;
import data.scripts.cosmicon.prismatic.PrismaticDiceType;
import data.scripts.cosmicon.util.CosmiconLogger;

public class TutorialDiceRoller {

    private final TutorialController controller;
    private int playerRollIndex;
    private int opponentRollIndex;
    private boolean rerollDone;

    private static final int[][] GAME1_PLAYER_ROLLS = {
        {6, 5, 4, 3, 2},
        {4, 4, 3, 2, 1},
        {1, 1, 2, 4, 3}
    };

    private static final int[][] GAME1_OPPONENT_ROLLS = {
        {3, 2, 2, 1, 1},
        {4, 3, 2, 1, 1},
        {3, 2, 2, 1, 1}
    };

    private static final int[] GAME1_REROLL_RESULT = {6, 5, 4, 4, 3};

    private static final int[][] GAME2_PLAYER_ROLLS = {
        {4, 4, 3, 2, 1, 1, 1},
        {4, 3, 2, 2, 1, 1, 1},
        {4, 3, 2, 1, 1, 1, 1},
        {3, 2, 2, 1, 1, 1, 1},
        {4, 3, 2, 1, 1, 1, 1},
        {4, 4, 4, 3, 2, 1, 1}
    };

    private static final int[][] GAME2_OPPONENT_ROLLS = {
        {3, 2, 2, 1, 1},
        {5, 4, 3, 2, 1},
        {5, 4, 3, 2, 1},
        {6, 5, 4, 4, 3},
        {3, 2, 2, 1, 1},
        {3, 2, 2, 1, 1}
    };

    private static final int[] GAME2_REROLL_RESULT = {3, 3, 2, 2, 1};

    private static final int GAME2_PRISMATIC_FACE = 4;
    private static final int GAME2_PRISMATIC_FACE_INDEX = 4;

    public TutorialDiceRoller(TutorialController controller) {
        this.controller = controller;
        this.playerRollIndex = 0;
        this.opponentRollIndex = 0;
        this.rerollDone = false;
    }

    public boolean shouldInterceptPrismaticRoll() {
        return controller.getGame() == TutorialController.TutorialGame.GAME_2_ACHERON;
    }

    public PrismaticDiceInstance getFixedPrismaticRoll(PrismaticDiceType type, boolean isTrueVersion) {
        if (controller.getGame() == TutorialController.TutorialGame.GAME_2_ACHERON
                && "repeater".equals(type.getId()) && !isTrueVersion) {
            PrismaticDiceInstance instance = new PrismaticDiceInstance(
                type, false, GAME2_PRISMATIC_FACE, GAME2_PRISMATIC_FACE_INDEX);
            CosmiconLogger.debug("TutorialDiceRoller: Fixed prismatic roll for %s - face: %d, special: %s",
                type.getId(), instance.rolledFace, instance.isSpecialFace);
            return instance;
        }
        return PrismaticDiceInstance.roll(type, isTrueVersion);
    }

    public boolean shouldInterceptReroll(boolean forPlayer) {
        if (!forPlayer || rerollDone) return false;

        if (controller.getGame() == TutorialController.TutorialGame.GAME_1_SPARXIE) {
            return controller.getCurrentStep() == TutorialController.TutorialStep.G1_T2_ATTACK_REROLL;
        }
        if (controller.getGame() == TutorialController.TutorialGame.GAME_2_ACHERON) {
            return controller.getCurrentStep() == TutorialController.TutorialStep.G2_T3_ATTACK_REROLL;
        }
        return false;
    }

    public void rollForParticipant(BattleState state, boolean forPlayer) {
        if (controller.getGame() == TutorialController.TutorialGame.GAME_1_SPARXIE) {
            rollGame1(state, forPlayer);
        } else {
            rollGame2(state, forPlayer);
        }
    }

    public void rerollSelected(BattleState state, boolean forPlayer) {
        if (rerollDone) return;

        if (controller.getGame() == TutorialController.TutorialGame.GAME_1_SPARXIE
                && controller.getCurrentStep() == TutorialController.TutorialStep.G1_T2_ATTACK_REROLL) {
            rerollGame1Turn3(state, forPlayer);
            rerollDone = true;
        } else if (controller.getGame() == TutorialController.TutorialGame.GAME_2_ACHERON
                && controller.getCurrentStep() == TutorialController.TutorialStep.G2_T3_ATTACK_REROLL) {
            rerollGame2T3(state, forPlayer);
            rerollDone = true;
        }
    }

    private void rollGame1(BattleState state, boolean forPlayer) {
        if (forPlayer) {
            int idx = Math.min(playerRollIndex, GAME1_PLAYER_ROLLS.length - 1);
            setFixedRoll(state, true, GAME1_PLAYER_ROLLS[idx]);
            playerRollIndex++;
        } else {
            int idx = Math.min(opponentRollIndex, GAME1_OPPONENT_ROLLS.length - 1);
            setFixedRoll(state, false, GAME1_OPPONENT_ROLLS[idx]);
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
            setFixedRoll(state, true, GAME2_PLAYER_ROLLS[idx]);
            playerRollIndex++;
        } else {
            int idx = Math.min(opponentRollIndex, GAME2_OPPONENT_ROLLS.length - 1);
            setFixedRoll(state, false, GAME2_OPPONENT_ROLLS[idx]);
            opponentRollIndex++;
        }
    }

    private void rerollGame2T3(BattleState state, boolean forPlayer) {
        List<Integer> values = state.getDiceValues(forPlayer);
        List<Boolean> selected = state.getDiceSelected(forPlayer);
        List<DiceType> types = state.getDiceTypes(forPlayer);
        List<Integer> rerolledIndices = new ArrayList<>();

        int resultIdx = 0;
        for (int i = 0; i < selected.size(); i++) {
            if (selected.get(i) && types.get(i) != DiceType.PRISMATIC) {
                if (resultIdx < GAME2_REROLL_RESULT.length) {
                    values.set(i, GAME2_REROLL_RESULT[resultIdx++]);
                }
                rerolledIndices.add(i);
            }
        }

        state.setDiceValues(forPlayer, values);
        state.decrementRerolls(forPlayer);
        state.incrementRerollsUsed(forPlayer);

        CosmiconLogger.debug("TutorialDiceRoller: Game 2 T3 reroll (non-prismatic) - indices: %s", rerolledIndices);
    }

    private void setFixedRoll(BattleState state, boolean forPlayer, int[] values) {
        CharacterCard card = state.getCard(forPlayer);
        List<DiceType> cardTypes = card.getDicePool();
        List<DiceType> typeList = new ArrayList<>();
        List<Integer> valueList = new ArrayList<>();
        List<Boolean> selectedList = new ArrayList<>();

        for (int i = 0; i < cardTypes.size(); i++) {
            typeList.add(cardTypes.get(i));
            valueList.add(i < values.length ? values[i] : 1);
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
